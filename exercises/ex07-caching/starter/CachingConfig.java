package com.javabanker.exercises.ex07;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring configuration of the exercise. The tests load it together with a FxRateClient bean. */
@Configuration(proxyBeanMethods = false)
public class CachingConfig {

    @Bean
    FxRateService fxRateService(FxRateClient client) {
        return new FxRateService(client);
    }
}
