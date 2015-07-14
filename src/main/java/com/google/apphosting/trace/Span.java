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

import com.google.apphosting.context.Context;
import com.google.apphosting.trace.spi.TraceContext;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 *
 */
public final class Span implements AutoCloseable {

    static final Context.Key<Span> SPAN_KEY = Context.key(Span.class.getName(), Span.class);

    public static void span(String name, Runnable runnable) {
        span(name).run(runnable);
    }

    public static <T> T span(String name, Callable<T> callable) throws Exception {
        return span(name).call(callable);
    }

    public static Span span(String name) {
        Optional<Span> currentSpan = SPAN_KEY.currentValue();
        if (currentSpan.isPresent()) {
            Span span =  currentSpan.get();
            Trace trace = span.trace;
            return new Span(trace, span.id, trace.nextSpanId(), name);
        } else {
            Trace trace = Trace.nullTrace();
            return new Span(trace, TraceContext.nullContext().getSpanId(), trace.nextSpanId(), name);
        }
    }

    private final Trace trace;
    private final long parentId;
    private final long id;
    private final String name;
    private final Instant startTime;
    private volatile Instant stopTime;

    Span(Trace trace, long parentId, long id, String name) {
        this.trace = trace;
        this.parentId = parentId;
        this.id = id;
        this.name = name;
        this.startTime = trace.instant();
    }

    public void run(Runnable runnable) {
        try {
            Context.current().newChildContext(SPAN_KEY, this).run(runnable);
        } finally {
            close();
        }
    }

    public <T> T call(Callable<T> callable) throws Exception {
        try {
            return Context.current().newChildContext(SPAN_KEY, this).call(callable);
        } finally {
            close();
        }
    }

    public String getName() {
        return name;
    }

    public Trace getTrace() {
        return trace;
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getStopTime() {
        return stopTime;
    }

    @Override
    public void close() {
        if (this.stopTime == null) {
            this.stopTime = trace.instant();
            trace.spanClosed(this);
        }
    }
}
