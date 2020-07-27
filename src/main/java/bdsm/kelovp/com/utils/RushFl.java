package bdsm.kelovp.com.utils;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author KelovpString
 */
@Slf4j
public class RushFl {


    private static AtomicInteger threadId = new AtomicInteger();

    private static Integer size = 12;

    private static Map<String,String> rs = new HashMap<>();

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(size, size,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            int id = threadId.getAndIncrement();
            String name = "work-" + id;
            rs.put(name,urls[id]);
            return new Thread(r,name);
        }
    });

    private static String[] urls = {
            "http://127.0.0.1:23030/services/sycn/sycnOrder",
    };


    public static void main(String[] args) {
        System.out.println();
    }

    @SneakyThrows
    private void produce(){

        // 01-10 00:00:00 2020-07-10 00:00:00
        // 2019-03-01 00:00:00
        Date begin = DateUtil.stringToDate("2020-05-25 00:00:00",DateUtil.COMMON_TIME_FORMAT);
        Date end = DateUtil.stringToDate("2020-06-01 00:00:00",DateUtil.COMMON_TIME_FORMAT);



        while (begin.before(end)){
            int rx = 0;
            Date endTime = DateUtil.dateIncreaseByHour(begin, 2);

//            if (isTimeRange(begin)) {
//                endTime = DateUtil.dateIncreaseByHour(begin, 2);
//            }

            if (endTime.after(end)){
                endTime = end;
                rx = 1;
            }
            Date finalBegin = begin;
            Date finalEndTime = endTime;
            CompletableFuture.runAsync(() ->{
                String beginStr = DateUtil.dateToString(finalBegin,"yyyy/MM/dd HH:mm:ss");
                String endStr = DateUtil.dateToString(finalEndTime,"yyyy/MM/dd HH:mm:ss");
                Thread thread = Thread.currentThread();
                try {
                    Boolean e = post(rs.get(thread.getName()),beginStr,endStr,thread);
                    if (!e){
                        throw new RuntimeException("Ex");
                    }
                }catch (Exception e){
                    System.out.println("线程["+thread.getName() + "]"+"[{"+beginStr +"}]-[{"+endStr+"}],refresh Ex");
                }
            },executor);
            if (rx > 0){
                break;
            }
            begin = endTime;
//            // 给ES 一条活路
            TimeUnit.SECONDS.sleep(5);

        }
    }

    private static boolean isTimeRange(Date now){
        String str = DateUtil.dateToString(now,DateUtil.ISO_EXPANDED_DATE_FORMAT);

        return  now.before( DateUtil.stringToDateWithTime(str+ " 07:00:00"));
    }


    private static Boolean post(String url, String beginStr, String endStr,Thread thread){
        System.out.println("["+ DateUtil.getCurrentDateString(DateUtil.DATETIME_PATTERN)+"]线程["+ thread.getName() +"]"+ "正在调用：["+url +"],参数["+beginStr +"]-[" + endStr +"]");
        Map<String,Object> param = new HashMap<>();
        param.put("begin",beginStr);
        param.put("end",endStr);
        String res = HttpClients.POST(url,param,false);
        System.out.println("["+ DateUtil.getCurrentDateString(DateUtil.DATETIME_PATTERN)+"]线程["+ thread.getName() +"]结果是[" + res +"]");
        return JSON.parseObject(res).getBoolean("result");
    }
}

