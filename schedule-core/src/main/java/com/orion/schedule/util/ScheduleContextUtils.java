package com.orion.schedule.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author beedoorwei
 * @Date 2019/5/30 10:52
 * @Version 1.0.0
 */
@Component
public class ScheduleContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public static <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    public static <T> T getBean(Class beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ScheduleContextUtils.applicationContext = applicationContext;
    }
}
