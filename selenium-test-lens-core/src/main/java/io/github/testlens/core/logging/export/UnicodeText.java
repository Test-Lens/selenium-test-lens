package io.github.testlens.core.logging.export;

final class UnicodeText {
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int KEYCAP = 0x20E3;

    private UnicodeText() {
    }

    static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        int boundary = 0;
        int cursor = 0;
        while (cursor < value.length()) {
            int clusterEnd = nextClusterEnd(value, cursor);
            if (clusterEnd > maxLength) {
                break;
            }
            boundary = clusterEnd;
            cursor = clusterEnd;
        }
        return value.substring(0, boundary) + "...";
    }

    private static int nextClusterEnd(String value, int start) {
        int first = value.codePointAt(start);
        int cursor = start + Character.charCount(first);
        if (isRegionalIndicator(first) && cursor < value.length()) {
            int second = value.codePointAt(cursor);
            if (isRegionalIndicator(second)) {
                cursor += Character.charCount(second);
            }
        }

        cursor = consumeExtenders(value, cursor);
        while (cursor < value.length() && value.codePointAt(cursor) == ZERO_WIDTH_JOINER) {
            int joiner = cursor;
            cursor += Character.charCount(ZERO_WIDTH_JOINER);
            if (cursor >= value.length()) {
                return joiner;
            }
            int joined = value.codePointAt(cursor);
            cursor += Character.charCount(joined);
            cursor = consumeExtenders(value, cursor);
        }
        return cursor;
    }

    private static int consumeExtenders(String value, int start) {
        int cursor = start;
        while (cursor < value.length()) {
            int codePoint = value.codePointAt(cursor);
            if (!isExtender(codePoint)) {
                break;
            }
            cursor += Character.charCount(codePoint);
        }
        return cursor;
    }

    private static boolean isExtender(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint == 0xFE0E
                || codePoint == 0xFE0F
                || codePoint == KEYCAP
                || (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF);
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }
}
