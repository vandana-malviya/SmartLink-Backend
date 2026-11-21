package com.vandana.smartlink.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "short_links",
    indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
    }
)
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String defaultUrl;

    @Column(length = 2048)
    private String androidUrl;

    @Column(length = 2048)
    private String iosUrl;

    @Column(length = 2048)
    private String fallbackUrl;

    @Column(nullable = false)
    private long totalClicks = 0L;

    @Column
    private Long maxClicks;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "shortLink", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClickLog> clickLogs = new ArrayList<>();

    public ShortLink() {
    }

    public ShortLink(String shortCode, String defaultUrl, String androidUrl, String iosUrl, String fallbackUrl, Long maxClicks, LocalDateTime expiresAt) {
        this.shortCode = shortCode;
        this.defaultUrl = defaultUrl;
        this.androidUrl = androidUrl;
        this.iosUrl = iosUrl;
        this.fallbackUrl = fallbackUrl;
        this.maxClicks = maxClicks;
        this.expiresAt = expiresAt;
        this.totalClicks = 0L;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        boolean timeExpired = (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
        boolean clickCapReached = (maxClicks != null && totalClicks >= maxClicks);
        return timeExpired || clickCapReached;
    }

    public void incrementClicks() {
        this.totalClicks++;
    }

    // Explicit Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ClickLog> getClickLogs() {
        return clickLogs;
    }

    public void setClickLogs(List<ClickLog> clickLogs) {
        this.clickLogs = clickLogs;
    }
}
