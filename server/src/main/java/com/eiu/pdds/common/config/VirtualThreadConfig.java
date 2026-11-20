package com.eiu.pdds.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configures Spring-managed Java 21 Virtual Thread executors for parallel diagnosis I/O.
 * Uses a dedicated bean name to avoid conflicting with Spring Boot's default applicationTaskExecutor.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean(name = "diagnosisIoExecutor", destroyMethod = "close")
    public ExecutorService diagnosisIoExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
