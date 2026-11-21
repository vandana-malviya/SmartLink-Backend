package com.vandana.smartlink.service;

import com.vandana.smartlink.dto.CreateLinkRequest;
import com.vandana.smartlink.dto.CreateLinkResponse;
import com.vandana.smartlink.dto.LinkStatsResponse;
import com.vandana.smartlink.exception.LinkExpiredException;
import com.vandana.smartlink.exception.LinkNotFoundException;
import com.vandana.smartlink.exception.UnsafeUrlException;
import com.vandana.smartlink.model.ClickLog;
import com.vandana.smartlink.model.ShortLink;
import com.vandana.smartlink.repository.ClickLogRepository;
import com.vandana.smartlink.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {

    private ShortLinkRepository shortLinkRepository;
    private ClickLogRepository clickLogRepository;
    private LinkService linkService;

    private Map<String, ShortLink> linksByCode;
    private Map<Long, ShortLink> linksById;
    private List<ClickLog> clickLogs;
    private AtomicLong idCounter;

    @BeforeEach
    void setUp() {
        linksByCode = new HashMap<>();
        linksById = new HashMap<>();
        clickLogs = new ArrayList<>();
        idCounter = new AtomicLong(0);

        shortLinkRepository = (ShortLinkRepository) Proxy.newProxyInstance(
                ShortLinkRepository.class.getClassLoader(),
                new Class<?>[]{ShortLinkRepository.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("findByShortCode".equals(methodName)) {
                        return Optional.ofNullable(linksByCode.get(args[0]));
                    } else if ("existsByShortCode".equals(methodName)) {
                        return linksByCode.containsKey(args[0]);
                    } else if ("save".equals(methodName) || "saveAndFlush".equals(methodName)) {
                        ShortLink link = (ShortLink) args[0];
                        if (link.getId() == null) {
                            link.setId(idCounter.incrementAndGet());
                        }
                        if (link.getShortCode() != null) {
                            linksByCode.put(link.getShortCode(), link);
                        }
                        linksById.put(link.getId(), link);
                        return link;
                    }
                    return null;
                }
        );

        clickLogRepository = (ClickLogRepository) Proxy.newProxyInstance(
                ClickLogRepository.class.getClassLoader(),
                new Class<?>[]{ClickLogRepository.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("countByShortLinkAndDeviceType".equals(methodName)) {
                        ShortLink link = (ShortLink) args[0];
                        String deviceType = (String) args[1];
                        return clickLogs.stream()
                                .filter(c -> c.getShortLink() != null
                                        && c.getShortLink().getId().equals(link.getId())
                                        && deviceType.equalsIgnoreCase(c.getDeviceType()))
                                .count();
                    } else if ("save".equals(methodName)) {
                        ClickLog log = (ClickLog) args[0];
                        clickLogs.add(log);
                        return log;
                    }
                    return null;
                }
        );

        linkService = new LinkService(shortLinkRepository, clickLogRepository);
    }

    @Test
    @DisplayName("Should create short link with custom alias when provided")
    void testCreateWithCustomAlias() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setCustomAlias("my-campaign");

        CreateLinkResponse response = linkService.createShortLink(request);

        assertNotNull(response);
        assertEquals("my-campaign", response.getShortCode());
        assertEquals("http://localhost:8080/r/my-campaign", response.getShortUrl());
        assertEquals("https://example.com/web", response.getDefaultUrl());
        assertTrue(linksByCode.containsKey("my-campaign"));
    }

    @Test
    @DisplayName("Should create short link with Base62 generated code when no custom alias is provided")
    void testCreateWithBase62Code() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/portal");

        CreateLinkResponse response = linkService.createShortLink(request);

        assertNotNull(response);
        assertNotNull(response.getShortCode());
        assertFalse(response.getShortCode().startsWith("TEMP_"));
        assertEquals("http://localhost:8080/r/" + response.getShortCode(), response.getShortUrl());
    }

    @Test
    @DisplayName("Should throw exception if custom alias is already taken")
    void testDuplicateCustomAlias() {
        CreateLinkRequest request1 = new CreateLinkRequest();
        request1.setDefaultUrl("https://example.com/1");
        request1.setCustomAlias("promo2026");
        linkService.createShortLink(request1);

        CreateLinkRequest request2 = new CreateLinkRequest();
        request2.setDefaultUrl("https://example.com/2");
        request2.setCustomAlias("promo2026");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> linkService.createShortLink(request2));
        assertTrue(ex.getMessage().contains("already in use"));
    }

    @Test
    @DisplayName("Should throw UnsafeUrlException if default URL is blacklisted")
    void testUnsafeDefaultUrl() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("http://malicious-test.com/payload");

        assertThrows(UnsafeUrlException.class, () -> linkService.createShortLink(request));
    }

    @Test
    @DisplayName("Should route to Android URL for Android User-Agent")
    void testResolveRedirectAndroid() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setAndroidUrl("https://play.google.com/store/apps/details?id=com.example");
        request.setCustomAlias("app-link");
        linkService.createShortLink(request);

        String target = linkService.resolveRedirectUrl("app-link", "Mozilla/5.0 (Linux; Android 14; Pixel 8)", "192.168.1.1");

        assertEquals("https://play.google.com/store/apps/details?id=com.example", target);
        assertEquals(1, clickLogs.size());
        assertEquals("ANDROID", clickLogs.get(0).getDeviceType());
        assertEquals("192.168.1.1", clickLogs.get(0).getIpAddress());
    }

    @Test
    @DisplayName("Should route to iOS URL for iPhone User-Agent")
    void testResolveRedirectIos() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setIosUrl("https://apps.apple.com/app/id123456");
        request.setCustomAlias("ios-link");
        linkService.createShortLink(request);

        String target = linkService.resolveRedirectUrl("ios-link", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)", "10.0.0.1");

        assertEquals("https://apps.apple.com/app/id123456", target);
        assertEquals(1, clickLogs.size());
        assertEquals("IOS", clickLogs.get(0).getDeviceType());
    }

    @Test
    @DisplayName("Should route to Default URL for Desktop User-Agent")
    void testResolveRedirectDesktop() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/desktop-landing");
        request.setCustomAlias("desktop-link");
        linkService.createShortLink(request);

        String target = linkService.resolveRedirectUrl("desktop-link", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "127.0.0.1");

        assertEquals("https://example.com/desktop-landing", target);
        assertEquals(1, clickLogs.size());
        assertEquals("DESKTOP", clickLogs.get(0).getDeviceType());
    }

    @Test
    @DisplayName("Should redirect to fallbackUrl when expired and fallbackUrl is present")
    void testExpiredWithFallbackUrl() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setFallbackUrl("https://example.com/backup-page");
        request.setCustomAlias("exp-link");
        request.setExpiresAt(LocalDateTime.now().minusHours(1));
        linkService.createShortLink(request);

        String target = linkService.resolveRedirectUrl("exp-link", "Desktop", "127.0.0.1");
        assertEquals("https://example.com/backup-page", target);
    }

    @Test
    @DisplayName("Should throw LinkExpiredException when expired and no fallbackUrl is configured")
    void testExpiredWithoutFallbackUrl() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setCustomAlias("expired-no-fallback");
        request.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        linkService.createShortLink(request);

        assertThrows(LinkExpiredException.class,
                () -> linkService.resolveRedirectUrl("expired-no-fallback", "Desktop", "127.0.0.1"));
    }

    @Test
    @DisplayName("Should throw LinkExpiredException when click cap reached and no fallbackUrl")
    void testClickCapReachedWithoutFallbackUrl() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setCustomAlias("capped-link");
        request.setMaxClicks(2L);
        linkService.createShortLink(request);

        // Click 1
        linkService.resolveRedirectUrl("capped-link", "Desktop", "127.0.0.1");
        // Click 2
        linkService.resolveRedirectUrl("capped-link", "Desktop", "127.0.0.1");

        // Click 3: Cap reached -> throws LinkExpiredException
        assertThrows(LinkExpiredException.class,
                () -> linkService.resolveRedirectUrl("capped-link", "Desktop", "127.0.0.1"));
    }

    @Test
    @DisplayName("Should throw LinkNotFoundException for non-existent short code")
    void testLinkNotFound() {
        assertThrows(LinkNotFoundException.class,
                () -> linkService.resolveRedirectUrl("non-existent-code", "Desktop", "127.0.0.1"));
    }

    @Test
    @DisplayName("Should calculate statistics and device breakdown accurately")
    void testGetLinkStats() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/analytics");
        request.setAndroidUrl("https://example.com/android");
        request.setIosUrl("https://example.com/ios");
        request.setCustomAlias("analytics-test");
        linkService.createShortLink(request);

        // Generate clicks
        linkService.resolveRedirectUrl("analytics-test", "Mozilla/5.0 (Linux; Android 14)", "1.1.1.1");
        linkService.resolveRedirectUrl("analytics-test", "Mozilla/5.0 (Linux; Android 14)", "1.1.1.2");
        linkService.resolveRedirectUrl("analytics-test", "Mozilla/5.0 (iPhone; CPU OS 17)", "2.2.2.1");
        linkService.resolveRedirectUrl("analytics-test", "Mozilla/5.0 (Windows NT 10.0)", "3.3.3.1");

        LinkStatsResponse stats = linkService.getLinkStats("analytics-test");

        assertNotNull(stats);
        assertEquals("analytics-test", stats.getShortCode());
        assertEquals("ACTIVE", stats.getStatus());
        assertEquals(4L, stats.getTotalClicks());
        assertEquals(2L, stats.getDeviceBreakdown().get("ANDROID"));
        assertEquals(1L, stats.getDeviceBreakdown().get("IOS"));
        assertEquals(1L, stats.getDeviceBreakdown().get("DESKTOP"));
    }
}
