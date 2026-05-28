/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 *
 * Aircraft Simulation Project
 */

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Live OS resource monitor.
 *
 * Runs on its own thread and polls process / system CPU load and heap usage at
 * a fixed cadence using {@link com.sun.management.OperatingSystemMXBean}, which
 * is available on the HotSpot JVMs shipped on Ubuntu, Windows, and macOS.
 *
 * After each sample the monitor decides on a {@link PerformanceLevel} and
 * invokes the supplied listener whenever the level changes, so the GUI can
 * throttle its frame rate / quality without polling.
 */
public class ResourceMonitor implements Runnable {

    public enum PerformanceLevel {
        NORMAL,   // CPU low or moderate, headroom available
        REDUCED,  // CPU getting busy - cut frame rate
        MINIMAL   // Host is hot - cut FPS and disable extras
    }

    private final long sampleIntervalMs;
    private final Consumer<PerformanceLevel> levelListener;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Latest samples (volatile so other threads can read without locking).
    private volatile double systemCpuLoad = -1.0;   // 0.0 .. 1.0
    private volatile double processCpuLoad = -1.0;  // 0.0 .. 1.0
    private volatile double heapUsedFraction = 0.0; // 0.0 .. 1.0
    private volatile PerformanceLevel currentLevel = PerformanceLevel.NORMAL;

    // OS bean - obtained reflectively so the code still compiles if the
    // com.sun.management interface isn't on the classpath.
    private final com.sun.management.OperatingSystemMXBean osBean;

    public ResourceMonitor(long sampleIntervalMs, Consumer<PerformanceLevel> levelListener) {
        this.sampleIntervalMs = sampleIntervalMs;
        this.levelListener = levelListener;
        com.sun.management.OperatingSystemMXBean bean = null;
        try {
            bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        } catch (ClassCastException ignored) {
            // Non-HotSpot JVM that doesn't expose com.sun.management. We'll fall
            // back to heap-only monitoring below.
        }
        this.osBean = bean;
    }

    public Thread start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("ResourceMonitor already running");
        }
        Thread t = new Thread(this, "ResourceMonitor");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        PerformanceLevel last = currentLevel;
        // Call the listener once with the initial level so the GUI starts in a
        // known state.
        if (levelListener != null) levelListener.accept(last);

        while (running.get()) {
            sample();
            PerformanceLevel next = classify();
            if (next != last) {
                currentLevel = next;
                if (levelListener != null) levelListener.accept(next);
                last = next;
            }
            try {
                Thread.sleep(sampleIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sample() {
        if (osBean != null) {
            // getCpuLoad() returns -1 the very first time it's called; that's
            // fine - we just leave the previous value.
            double sys = osBean.getCpuLoad();
            double proc = osBean.getProcessCpuLoad();
            if (sys >= 0) systemCpuLoad = sys;
            if (proc >= 0) processCpuLoad = proc;
        }

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long used = rt.totalMemory() - rt.freeMemory();
        heapUsedFraction = max > 0 ? (double) used / max : 0.0;
    }

    private PerformanceLevel classify() {
        // Use whichever CPU signal we have. System load is the one we really
        // care about for "is the host hot"; fall back to process load.
        double cpu = systemCpuLoad >= 0 ? systemCpuLoad : processCpuLoad;

        if (cpu >= 0.85 || heapUsedFraction >= 0.90) return PerformanceLevel.MINIMAL;
        if (cpu >= 0.70 || heapUsedFraction >= 0.75) return PerformanceLevel.REDUCED;
        return PerformanceLevel.NORMAL;
    }

    public double getSystemCpuLoad()  { return systemCpuLoad; }
    public double getProcessCpuLoad() { return processCpuLoad; }
    public double getHeapUsedFraction() { return heapUsedFraction; }
    public PerformanceLevel getCurrentLevel() { return currentLevel; }
}
