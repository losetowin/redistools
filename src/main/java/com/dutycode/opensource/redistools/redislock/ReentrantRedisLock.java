package com.dutycode.opensource.redistools.redislock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis分布式锁, 可重入锁
 * 
 * @Title：ReentrantRedisLock
 * @author: zzh
 * @date: 2017年9月30日 下午4:43:00
 * @version
 *
 */
public class ReentrantRedisLock {

	private static final ThreadLocal<LockInfo> redisLockInfo = new ThreadLocal<ReentrantRedisLock.LockInfo>();
	private static Logger logger = LoggerFactory.getLogger(ReentrantRedisLock.class);

	class LockInfo {
		AtomicInteger lockCount;
		String lockVal;

		
		public LockInfo(AtomicInteger lockCount, String lockVal) {
			super();
			this.lockCount = lockCount;
			this.lockVal = lockVal;
		}

		public AtomicInteger getLockCount() {
			return lockCount;
		}


		public String getLockVal() {
			return lockVal;
		}

	}

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

		LockInfo lockInfo = redisLockInfo.get();
		if (lockInfo != null && StringUtils.isNotEmpty(lockInfo.getLockVal())) {
			logger.info(String.format("current thread :%s hold the lock , lockcount:%s", Thread.currentThread()
					.getName(), lockInfo.getLockCount()));
			// 当前线程已经持有锁，上锁计数+1；
			lockInfo.getLockCount().incrementAndGet();
			return lockInfo.getLockVal();
		}

		// 当前线程未持有锁，申请获得锁。
		String val = RedisLock.tryLock(lockkey, locktime, unit, acquireTimeout);
		if (StringUtils.isNotEmpty(val)){
			//获取到锁，设置到ThreadLocal中
			redisLockInfo.set(new ReentrantRedisLock().new LockInfo(new AtomicInteger(1), val));
		}
		return val;

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
		LockInfo lockInfo = redisLockInfo.get();
		if (lockInfo != null && StringUtils.isNotEmpty(lockInfo.getLockVal())) {
			// 当前线程已经持有锁，释放锁时将计数-1.当加锁次数为1时，则释放Redis锁
			int lockcount = lockInfo.getLockCount().decrementAndGet();
			logger.info(String.format("release lock, current thread :%s , lockcount:%s", Thread.currentThread()
					.getName(), lockInfo.getLockCount()));
			if (lockcount <= 0) {
				logger.info(String.format("current thread :%s unlock redis lock", Thread.currentThread().getName()));
				RedisLock.unlock(lockKey, lockVal);
				//移除掉threadlocal数据，防止内存泄露
				redisLockInfo.remove();
			}
		}

	}
}
