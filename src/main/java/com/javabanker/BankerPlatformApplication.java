package com.javabanker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Same as @SpringBootApplication, plus one exclusion: the study exercises (com.javabanker.exercises)
 * contain their own @Configuration classes (e.g. ex07 enables caching) that must not leak into the app.
 * Their tests load them explicitly.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.javabanker\\.exercises\\..*")})
@ConfigurationPropertiesScan
public class BankerPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankerPlatformApplication.class, args);
    }
}
