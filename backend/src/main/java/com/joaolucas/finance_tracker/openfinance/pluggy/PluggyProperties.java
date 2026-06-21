package com.joaolucas.finance_tracker.openfinance.pluggy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pluggy")
@Getter
@Setter
public class PluggyProperties {

    private String clientId;
    private String clientSecret;
    private String baseUrl = "https://api.pluggy.ai";
}
