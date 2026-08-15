package com.chatapp.api.config;

import com.chatapp.api.config.properties.ApiProperties;
import com.chatapp.api.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ApiProperties.class, JwtProperties.class})
public class PropertiesConfig {
}
