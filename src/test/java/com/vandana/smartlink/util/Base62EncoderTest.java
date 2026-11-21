package com.vandana.smartlink.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    @DisplayName("Should encode 0 correctly")
    void testEncodeZero() {
        assertEquals("0", Base62Encoder.encode(0));
    }

    @Test
    @DisplayName("Should encode database ID with 10000L offset")
    void testEncodeDatabaseId() {
        // ID = 1 -> number = 10001
        String code1 = Base62Encoder.encodeDatabaseId(1L);
        assertNotNull(code1);
        assertFalse(code1.isEmpty());
        assertEquals(10001L, Base62Encoder.decode(code1));

        // Two consecutive IDs must produce different codes
        String code2 = Base62Encoder.encodeDatabaseId(2L);
        assertNotEquals(code1, code2);
        assertEquals(10002L, Base62Encoder.decode(code2));
    }

    @Test
    @DisplayName("Should encode and decode roundtrip successfully")
    void testEncodeDecodeRoundtrip() {
        long[] testValues = {0L, 1L, 61L, 62L, 10000L, 9999999L, Long.MAX_VALUE / 2};
        for (long value : testValues) {
            String encoded = Base62Encoder.encode(value);
            long decoded = Base62Encoder.decode(encoded);
            assertEquals(value, decoded, "Failed roundtrip for value: " + value);
        }
    }

    @Test
    @DisplayName("Should throw exception for negative database ID")
    void testNegativeDatabaseId() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encodeDatabaseId(-1L));
    }

    @Test
    @DisplayName("Should throw exception for invalid Base62 characters")
    void testInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("abc$123"));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.decode("hello world"));
    }
}
