package com.dutycode.opensource.redistools.redislock;

import java.util.concurrent.TimeUnit;

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

	/**
	 * 获取锁， 未获取到锁，将会自旋等待直到获取到锁或者超时
	 * 
	 * @Title: tryLock
	 * @param lockkey
	 *            锁Key值
	 * @param locktime
	 *            锁最长持有时间
	 * @param unit
	 *            时间单位 默认TimeUnit.MILLISECONDS
	 * @param acquireTimeout
	 *            申请锁超时时间
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
		long millTisLockTime = unit.toMillis(locktime);// 转换成

	}
}
