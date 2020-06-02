package com.orion.schedule.context;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/2 17:04
 * @Version 1.0.0
 */
public class TaskState {

    /**
     * 0 normal ,1 stop,2 finish
     */
    private AtomicInteger taskState = new AtomicInteger(0);

    /**
     * 追踪ID
     */
    private ConcurrentLinkedQueue<String> traceMap = new ConcurrentLinkedQueue<>();

    public boolean addTrace(String traceId) {
        this.traceMap.add(traceId);
        if (traceMap.size() > 30) {
            traceMap.poll();
        }
        return true;
    }

    public boolean isRepeat(String stepTraceId) {
        return traceMap.contains(stepTraceId);
    }

    public void stop() {
        this.taskState.set(1);
    }

    public boolean isStoped() {
        return this.taskState.get() == 1;
    }

}
