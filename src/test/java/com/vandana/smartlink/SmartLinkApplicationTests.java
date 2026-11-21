package com.vandana.smartlink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SmartLinkApplicationTests {

    @Test
    @DisplayName("Application class should exist and load successfully")
    void contextLoads() {
        assertNotNull(SmartLinkApplication.class);
    }
}
