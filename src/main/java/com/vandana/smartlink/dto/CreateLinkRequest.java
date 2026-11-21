package com.vandana.smartlink.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class CreateLinkRequest {

    @NotBlank(message = "defaultUrl is required and cannot be blank")
    private String defaultUrl;

    private String customAlias;

    private String androidUrl;

    private String iosUrl;

    private String fallbackUrl;

    private Long maxClicks;

    private LocalDateTime expiresAt;

    public CreateLinkRequest() {
    }

    public CreateLinkRequest(String defaultUrl, String customAlias, String androidUrl, String iosUrl, String fallbackUrl, Long maxClicks, LocalDateTime expiresAt) {
        this.defaultUrl = defaultUrl;
        this.customAlias = customAlias;
        this.androidUrl = androidUrl;
        this.iosUrl = iosUrl;
        this.fallbackUrl = fallbackUrl;
        this.maxClicks = maxClicks;
        this.expiresAt = expiresAt;
    }

    // Explicit Getters and Setters

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public String getAndroidUrl() {
        return androidUrl;
    }

    public void setAndroidUrl(String androidUrl) {
        this.androidUrl = androidUrl;
    }

    public String getIosUrl() {
        return iosUrl;
    }

    public void setIosUrl(String iosUrl) {
        this.iosUrl = iosUrl;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public Long getMaxClicks() {
        return maxClicks;
    }

    public void setMaxClicks(Long maxClicks) {
        this.maxClicks = maxClicks;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
