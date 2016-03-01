package com.sladematthew.apm;

import java.io.Serializable;
import java.util.regex.Pattern;

public class Log implements Serializable {

	private final Class tag;

	private Log(Class tag) {
		this.tag = tag;
	}

	public static Log getLogger(Class clazz) {
		return new Log(clazz);
	}

	public void debug(String message, Object... properties) {
		Log.debug(tag, message, properties);
	}

	public void error(String message, Throwable t) {
		Log.error(tag, message, t);
	}

	public void info(String message, Object... properties) {
		Log.info(tag, message, properties);
	}

	public void warn(String message, Object... properties) {
		Log.warn(tag, message, properties);
	}

	public void warn(String message, Throwable t) {
		Log.warn(tag, message, t);
	}

	public static void debug(Class tag, String message, Object... properties) {
		if (BuildConfig.DEBUG)
		android.util.Log.d(getTag(tag), String.format(replaceStringFormat(message), properties));
	}

	public static void error(Class tag, String message, Throwable t) {
		if (BuildConfig.DEBUG)
			android.util.Log.e(getTag(tag), message, t);
	}

	public static void info(Class tag, String message, Object... properties) {
		if (BuildConfig.DEBUG)
			android.util.Log.i(getTag(tag), String.format(replaceStringFormat(message), properties));
	}

	public static void warn(Class tag, String message, Object... properties) {
		if (BuildConfig.DEBUG)
			android.util.Log.w(getTag(tag), String.format(replaceStringFormat(message), properties));
	}

	public static void warn(Class tag, String message, Throwable t) {
		if (BuildConfig.DEBUG)
			android.util.Log.w(getTag(tag), message, t);
	}

	private static String replaceStringFormat(String message) {
		return message.replaceAll(Pattern.quote("{}"), "%s");
	}

	private static String getTag(Class clazz) {
		if (clazz == null) {
			return "gyft";
		}
		return clazz.getSimpleName();
	}
}
