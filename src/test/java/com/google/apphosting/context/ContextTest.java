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
package com.google.apphosting.context;

import org.junit.Test;

import static com.google.apphosting.context.Context.current;
import static com.google.apphosting.context.Context.emptyContext;
import static org.junit.Assert.assertSame;

public class ContextTest {
    private static final Context.Key<Object> KEY = Context.key("key", Object.class);
    private static final Context.Key<Object> KEY2 = Context.key("key2", Object.class);
    private static final Object VALUE = new Object();
    private static final Object VALUE2 = new Object();

    @Test
    public void emptyContextIsDefault() {
        assertSame(emptyContext(), current());
    }

    @Test
    public void invokedRunnableHasContext() {
        Context<?> outer = emptyContext().newChildContext(KEY, VALUE);
        outer.run(() -> {
            assertSame(outer, current());
            Context<?> inner = emptyContext().newChildContext(KEY2, VALUE2);
            inner.run(() -> assertSame(inner, current()));
            assertSame(outer, current());
        });
        assertSame(emptyContext(), current());
    }

    @Test
    public void invokedCallableHasContext() throws Exception {
        Object value = new Object();
        Context<?> outer = emptyContext().newChildContext(KEY, VALUE);
        assertSame(value, outer.call(() -> {
            assertSame(outer, current());
            Context<?> inner = emptyContext().newChildContext(KEY2, VALUE2);
            Object result = inner.call(() -> {
                assertSame(inner, current());
                return value;
            });
            assertSame(outer, current());
            return result;
        }));
        assertSame(emptyContext(), current());
    }

    @Test
    public void currentValueSearchesChain() {
        Context<?> context = emptyContext()
                .newChildContext(KEY, VALUE)
                .newChildContext(KEY2, VALUE2);
        context.run(() -> {
            assertSame(VALUE, KEY.currentValue().get());
            assertSame(VALUE2, KEY2.currentValue().get());
        });
    }
}
