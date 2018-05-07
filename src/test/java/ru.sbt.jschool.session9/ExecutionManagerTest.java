package ru.sbt.jschool.session9;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecutionManagerTest {
    @Test
    public void testCompletedFailedCallback() throws Exception {
        final int RUNNABLE_CNT = 10_000;
        final int FAILED_CNT = RUNNABLE_CNT / 2;
        Runnable[] list = new Runnable[RUNNABLE_CNT];
        AtomicInteger inc = new AtomicInteger();
        AtomicBoolean callbackCheck = new AtomicBoolean();

        Runnable callback = () -> {
            callbackCheck.set(true);
        };

        for (int i = 0; i < RUNNABLE_CNT; i++) {
            list[i] = () -> {

                if (inc.getAndIncrement() >= FAILED_CNT) {
                    throw new RuntimeException();
                }
            };
        }
        ExecutionManagerImpl executionManager = new ExecutionManagerImpl();
        Context context = executionManager.execute(callback, list);
        assertEquals(false, callbackCheck.get());
        while (!context.isFinished()) {
        }
        assertEquals(RUNNABLE_CNT, inc.get());
        assertEquals(RUNNABLE_CNT,context.getCompletedTaskCount());
        assertEquals(FAILED_CNT, context.getFailedTaskCount());
        assertEquals(true, callbackCheck.get());

    }

    @Test
    public void testInterrupt() throws Exception {
        final int RUNNABLE_CNT = 100_000;
        Runnable[] list = new Runnable[RUNNABLE_CNT];
        AtomicInteger inc = new AtomicInteger();
        AtomicBoolean callbackCheck = new AtomicBoolean();

        Runnable callback = () -> {
            callbackCheck.set(true);
        };

        for (int i = 0; i < RUNNABLE_CNT; i++) {
            list[i] = () -> {
                inc.getAndIncrement();
            };
        }
        ExecutionManagerImpl executionManager = new ExecutionManagerImpl();
        Context context = executionManager.execute(callback, list);
        Thread.sleep(10);
        assertEquals(false, callbackCheck.get());
        assertEquals(0, context.getInterruptedTaskCount());
        context.interrupt();
        while (!context.isFinished()) {
        }
        assertEquals(context.getCompletedTaskCount(), inc.get());
        assertTrue(context.getInterruptedTaskCount() > 0);
        assertTrue(context.getCompletedTaskCount() < RUNNABLE_CNT );
        assertEquals(true, callbackCheck.get());
    }




}
