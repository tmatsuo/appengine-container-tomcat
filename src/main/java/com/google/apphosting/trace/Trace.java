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

import com.google.apphosting.trace.spi.TraceContext;
import com.google.apphosting.trace.spi.TraceWriter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;

/**
 * |
 */
public final class Trace {

    private static final Trace NULL_TRACE;
    static {
        TraceWriter writer = (span) -> {};
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.EPOCH;
            }
        };
        Random random = new Random() {
            @Override
            public long nextLong() {
                return 0;
            }
        };
        NULL_TRACE = new Trace(TraceContext.nullContext(), writer, clock, random);
    }

    public static Trace nullTrace() {
        return NULL_TRACE;
    }

    private final TraceContext context;
    private final TraceWriter writer;
    private final Clock clock;
    private final Random random;

    public Trace(TraceContext context, TraceWriter writer, Clock clock, Random random) {
        this.context = context;
        this.writer = writer;
        this.clock = clock;
        this.random = random;
    }

    public void run(String spanName, Runnable runnable) {
        Span span = new Span(this, context.getSpanId(), nextSpanId(), spanName);
        span.run(runnable);
    }

    Instant instant() {
        return clock.instant();
    }

    long nextSpanId() {
        return random.nextLong();
    }

    void spanClosed(Span span) {
        writer.spanClosed(span);
    }
}
