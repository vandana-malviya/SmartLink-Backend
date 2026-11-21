package com.vandana.smartlink.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class LinkStatsResponse {

    private String shortCode;
    private String defaultUrl;
    private String status;
    private long totalClicks;
    private Long maxClicks;
    private LocalDateTime expiresAt;
    private Map<String, Long> deviceBreakdown;

    public LinkStatsResponse() {
    }

    public LinkStatsResponse(String shortCode, String defaultUrl, String status, long totalClicks, Long maxClicks, LocalDateTime expiresAt, Map<String, Long> deviceBreakdown) {
        this.shortCode = shortCode;
        this.defaultUrl = defaultUrl;
        this.status = status;
        this.totalClicks = totalClicks;
        this.maxClicks = maxClicks;
        this.expiresAt = expiresAt;
        this.deviceBreakdown = deviceBreakdown;
    }

    // Explicit Getters and Setters

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
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

    public Map<String, Long> getDeviceBreakdown() {
        return deviceBreakdown;
    }

    public void setDeviceBreakdown(Map<String, Long> deviceBreakdown) {
        this.deviceBreakdown = deviceBreakdown;
    }
}
