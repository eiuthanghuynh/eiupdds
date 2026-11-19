package com.eiu.pdds.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration properties for API rate limiting under {@code pdds.rate-limit}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pdds.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String trustedProxies = "";
    private int diagnosisGuestLimit = 3;
    private long diagnosisGuestWindowSeconds = 3600L;
    private int diagnosisUserLimit = 10;
    private long diagnosisUserWindowSeconds = 60L;
    private int authIpLimit = 10;
    private long authIpWindowSeconds = 60L;

    /**
     * Parses comma-separated trusted proxy entries into a trimmed, unmodifiable set.
     */
    public Set<String> trustedProxySet() {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
