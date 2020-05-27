package bdsm.kelovp.com.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * Map 常用方法
 */
@Slf4j
@SuppressWarnings("unused")
public class Maps {

	/**
	 * 新建HashMap实例
	 */
	public static <K, V> HashMap<K, V> ofH() {
		return new HashMap<>();
	}

	/**
	 * 为空
	 */
	public static <K, V> Boolean isEmpty(Map<K, V> map) {
		return null == map || map.isEmpty();
	}

	/**
	 * 非空
	 */
	public static <K, V> Boolean noEmpty(Map<K, V> map) {
		return !isEmpty(map);
	}

	/**
	 * Stream合并，保留左侧参数值
	 */
	public static <T> BinaryOperator<T> retainLeft() {
		return (left, right) -> left;
	}

	/**
	 * Stream合并，保留右侧参数值
	 */
	public static <T> BinaryOperator<T> retainRight() {
		return (left, right) -> right;
	}
}
