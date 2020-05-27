package bdsm.kelovp.com.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * List 常用方法
 */
@Slf4j
@SuppressWarnings("unused")
public class Lists {

	/**
	 * 单成员列表
	 */
	public static <T> List<T> single(T t) {
		return Collections.singletonList(t);
	}

	/**
	 * 新建ArrayList实例
	 */
	public static <T> ArrayList<T> ofA() {
		return new ArrayList<>();
	}

	/**
	 * 新建ArrayList实例
	 */
	@SafeVarargs
	public static <T> ArrayList<T> ofA(T... ta) {
		return new ArrayList<>(Arrays.asList(ta));
	}

	/**
	 * 新建LinkedList实例
	 */
	public static <T> LinkedList<T> ofL() {
		return new LinkedList<>();
	}

	/**
	 * 新建ArrayList实例
	 */
	@SafeVarargs
	public static <T> LinkedList<T> ofL(T... ta) {
		return new LinkedList<>(Arrays.asList(ta));
	}

	/**
	 * 为空判断
	 */
	public static <T> Boolean isEmpty(List<T> list) {
		return list == null || list.isEmpty();
	}

	/**
	 * 非空判断
	 */
	public static <T> Boolean noEmpty(List<T> list) {
		return !isEmpty(list);
	}

	/**
	 * array 转 list，转换后的list仅限读取，无法进行add、remove等操作
	 */
	public static <T> List<T> toList(T[] t) {
		return Arrays.asList(t);
	}

	/**
	 * array 转 list，转换后的list可支持add、remove等操作
	 */
	public static <T> List<T> toListForUpdate(T[] t) {
		return new ArrayList<>(toList(t));
	}

	/**
	 * list 转 map
	 */
	public static <T, K, V> Map<K, V> listToMap(List<T> list, Function<? super T, ? extends K> key, Function<? super T, ? extends V> value) {
		return list.stream().collect(Collectors.toMap(key, value));
	}

	/**
	 * 分割字符串 - 仅限读取
	 */
	public static List<String> splitForRead(String str, String delimiter) {
		return Strings.isEmpty(str) ? new ArrayList<>() : Arrays.asList(str.split(delimiter));
	}

	/**
	 * 分割字符串 - 可读写
	 */
	public static List<String> split(String str, String delimiter) {
		return Strings.isEmpty(str) ? new ArrayList<>() : toListForUpdate(str.split(delimiter));
	}

	/**
	 * list截取
	 */
	public static <T> List<T> cutList(List<T> list, Integer offset, Integer limit) {
		return list.stream().skip(offset).limit(limit).collect(Collectors.toList());
	}

	/**
	 * list转换
	 */
	public static <T, R> List<R> translate(List<T> list, Function<? super T, ? extends R> function) {
		return list.stream().map(function).collect(Collectors.toList());
	}
}
