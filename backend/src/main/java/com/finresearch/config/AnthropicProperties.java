package com.finresearch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "anthropic")
public class AnthropicProperties {
    private String apiKey = "";
    private String model = "claude-sonnet-4-20250514";
    private int maxTokens = 8192;
}
