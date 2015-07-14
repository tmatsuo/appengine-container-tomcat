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

import com.google.apphosting.context.ContextPropagatingExecutorService;
import com.google.apphosting.trace.spi.TraceContext;
import com.google.apphosting.trace.spi.TraceWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.apphosting.trace.Span.span;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * |
 */
@RunWith(MockitoJUnitRunner.class)
public class SpanTest {

    @Mock
    public TraceWriter writer;

    private Clock clock;

    @Mock
    public Random random;

    @Captor
    public ArgumentCaptor<Span> spanCaptor;

    private TraceContext context;
    private Trace trace;

    @Before
    public void initTrace() {
        clock = new Clock() {
            private long now = 1;

            @Override
            public ZoneId getZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochSecond(now++);
            }
        };
        random = new Random() {
            private long seed = 20000L;

            @Override
            public long nextLong() {
                return seed++;
            }
        };
        context = TraceContext.parse("1234abcd/12345");
        trace = new Trace(context, writer, clock, random);
    }

    @Test
    public void simpleSpan() {
        trace.run("simple", () -> {});
        verify(writer).spanClosed(spanCaptor.capture());
        Span span = spanCaptor.getValue();
        assertEquals("simple", span.getName());
        assertEquals(20000L, span.getId());
        assertEquals(Instant.ofEpochSecond(1), span.getStartTime());
        assertEquals(Instant.ofEpochSecond(2), span.getStopTime());
    }

    @Test
    public void nestedSpan() {
        trace.run("outer", () -> span("inner", () -> {}));
        verify(writer, times(2)).spanClosed(spanCaptor.capture());
        List<Span> spans = spanCaptor.getAllValues();
        Span inner = spans.get(0);
        Span outer = spans.get(1);
        assertEquals("inner", inner.getName());
        assertEquals("outer", outer.getName());
        assertEquals(context.getSpanId(), outer.getParentId());
        assertEquals(outer.getId(), inner.getParentId());

        assertEquals(Instant.ofEpochSecond(1), outer.getStartTime());
        assertEquals(Instant.ofEpochSecond(2), inner.getStartTime());
        assertEquals(Instant.ofEpochSecond(3), inner.getStopTime());
        assertEquals(Instant.ofEpochSecond(4), outer.getStopTime());
    }

    @Test
    public void acrossThreads() {
        ExecutorService executor = new ContextPropagatingExecutorService(Executors.newSingleThreadExecutor());
        trace.run("main", () -> {
            Future<Integer> task = executor.submit(() -> {
                span("task", () -> {});
                return 1;
            });
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new AssertionError(e);
            }
        });
        verify(writer, times(2)).spanClosed(spanCaptor.capture());
        List<Span> spans = spanCaptor.getAllValues();
        Span task = spans.get(0);
        Span main = spans.get(1);
        assertEquals("task", task.getName());
        assertEquals("main", main.getName());

        assertEquals(context.getSpanId(), main.getParentId());
        assertEquals(main.getId(), task.getParentId());

        assertEquals(Instant.ofEpochSecond(1), main.getStartTime());
        assertEquals(Instant.ofEpochSecond(2), task.getStartTime());
        assertEquals(Instant.ofEpochSecond(3), task.getStopTime());
        assertEquals(Instant.ofEpochSecond(4), main.getStopTime());
    }

    @Test
    public void nullSpan() {
        Span span = span("null");
        span.close();

        assertSame(Trace.nullTrace(), span.getTrace());
        assertEquals(0, span.getParentId());
        assertEquals(0, span.getId());
        assertEquals(Instant.EPOCH, span.getStartTime());
        assertEquals(Instant.EPOCH, span.getStopTime());
    }
}
