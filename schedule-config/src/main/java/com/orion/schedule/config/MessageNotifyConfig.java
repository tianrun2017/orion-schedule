package com.orion.schedule.config;

import lombok.Data;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/16 14:21
 * @Version 1.0.0
 */
@Data
public class MessageNotifyConfig {
    private String register;
    private boolean useSsl;
    private String topic;
    private boolean useKerberos;
    private String krbPath = "kerberos/krb5.conf";
    private String keyTabPath = "kerberos/hduser2010.keytab";
    private String keberosUser;
}
