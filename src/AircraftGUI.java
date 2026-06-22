/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 * 
 * Aircraft Simulation Project
 */

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

/**
 * Professional aircraft simulation controller with multithreaded architecture.
 * 
 * This class manages the entire simulation lifecycle including:
 * 1. Realistic flight dynamics with energy management
 * 2. Coordinated turns with proper bank/roll relationship
 * 3. Professional turbulence modeling and response
 * 4. Stability systems with flight envelope protection
 * 5. Day/night cycle and weather simulation
 * 
 * Architecture Diagram:
 * 
 * +-------------------+      +----------------------+      +------------------+
 * | User Input Thread  | ---> | Flight Control Logic | ---> | Simulation Thread |
 * +-------------------+      +----------------------+      +------------------+
 *                                      |                            |
 *                                      v                            v
 *                             +----------------+           +------------------+
 *                             | Weather System | <-------- | Flight Dynamics  |
 *                             +----------------+           +------------------+
 *                                      |                            |
 *                                      v                            v
 *                             +----------------+           +------------------+
 *                             | Environment   | --------> | Aircraft State   |
 *                             | Effects       |           | Management       |
 *                             +----------------+           +------------------+
 *                                                                  |
 *                                                                  v
 *                                                         +------------------+
 *                                                         | Rendering Thread |
 *                                                         +------------------+
 * 
 * Flow of Control:
 * 1. Input processing and flight controls
 * 2. Physics simulation and aircraft dynamics
 * 3. Environmental effects and weather simulation
 * 4. State management and coordination
 * 5. Rendering and visualization
 * 
 * The simulation uses a Swing-based UI to render a realistic aircraft display with
 * professional flight parameters, navigation data, and system status information.
 */
public class AircraftGUI {
    // Constants
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    
    // Thread control and performance monitoring
    private final ThreadFactory namedThreadFactory = new ThreadFactory() {
        private int counter = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("AircraftSim-" + counter++);
            t.setPriority(Thread.NORM_PRIORITY);
            t.setUncaughtExceptionHandler((thread, ex) -> {
                System.err.println("Uncaught exception in thread " + thread.getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            });
            return t;
        }
    };
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, namedThreadFactory);
    private final ExecutorService executor = Executors.newCachedThreadPool(namedThreadFactory);
    
    // Thread synchronization and control
    private volatile boolean running = true;
    private final Object environmentLock = new Object();
    private final Object aircraftLock = new Object();
    private final Object obstacleLock = new Object();
    
    // Thread performance monitoring
    private long[] threadExecutionTimes = new long[5];
    private long[] threadExecutionCounts = new long[5];
    private long monitoringStartTime;
    private final String[] threadNames = {"Environment", "Aircraft", "Obstacles", "Rendering", "Overall"};
    private boolean showPerformanceOverlay = true;
    private long lastFrameTime = 0;
    private double avgFrameTime = 16.67;
    private final DecimalFormat df = new DecimalFormat("#0.00");
    
    // Direction controls that drive aircraft orientation.
    private final DirectionControl rollControl;
    private final DirectionControl pitchControl;
    private final DirectionControl yawControl;

    // Latest values published by observers - volatile for safe cross-thread visibility.
    // The simulation thread writes these via the listener callbacks;
    // the EDT reads them in updateAircraft(). Volatile guarantees the EDT
    // always sees the most recent value without needing a lock.
    private volatile double latestRoll = 0.0;
    private volatile double latestPitch = 0.0;
    private volatile double latestYaw = 0.0;

    // Optional resource monitor
    private ResourceMonitor resourceMonitor;

    // Aircraft state variables
    private double roll = 0.0;
    private double pitch = 0.0;
    private double yaw = 0.0;
    private double flightSpeed = 250.0;
    private double currentAltitude = 11000.0;
    private double targetAltitude = 11000.0;

    // Enhanced flight dynamics variables
    private long simulationStartTime = System.currentTimeMillis();
    private double turbulenceFactor = 0.0;
    private boolean isClimbingDecision = false;
    private String currentDecision = "LEVEL FLIGHT";
    private boolean thunderstormAhead = false;
    
    // Environment variables
    private double timeOfDay = 0.5;
    private int dayNightCycleCounter = 0;
    private boolean isDayTime = true;
    
    // Animation timer
    private Timer timer;
    private Random random = new Random();
    
    // UI Components
    private JFrame frame;
    private AircraftPanel panel;
    
    // Fonts for text displays
    private Font mediumFont = new Font("Arial", Font.BOLD, 16);

    /**
     * Creates the GUI bound to the simulation's three orientation controls.
     * Registers observers on all three axes so the GUI reacts to changes
     * instead of polling getCurrentValue() every frame.
     */
    public AircraftGUI(DirectionControl rollControl,
                       DirectionControl pitchControl,
                       DirectionControl yawControl) {
        this.rollControl = rollControl;
        this.pitchControl = pitchControl;
        this.yawControl = yawControl;

        /*
         * Thread-safety note: DirectionControl.update() runs on the simulation
         * thread. The listener writes to a volatile double, which guarantees
         * the EDT will see the latest value when the Swing timer fires.
         * We never call any Swing method from inside the listener.
         */
        rollControl.addListener(c -> latestRoll = c.getCurrentValue());
        pitchControl.addListener(c -> latestPitch = c.getCurrentValue());
        yawControl.addListener(c -> latestYaw = c.getCurrentValue());
    }

    public void setResourceMonitor(ResourceMonitor monitor) {
        this.resourceMonitor = monitor;
    }

    /**
     * Adjusts the render-timer cadence in response to the host's CPU pressure.
     */
    public void setPerformanceLevel(ResourceMonitor.PerformanceLevel level) {
        SwingUtilities.invokeLater(() -> {
            if (timer == null) return;
            int newDelay;
            switch (level) {
                case MINIMAL: newDelay = 66; break;
                case REDUCED: newDelay = 33; break;
                case NORMAL:
                default:      newDelay = 16; break;
            }
            if (timer.getDelay() != newDelay) {
                timer.setDelay(newDelay);
                System.out.println("Performance level: " + level + " -> timer delay " + newDelay + "ms");
            }
        });
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            startMultithreadedSimulation();
            System.out.println("Aircraft simulation GUI is now visible");
        });
    }
    
    private void startMultithreadedSimulation() {
        monitoringStartTime = System.currentTimeMillis();
        running = true;
        
        System.out.println("Starting multithreaded simulation with performance monitoring");
        System.out.println("Toggle performance overlay with 'P' key");
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            long startTime = System.nanoTime();
            try {
                synchronized (environmentLock) {
                    updateEnvironment();
                }
            } catch (Exception ex) {
                System.err.println("Error in environment thread: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                long duration = System.nanoTime() - startTime;
                threadExecutionTimes[0] += duration;
                threadExecutionCounts[0]++;
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            long startTime = System.nanoTime();
            try {
                synchronized (aircraftLock) {
                    updateAircraft();
                }
            } catch (Exception ex) {
                System.err.println("Error in aircraft physics thread: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                long duration = System.nanoTime() - startTime;
                threadExecutionTimes[1] += duration;
                threadExecutionCounts[1]++;
            }
        }, 0, 33, TimeUnit.MILLISECONDS);
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            long startTime = System.nanoTime();
            try {
                synchronized (obstacleLock) {
                    processObstacles();
                    updateTurbulence();
                }
            } catch (Exception ex) {
                System.err.println("Error in obstacle/turbulence thread: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                long duration = System.nanoTime() - startTime;
                threadExecutionTimes[2] += duration;
                threadExecutionCounts[2]++;
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        lastFrameTime = System.currentTimeMillis();
        
        timer = new Timer(16, e -> {
            long startTime = System.nanoTime();
            long now = System.currentTimeMillis();
            long frameTime = now - lastFrameTime;
            lastFrameTime = now;
            
            avgFrameTime = (avgFrameTime * 0.95) + (frameTime * 0.05);

            if (resourceMonitor != null && dayNightCycleCounter % 60 == 0) {
                double cpu = resourceMonitor.getSystemCpuLoad();
                double proc = resourceMonitor.getProcessCpuLoad();
                double heap = resourceMonitor.getHeapUsedFraction();
                System.out.printf("[resources] sysCPU=%5.1f%%  procCPU=%5.1f%%  heap=%5.1f%%  level=%s  fps~%d%n",
                        cpu * 100, proc * 100, heap * 100,
                        resourceMonitor.getCurrentLevel(),
                        timer != null ? 1000 / Math.max(1, timer.getDelay()) : 0);
            }

            dayNightCycleCounter++;
            if (dayNightCycleCounter > 300) {
                dayNightCycleCounter = 0;
                timeOfDay += 0.1;
                if (timeOfDay > 2.0) timeOfDay = 0;
                isDayTime = timeOfDay < 1.0 || timeOfDay > 1.8;
            }
            
            if (panel != null) {
                synchronized (aircraftLock) {
                    panel.setRoll(roll);
                    panel.setPitch(pitch);
                    panel.setYaw(yaw);
                    panel.setFlightSpeed(flightSpeed);
                    panel.setAltitude(currentAltitude);
                    panel.setTurbulenceFactor(turbulenceFactor);
                }
                
                synchronized (environmentLock) {
                    panel.setTimeOfDay(timeOfDay);
                    panel.setIsDayTime(isDayTime);
                }
                
                synchronized (obstacleLock) {
                    panel.setThunderstormAhead(thunderstormAhead);
                    panel.setTurbulenceFactor(turbulenceFactor);
                    panel.setCurrentDecision(currentDecision);
                }
                
                panel.setThreadExecutionTimes(threadExecutionTimes);
                panel.setThreadExecutionCounts(threadExecutionCounts);
                panel.setThreadNames(threadNames);
                panel.setShowPerformanceOverlay(showPerformanceOverlay);
                panel.setAvgFrameTime(avgFrameTime);
                panel.setMonitoringStartTime(monitoringStartTime);
                
                panel.repaint();
            }
            
            long duration = System.nanoTime() - startTime;
            threadExecutionTimes[3] += duration;
            threadExecutionCounts[3]++;
        });
        
        timer.start();
        
        scheduler.schedule(() -> {
            System.out.println("Multithreaded simulation running with " + threadNames.length + " threads");
            for (int i = 0; i < threadNames.length; i++) {
                if (threadExecutionCounts[i] > 0) {
                    double avgExecTimeMs = (threadExecutionTimes[i] / (double)threadExecutionCounts[i]) / 1_000_000.0;
                    System.out.println(threadNames[i] + " thread: " + df.format(avgExecTimeMs) + "ms avg");
                }
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    private void updateEnvironment() {
    }
    
    /**
     * Reads roll/pitch/yaw from the volatile fields updated by observers
     * instead of polling DirectionControl directly.
     */
    private void updateAircraft() {
        long currentTime = System.currentTimeMillis();
        double timeSeconds = (currentTime - simulationStartTime) / 1000.0;

        // Read from volatile fields updated by observers instead of polling
        roll = latestRoll;
        pitch = latestPitch;
        yaw = latestYaw;

        double altitudeDifference = targetAltitude - currentAltitude;
        double climbRate = Math.min(Math.max(altitudeDifference / 10.0, -500), 500);
        currentAltitude += climbRate / 60.0;
        currentAltitude += (random.nextDouble() - 0.5) * 5.0;

        double baseSpeed = 250.0;
        double speedVariation = Math.sin(timeSeconds * 0.1) * 15.0;
        flightSpeed = baseSpeed + speedVariation + (random.nextDouble() - 0.5) * 5.0;
        flightSpeed = Math.max(180, Math.min(320, flightSpeed));
    }
    
    private void processObstacles() {
        thunderstormAhead = random.nextInt(100) < 5;
        
        if (thunderstormAhead) {
            targetAltitude = 30000;
            
            int direction = targetAltitude > currentAltitude ? 1 : -1;
            if (Math.abs(targetAltitude - currentAltitude) > 200) {
                isClimbingDecision = true;
                if (direction > 0) {
                    currentDecision = "CLIMBING";
                } else {
                    currentDecision = "DESCENDING";
                }
            } else {
                currentDecision = "LEVEL FLIGHT";
                isClimbingDecision = false;
            }
        } else {
            targetAltitude = 11000;
            currentDecision = "LEVEL FLIGHT";
            isClimbingDecision = false;
        }
    }
    
    private void updateTurbulence() {
        if (thunderstormAhead) {
            turbulenceFactor = 10.0 + random.nextDouble() * 15.0;
            synchronized (aircraftLock) {
                if (thunderstormAhead && Math.abs(turbulenceFactor) > 10) {
                    currentDecision = "TURBULENCE!";
                }
            }
        } else {
            turbulenceFactor = Math.max(0, turbulenceFactor * 0.95 - 0.5);
            if (random.nextInt(100) < 5) {
                turbulenceFactor = random.nextDouble() * 7.0;
            }
        }
    }
    
    private void createAndShowGUI() {
        frame = new JFrame("Aircraft Simulation with Performance Monitoring");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    showPerformanceOverlay = !showPerformanceOverlay;
                    System.out.println("Performance overlay: " + 
                                      (showPerformanceOverlay ? "ON" : "OFF"));
                }
            }
        };
        
        panel = new AircraftPanel();
        panel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        panel.setFocusable(true);
        panel.addKeyListener(keyListener);
        
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        panel.requestFocusInWindow();
        frame.addKeyListener(keyListener);
    }
    
    public void shutdown() {
        running = false;
        System.out.println("Shutting down aircraft simulation...");
        
        if (timer != null) {
            timer.stop();
        }
        
        scheduler.shutdown();
        executor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n==== THREAD PERFORMANCE STATISTICS ====");
        System.out.println("Total simulation time: " + 
                          ((System.currentTimeMillis() - monitoringStartTime) / 1000.0) + " seconds");
        
        for (int i = 0; i < threadNames.length; i++) {
            if (threadExecutionCounts[i] > 0) {
                double avgExecTimeMs = (threadExecutionTimes[i] / (double)threadExecutionCounts[i]) / 1_000_000.0;
                System.out.println(threadNames[i] + " Thread:" +
                                  "\n  Total calls: " + threadExecutionCounts[i] +
                                  "\n  Average execution time: " + df.format(avgExecTimeMs) + "ms" +
                                  "\n  Total time: " + df.format(threadExecutionTimes[i] / 1_000_000_000.0) + "s" +
                                  "\n  CPU %: " + df.format((threadExecutionTimes[i] / 1_000_000.0) / 
                                             (System.currentTimeMillis() - monitoringStartTime) * 100) + "%");
            }
        }
        System.out.println("====================================\n");
    }
    
    public static Thread createGUIUpdateThread(AircraftGUI gui, AtomicBoolean running) {
        return new Thread(() -> {
            try {
                while (running.get()) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                gui.shutdown();
            }
        }, "GUI-Update");
    }
    
    private Runnable quitAction;
    
    public void setQuitAction(Runnable action) {
        this.quitAction = action;
    }
    
    public void executeQuitAction() {
        if (quitAction != null) {
            quitAction.run();
        }
    }
}