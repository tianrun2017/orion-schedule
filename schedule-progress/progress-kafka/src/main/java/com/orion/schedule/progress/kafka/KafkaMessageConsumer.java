package com.orion.schedule.progress.kafka;

import com.alibaba.fastjson.JSON;
import com.orion.schedule.config.progress.ProcessMsgDto;
import com.orion.schedule.config.progress.ProgressListener;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/16 21:02
 * @Version 1.0.0
 */
public class KafkaMessageConsumer {

    ExecutorService executorService = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("service-progress-%d").priority(8).daemon(true).build());
    private Logger logger = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private AtomicBoolean runFlag = new AtomicBoolean(true);

    public void initConsumer(boolean useSsl, String topic, String server, ProgressListener progressListener) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "SCHEDULE-SERVER");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        if (useSsl) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "GSSAPI");
            props.put("sasl.kerberos.service.name", "hadoop");
        }
        final Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        executorService.submit((Runnable) () -> {
            while (runFlag.get()) {
                try {
                    final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                    if (!consumerRecords.isEmpty()) {
                        consumerRecords.forEach(record -> {
                            try {
                                ProcessMsgDto process = process(record.value());
                                progressListener.notifyProgressUpdate(process);
                            } catch (Throwable e) {
                                logger.error("messge info not validate " + record.value());
                            }
                        });
                        consumer.commitSync();
                    }
                } catch (Throwable e) {
                    logger.error("process error  ", e);
                }
            }
        });
        addShutdownHolder();
    }

    private void addShutdownHolder() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutdown client ");
            runFlag.set(false);
        }));
    }


    private ProcessMsgDto process(String value) {
        return JSON.parseObject(value, ProcessMsgDto.class);
    }
}
