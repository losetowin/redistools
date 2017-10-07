package com.dutycode.opensource.redislock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.dutycode.opensource.redistools.redislock.RedisLock;
import com.dutycode.opensource.redistools.redislock.ReentrantRedisLock;

public class RedisLockTest {

	@Test
	public void testGetLock() {
		String lockKey = "lock_test_key";
		for (int i = 0; i < 10; i++) {
			String tmp = RedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
			System.out.println(tmp);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testReentrantLock() {
		String lockKey = "lock_test_key1";
		String tmp = ReentrantRedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
		System.out.println(tmp);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ReentrantRedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
		ReentrantRedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
		ReentrantRedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
		
		
		System.out.println("释放锁");
		ReentrantRedisLock.unlock(lockKey, tmp);
		ReentrantRedisLock.unlock(lockKey, tmp);
		ReentrantRedisLock.unlock(lockKey, tmp);
		
		for (int i=0; i < 10; i++){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					ReentrantRedisLock.tryLock(lockKey, 10, TimeUnit.SECONDS, 5);
				}
			}).start();
		}
		
		
		ReentrantRedisLock.unlock(lockKey, tmp);
		
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
