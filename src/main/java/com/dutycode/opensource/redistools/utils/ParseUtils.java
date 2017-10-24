package com.dutycode.opensource.redistools.utils;

import org.apache.commons.lang3.StringUtils;

public class ParseUtils {

	public static int getInt(String number, int defaultNum){
		try {
			return Integer.parseInt(number);
		}catch (Exception e){
			return defaultNum;
		}
	}
	
	public static long getLong(String number, long defaultNum){
		try {
			return Long.parseLong(number);
		}catch (Exception e){
			return defaultNum;
		}
	}
	
	
	/**
	 * 获取字符串， 字符串为空时，则替换成默认的字符串。 
	 * @Title: getStringNoEmpty   
	 * @param str
	 * @param defaultStr
	 * @return
	 * @author: zzh    
	 * @date: 2017年9月30日 下午4:12:36       
	 * @version: v1.0.0
	 *
	 */
	public static String getStringNoEmpty(String str, String defaultStr){
		if (StringUtils.isEmpty(str)){
			return defaultStr;
		}else {
			return str;
		}
	}
	
	
	
	public static Boolean getBoolean(String bool, Boolean defaultBool){
		try {
			return Boolean.parseBoolean(bool);
		}catch (Exception e){
			return defaultBool;
		}
	}
}
