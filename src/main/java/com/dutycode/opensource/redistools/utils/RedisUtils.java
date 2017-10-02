package com.dutycode.opensource.redistools.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtils {

	private static JedisPool jedisPool = null;

	static {
		// 初始化Jedis
		initJedisPool();
	}

	private static JedisPool initJedisPool() {
		Properties p = new Properties();
		String configFilePath = "/Users/zzh/Documents/workspace/redistools/src/main/resources/redis.properties";

		InputStream in = null;
		try {
			// 读取配置文件，初始化默认数据
			in = new BufferedInputStream(new FileInputStream(configFilePath));
			p.load(in);

			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxIdle(ParseUtils.getInt(p.getProperty("jedis.config.maxidle"), 1000));
			config.setMaxTotal(ParseUtils.getInt(p.getProperty("jedis.config.maxtotal"), 1000));
			config.setMaxWaitMillis(ParseUtils.getInt(p.getProperty("jedis.config.maxwaitmillis"), 1000));
			config.setMinIdle(ParseUtils.getInt(p.getProperty("jedis.config.minidle"), 1));
			config.setTestOnBorrow(ParseUtils.getBoolean(p.getProperty("jedis.config.testonborrow"), false));

			jedisPool = new JedisPool(config, p.getProperty("jedis.config.host", "127.0.0.1"), ParseUtils.getInt(
					p.getProperty("jedis.config.port"), 6379));

			return jedisPool;

		} catch (Exception e) {
			// TODO 日志
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return jedisPool;

	}

	public static JedisPool getJedisPool() {
		if (jedisPool != null) {
			return jedisPool;
		} else {
			return initJedisPool();
		}
	}

	public static Jedis getJedis() {
		if (jedisPool != null) {
			return jedisPool.getResource();
		} else {
			return null;
		}
	}

	public static void returnJedis(Jedis jedis) {
		if (jedis != null) {
			jedis.close();
		}
	}

	public static String get(String key) {
		Jedis jedis = getJedis();
		try {
			if (jedis != null) {
				return jedis.get(key);
			}
		} finally {
			returnJedis(jedis);
		}
		return null;
	}

	public static void set(String key, String val) {
		Jedis jedis = getJedis();
		try {
			if (jedis != null) {
				jedis.set(key, val);
			}
		} finally {
			returnJedis(jedis);
		}
	}

	public static Object lua(String luaScript, List<String> keys, List<String> args) {
		Jedis jedis = getJedis();
		try {
			if (jedis != null) {
				return jedis.eval(luaScript, keys, args);
			}
		} finally {
			returnJedis(jedis);
		}
		return null;
	}
}
