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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * |
 */
public class TraceContextTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void stringFormatWithTraceAndSpan() {
        TraceContext context =
                new TraceContext(new BigInteger("12345678", 16), 12345L, false, false, Collections.emptyMap());
        assertEquals("12345678/12345", context.toString());
    }

    @Test
    public void stringFormatWithTraceEnabled() {
        TraceContext context =
                new TraceContext(new BigInteger("1234abcd", 16), 12345L, true, false, Collections.emptyMap());
        assertEquals("1234abcd/12345;o=1", context.toString());
    }

    @Test
    public void stringFormatWithTraceAndStackEnabled() {
        TraceContext context =
                new TraceContext(new BigInteger("1234abcd", 16), 12345L, true, true, Collections.emptyMap());
        assertEquals("1234abcd/12345;o=3", context.toString());
    }

    @Test
    public void stringFormatWithEverything() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("foo", "bar");
        params.put("foo1", "bar1");
        TraceContext context =
                new TraceContext(new BigInteger("1234abcd", 16), 12345L, true, true, params);
        assertEquals("1234abcd/12345;o=3;foo=bar;foo1=bar1", context.toString());
    }

    @Test
    public void parseMinimalTraceId() {
        TraceContext context = TraceContext.parse(" 1234abcd / 12345 ");
        assertEquals(new BigInteger("1234abcd", 16), context.getTraceId());
        assertEquals(12345L, context.getSpanId());
        assertFalse(context.isEnabled());
        assertFalse(context.isStackTraceEnabled());
    }

    @Test
    public void parseEnabledTraceId() {
        TraceContext context = TraceContext.parse(" 1234abcd / 12345 ; o=1");
        assertEquals(new BigInteger("1234abcd", 16), context.getTraceId());
        assertEquals(12345L, context.getSpanId());
        assertTrue(context.isEnabled());
        assertFalse(context.isStackTraceEnabled());
    }

    @Test
    public void parseStackEnabledTraceId() {
        TraceContext context = TraceContext.parse(" 1234abcd / 12345 ; o=3");
        assertEquals(new BigInteger("1234abcd", 16), context.getTraceId());
        assertEquals(12345L, context.getSpanId());
        assertTrue(context.isEnabled());
        assertTrue(context.isStackTraceEnabled());
    }

    @Test
    public void parseInvalidTraceId() {
        thrown.expect(IllegalArgumentException.class);
        TraceContext.parse(" 1234?abcd ");
    }

    @Test
    public void parseMissingSpanId() {
        thrown.expect(IllegalArgumentException.class);
        TraceContext.parse(" 1234abcd ");
    }

    @Test
    public void parseInvalidSpanId() {
        thrown.expect(IllegalArgumentException.class);
        TraceContext.parse(" 1234abcd / 12x45");
    }
}
