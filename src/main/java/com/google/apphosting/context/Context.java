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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A general purpose holder for Context associated with an executing Thread.
 *
 * <p>Every context specifies a value identified by a specific Key. The value can be looked up by key either in the
 * currently executing Context or in a specific static Context.
 */
public final class Context<T> {

    private static final ThreadLocal<Context<?>> threadContext = new ThreadLocal<>();
    private static final Context<?> EMPTY_CONTEXT = new Context<>(null, null, null);

    private final Context<?> parent;
    private final Key<T> key;
    private final T value;

    /**
     * Returns the Context for the current Thread.
     */
    public static Context<?> current() {
        Context<?> context = threadContext.get();
        return (context == null) ? emptyContext() : context;
    }

    /**
     * Returns an empty context with no defined key or value.
     */
    public static Context<?> emptyContext() {
        return EMPTY_CONTEXT;
    }

    private Context(Context<?> parent, Key<T> key, T value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    /**
     * Create a new child Context defining the value of a key.
     *
     * @param key   the identifier for the value
     * @param value the value
     */
    public <C> Context<C> newChildContext(Key<C> key, C value) {
        return new Context<>(this, key, value);
    }

    /**
     * Execute the supplied Runnable in the scope of this Context.
     */
    public void run(Runnable runnable) {
        Context<?> parent = threadContext.get();
        try {
            threadContext.set(this);
            runnable.run();
        } finally {
            if (parent != null) {
                threadContext.set(parent);
            } else {
                threadContext.remove();
            }
        }
    }

    /**
     * Wraps a Runnable so that it runs in this Context.
     */
    public Runnable wrap(Runnable runnable) {
        return () -> Context.this.run(runnable);
    }

    /**
     * Execute the supplied Callable in the scope of this Context.
     */
    public <R> R call(Callable<R> callable) throws Exception {
        Context<?> parent = threadContext.get();
        try {
            threadContext.set(this);
            return callable.call();
        } finally {
            if (parent != null) {
                threadContext.set(parent);
            } else {
                threadContext.remove();
            }
        }
    }

    /**
     * Wraps a Callable to that it runs in this Context.
     */
    public <R> Callable<R> wrap(Callable<R> callable) {
        return () -> Context.this.call(callable);
    }

    /**
     * Return a new Key for a specific name.
     *
     * @param name      the name that identifies the key
     * @param valueType the type of value allowed for this key
     */
    public static <T> Key<T> key(String name, Class<T> valueType) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(valueType, "valueType");
        return new Key<>(name, valueType);
    }

    /**
     * An identifier for a value that can be associated with a context.
     *
     * @param <T> the type of value allowed
     */
    public static final class Key<T> {
        private final String name;
        private final Class<T> valueType;

        private Key(String name, Class<T> valueType) {
            this.name = name;
            this.valueType = valueType;
        }

        /**
         * The name of this key.
         */
        public String getName() {
            return name;
        }

        /**
         * The type of value this key allows.
         */
        public Class<T> getValueType() {
            return valueType;
        }

        /**
         * Returns the current Context's value for this key.
         */
        public Optional<T> currentValue() {
            Context<?> current = current();
            if (current != null) {
                return contextValue(current);
            } else {
                return Optional.empty();
            }
        }

        /**
         * Returns the value of this key in a specific Context.
         *
         * @param context the Context used to look up the value
         */
        public Optional<T> contextValue(Context<?> context) {
            Objects.requireNonNull(context, "context");
            do {
                if (this.equals(context.key)) {
                    return Optional.of(valueType.cast(context.value));
                }
                context = context.parent;
            } while (context != null);
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key<?> other = (Key<?>) o;
            return Objects.equals(name, other.name) && Objects.equals(valueType, other.valueType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, valueType);
        }
    }
}
