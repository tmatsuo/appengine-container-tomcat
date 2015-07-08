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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * An ExecutorService that executes tasks using the current context of the Thread submitting the task.
 */
public class ContextPropagatingExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    public ContextPropagatingExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(Context.current().wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(Context.current().wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(Context.current().wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Context<?> context = Context.current();
        List<Callable<T>> wrapped = tasks.stream().map(context::wrap).collect(Collectors.toList());
        return delegate.invokeAll(wrapped);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        Context<?> context = Context.current();
        List<Callable<T>> wrapped = tasks.stream().map(context::wrap).collect(Collectors.toList());
        return delegate.invokeAll(wrapped, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Context<?> context = Context.current();
        List<Callable<T>> wrapped = tasks.stream().map(context::wrap).collect(Collectors.toList());
        return delegate.invokeAny(wrapped);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        Context<?> context = Context.current();
        List<Callable<T>> wrapped = tasks.stream().map(context::wrap).collect(Collectors.toList());
        return delegate.invokeAny(wrapped, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(Context.current().wrap(command));
    }
}
