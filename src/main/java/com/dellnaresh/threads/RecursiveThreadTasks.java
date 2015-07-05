package com.dellnaresh.threads;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class RecursiveThreadTasks {
    final RecursiveThreadExecutor es;

    final List<RecursiveThreadExecutor.Task> tasks = new ArrayList<>();

    final AtomicBoolean interrupted;

    public RecursiveThreadTasks(RecursiveThreadExecutor e) {
        this.es = e;
        interrupted = new AtomicBoolean(false);
    }

    public RecursiveThreadTasks(RecursiveThreadExecutor e, AtomicBoolean interrupted) {
        this.es = e;
        this.interrupted = interrupted;
    }

    public void execute(Runnable r) {
        RecursiveThreadExecutor.Task t = new RecursiveThreadExecutor.Task(r) {
            @Override
            public boolean interrupted() {
                return interrupted.get();
            }
        };
        tasks.add(t);
        es.execute(t);
    }

    public void waitTermination() throws InterruptedException {
        try {
            for (RecursiveThreadExecutor.Task r : tasks) {
                es.waitTermination(r);

                // we may lose some exception occured in next tasks
                if (r.e != null) {
                    if (r.e instanceof InterruptedException)
                        throw (InterruptedException) r.e;
                    else if (r.e instanceof RuntimeException)
                        throw (RuntimeException) r.e;
                    else
                        throw new RuntimeException(r.e);
                }
            }
        } catch (InterruptedException e) {
            interrupted.set(true);
            throw e;
        }
    }
}
