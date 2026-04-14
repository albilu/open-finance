package org.openfinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for automatic logo fetching.
 *
 * <p>Bound to {@code application.logo-fetch.*} in {@code application.yml}. Follows the same pattern
 * as {@link SchedulerProperties}.
 */
@Component
@ConfigurationProperties(prefix = "application.logo-fetch")
public class LogoFetchProperties {

    /** Master on/off switch. Defaults to {@code false}. */
    private boolean enabled = false;

    /** HTTP connect + read timeout in seconds. Defaults to {@code 5}. */
    private int timeoutSeconds = 5;

    /**
     * Minimum acceptable response body size in bytes. Responses smaller than this are rejected as
     * generic/empty icons. Defaults to {@code 100}.
     */
    private int minResponseBytes = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMinResponseBytes() {
        return minResponseBytes;
    }

    public void setMinResponseBytes(int minResponseBytes) {
        this.minResponseBytes = minResponseBytes;
    }
}
