package ru.sbt.jschool.session9;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProcessor implements Runnable {
    private static final int MAX_THREAD_CNT = 3;

    private final Context context = this.new ContextImpl();
    private final AtomicInteger failedTaskCtr = new AtomicInteger();
    private final AtomicInteger endedTaskCtr = new AtomicInteger();
    private final AtomicInteger execTaskCtr = new AtomicInteger();
    private final AtomicBoolean isInterrupted = new AtomicBoolean();
    private final AtomicBoolean isFinished = new AtomicBoolean();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_CNT);
    private final Runnable callback;
    private final ArrayList<Task> taskList;
    private final AtomicInteger taskCtr;

    private int interruptedTaskCtr;

    public TaskProcessor(Runnable callback, Runnable... tasks) {
        this.callback = callback;
        taskCtr = new AtomicInteger(tasks.length);
        taskList = new ArrayList<>(tasks.length);

        for (Runnable job : tasks) {
            Task task = new Task(job);

            task.addTaskEndedHandler(() -> {
                if (endedTaskCtr.incrementAndGet() == taskCtr.get()) {
                    handleCompletedEvent();
                }
            });

            task.addTaskFailedHandler(() -> {
                failedTaskCtr.getAndIncrement();
            });

            taskList.add(task);
        }
    }

    private void handleCompletedEvent() {
        executor.submit(() -> {
            callback.run();
            isFinished.compareAndSet(false, true);
        });
    }

    private class ContextImpl implements Context {
        @Override
        public int getCompletedTaskCount() {
            return endedTaskCtr.get();
        }

        @Override
        public int getFailedTaskCount() {
            return failedTaskCtr.get();
        }

        @Override
        public int getInterruptedTaskCount() {
            return interruptedTaskCtr;
        }

        @Override
        public void interrupt() {
            isInterrupted.compareAndSet(false, true);
            interruptedTaskCtr = taskList.size() - execTaskCtr.get();
            taskCtr.getAndAdd(-interruptedTaskCtr);
        }

        @Override
        public boolean isFinished() {
            return isFinished.get();
        }
    }

    public Context getContext() {
        return context;
    }


    @Override
    public void run() {
        if (taskCtr.get() == 0) {
            handleCompletedEvent();
            return;
        }

        for (Task task : taskList) {
            execTaskCtr.incrementAndGet();
            executor.submit(task);
            if (isInterrupted.get()) {
                break;
            }
        }
    }
}
