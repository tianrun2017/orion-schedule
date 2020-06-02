package com.orion.schedule.progress.kafka;

import com.orion.schedule.common.util.IoUtils;
import com.orion.schedule.config.MessageNotifyConfig;
import com.orion.schedule.config.ScheduleServerConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.text.MessageFormat;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/17 20:40
 * @Version 1.0.0
 */
public class KafkaChannelInitialize {
    private static final String JAAS_TEMPLATE = "KafkaClient '{'\n" +
            "\tcom.sun.security.auth.module.Krb5LoginModule required\n" +
            "\tuseKeyTab=true\n" +
            "\tkeyTab=\"{0}\"\n" +
            "\tprincipal=\"{1}\";\n" +
            "'}';";
    @Autowired
    ScheduleServerConfig scheduleServerConfig;
    private Logger logger = LoggerFactory.getLogger(KafkaChannelInitialize.class);

    public void initProperties() {
        configureKerberosJAAS();
    }

    private void configureKerberosJAAS() {
        MessageNotifyConfig messageConfig = scheduleServerConfig.getTask().getMessageConfig();
        if (!messageConfig.isUseKerberos() || StringUtils.isNotEmpty(System.getProperties().getProperty("java.security.krb5.conf"))) {
            logger.info("kafka config is alread init ");
            return;
        }

        File krb5Conf = IoUtils.copyResourceAsTempFile(messageConfig.getKrbPath(), "krb5", "conf");
        System.setProperty("java.security.krb5.conf", krb5Conf.getAbsolutePath());
        File keytab = IoUtils.copyResourceAsTempFile(messageConfig.getKeyTabPath(), "user", "keyTab");
        logger.info("keytab path {} ", keytab.getAbsolutePath());
        String replace = StringUtils.replace(keytab.getAbsolutePath(), "\\", "/");
        String content = MessageFormat.format(JAAS_TEMPLATE, replace, messageConfig.getKeberosUser());
        File jaasConf = IoUtils.saveContentToFile(content, "jaas-config", "conf");
        logger.info("login config {} ", jaasConf.getAbsolutePath());
        System.setProperty("java.security.auth.login.config", jaasConf.getAbsolutePath());
    }
}
