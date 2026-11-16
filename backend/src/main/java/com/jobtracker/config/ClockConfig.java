package com.jobtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** One injectable clock so "now" is configurable (APP_TIMEZONE) and easy to fix in unit tests. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:UTC}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
