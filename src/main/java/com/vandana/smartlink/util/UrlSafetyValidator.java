package com.vandana.smartlink.util;

import com.vandana.smartlink.exception.UnsafeUrlException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class UrlSafetyValidator {

    private static final Set<String> BLACKLISTED_DOMAINS;

    static {
        Set<String> domains = new HashSet<>();
        domains.add("malicious-test.com");
        domains.add("phishing-demo.org");
        domains.add("spam-link.net");
        BLACKLISTED_DOMAINS = Collections.unmodifiableSet(domains);
    }

    private UrlSafetyValidator() {
        // Prevent instantiation of utility class
    }

    /**
     * Validates that the provided URL starts with http:// or https:// and does not
     * match known malicious or phishing domains.
     *
     * @param url the target URL to validate
     * @throws UnsafeUrlException if the URL is invalid, unsafe, or blacklisted
     */
    public static void validate(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new UnsafeUrlException("URL cannot be null or empty.");
        }

        String trimmedUrl = url.trim();
        String lowerUrl = trimmedUrl.toLowerCase(Locale.ROOT);

        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            throw new UnsafeUrlException("Invalid URL protocol. Target URL must start with 'http://' or 'https://': " + trimmedUrl);
        }

        try {
            URI uri = new URI(trimmedUrl);
            String host = uri.getHost();

            if (host == null || host.trim().isEmpty()) {
                throw new UnsafeUrlException("Invalid URL structure: Unable to extract host from '" + trimmedUrl + "'");
            }

            String lowerHost = host.toLowerCase(Locale.ROOT);

            for (String blacklisted : BLACKLISTED_DOMAINS) {
                if (lowerHost.equals(blacklisted) || lowerHost.endsWith("." + blacklisted)) {
                    throw new UnsafeUrlException("Blocked malicious or phishing domain: " + blacklisted);
                }
            }

            // Also safeguard against URLs with embedded blacklisted patterns
            for (String blacklisted : BLACKLISTED_DOMAINS) {
                if (lowerUrl.contains("://" + blacklisted) || lowerUrl.contains("@" + blacklisted)) {
                    throw new UnsafeUrlException("Blocked malicious or phishing domain: " + blacklisted);
                }
            }

        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("Malformed URL syntax: " + trimmedUrl, e);
        }
    }
}
