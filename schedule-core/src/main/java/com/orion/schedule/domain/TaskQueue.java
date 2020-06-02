package com.orion.schedule.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/24 11:54
 * @Version 1.0.0
 */
public class TaskQueue<T> {

    /**
     *
     */
    List<T> dataList = null;
    private ReentrantLock lock = new ReentrantLock(true);
    /**
     * not full condition
     */
    private Condition notFull = lock.newCondition();
    /**
     * not empty condition
     */
    private Condition notEmpty = lock.newCondition();
    private int size;

    public TaskQueue(int size) {
        this.size = size;
        this.dataList = new ArrayList<>(size);
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, String> set = new ConcurrentHashMap<>();
        AtomicLong atomicLong = new AtomicLong(0);
        TaskQueue<String> queue = new TaskQueue<>(100);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);
        for (int i = 0; i < 4; i++) {
            scheduledExecutorService.schedule((Runnable) () -> {
                Random random = new Random();
                while (true) {
                    try {
                        queue.put(atomicLong.incrementAndGet() + "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }
        for (int i = 0; i < 5; i++) {
            scheduledExecutorService.schedule((Runnable) () -> {
                while (true) {
                    try {
                        String take = queue.take();
                        String s = set.putIfAbsent(take, take);
                        if (s != null) {
                            System.out.println("aa");
                        }
//                        System.out.println(take,);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    public void put(T t) throws Exception {
        checkNotNull(t);
        lock.lockInterruptibly();
        try {
            while (dataList.size() >= size) {
                notFull.await();
            }
            dataList.add(t);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * add to the queue
     *
     * @param t
     * @throws Exception
     */
    public void putFirst(T t) throws Exception {
        checkNotNull(t);
        lock.lockInterruptibly();
        try {
            while (dataList.size() >= size) {
                notFull.await();
            }
            dataList.add(0, t);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * consume
     *
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T take() throws Exception {
        lock.lockInterruptibly();
        try {
            while (dataList.size() == 0) {
                notEmpty.await();
            }
            T remove = (T) dataList.remove(0);
            notFull.signal();
            return remove;
        } finally {
            lock.unlock();
        }
    }

    private void checkNotNull(T e) {
        if (e == null) {
            throw new RuntimeException();
        }
    }

}
