package com.vandana.smartlink.util;

import com.vandana.smartlink.exception.UnsafeUrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "http://example.com",
        "https://example.com/path?arg=val",
        "https://sub.domain.co.uk/page#hash",
        "http://127.0.0.1:8080/test"
    })
    @DisplayName("Should accept valid HTTP/HTTPS URLs")
    void testValidUrls(String url) {
        assertDoesNotThrow(() -> UrlSafetyValidator.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "ftp://example.com/file.txt",
        "javascript:alert(1)",
        "file:///etc/passwd",
        "htp://typo.com",
        "custom-scheme://app"
    })
    @DisplayName("Should reject invalid or unsupported protocols")
    void testInvalidProtocols(String url) {
        assertThrows(UnsafeUrlException.class, () -> UrlSafetyValidator.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://malicious-test.com",
        "https://malicious-test.com/login",
        "https://sub.malicious-test.com/attack",
        "http://phishing-demo.org",
        "https://phishing-demo.org/steal-password",
        "https://secure.phishing-demo.org",
        "http://spam-link.net",
        "https://spam-link.net/free-gift",
        "https://promo.spam-link.net"
    })
    @DisplayName("Should block blacklisted and phishing domains")
    void testBlacklistedDomains(String url) {
        UnsafeUrlException exception = assertThrows(UnsafeUrlException.class, () -> UrlSafetyValidator.validate(url));
        assertTrue(exception.getMessage().contains("Blocked malicious or phishing domain"));
    }

    @Test
    @DisplayName("Should throw for null URL")
    void testNullUrl() {
        assertThrows(UnsafeUrlException.class, () -> UrlSafetyValidator.validate(null));
    }
}
