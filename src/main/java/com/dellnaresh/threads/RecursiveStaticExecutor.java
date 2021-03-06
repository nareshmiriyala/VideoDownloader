package com.dellnaresh.threads;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * keep one instance of RecursiveThreadExecutor
 */
class RecursiveStaticExecutor {
    private static final AtomicInteger counter = new AtomicInteger();
    private static RecursiveThreadExecutor es = null;

    public RecursiveStaticExecutor() {
        counter.incrementAndGet();
    }

    @Override
    protected void finalize() {
        if (counter.decrementAndGet() == 0) {
            synchronized (counter) {
                if (es != null) {
                    es.close();
                    es = null;
                }
            }
        }
    }

    public RecursiveThreadExecutor getInstance() {
        synchronized (counter) {
            if (es == null)
                es = new RecursiveThreadExecutor();

            return es;
        }
    }
}
