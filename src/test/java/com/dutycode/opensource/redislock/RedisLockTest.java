package com.dutycode.opensource.redislock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.dutycode.opensource.redistools.redislock.RedisLock;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
