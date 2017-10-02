package com.dutycode.opensource.redislock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.dutycode.opensource.redistools.redislock.RedisLock;

public class RedisLockTest {

	@Test
	public void testGetLock(){
		String lockKey = "lock_test_key";
		String val = RedisLock.tryLock(lockKey, 50, TimeUnit.SECONDS, 5);
		System.out.println(val);
		
		for (int i =10; i < 10; i++){
			RedisLock.tryLock(lockKey, 50, TimeUnit.SECONDS, 5);
		}
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
