package com.vandana.smartlink.dto;

import java.time.LocalDateTime;

public class CreateLinkResponse {

    private String shortCode;
    private String shortUrl;
    private String defaultUrl;
    private String androidUrl;
    private String iosUrl;
    private String fallbackUrl;
    private Long maxClicks;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public CreateLinkResponse() {
    }

    public CreateLinkResponse(String shortCode, String shortUrl, String defaultUrl, String androidUrl, String iosUrl, String fallbackUrl, Long maxClicks, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.defaultUrl = defaultUrl;
        this.androidUrl = androidUrl;
        this.iosUrl = iosUrl;
        this.fallbackUrl = fallbackUrl;
        this.maxClicks = maxClicks;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    // Explicit Getters and Setters

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
