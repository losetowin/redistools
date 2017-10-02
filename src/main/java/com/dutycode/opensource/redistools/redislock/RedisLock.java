package com.dutycode.opensource.redistools.redislock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dutycode.opensource.redistools.utils.RedisUtils;

/**
 * Redis分布式锁 Lua脚本实现方式及SETNX和GETSET实现方式
 * 
 * @Title：RedisLock
 * @author: zzh
 * @date: 2017年9月30日 下午4:43:00
 * @version
 *
 */
public class RedisLock {

	// 默认超时时间
	private final static long DEFAULT_TIMEOUT = 10000; // 10S

	private static Logger logger = LoggerFactory.getLogger(RedisLock.class);
	
	
	/**
	 * 获取锁， 未获取到锁，将会自旋等待直到获取到锁或者超时
	 * 
	 * @Title: tryLock
	 * @param lockkey
	 *            锁Key值
	 * @param locktime
	 *            默认10s 锁最长持有时间
	 * @param unit
	 *            时间单位 默认TimeUnit.MILLISECONDS
	 * @param acquireTimeout
	 *            默认10s 申请锁超时时间
	 * @return
	 * @author: zzh
	 * @date: 2017年9月30日 下午4:45:47
	 * @version: v1.0.0
	 *
	 */
	public static String tryLock(String lockkey, long locktime, TimeUnit unit, long acquireTimeout) {
		if (unit == null) {
			unit = TimeUnit.MILLISECONDS;
		}

		long millTisLockTime = unit.toMillis(locktime);// 转换成毫秒
		long acquireTimeoutMilltis = unit.toMillis(acquireTimeout);

		String lockVal = UUID.randomUUID().toString();// 使用UUID。可替换成其他版本Val。
														// （UUID比较长，内存占用会多）
		if (locktime < 0) {
			millTisLockTime = DEFAULT_TIMEOUT;
		}
		if (acquireTimeout < 0) {
			acquireTimeoutMilltis = DEFAULT_TIMEOUT;
		}

		// 获取锁超时时间
		long acquireExpireTimeout = System.currentTimeMillis() + acquireTimeoutMilltis;

		while (System.currentTimeMillis() < acquireExpireTimeout) {
			String luaScript = "\nlocal r = tonumber(redis.call('SETNX', KEYS[1],ARGV[1]));"
					+ "\nredis.call('PEXPIRE',KEYS[1],ARGV[2]);" + "\nreturn r";

			List<String> keys = new ArrayList<String>();
			keys.add(lockkey);
			List<String> args = new ArrayList<String>();
			args.add(lockVal);
			args.add(String.valueOf(millTisLockTime));
			Long res = (Long) RedisUtils.lua(luaScript, keys, args);
			if (res != null && res.intValue() != 1) {
				logger.info(String.format("%s get the lock successed, lock val=%s", lockkey, lockVal));
				return lockVal;
			} else {
				// 未获取到锁，等待10ms之后，再次尝试获取锁
				try {
					TimeUnit.MILLISECONDS.sleep(10);
					logger.info(String.format("%s  waitting get the lock ", lockkey));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		logger.info(String.format("%s get the lock failed", lockkey));
		return null;

	}

	/**
	 * 释放锁
	 * 
	 * @Title: unlock
	 * @param lockKey
	 *            锁ID
	 * @param lockVal
	 *            获取到锁之后拿到的Value值
	 * @author: zzh
	 * @date: 2017年10月2日 下午1:58:11
	 * @version: v1.0.0
	 *
	 */
	public static void unlock(String lockKey, String lockVal) {
		if (StringUtils.isBlank(lockKey) || StringUtils.isBlank(lockVal)) {
			return;
		}
		String luaScipt = "\nlocal v = redis.call('GET', KEYS[1]); \nlocal r= 0; \nif v == ARGV[1] then"
				+ "\nr =redis.call('DEL',KEYS[1]); \nend \nreturn r";

		// 需要使用Lua脚本来保证原子性
		List<String> keys = new ArrayList<String>();
		keys.add(lockKey);
		List<String> args = new ArrayList<String>();
		args.add(lockVal);
		Long res = (Long) RedisUtils.lua(luaScipt, keys, args);

		// 下面这段判断逻辑可以去掉
		if (res != null && res.intValue() == 1) {
			// 锁释放成功 TODO 加日志
			logger.debug(String.format("%s unlock success, unlockval=%s", lockKey, lockVal));
		} else {
			// 锁释放失败 TODO 加日志
			logger.debug(String.format("%s unlock failed, unlockval=%s", lockKey, lockVal));
		}
	}
}
