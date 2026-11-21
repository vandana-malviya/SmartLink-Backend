package com.vandana.smartlink.service;

import com.vandana.smartlink.dto.CreateLinkRequest;
import com.vandana.smartlink.dto.CreateLinkResponse;
import com.vandana.smartlink.dto.LinkStatsResponse;
import com.vandana.smartlink.exception.LinkExpiredException;
import com.vandana.smartlink.exception.LinkNotFoundException;
import com.vandana.smartlink.model.ClickLog;
import com.vandana.smartlink.model.ShortLink;
import com.vandana.smartlink.repository.ClickLogRepository;
import com.vandana.smartlink.repository.ShortLinkRepository;
import com.vandana.smartlink.util.Base62Encoder;
import com.vandana.smartlink.util.UrlSafetyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickLogRepository clickLogRepository;

    public LinkService(ShortLinkRepository shortLinkRepository, ClickLogRepository clickLogRepository) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickLogRepository = clickLogRepository;
    }

    /**
     * Creates a new short link, validating safety and assigning either a custom alias
     * or a collision-free Base62 encoded ID.
     *
     * @param request the link creation request
     * @return link details with generated short URL
     */
    @Transactional
    public CreateLinkResponse createShortLink(CreateLinkRequest request) {
        // Validate URL safety
        UrlSafetyValidator.validate(request.getDefaultUrl());

        if (request.getAndroidUrl() != null && !request.getAndroidUrl().trim().isEmpty()) {
            UrlSafetyValidator.validate(request.getAndroidUrl().trim());
        }
        if (request.getIosUrl() != null && !request.getIosUrl().trim().isEmpty()) {
            UrlSafetyValidator.validate(request.getIosUrl().trim());
        }
        if (request.getFallbackUrl() != null && !request.getFallbackUrl().trim().isEmpty()) {
            UrlSafetyValidator.validate(request.getFallbackUrl().trim());
        }

        ShortLink link = new ShortLink();
        link.setDefaultUrl(request.getDefaultUrl().trim());
        link.setAndroidUrl(request.getAndroidUrl() != null && !request.getAndroidUrl().trim().isEmpty() ? request.getAndroidUrl().trim() : null);
        link.setIosUrl(request.getIosUrl() != null && !request.getIosUrl().trim().isEmpty() ? request.getIosUrl().trim() : null);
        link.setFallbackUrl(request.getFallbackUrl() != null && !request.getFallbackUrl().trim().isEmpty() ? request.getFallbackUrl().trim() : null);
        link.setMaxClicks(request.getMaxClicks());
        link.setExpiresAt(request.getExpiresAt());
        link.setCreatedAt(LocalDateTime.now());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            String alias = request.getCustomAlias().trim();
            if (shortLinkRepository.existsByShortCode(alias)) {
                throw new IllegalArgumentException("Custom alias '" + alias + "' is already in use.");
            }
            shortCode = alias;
            link.setShortCode(shortCode);
            link = shortLinkRepository.save(link);
        } else {
            // Assign temporary unique token to satisfy not-null constraint prior to obtaining DB ID
            link.setShortCode("TEMP_" + UUID.randomUUID().toString().replace("-", ""));
            link = shortLinkRepository.saveAndFlush(link);

            shortCode = Base62Encoder.encodeDatabaseId(link.getId());
            link.setShortCode(shortCode);
            link = shortLinkRepository.save(link);
        }

        String shortUrl = "http://localhost:8080/r/" + shortCode;
        return new CreateLinkResponse(
            link.getShortCode(),
            shortUrl,
            link.getDefaultUrl(),
            link.getAndroidUrl(),
            link.getIosUrl(),
            link.getFallbackUrl(),
            link.getMaxClicks(),
            link.getExpiresAt(),
            link.getCreatedAt()
        );
    }

    /**
     * Resolves the target destination URL based on device context, expiration checks,
     * and records click statistics.
     *
     * @param shortCode the unique short link code
     * @param userAgent the HTTP User-Agent header
     * @param ipAddress client IP address
     * @return target redirect URL
     */
    @Transactional
    public String resolveRedirectUrl(String shortCode, String userAgent, String ipAddress) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new LinkNotFoundException("Short link not found for code: " + shortCode));

        // Circuit breaker: check expiration and click cap
        if (shortLink.isExpired()) {
            if (shortLink.getFallbackUrl() != null && !shortLink.getFallbackUrl().trim().isEmpty()) {
                return shortLink.getFallbackUrl().trim();
            }
            throw new LinkExpiredException("Short link '" + shortCode + "' has expired or reached its maximum click limit.");
        }

        // Context-aware device routing
        String deviceType = parseDeviceType(userAgent);
        String targetUrl;
        if ("ANDROID".equals(deviceType) && shortLink.getAndroidUrl() != null && !shortLink.getAndroidUrl().trim().isEmpty()) {
            targetUrl = shortLink.getAndroidUrl().trim();
        } else if ("IOS".equals(deviceType) && shortLink.getIosUrl() != null && !shortLink.getIosUrl().trim().isEmpty()) {
            targetUrl = shortLink.getIosUrl().trim();
        } else {
            targetUrl = shortLink.getDefaultUrl().trim();
        }

        // Increment stats and log click event
        shortLink.incrementClicks();
        shortLinkRepository.save(shortLink);

        ClickLog clickLog = new ClickLog(deviceType, LocalDateTime.now(), ipAddress, shortLink);
        clickLogRepository.save(clickLog);

        return targetUrl;
    }

    /**
     * Retrieves click analytics and device breakdown for a given short link.
     *
     * @param shortCode the short link code
     * @return link statistics DTO
     */
    @Transactional(readOnly = true)
    public LinkStatsResponse getLinkStats(String shortCode) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new LinkNotFoundException("Short link not found for code: " + shortCode));

        String status = shortLink.isExpired() ? "EXPIRED" : "ACTIVE";

        long androidClicks = clickLogRepository.countByShortLinkAndDeviceType(shortLink, "ANDROID");
        long iosClicks = clickLogRepository.countByShortLinkAndDeviceType(shortLink, "IOS");
        long desktopClicks = clickLogRepository.countByShortLinkAndDeviceType(shortLink, "DESKTOP");

        Map<String, Long> deviceBreakdown = new LinkedHashMap<>();
        deviceBreakdown.put("ANDROID", androidClicks);
        deviceBreakdown.put("IOS", iosClicks);
        deviceBreakdown.put("DESKTOP", desktopClicks);

        return new LinkStatsResponse(
            shortLink.getShortCode(),
            shortLink.getDefaultUrl(),
            status,
            shortLink.getTotalClicks(),
            shortLink.getMaxClicks(),
            shortLink.getExpiresAt(),
            deviceBreakdown
        );
    }

    /**
     * Helper to classify device type based on User-Agent header.
     *
     * @param userAgent User-Agent string
     * @return ANDROID, IOS, or DESKTOP
     */
    public String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "DESKTOP";
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("android")) {
            return "ANDROID";
        } else if (lower.contains("iphone") || lower.contains("ipad")) {
            return "IOS";
        } else {
            return "DESKTOP";
        }
    }
}
