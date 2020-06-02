package com.orion.schedule.config.progress;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/17 21:24
 * @Version 1.0.0
 */
public interface ProgressListener {
    /**
     * notify process updaet
     *
     * @param processMsgDto
     */
    public void notifyProgressUpdate(ProcessMsgDto processMsgDto);
}
