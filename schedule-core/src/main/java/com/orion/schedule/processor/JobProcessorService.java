package com.orion.schedule.processor;

import com.google.common.collect.Maps;
import com.orion.schedule.codec.ServerCodecService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Map;
import java.util.Set;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/5/31 23:29
 * @Version 1.0.0
 */
public class JobProcessorService implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware, EnvironmentAware {

    Map<String, JobProcessor> jobProcessorMap = Maps.newHashMap();
    BeanFactory beanFactory;
    Environment environment;
    private Logger logger = LoggerFactory.getLogger(JobProcessorService.class);

    /**
     * get processor
     *
     * @param procssorName
     * @return
     */
    public JobProcessor getProcessor(String procssorName) {
        if (jobProcessorMap.isEmpty()) {
            synchronized (ServerCodecService.class) {
                Map<String, JobProcessor> beansOfType = ((DefaultListableBeanFactory) beanFactory).getBeansOfType(JobProcessor.class);
                if (MapUtils.isNotEmpty(beansOfType)) {
                    beansOfType.values().stream().forEach(bean -> {
                        jobProcessorMap.put(bean.getClass().getName(), bean);
                        logger.info("init task processor [{}] ", bean.getClass().getName());
                    });
                }
            }
        }
        return jobProcessorMap.get(procssorName);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        String basePackage = "com.orion";
        TypeFilter typeFilter = new AssignableTypeFilter(JobProcessor.class);
        scanner.addIncludeFilter(typeFilter);
        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
        if (CollectionUtils.isNotEmpty(candidateComponents)) {
            candidateComponents.stream().forEach(component -> {
                String beanClassName = component.getBeanClassName();
                beanDefinitionRegistry.registerBeanDefinition(beanClassName, component);
            });
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
            throws BeansException {

    }
}
