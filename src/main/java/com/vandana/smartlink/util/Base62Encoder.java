package com.vandana.smartlink.util;

public final class Base62Encoder {

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_ALPHABET.length();
    public static final long OFFSET = 10000L;

    private Base62Encoder() {
        // Prevent instantiation of utility class
    }

    /**
     * Encodes a database ID by offsetting it by 10000L to produce collision-free
     * short codes with consistent length.
     *
     * @param databaseId the primary key database ID
     * @return Base62 encoded string representation
     */
    public static String encodeDatabaseId(long databaseId) {
        if (databaseId < 0) {
            throw new IllegalArgumentException("Database ID cannot be negative: " + databaseId);
        }
        return encode(OFFSET + databaseId);
    }

    /**
     * Encodes a positive number into a Base62 string.
     *
     * @param number positive integer or zero
     * @return Base62 string
     */
    public static String encode(long number) {
        if (number < 0) {
            throw new IllegalArgumentException("Number to encode cannot be negative: " + number);
        }
        if (number == 0) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long current = number;
        while (current > 0) {
            int remainder = (int) (current % BASE);
            sb.append(BASE62_ALPHABET.charAt(remainder));
            current /= BASE;
        }

        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to its corresponding long value.
     *
     * @param encoded Base62 string
     * @return original long number
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int charIndex = BASE62_ALPHABET.indexOf(c);
            if (charIndex == -1) {
                throw new IllegalArgumentException("Invalid Base62 character '" + c + "' in string: " + encoded);
            }
            result = result * BASE + charIndex;
        }

        return result;
    }
}
