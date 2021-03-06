package com.application.base.cache.redisson.redisson.lock;

import com.application.base.cache.redisson.api.RedissonSession;
import com.application.base.cache.redisson.exception.RedissonDistributedLockException;
import com.application.base.cache.redisson.exception.RedissonException;
import com.application.base.cache.redisson.factory.RedissonSessionFactory;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @desc 分布式锁实现.
 * @author 孤狼
 */
public class RedissonDelegateDistributedLock implements RedissonDistributedLock {

    private Logger logger = LoggerFactory.getLogger(getClass());
	
	private RedissonSessionFactory cloudLockFactory;
	
	public RedissonSessionFactory getCloudLockFactory() {
		return cloudLockFactory;
	}
	public void setCloudLockFactory(RedissonSessionFactory cloudLockFactory) {
		this.cloudLockFactory = cloudLockFactory;
	}

    @Override
    public boolean loopLock(String uniqueKey) throws RedissonDistributedLockException, RedissonException {
       return loopLock(uniqueKey,5,TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean loopLock(String uniqueKey,long timeout,TimeUnit unit) throws RedissonDistributedLockException, RedissonException {
        RedissonSession session = null;
        try {
            session = cloudLockFactory.getRedissonSession();
            do {
                logger.debug("lock key: " + uniqueKey);
                RLock rLock = session.getRLock(uniqueKey);
                boolean flag = rLock.isLocked();
                if (!flag){
                    if (unit == null || timeout==0){
                        rLock.lock();
                    }else{
                        rLock.lock(timeout,unit);
                    }
                    return true;
                }
                Thread.sleep(300);
            } while (true);
        } catch (RedissonException e) {
            throw e;
        } catch (Exception e) {
            throw new RedissonDistributedLockException(e);
        }
    }
    

    @Override
    public boolean tryLock(String uniqueKey) throws RedissonDistributedLockException, RedissonException {
        return tryLock(uniqueKey, 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean tryLock(String uniqueKey, long timeout, TimeUnit unit) throws RedissonDistributedLockException, RedissonException {
        RedissonSession session = null;
        try {
            session = cloudLockFactory.getRedissonSession();
            logger.debug("try lock key: " + uniqueKey);
            RLock rLock = session.getRLock(uniqueKey);
            boolean flag = rLock.isLocked();
            if (!flag){
                if (unit == null || timeout==0 ){
                    rLock.tryLock();
                }else{
                    rLock.tryLock(timeout,unit);
                }
                return true;
            }
            return Boolean.FALSE;
        } catch (RedissonException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RedissonDistributedLockException(e);
        }
    }

    @Override
    public boolean unLock(String uniqueKey) throws RedissonDistributedLockException, RedissonException {
        RedissonSession session = null;
        try {
            logger.debug("unlock key: " + uniqueKey);
            session = cloudLockFactory.getRedissonSession();
            if (session!=null){
                session.getRLock(uniqueKey).unlock();
                return true;
            }else{
                return false;
            }
        } catch (RedissonException e) {
           throw e;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new RedissonDistributedLockException(e);
        }
    }
    
    @Override
    public boolean isLock(String uniqueKey) throws RedissonDistributedLockException, RedissonException {
        RedissonSession session = null;
        try {
            logger.debug("judge key : " + uniqueKey);
            session = cloudLockFactory.getRedissonSession();
            if (session!=null){
                return session.getRLock(uniqueKey).isLocked();
            }else{
                return false;
            }
        } catch (RedissonException e) {
            throw e;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new RedissonDistributedLockException(e);
        }
    }
}
