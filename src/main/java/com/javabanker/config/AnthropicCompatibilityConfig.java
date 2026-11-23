package com.javabanker.config;

import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 1.1 gives the Anthropic chat options a default temperature, but current Claude models
 * (e.g. claude-opus-5-5) reject requests that set sampling parameters (HTTP 400). Spring's binder can't
 * reset a default to null, so it's removed here, after the properties have been bound.
 * Verified by AnthropicAssistantContractTest.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AnthropicChatProperties.class)
public class AnthropicCompatibilityConfig {

    @Bean
    static BeanPostProcessor removeAnthropicSamplingDefaults() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof AnthropicChatProperties properties) {
                    properties.getOptions().setTemperature(null);
                    properties.getOptions().setTopP(null);
                    properties.getOptions().setTopK(null);
                }
                return bean;
            }
        };
    }
}
