package com.teachnet.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed, Admin admin) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Seed(boolean demoData) {}

    public record Admin(String email, String password) {}
}
