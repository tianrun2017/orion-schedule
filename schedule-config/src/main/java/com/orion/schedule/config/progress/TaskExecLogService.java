package com.orion.schedule.config.progress;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/8 23:19
 * @Version 1.0.0
 */
public interface TaskExecLogService {
    /**
     * update the task run result
     *
     * @param commandId
     * @param success
     * @param fail
     * @return
     * @throws Exception
     */
    public boolean updateRunResult(Long commandId, int success, int fail) throws Exception;

    /**
     * 更新任务取数状态
     *
     * @param commandId
     * @return
     * @throws Exception
     */
    public boolean updateFetchResult(Long commandId, Long total) throws Exception;

    /**
     * @param
     * @throws Exception
     */
    public void updateStopState(Long commandId, Long taskId) throws Exception;

}
