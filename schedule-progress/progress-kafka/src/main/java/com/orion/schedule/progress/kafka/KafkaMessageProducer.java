package com.orion.schedule.progress.kafka;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.common.util.InetUtils;
import com.orion.schedule.config.ScheduleServerConfig;
import com.orion.schedule.config.progress.ProcessMsgDto;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Future;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/16 21:02
 * @Version 1.0.0
 */
public class KafkaMessageProducer {

    private Logger logger = LoggerFactory.getLogger(KafkaMessageProducer.class);

    @Autowired
    private ScheduleServerConfig scheduleServerConfig;

    private Producer<String, String> producer;

    @PostConstruct
    public void initProducer() {
        String register = scheduleServerConfig.getTask().getMessageConfig().getRegister();

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, register);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        if (scheduleServerConfig.getTask().getMessageConfig().isUseSsl()) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "GSSAPI");
            props.put("sasl.kerberos.service.name", "hadoop");
        }
        props.setProperty(CLIENT_ID_CONFIG, InetUtils.getSelfIp());
        props.put(ACKS_CONFIG, "all");
        props.put(RETRIES_CONFIG, 3);
        props.put(BATCH_SIZE_CONFIG, 323840);
        props.put(LINGER_MS_CONFIG, 10);
        props.put(BUFFER_MEMORY_CONFIG, 33554432);
        props.put(MAX_BLOCK_MS_CONFIG, 3000);
        producer = new KafkaProducer<>(props);
    }

    public boolean sendMsg(Long commandId, ProcessMsgDto data) {
        String s = JSON.toJSONString(data);
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(scheduleServerConfig.getTask().getMessageConfig().getTopic(), String.valueOf(commandId), s);
            Future<RecordMetadata> send = producer.send(record);
            RecordMetadata recordMetadata = send.get();
            int partition = recordMetadata.partition();
            logger.info("send msg suc partition {} {} ", s, partition);
            return true;
        } catch (Throwable e) {
            logger.error("send msg error  " + s, e);
        }
        return false;
    }
}
