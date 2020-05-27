package bdsm.kelovp.com.utils;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * 对象方法抽取
 */
@Slf4j
@SuppressWarnings("unused")
public class Objects {

	/**
	 * 判断是否为空
	 */
	public static Boolean isNull(Object o) {
		return null == o;
	}

	/**
	 * 判断是否不为空
	 */
	public static Boolean nonNull(Object o) {
		return null != o;
	}

	/**
	 * 相等
	 */
	public static <T> Boolean eq(T a, T b) {
		return java.util.Objects.equals(a, b);
	}

	/**
	 * 不相等
	 */
	public static <T> Boolean noEq(T a, T b) {
		return !eq(a, b);
	}

	/**
	 * 处于枚举范围内
	 */
	public static Boolean in(Object origin, Object... collections) {
		return Arrays.stream(collections).anyMatch(a -> eq(a, origin));
	}

	/**
	 * 处于枚举范围内，biPredicate进行过滤
	 */
	@SafeVarargs
	public static <T> Boolean in(T t, BiPredicate<? super T, ? super T> biPredicate, T... collections) {
		return Arrays.stream(collections).anyMatch(a -> biPredicate.test(t, a));
	}

	/**
	 * 不在枚举范围内
	 */
	public static Boolean notIn(Object origin, Object... collections) {
		return Arrays.stream(collections).noneMatch(a -> eq(a, origin));
	}

	/**
	 * 处于枚举范围内，biPredicate进行过滤
	 */
	@SafeVarargs
	public static <T> Boolean notIn(T t, BiPredicate<? super T, ? super T> biPredicate, T... collections) {
		return Arrays.stream(collections).noneMatch(a -> biPredicate.test(t, a));
	}

	/**
	 * 为空的默认值
	 */
	public static <T> T nullElse(T t, T defaultValue) {
		return null == t ? defaultValue : t;
	}


	/**
	 * 按输入顺序，返回第一个可用的对象
	 */
	@SafeVarargs
	public static <O> O choose(O... all) {
		for (O o : all) {
			if (nonNull(o)) {
				return o;
			}
		}
		return null;
	}

	/**
	 * 按输入顺序，返回第一个可用的对象
	 */
	@SafeVarargs
	public static <O> O choose(Supplier<O>... all) {
		O o;
		for (Supplier<O> oSupplier : all) {
			o = oSupplier.get();
			if (nonNull(o)) {
				return o;
			}
		}
		return null;
	}

}
