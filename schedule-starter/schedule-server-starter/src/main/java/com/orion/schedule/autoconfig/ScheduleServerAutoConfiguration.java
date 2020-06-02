package com.orion.schedule.autoconfig;

import com.orion.schedule.codec.ServerCodecService;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.progress.dao.DBExecLogService;
import com.orion.schedule.progress.dao.SchedulePersistent;
import com.orion.schedule.progress.kafka.KafkaChannelInitialize;
import com.orion.schedule.register.ServerRegisterService;
import com.orion.schedule.service.ConnectionManagerService;
import com.orion.schedule.service.ProgressProcessService;
import com.orion.schedule.service.ScheduleCommandService;
import com.orion.schedule.transport.ServerTransportService;
import com.orion.schedule.util.ScheduleContextUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/6/4 10:29
 * @Version 1.0.0
 */
@EnableConfigurationProperties(ScheduleServerConfig.class)
public class ScheduleServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServerRegisterService serverRegisterService() {
        return new ServerRegisterService();
    }

    @Bean(initMethod = "buildDataSource")
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('DB')")
    public SchedulePersistent schedulePersistent() throws Exception {
        return new SchedulePersistent();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA')")
    public DBExecLogService dbExecLogService() {
        return new DBExecLogService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerTransportService serverTransportService() {
        return new ServerTransportService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerCodecService serverCodecService() {
        return new ServerCodecService();
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @DependsOn({"serverCodecService", "serverTransportService", "serverRegisterService"})
    public ConnectionManagerService connectionManagerService() {
        return new ConnectionManagerService(true);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduleCommandService scheduleCommandService() {
        return new ScheduleCommandService();
    }

    @Bean(initMethod = "initProperties")
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA')")
    public KafkaChannelInitialize kafkaChannelInitialize() {
        return new KafkaChannelInitialize();
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA')")
    @DependsOn("kafkaChannelInitialize")
    public ProgressProcessService progressProcessService() {
        return new ProgressProcessService();
    }


    @Bean
    @ConditionalOnMissingBean
    public ScheduleContextUtils applicationContextUtils() {
        return new ScheduleContextUtils();
    }
}
