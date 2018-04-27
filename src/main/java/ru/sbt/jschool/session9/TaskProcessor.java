package ru.sbt.jschool.session9;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProcessor implements Runnable {
    private final List<Task> taskList = new ArrayList<>();
    private final Context context = this.new ContextImpl();
    private final AtomicInteger failedTaskCtr = new AtomicInteger();
    private final AtomicInteger endedTaskCtr = new AtomicInteger();
    private final AtomicBoolean isInterrupted = new AtomicBoolean();
    private final AtomicBoolean isFinished = new AtomicBoolean();
    private int interruptedTaskCtr = 0;

    private final Runnable callback;
    private int taskToProcessCtr;

    public TaskProcessor(Runnable callback, Runnable... tasks) {
        this.callback = callback;
        taskToProcessCtr = tasks.length;

        for (Runnable job : tasks) {
            Task task = new Task(job);

            task.addTaskEndedHandler(() -> {
                endedTaskCtr.incrementAndGet();
                if (endedTaskCtr.get() == taskToProcessCtr) {
                    callback.run();
                    isFinished.compareAndSet(false, true);
                }
            });

            task.addTaskFailedHandler(() -> {
                failedTaskCtr.getAndIncrement();
            });

            taskList.add(task);
        }
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
        for (int i = 0; i < taskList.size(); i++) {
            if (isInterrupted.get()) {
                interruptedTaskCtr = taskList.size() - i;
                taskToProcessCtr = i;
                break;
            }

            Thread thread = new Thread(taskList.get(i));
            thread.start();
        }
    }
}
