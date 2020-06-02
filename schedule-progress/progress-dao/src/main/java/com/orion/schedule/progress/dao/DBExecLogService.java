package com.orion.schedule.progress.dao;

import com.orion.schedule.config.progress.TaskExecLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/8 23:19
 * @Version 1.0.0
 */
public class DBExecLogService implements TaskExecLogService {

    @Autowired
    SchedulePersistent schedulePersistent;
    private Logger logger = LoggerFactory.getLogger(DBExecLogService.class);

    /**
     * update the task's total data to process
     *
     * @param commandId
     * @param batchSize
     * @return
     * @throws Exception
     */
    @Deprecated
    public boolean addBatchToTotal(Long commandId, int batchSize) throws Exception {
        Connection connection = schedulePersistent.getDruidDataSource().getConnection();
        try {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("update task_log set total=total+?,update_time=now() where command_id=?");
            preparedStatement.setInt(1, batchSize);
            preparedStatement.setLong(2, commandId);
            int i = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.commit();
            return i > 0;
        } catch (Throwable e) {
            logger.error("add Batch to total exception ", e);
            connection.rollback();
        } finally {
            connection.close();
        }
        return false;
    }

    /**
     * update the task run result
     *
     * @param commandId
     * @param success
     * @param fail
     * @return
     * @throws Exception
     */
    @Deprecated
    public boolean addBatchResult(Long commandId, int success, int fail) throws Exception {
        Connection connection = schedulePersistent.getDruidDataSource().getConnection();
        try {
            doUpdateRun(connection, commandId, success, fail);
            connection.commit();
        } catch (Throwable e) {
            logger.error("addBatchResult exception ", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    /**
     * update the task run result
     *
     * @param commandId
     * @param success
     * @param fail
     * @return
     * @throws Exception
     */
    public boolean updateRunResult(Long commandId, int success, int fail) throws Exception {
        Connection connection = schedulePersistent.getDruidDataSource().getConnection();
        try {
            doUpdateRun(connection, commandId, success, fail);
            doCleanRunState(connection, commandId);
            connection.commit();
        } catch (Throwable e) {
            logger.error("updateRunResult exception ", e);
            connection.rollback();
            return false;
        } finally {
            connection.close();
        }
        return true;
    }

    /**
     * clean the run state
     *
     * @param commandId
     * @throws SQLException
     */
    private void doCleanRunState(Connection connection, Long commandId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "select total,success,fail from task_log where fetch_finish=1 and command_id=?");
        preparedStatement.setLong(1, commandId);
        ResultSet resultSet = preparedStatement.executeQuery();
        boolean isFinish = false;
        if (resultSet.next()) {
            long total = resultSet.getLong(1);
            long success = resultSet.getLong(2);
            long fail = resultSet.getLong(3);
            logger.info("batch state is [{}] {} {} {}", commandId, total, success, fail);
            if (total <= success + fail) {
                //all data process success,update task state
                isFinish = true;
            }
        }
        resultSet.close();
        preparedStatement.close();

        if (isFinish) {
            logger.info("the task command [{}] run finish ", commandId);
            preparedStatement = connection.prepareStatement(
                    "update task_log set end_time =now() ,is_finished=1,update_time=now() where command_id=? ");
            preparedStatement.setLong(1, commandId);
            preparedStatement.executeUpdate();
            preparedStatement.close();

            preparedStatement = connection
                    .prepareStatement("update task_command set state=?,update_time=now() where id=? and state='RUN' ");
            preparedStatement.setString(1, "FINISH");
            preparedStatement.setLong(2, commandId);
            preparedStatement.executeUpdate();
            preparedStatement.close();

            preparedStatement = connection
                    .prepareStatement("update task_schedule set running_cmd_id=0,update_time=now() where running_cmd_id=?");
            preparedStatement.setLong(1, commandId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    /**
     * update run result
     *
     * @param connection connection info
     * @param commandId
     * @param success
     * @param fail
     * @return
     * @throws SQLException
     */
    private void doUpdateRun(Connection connection, Long commandId, int success, int fail) throws SQLException {
        //update state
        PreparedStatement preparedStatement = connection.prepareStatement(
                "update task_log set success=success + ?,fail=fail + ? ,update_time=now() where command_id=?");
        preparedStatement.setInt(1, success);
        preparedStatement.setInt(2, fail);
        preparedStatement.setLong(3, commandId);
        int i = preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    /**
     * 更新任务取数状态
     *
     * @param commandId
     * @return
     * @throws Exception
     */
    public boolean updateFetchResult(Long commandId, Long total) throws Exception {
        Connection connection = schedulePersistent.getDruidDataSource().getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "update task_log set fetch_finish=1,update_time=now(),total=? where  command_id=? ");
            preparedStatement.setLong(1, total);
            preparedStatement.setLong(2, commandId);
            int i = preparedStatement.executeUpdate();
            preparedStatement.close();
            doCleanRunState(connection, commandId);
            connection.commit();
            return i > 0;
        } catch (Throwable e) {
            logger.error("update FetchResult error ", e);
            connection.rollback();
        } finally {
            connection.close();
        }
        return false;
    }

    public void updateStopState(Long commandId, Long taskId) throws Exception {
        updateState("FINISH", commandId, taskId);
    }

    /**
     * update task state
     *
     * @param state
     * @param commandId
     * @throws Exception
     */
    private void updateState(String state, Long commandId, Long taskId) throws Exception {
        Connection connection = schedulePersistent.getDruidDataSource().getConnection();
        try {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("update task_command set state=?,update_time=now() where  id=? ");
            preparedStatement.setString(1, state);
            preparedStatement.setLong(2, commandId);
            int i = preparedStatement.executeUpdate();
            preparedStatement.close();
            preparedStatement = connection.prepareStatement(
                    "update task_log set fetch_finish=1,update_time=now(),is_finished=1,end_time=now() where command_id=? ");
            preparedStatement.setLong(1, commandId);
            i = preparedStatement.executeUpdate();
            preparedStatement.close();
            preparedStatement = connection.prepareStatement(
                    "update task_schedule set  running_cmd_id=0,update_time=now() where task_id=?");
            preparedStatement.setLong(1, taskId);
            i = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.commit();
        } catch (Throwable e) {
            logger.error("updateState exception", e);
            connection.rollback();
        } finally {
            connection.close();
        }
    }
}
