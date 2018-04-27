package ru.sbt.jschool.session9;

import java.util.ArrayList;
import java.util.List;

public class Task implements Runnable {
    private List<TaskEndedHandler> taskEndedHandlers = new ArrayList<>();
    private List<TaskFailedHandler> taskFailedHandlers = new ArrayList<>();
    private Runnable job;

    public void addTaskEndedHandler(TaskEndedHandler taskEndedHandler) {
        taskEndedHandlers.add(taskEndedHandler);
    }

    public void addTaskFailedHandler(TaskFailedHandler taskFailedHandler) {
        taskFailedHandlers.add(taskFailedHandler);
    }

    public Task(Runnable job) {
        this.job = job;
    }

    @Override
    public void run() {
        try {
            job.run();
        } catch (Exception e) {
            for (TaskFailedHandler failedHandler : taskFailedHandlers) {
                failedHandler.handle();
            }
        } finally {
            for (TaskEndedHandler endedHandler : taskEndedHandlers) {
                endedHandler.handle();
            }
        }
    }
}