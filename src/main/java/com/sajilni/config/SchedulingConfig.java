package com.sajilni.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enable scheduling for OTP cleanup task
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Configuration for scheduled tasks
    // The @EnableScheduling annotation enables the @Scheduled annotation processing
}