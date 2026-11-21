package com.vandana.smartlink.controller;

import com.vandana.smartlink.dto.CreateLinkRequest;
import com.vandana.smartlink.dto.CreateLinkResponse;
import com.vandana.smartlink.dto.LinkStatsResponse;
import com.vandana.smartlink.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * Creates a new context-aware short link.
     *
     * @param request the link creation payload
     * @return 201 CREATED with link details
     */
    @PostMapping("/api/links")
    public ResponseEntity<CreateLinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        CreateLinkResponse response = linkService.createShortLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Resolves and redirects the client to the context-aware target URL (HTTP 302).
     *
     * @param shortCode the unique short link code
     * @param userAgent the User-Agent header of the caller
     * @param request   the HTTP servlet request for client IP resolution
     * @return HTTP 302 FOUND redirect to the resolved URL
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        String clientIp = extractClientIp(request);
        String targetUrl = linkService.resolveRedirectUrl(shortCode, userAgent, clientIp);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }

    /**
     * Retrieves analytics, click counts, and device breakdown for a short link.
     *
     * @param shortCode the short link code
     * @return 200 OK with link statistics
     */
    @GetMapping("/api/links/{shortCode}/stats")
    public ResponseEntity<LinkStatsResponse> getStats(@PathVariable String shortCode) {
        LinkStatsResponse stats = linkService.getLinkStats(shortCode);
        return ResponseEntity.ok(stats);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty() && !"unknown".equalsIgnoreCase(forwardedFor.trim())) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(realIp.trim())) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
