package com.finresearch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "finresearch")
public class FinResearchProperties {
    private String dataDir = "./data";
    private Jwt jwt = new Jwt();
    private Encryption encryption = new Encryption();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000L;
    }

    @Data
    public static class Encryption {
        /** Base64-encoded 32-byte key for AES-256 */
        private String keyBase64;
    }
}
