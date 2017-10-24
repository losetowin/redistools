package com.dutycode.opensource.redistools.redislock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dutycode.opensource.redistools.utils.ParseUtils;
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

	/**
	 * 默认超时时间 10s
	 */
	private final static long DEFAULT_TIMEOUT = 10000;

	/**
	 * GETSET VAL中使用的分隔符
	 */
	private final static String SEPARATOR = "_";

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
		// 转换成毫秒
		long millTisLockTime = unit.toMillis(locktime);
		long acquireTimeoutMilltis = unit.toMillis(acquireTimeout);

		// 使用UUID。可替换成其他版本Val。（UUID比较长，内存占用会多）
		String lockVal = UUID.randomUUID().toString();
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
			if (res != null && res.intValue() == 1) {
				logger.info(String.format("%s get the lock successed, lock val=%s", lockkey, lockVal));
				return lockVal;
			} else {
				// 未获取到锁，等待10ms之后，再次尝试获取锁
				try {
					TimeUnit.MILLISECONDS.sleep(100);
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
	 * 释放锁,使用lua脚本保证原子性
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
			// 锁释放成功
			logger.debug(String.format("%s unlock success, unlockval=%s", lockKey, lockVal));
		} else {
			// 锁释放失败
			logger.debug(String.format("%s unlock failed, unlockval=%s", lockKey, lockVal));
		}
	}

	/**
	 * 使用GETSET 和SETNX 来实现分布式锁
	 * 
	 * @Title: tryLock2
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
	 * @date: 2017年10月19日 下午7:19:33
	 * @version: v1.0.0
	 *
	 */
	public static String tryLock2(String lockkey, long locktime, TimeUnit unit, long acquireTimeout) {

		if (unit == null) {
			// 默认是毫秒
			unit = TimeUnit.MILLISECONDS;
		}

		long millLocktime = unit.toMillis(locktime);
		long millAcquireTime = unit.toMillis(acquireTimeout);

		// locktime和acquiretimeout不合法的时候，使用默认的超时时间
		if (millLocktime <= 0) {
			millLocktime = DEFAULT_TIMEOUT;
		}
		if (millAcquireTime <= 0) {
			millAcquireTime = DEFAULT_TIMEOUT;
		}

		long locktimeTimestamp = System.currentTimeMillis() + millLocktime;
		// lockval的格式为： 随机字符串_超时过期时间戳

		String lockVal = String.format("%s" + SEPARATOR + "%s", UUID.randomUUID().toString(), locktimeTimestamp);
		// 获取锁超时时间
		long acquireTimeoutTimestamp = System.currentTimeMillis() + millAcquireTime;

		while (System.currentTimeMillis() < acquireTimeoutTimestamp) {
			// 自旋获取锁，如果获取成功，则跳出直接返回loclVal
			Long lockRes = RedisUtils.setNx(lockkey, lockVal);
			if (lockRes != null && (lockRes.intValue() == 1)) {
				// 获取锁成功
				logger.info(String.format("tryLock2() getlocksuccess, lockkey=%s, lockval=%s", lockkey, lockVal));
				return lockVal;
			}
			// 获取锁不成功，检查val值，是否超时
			String tmpVal = RedisUtils.get(lockkey);
			// 获取时间
			if (StringUtils.isNotBlank(tmpVal) && tmpVal.contains(SEPARATOR)) {
				String[] arr = tmpVal.split(SEPARATOR);
				if (arr != null && arr.length >= 2) {

					// 获取超时时间戳
					long tmpLockTimestamp = ParseUtils.getLong(arr[1], 0);

					// 检测是否超时
					if (System.currentTimeMillis() > tmpLockTimestamp) {
						// 超时，可尝试获取锁
						long relocktimeTimestamp = System.currentTimeMillis() + millLocktime;
						String relockVal = String.format("%s" + SEPARATOR + "%s", UUID.randomUUID().toString(),
								relocktimeTimestamp);

						String relockRes = RedisUtils.getSet(lockkey, relockVal);

						if (tmpVal.equals(relockRes)) {

							logger.info(String
									.format("tryLock2() the orign lock is timecout, reget the lock success, lockKey=%s,lockVal=%s",
											lockkey, relockVal));
							// 获取锁成功，返回数据
							return relockVal;
						}
					}
				}
			}

			// 未获取到锁，自旋等待
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		return null;
	}

	/**
	 * 释放锁，释放来自SETNX方式产生的锁 注：可能存在误释放的情况（释放过程非原子操作），极少可能发生。 误释放场景：
	 * 使用get的时候获取到lockval，此时发生了一次GETSET操作
	 * ，再然后因为lockval相等会删除key值，就相当于误删除了其他的锁。但这个的前提是，当前锁已经超时并被其他线程持有锁。 <br>
	 * 建议使用{@link com.dutycode.opensource.redistools.redislock.RedisLock#unlock(String lockKey, String lockVal) unlock()}方法来释放锁，使用lua脚本可以保证原子性
	 * 
	 * @Title: unlock2
	 * @param lockKey
	 * @param lockVal
	 * @author: zzh
	 * @date: 2017年10月24日 下午5:44:17
	 * @version: v1.0.0
	 *
	 */
	public static void unlock2(String lockKey, String lockVal) {
		if (StringUtils.isBlank(lockKey) || StringUtils.isBlank(lockVal)) {
			return;
		}

		String tmpLockVal = RedisUtils.get(lockKey);

		if (lockVal.equals(tmpLockVal)) {
			logger.info(String.format("unlock2 success, lockkey=%s", lockKey));
			// 相同锁，可以释放 ，BTW：存在中间时间差，可能出现检测正常，但实际上被其他线程持有的情况
			RedisUtils.del(lockKey);
		}

	}
}
