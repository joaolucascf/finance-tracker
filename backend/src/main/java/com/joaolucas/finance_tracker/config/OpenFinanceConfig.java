package com.joaolucas.finance_tracker.config;

import com.joaolucas.finance_tracker.openfinance.pluggy.PluggyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PluggyProperties.class)
public class OpenFinanceConfig {
}
