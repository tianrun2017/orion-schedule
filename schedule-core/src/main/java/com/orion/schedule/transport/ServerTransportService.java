package com.orion.schedule.transport;

import com.google.common.collect.Maps;
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
 * @Date 2019/6/4 15:21
 * @Version 1.0.0
 */
public class ServerTransportService implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware, EnvironmentAware {

    Map<String, ServerTransport> serverTransportMap = Maps.newConcurrentMap();
    BeanFactory beanFactory;
    Environment environment;
    private Logger logger = LoggerFactory.getLogger(ServerTransportService.class);

    /**
     * get server transport by code
     */
    public ServerTransport serverTransport(String code) {
        if (serverTransportMap.isEmpty()) {
            synchronized (ServerTransportService.class) {
                Map<String, ServerTransport> beansOfType = ((DefaultListableBeanFactory) beanFactory).getBeansOfType(ServerTransport.class);
                if (MapUtils.isNotEmpty(beansOfType)) {
                    beansOfType.values().stream().forEach(bean -> {
                        serverTransportMap.put(bean.transportType(), bean);
                        logger.info("init server transport [{}] ", bean.transportType());
                    });
                }
            }
        }
        return serverTransportMap.get(code);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        String basePackage = "com.orion.schedule.transport";
        TypeFilter typeFilter = new AssignableTypeFilter(ServerTransport.class);
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
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
