package com.vandana.smartlink.controller;

import com.vandana.smartlink.dto.CreateLinkRequest;
import com.vandana.smartlink.dto.CreateLinkResponse;
import com.vandana.smartlink.dto.LinkStatsResponse;
import com.vandana.smartlink.exception.GlobalExceptionHandler;
import com.vandana.smartlink.exception.LinkExpiredException;
import com.vandana.smartlink.exception.LinkNotFoundException;
import com.vandana.smartlink.exception.UnsafeUrlException;
import com.vandana.smartlink.model.ClickLog;
import com.vandana.smartlink.model.ShortLink;
import com.vandana.smartlink.repository.ClickLogRepository;
import com.vandana.smartlink.repository.ShortLinkRepository;
import com.vandana.smartlink.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LinkControllerTest {

    private LinkController linkController;
    private GlobalExceptionHandler exceptionHandler;
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

        ShortLinkRepository shortLinkRepository = (ShortLinkRepository) Proxy.newProxyInstance(
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

        ClickLogRepository clickLogRepository = (ClickLogRepository) Proxy.newProxyInstance(
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
        linkController = new LinkController(linkService);
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("POST /api/links should create short link and return HTTP 201 Created")
    void testCreateLink() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com");
        request.setCustomAlias("spring-boot-2026");

        ResponseEntity<CreateLinkResponse> response = linkController.createLink(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("spring-boot-2026", response.getBody().getShortCode());
        assertEquals("http://localhost:8080/r/spring-boot-2026", response.getBody().getShortUrl());
    }

    @Test
    @DisplayName("GET /r/{shortCode} should return HTTP 302 with Location header")
    void testRedirect() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com/web");
        request.setAndroidUrl("https://example.com/android-app");
        request.setCustomAlias("promo");
        linkController.createLink(request);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)");
        servletRequest.setRemoteAddr("10.0.0.5");

        ResponseEntity<Void> response = linkController.redirect("promo", "Mozilla/5.0 (Linux; Android 14)", servletRequest);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create("https://example.com/android-app"), response.getHeaders().getLocation());
    }

    @Test
    @DisplayName("GET /api/links/{shortCode}/stats should return HTTP 200 with stats")
    void testGetStats() {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setDefaultUrl("https://example.com");
        request.setCustomAlias("stats-check");
        linkController.createLink(request);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        linkController.redirect("stats-check", "Desktop Browser", servletRequest);

        ResponseEntity<LinkStatsResponse> statsResponse = linkController.getStats("stats-check");

        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        assertNotNull(statsResponse.getBody());
        assertEquals("stats-check", statsResponse.getBody().getShortCode());
        assertEquals(1L, statsResponse.getBody().getTotalClicks());
        assertEquals(1L, statsResponse.getBody().getDeviceBreakdown().get("DESKTOP"));
    }

    @Test
    @DisplayName("GlobalExceptionHandler should map exceptions to correct HTTP status codes")
    void testExceptionHandler() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/r/unknown");

        // 404 NOT FOUND
        ResponseEntity<Map<String, Object>> notFoundResponse =
                exceptionHandler.handleLinkNotFound(new LinkNotFoundException("Link not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertEquals(404, notFoundResponse.getBody().get("status"));

        // 410 GONE
        ResponseEntity<Map<String, Object>> goneResponse =
                exceptionHandler.handleLinkExpired(new LinkExpiredException("Link expired"), request);
        assertEquals(HttpStatus.GONE, goneResponse.getStatusCode());
        assertEquals(410, goneResponse.getBody().get("status"));

        // 400 BAD REQUEST for Unsafe URL
        ResponseEntity<Map<String, Object>> unsafeResponse =
                exceptionHandler.handleUnsafeUrl(new UnsafeUrlException("Unsafe domain"), request);
        assertEquals(HttpStatus.BAD_REQUEST, unsafeResponse.getStatusCode());
        assertEquals(400, unsafeResponse.getBody().get("status"));
    }
}
