/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.google.apphosting.trace;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * |
 */
public class TraceContext {
    private static final int DECIMAL = 10;
    private static final int HEX = 16;
    private static final BitSet TCHARS;
    static {
        TCHARS = new BitSet(128);
        TCHARS.set('A', 'Z' + 1);
        TCHARS.set('a', 'z' + 1);
        TCHARS.set('0', '9' + 1);
        String specials = "!#$%&'*+-.^_`|~";
        for (int i = 0; i < specials.length(); i++) {
            TCHARS.set(specials.charAt(i));
        }
    }

    private final BigInteger traceId;
    private final long spanId;
    private final boolean enabled;
    private final boolean stackTraceEnabled;
    private final Map<String, String> parameters;

    public static TraceContext parse(String text) {
        Objects.requireNonNull(text, "text");

        // traceId is required
        int traceStart = skipWhiteSpace(text, 0);
        int cursor = expectNumber(text, traceStart, HEX);
        if (cursor == traceStart || (cursor - traceStart) > 32) {
            throw new IllegalArgumentException("Invalid TraceContext: " + text);
        }
        BigInteger traceId = new BigInteger(text.substring(traceStart, cursor), HEX);

        cursor = skipWhiteSpace(text, cursor);
        if (cursor == text.length()) {
            throw new IllegalArgumentException("Invalid TraceContext: " + text);
        }

        // spanId is required
        if (text.charAt(cursor++) != '/') {
            throw new IllegalArgumentException("Invalid TraceContext: " + text);
        }
        cursor = skipWhiteSpace(text, cursor);
        int spanStart = cursor;
        cursor = expectNumber(text, spanStart, DECIMAL);
        long spanId = Long.parseLong(text.substring(spanStart, cursor));

        boolean enabled = false;
        boolean stackEnabled = false;
        Map<String, String> params = new HashMap<>();

        cursor = skipWhiteSpace(text, cursor);
        while (cursor < text.length()) {
            if (text.charAt(cursor++) != ';') {
                throw new IllegalArgumentException("Invalid TraceContext: " + text);
            }
            cursor = skipWhiteSpace(text, cursor);
            int keyStart = cursor;
            cursor = expectToken(text, cursor);
            String key = text.substring(keyStart, cursor);
            cursor = skipWhiteSpace(text, cursor);
            if (text.charAt(cursor++) != '=') {
                throw new IllegalArgumentException("Invalid TraceContext: " + text);
            }
            cursor = skipWhiteSpace(text, cursor);
            int valueStart = cursor;
            cursor = expectValue(text, cursor);
            String value = text.substring(valueStart, cursor);
            if ("o".equalsIgnoreCase(key)) {
                int mask = Integer.parseInt(value);
                enabled = (mask & 1) != 0;
                stackEnabled = (mask & 2) != 0;
            } else {
                params.put(key, value);
            }
        }
        return new TraceContext(traceId, spanId, enabled, stackEnabled, params);
    }

    private static int expectNumber(String text, int cursor, int radix) {
        while (cursor < text.length()) {
            char ch = text.charAt(cursor);
            if (Character.digit(ch, radix) < 0) {
                return cursor;
            }
            cursor += 1;
        }
        return cursor;
    }

    private static int expectValue(String text, int cursor) {
        // TODO: handle quoted-string
        return expectToken(text, cursor);
    }

    private static int expectToken(String text, int cursor) {
        while (cursor < text.length()) {
            char ch = text.charAt(cursor);
            if (!TCHARS.get(ch)) {
                return cursor;
            }
            cursor += 1;
        }
        return cursor;
    }

    private static int skipWhiteSpace(String text, int cursor) {
        while (cursor < text.length()) {
            char ch = text.charAt(cursor);
            if (!Character.isWhitespace(ch)) {
                return cursor;
            }
            cursor += 1;
        }
        return cursor;
    }

    TraceContext(BigInteger traceId, long spanId, boolean enabled, boolean stackTraceEnabled,
                        Map<String, String> parameters) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.enabled = enabled;
        this.stackTraceEnabled = stackTraceEnabled;
        this.parameters = parameters;
    }

    public BigInteger getTraceId() {
        return traceId;
    }

    public long getSpanId() {
        return spanId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isStackTraceEnabled() {
        return stackTraceEnabled;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder
                .append(traceId.toString(HEX))
                .append('/')
                .append(spanId);
        int options = (enabled ? 1 : 0) | (stackTraceEnabled ? 2 : 0);
        if (options > 0) {
            builder.append(";o=").append(options);
        }
        parameters.entrySet().stream()
                .forEach((entry) -> builder.append(';').append(entry.getKey()).append('=').append(entry.getValue()));
        return builder.toString();
    }
}
