package com.orion.schedule.autoconfig;

import com.orion.schedule.codec.ServerCodecService;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.processor.JobProcessorService;
import com.orion.schedule.progress.dao.DBExecLogService;
import com.orion.schedule.progress.dao.SchedulePersistent;
import com.orion.schedule.progress.kafka.KafkaChannelInitialize;
import com.orion.schedule.progress.kafka.KafkaExecLogService;
import com.orion.schedule.progress.kafka.KafkaMessageProducer;
import com.orion.schedule.register.ServerRegisterService;
import com.orion.schedule.service.ConnectionManagerService;
import com.orion.schedule.service.MessageProcessService;
import com.orion.schedule.service.ScheduleServerService;
import com.orion.schedule.service.TaskProcessorService;
import com.orion.schedule.transport.ServerTransportService;
import com.orion.schedule.util.ScheduleContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * @Description
 * @Author beedoorwei
 * @Date 2019/6/4 10:29
 * @Version 1.0.0
 */
@EnableConfigurationProperties(ScheduleServerConfig.class)
public class ScheduleClientAutoConfiguration {


    @Autowired
    ScheduleServerConfig scheduleServerConfig;

    @Bean(initMethod = "buildDataSource")
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('DB')")
    public SchedulePersistent schedulePersistent() throws Exception {
        return new SchedulePersistent();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('DB')")
    public DBExecLogService dbExecLogService() {
        return new DBExecLogService();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA')")
    public KafkaExecLogService kafkaExecLogService() {
        return new KafkaExecLogService();
    }

    @Bean(initMethod = "initProperties")
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA')")
    public KafkaChannelInitialize KafkaChannelInitialize() {
        return new KafkaChannelInitialize();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("'${schedule.server.task.notifyType}'.equals('KAFKA') && ${schedule.server.task.messageConfig.useKerberos}")
    @DependsOn("KafkaChannelInitialize")
    public KafkaMessageProducer kafkaMessageProducer() {
        return new KafkaMessageProducer();
    }

    @Bean(initMethod = "startServer")
    @ConditionalOnMissingBean
    public ScheduleServerService scheduleService() {
        ScheduleServerService scheduleServerService = new ScheduleServerService();
        return scheduleServerService;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageProcessService messageProcessService() {
        return new MessageProcessService();
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @DependsOn("messageProcessService")
    public TaskProcessorService taskProcessorService() {
        return new TaskProcessorService();
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

    @Bean
    @ConditionalOnMissingBean
    public JobProcessorService jobProcessorService() {
        JobProcessorService jobProcessorService = new JobProcessorService();
        return jobProcessorService;
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @DependsOn({"serverCodecService", "serverTransportService", "serverRegisterService"})
    public ConnectionManagerService connectionManagerService() {
        return new ConnectionManagerService(false);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerRegisterService serverRegisterService() {
        return new ServerRegisterService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduleContextUtils applicationContextUtils() {
        return new ScheduleContextUtils();
    }
}
