package com.orion.schedule.context;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/30 9:54
 * @Version 1.0.0
 */
@Data
public class TaskContext {

    private static Logger logger = LoggerFactory.getLogger(TaskContext.class);

    private ConcurrentLinkedQueue<Long> commandIdQueue = new ConcurrentLinkedQueue<>();

    private ReentrantLock reentrantLock = new ReentrantLock(true);
    /**
     * 任务状态,KEY
     */
    private Map<Long, TaskState> taskStateMap = new ConcurrentHashMap<>();

    public boolean validateRepeat(Long commandId, String stepTraceId) {
        reentrantLock.lock();
        try {
            TaskState taskState = taskStateMap.get(commandId);
            if (taskState != null) {
                boolean v = taskState.isRepeat(stepTraceId);
                logger.info("validate repeat {} {} {}", commandId, stepTraceId, v);
                if (!v) {
                    taskState.addTrace(stepTraceId);
                }
                return v;
            } else {
                logger.info("validate repeat not exist {} {} {}", commandId, stepTraceId);
                init(commandId);
                taskState = taskStateMap.get(commandId);
                taskState.addTrace(stepTraceId);
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void stop(Long commandId) {
        reentrantLock.lock();
        try {
            if (!taskStateMap.containsKey(commandId)) {
                TaskState taskState = new TaskState();
                taskStateMap.put(commandId, taskState);
            }
            taskStateMap.get(commandId).stop();
            logger.debug("stop result {} ", taskStateMap.get(commandId).isStoped());
        } finally {
            reentrantLock.unlock();
        }
    }


    /**
     * init task context
     *
     * @param commandId
     */
    public void init(Long commandId) {
        if (taskStateMap.containsKey(commandId)) {
            return;
        }
        reentrantLock.lock();
        try {
            if (taskStateMap.containsKey(commandId)) {
                return;
            }
            taskStateMap.put(commandId, new TaskState());
            commandIdQueue.add(commandId);
            if (commandIdQueue.size() > 50) {
                Long poll = commandIdQueue.poll();
                if (poll != null) {
                    taskStateMap.remove(poll);
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }


    /**
     * 任务是否能运行
     *
     * @param commandId
     * @return
     */
    public boolean stateNormal(Long commandId) {
        TaskState taskState = taskStateMap.get(commandId);
        if (taskState != null) {
            return !taskState.isStoped();
        }
        return true;
    }
}

