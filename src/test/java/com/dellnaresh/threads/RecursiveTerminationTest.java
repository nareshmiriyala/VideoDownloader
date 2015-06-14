package com.dellnaresh.threads;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecursiveTerminationTest {

    // create 4 working threads. all tasks will be distributed between.
    // cross instances, may run simultaneously multiple independent task sets
    static final RecursiveThreadExecutor threadsHolder = new RecursiveThreadExecutor(4);

    // synchronized flag for proper tasks interruption. per tasks group flag.
    static AtomicBoolean interrupted = new AtomicBoolean(false);

    // Recursive function, which do some job, and produce more job
    public static int recursiveTaks(final int level, final int maxLevel) {
        RecursiveThreadTasks threadTasks = new RecursiveThreadTasks(threadsHolder, interrupted);

        final AtomicInteger r = new AtomicInteger(0);

        if (level >= maxLevel)
            return r.get();

        for (int i = 0; i < 100; i++) {
            // put first task
            threadTasks.execute(new Runnable() {
                @Override
                public void run() {
                    for (int k = 0; k < 10000; k++)
                        r.incrementAndGet();

                    // create depened tasks which should ends before current
                    // task, which can also create another tasks
                    recursiveTaks(level + 1, maxLevel);
                }
            });
        }
        try {
            threadTasks.waitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        return r.get();
    }

    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int result = recursiveTaks(0, 5);
                System.out.println(result);
            }
        }, "Main Thread");
        t.start();
        try {
            t.join(5000);
            if (t.isAlive()) {
                t.interrupt();
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // threadsHolder.close();
    }
}