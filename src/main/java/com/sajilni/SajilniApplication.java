// src/main/java/com/sajilni/SajilniApplication.java
package com.sajilni;

import com.sajilni.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication  // scans com.sajilni.* packages
@EnableConfigurationProperties(JwtConfig.class) // binds app.jwt.* to JwtConfig
public class SajilniApplication {

    public static void main(String[] args) {
        // Keep all timestamps in UTC (DB + JVM)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SajilniApplication.class, args);
    }
}
