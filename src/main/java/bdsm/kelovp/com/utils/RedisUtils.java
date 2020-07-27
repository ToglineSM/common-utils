package bdsm.kelovp.com.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author KelovpStrinng
 */
@Slf4j
@Component
public class RedisUtils {

    private static final String LOCK_SCRIPT = "return redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2])";
    private static final String LOCK_SUCCESS = "OK";
    private static final String RELEASE_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final long RELEASE_SUCCESS = 1L;

    // 锁最长等待时间，单位ms
    private static final String MAX_WAIT_LOCK_TIME = "3000";
    // 锁轮询间隔时间，单位ms
    private static final int WAIT_LOCK_PER_CHECK = 200;

    @Resource(name = "orderCountRedisTemplate")
    private StringRedisTemplate orderTemplate;

    public String get(String key){
        return orderTemplate.opsForValue().get(key);
    }

    public List<String> batchGet(List<String> keys){
        return orderTemplate.opsForValue().multiGet(keys);
    }


    public void batchSet(Map<String,String> objs){
        orderTemplate.opsForValue().multiSet(objs);
    }

    public void batchSetPipLineString(Map<String,String> objs){
        orderTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                for (String key : objs.keySet()) {
                    byte[] rawKey = orderTemplate.getStringSerializer().serialize(key);
                    redisConnection.set(rawKey, orderTemplate.getStringSerializer().serialize(objs.get(key)));
                }
                return null;
            }
        });
    }

    public void batchSetPipLineMerge(Map<String,List<String>> objs){
        orderTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                for (String key : objs.keySet()) {
                    byte[] rawKey = orderTemplate.getStringSerializer().serialize(key);
                    String res = orderTemplate.getStringSerializer().deserialize(redisConnection.get(rawKey));
                    Set<String> resSet = new HashSet<>();
                    if (StringUtils.isNotEmpty(res)){
                        List<String> strings = JSON.parseArray(res,String.class);
                        resSet.addAll(strings);
                    }
                    List<String> newStrings = objs.get(key);
                    resSet.addAll(newStrings);
                    String inner = JSON.toJSONString(resSet);
                    redisConnection.set(rawKey, orderTemplate.getStringSerializer().serialize(inner));
                }
                return null;
            }
        });
    }

    public void batchSetIfAbsent(Map<String,String> objs){
        orderTemplate.opsForValue().multiSetIfAbsent(objs);
    }

    public void set(String key,Object o){
        String in = JSON.toJSONString(o);
        log.info("INTO [order-count],KEY:{},VALUE:{}",key,in);
        orderTemplate.opsForValue().set(key, in);
    }

    public void set(String key,String value){
        log.info("INTO [order-count],KEY:{},VALUE:{}",key,value);
        orderTemplate.opsForValue().set(key, value);
    }

    public void setExpiredMinutes(String key,Object o,Long minutes){
        String in = JSON.toJSONString(o);
        log.info("INTO [order-count],KEY:{},EXPIRED:{}min,VALUE:{}",key,minutes,in);
        orderTemplate.opsForValue().set(key,in,minutes, TimeUnit.MINUTES);
    }

    /**
     * 尝试获取分布式锁
     * @param key 锁名称
     * @param identification 身份认证标识
     * @param expireTime 超时时间，ms
     * @return 是否成功获取锁
     */
    private  boolean tryLock(String key, String identification, String expireTime) {
        Object result = orderTemplate.execute(new DefaultRedisScript<>(LOCK_SCRIPT, String.class), Collections.singletonList(key), identification, expireTime);
        return Objects.equals(result, LOCK_SUCCESS);
    }


    private  boolean tryLockLoop(String key, String identification, String expireTime) {
        // 或许会导致死循环？
        while (!tryLock(key, identification, expireTime)) {
            log.info("wait lock {}-{}", key, identification);
            try {
                Thread.sleep(WAIT_LOCK_PER_CHECK);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        return true;
    }

    /**
     * 尝试释放分布式锁
     * @param key 锁名称
     * @param identification 身份认证标识
     * @return 是否成功释放锁
     */
    private  boolean releaseLock(String key, String identification) {
        Object result = orderTemplate.execute(new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class), Collections.singletonList(key), identification);
        return Objects.equals(result,RELEASE_SUCCESS);
    }

    private  <R> R baseLockProcess(boolean isLoop, String key, Function<? super String, ? extends R> successProcess, Function<? super String, ? extends R> failProcess) {
        String identification = UUID.randomUUID().toString().replaceAll("-", "");
        try {
            if (isLoop ? tryLockLoop(key, identification, MAX_WAIT_LOCK_TIME) : tryLock(key, identification, MAX_WAIT_LOCK_TIME)) {
                log.info("success lock {}-{}", key, identification);
                return successProcess.apply(identification);
            } else {
                log.info("fail lock {}-{}", key, identification);
                return failProcess.apply(identification);
            }
        } finally {
            if (releaseLock(key, identification)) {
                log.info("success release lock {}-{}", key, identification);
            } else {
                log.info("fail release lock {}-{}", key, identification);
            }
        }
    }

    /**
     * 通用锁控制流程（等待锁释放）
     * @param key 加锁主键
     * @param successProcess 获取锁成功后执行流程
     * @param failProcess 获取锁失败后执行流程
     * @return 执行流程返回结果
     */
    @SuppressWarnings("UnusedReturnValue")
    public  <R> R processWithWait(String key, Function<? super String, ? extends R> successProcess, Function<? super String, ? extends R> failProcess) {
        return baseLockProcess(true, key, successProcess, failProcess);
    }

    /**
     * 通用锁控制流程（只尝试一次）
     * @param key 加锁主键
     * @param successProcess 获取锁成功后执行流程
     * @param failProcess 获取锁失败后执行流程
     * @return 执行流程返回结果
     */
    @SuppressWarnings("UnusedReturnValue")
    public  <R> R processWithoutWait(String key, Function<? super String, ? extends R> successProcess, Function<? super String, ? extends R> failProcess) {
        return baseLockProcess(false, key, successProcess, failProcess);
    }

}
