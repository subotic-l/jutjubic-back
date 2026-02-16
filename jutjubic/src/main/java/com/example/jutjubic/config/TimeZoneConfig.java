package com.example.jutjubic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    // Postavi vremensku zonu na Europe/Belgrade (CET/CEST - UTC+1/UTC+2)
    private static final String DEFAULT_TIMEZONE = "Europe/Belgrade";

    @PostConstruct
    void started() {
        // Postavi Europe/Belgrade kao default vremensku zonu za celu aplikaciju
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(DEFAULT_TIMEZONE)));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder
                .json()
                .timeZone(TimeZone.getTimeZone(ZoneId.of(DEFAULT_TIMEZONE)))
                .modules(new JavaTimeModule())
                .build();
    }
}
