package ru.sbt.jschool.session9;

public class ExecutionManagerImpl implements ExecutionManager {

    @Override
    public Context execute(Runnable callback, Runnable... tasks) {
        TaskProcessor taskProcessor = new TaskProcessor(callback, tasks);
        Thread thread = new Thread(taskProcessor);
        thread.start();
        return taskProcessor.getContext();
    }
}
