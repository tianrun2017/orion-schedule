package com.orion.schedule.register;

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
 * @Date 2019/6/3 18:55
 * @Version 1.0.0
 */
public class ServerRegisterService implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware, EnvironmentAware {
    Map<String, ServerRegister> serverRegisterMap = Maps.newConcurrentMap();
    BeanFactory beanFactory;
    Environment environment;
    private Logger logger = LoggerFactory.getLogger(ServerRegisterService.class);

    /**
     * get server register by code
     */
    public ServerRegister serverRegister(String code) {
        if (serverRegisterMap.isEmpty()) {
            synchronized (ServerRegisterService.class) {
                Map<String, ServerRegister> beansOfType = ((DefaultListableBeanFactory) beanFactory).getBeansOfType(ServerRegister.class);
                if (MapUtils.isNotEmpty(beansOfType)) {
                    beansOfType.values().stream().forEach(bean -> {
                        if (bean.registerCode().equals(code)) {
                            bean.init();
                            serverRegisterMap.put(bean.registerCode(), bean);
                            logger.info("init server register [{}] ", bean.registerCode());
                        }
                    });
                }
            }
        }
        return serverRegisterMap.get(code);
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        String basePackage = "com.orion.schedule.register";
        TypeFilter typeFilter = new AssignableTypeFilter(ServerRegister.class);
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
