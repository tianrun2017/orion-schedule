package com.orion.schedule.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/6 6:47
 * @Version 1.0.0
 */
public class Netty4Cleaner {

    static AtomicBoolean isCleaning = new AtomicBoolean(false);
    private static Logger logger = LoggerFactory.getLogger(Netty4Cleaner.class);

    public static boolean clean(int second) {
        synchronized (Netty4Cleaner.class) {
            if (isCleaning.get()) {
                return true;
            }
            isCleaning.set(true);
        }
        AtomicInteger integer = new AtomicInteger(second);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            if (integer.get() > 0) {
                logger.info("wait for schedule application clean [{}]", integer.getAndDecrement());
            }
            if (integer.get() == 0) {
                countDownLatch.countDown();
            }
        }, 0, 1, TimeUnit.SECONDS);
        try {
            countDownLatch.await();
        } catch (Throwable e) {
            logger.error("clean exception ", e);
        }
        return true;
    }
}
