package bdsm.kelovp.com.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * 字符串操作
 */
@Slf4j
@SuppressWarnings("unused")
public class Strings {

	public static final String GLOBAL_QUESTION_MARK = "?";
	public static final String GLOBAL_COMMA = ",";
	public static final String GLOBAL_AT = "@";
	public static final String GLOBAL_AND = "&";
	public static final String GLOBAL_SPACE = " ";
	public static final String GLOBAL_EMPTY = "";
	public static final String GLOBAL_UNDERLINE = "_";
	public static final String GLOBAL_PLUS = "+";
	public static final String GLOBAL_MINUS = "-";
	public static final String GLOBAL_ASTERISK = "*";

	/**
	 * 字符串无效
	 */
	public static boolean isEmpty(Object o) {
		return null == o || "".equals(toString(o));
	}

	/**
	 * 字符串有效
	 */
	public static boolean noEmpty(Object o) {
		return !isEmpty(o);
	}

	/**
	 * 字符串相等
	 */
	public static boolean isEqual(Object a, Object b) {
		return toString(a).equals(toString(b));
	}

	/**
	 * 字符串不等
	 */
	public static boolean noEqual(Object a, Object b) {
		return !isEqual(a, b);
	}

	/**
	 * Object转String
	 */
	public static String toString(Object o) {
		return null == o ? "" : o.toString();
	}

	/**
	 * 字符替换
	 */
	public static String replace(String origin, String from, String to) {
		return StringUtils.replace(origin, from, to);
	}

	/**
	 * 计算字符串长度
	 */
	public static int length(String s) {
		return s.getBytes(StandardCharsets.UTF_8).length;
	}

	/**
	 * 判断字符串是否为数字
	 */
	public static boolean isNumeric(String str) {
		return noEmpty(str) && Pattern.compile("^-?\\d+$").matcher(str).matches();
	}

	/**
	 * 按输入顺序，返回第一个非空的字符串
	 */
	public static String choose(String... str) {
		for (String s : str) {
			if (Strings.noEmpty(s)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * 随机字符串（数字+小写字母）
	 */
	public static String randomString(int length) {
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			int j = new Random().nextInt(36);
			j = j <= 9 ? 48 + j : 87 + j;
			chars[i] = (char) j;
		}
		return String.valueOf(chars);
	}


	/**
	 * 除去首尾的 分割符
	 * @param srcStr 需要处理的字符串
	 * @param splitter 包含字符串
	 * @return 处理后字符串
	 */
	public static String trimBothEndsChars(String srcStr, String splitter) {
		String regex = "^" + splitter + "*|" + splitter + "*$";
		return srcStr.replaceAll(regex, "");
	}
}
