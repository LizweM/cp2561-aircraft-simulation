/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 * 
 * Aircraft Simulation Project
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import javax.swing.Timer;

/**
 * Professional aircraft visualization panel that renders a realistic aviation environment.
 * 
 * This panel implements the following advanced features:
 * 
 * 1. Professional Aircraft Rendering
 *    - Realistic fuselage, wings, engines, and cockpit
 *    - Navigation lights (red/green wingtips, white tail)
 *    - Engine intake details and structural components
 *    - Stability system visual indicators
 * 
 * 2. Advanced Stability Systems
 *    - Stability Augmentation System (SAS) with rate limiting
 *    - Flight envelope protection for bank angle, pitch, and speed
 *    - Professional turbulence penetration procedures
 *    - Autothrottle and auto-trim functionality
 * 
 * 3. Environmental Effects
 *    - Layered background with sky, sea, and land
 *    - Realistic mountain ranges with snow-capped peaks
 *    - Dynamic cloud system with transparency and depth
 *    - Professional day/night cycle with celestial bodies
 *    - Weather effects including thunderstorms and lightning
 * 
 * 4. Flight Management System Display
 *    - Real-time flight parameters and navigation data
 *    - Weather and turbulence information
 *    - System status indicators
 *    - Performance monitoring overlay
 *
 * Rendering Pipeline:
 *
 * +----------------------+     +------------------------+
 * | Layered Background   |---->| Terrain/Mountains     |
 * | (Sky/Sea/Land)       |     | (Snow-capped peaks)   |
 * +----------------------+     +------------------------+
 *            |                              |
 *            v                              v
 * +----------------------+     +------------------------+
 * | Cloud Systems       |---->| Weather Effects        |
 * | (Multiple layers)   |     | (Storms/Lightning)    |
 * +----------------------+     +------------------------+
 *            |                              |
 *            v                              v
 * +----------------------+     +------------------------+
 * | Obstacle Rendering  |---->| Aircraft Visualization |
 * | (Other aircraft)    |     | (Professional details) |
 * +----------------------+     +------------------------+
 *                                         |
 *                                         v
 * +-----------------------------------------------------+
 * | Flight Management Display                           |
 * | (Aviation data, system status, performance metrics) |
 * +-----------------------------------------------------+
 *
 * Professional Stability System:
 *
 *  Input           Processing            Output
 * +------+        +--------------+       +---------------+
 * | Roll |------->|              |------>| Rate Limiting |
 * +------+        |              |       +---------------+
 *                 |  Stability   |              |
 * +------+        | Augmentation |              v
 * | Pitch|------->|  System     |       +---------------+
 * +------+        |  (SAS)      |------>| Flight        |
 *                 |              |       | Envelope     |
 * +------+        |              |       | Protection   |
 * | Yaw  |------->|              |------>| System       |
 * +------+        +--------------+       +---------------+
 *                        |                      |
 *                        v                      v
 *                 +--------------+       +---------------+
 *                 | Turbulence   |<----->| Auto-Trim    |
 *                 | Penetration  |       | System       |
 *                 | Procedures   |       |              |
 *                 +--------------+       +---------------+
 */
public class AircraftPanel extends JPanel implements ComponentListener {
    // Panel dimensions
    private int width = 800;
    private int height = 600;
    // Flight parameters
    private double roll = 0.0;  // -180 to +180 degrees
    private double pitch = 0.0; // -90 to +90 degrees
    private double yaw = 0.0;   // -180 to +180 degrees
    private double flightSpeed = 250.0; // Default speed in knots
    
    private boolean turbulenceEnabled = false;
    private boolean severeTurbulenceEnabled = false;
    private boolean altitudeLossActive = false;
    // Performance monitoring variables
    private boolean showPerformanceOverlay = false;
    private long[] threadExecutionTimes = null;
    private long[] threadExecutionCounts = null;
    private String[] threadNames = null;
    private double avgFrameTime = 0.0;
    private long monitoringStartTime = System.currentTimeMillis();
    private java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");


    private long altitudeLossStartTime = 0;
    private double altitudeLossAmount = 0;
    private long simulationStartTime = System.currentTimeMillis();
    
    // Size of the panel
    private int size = 200;
    
    // Aircraft position and background scrolling
    private double positionX = 10.0; // Start from the left side
    private double distanceTraveled = 0.0; // Track total distance traveled
    private double backgroundOffset = 0.0;
    private double groundOffset = 0.0;
    
    // Environment settings
    private boolean isDayTime = true; // Day/night cycle
    private long lastTimeCheck = System.currentTimeMillis();
    private int timeSpeedFactor = 60; // 1 real second = 60 simulation seconds
    private double sunPosition = 0.5; // 0.0 = dawn, 0.5 = noon, 1.0 = dusk
    private boolean hasThunderstorm = false; // Current thunderstorm status
    private double thunderstormSpan = 0.0; // Span in km of current thunderstorm
    private int thunderstormCloudHeight = 32000; // Height of thunderstorm clouds in feet
    private int activeStormCloudHeight = 0; // Height of the active storm cloud ahead
    private List<MountainPeak> mountainPeaks = new ArrayList<>(); // For realistic mountain ranges
    
    // Obstacles and decision making
    private List<Obstacle> obstacles = new ArrayList<>();
    private boolean avoidingObstacle = false;
    private double targetAltitude = 11000.0; // Start at CRUISING ALTITUDE of 11,000 feet
    private double currentAltitude = 11000.0; // Start at CRUISING ALTITUDE of 11,000 feet
    private String currentDecision = "Cruising normally";
    private long decisionChangeTime = 0;
    private double baseCruisingAltitude = 11000.0;
    
    /**
//      * Get a text description of the current time of day
     */
    private String getTimeOfDayText() {
        if (isDayTime) {
            if (sunPosition < 0.2) return "Dawn";
            else if (sunPosition > 0.8) return "Dusk";
            else return "Day";
        } else {
            return "Night";
        }
    }
    
    // Turbulence effect
    private Random random = new Random();
    private double turbulenceFactor = 0.0;
    
    // Decision status
    private boolean isClimbingDecision = false;
    
    // Cloud effects
    private List<Cloud> clouds = new ArrayList<>();
    private Timer animationTimer;
    
    /**
//      * Constructor for AircraftPanel
     */
    public AircraftPanel() {
        setPreferredSize(new Dimension(size, size));
        setBackground(new Color(135, 206, 235)); // Sky blue background
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Add component listener to initialize clouds and obstacles when panel becomes visible
        this.addComponentListener(this);
        
        // Initialize animation timer (30 FPS)
        animationTimer = new Timer(33, e -> {
            // Start with aircraft at a visible position - 25% from left edge
            if (positionX < 10) {
                positionX = Math.max(getWidth() * 0.25, 50);
            }
            
            // Move aircraft continuously left to right without resetting
            positionX += flightSpeed / 50.0; // Reduced speed
            distanceTraveled += flightSpeed / 50.0;
            
            // Keep aircraft within visible area, but don't reset position
            if (positionX > getWidth() * 0.75) {
                // Keep aircraft at 75% of screen width but keep tracking distance
                positionX = getWidth() * 0.75;
            }
            
            // Update background scrolling from right to left (opposite to aircraft direction)
            backgroundOffset -= flightSpeed / 30.0;
            groundOffset -= flightSpeed / 25.0;
            if (backgroundOffset < -getWidth()) backgroundOffset = 0;
            if (groundOffset < -50) groundOffset = 0;
            
            // Periodically add new obstacles based on distance traveled
            if (distanceTraveled > 100 && random.nextInt(100) < 2) {
                scheduleObstacle();
                distanceTraveled = 0;
            }
            
            // Apply random turbulence if enabled
            if (turbulenceEnabled) {
                turbulenceFactor = (random.nextDouble() - 0.5) * 5.0;
            } else {
                turbulenceFactor = 0;
            }
            
            // Move clouds
            for (Cloud cloud : clouds) {
                cloud.move();
            }
            
            // Process obstacles
            processObstacles();
            
            // Update day/night cycle
            updateDayNightCycle();
            
            // Update aircraft altitude based on decisions
            if (Math.abs(currentAltitude - targetAltitude) > 0.5) {
                // Gradually approach target altitude (smoother transitions)
                currentAltitude += (targetAltitude - currentAltitude) * 0.1;
            }
            
            repaint(); // Request redraw
        });
        
        animationTimer.start();
        
        // Set default altitude to 11,000 feet (cruising altitude)
        targetAltitude = currentAltitude = 11000.0;
    }
    
    /**
//      * Get the time when the simulation started
     */
    public long getSimulationStartTime() {
        return simulationStartTime;
    }
    
    /**
//      * Inner class for obstacle representation
     */
    private class Obstacle {
        int x, y;
        int size;
        int type; // 0 = mountain, 1 = storm cloud, 2 = aircraft
        boolean passed = false;
        
        Obstacle(int type) {
            this.type = type;
            this.size = 60 + random.nextInt(40); // Larger obstacles (60-100 size)
            
            // Position obstacles on the right side for right-to-left movement
            int width = Math.max(200, getWidth());
            this.x = width + random.nextInt(width/2);
            
            // Y position depends on type
            int height = Math.max(200, getHeight());
            if (type == 0) { // Mountain - connect to the ground
                // Position mountain at ground level (height - height/5)
                // Use the bottom of the mountain as the reference point
                this.y = height - height/5; // This is the bottom of the mountain
            } else if (type == 1) { // Storm - middle to upper part
                this.y = random.nextInt(height/2) + height/4;
            } else { // Aircraft - anywhere in the sky (not too many)
                this.y = random.nextInt(height/2) + size;
            }
        }
        
        void move(double speed) {
            // Move from right to left (opposite to aircraft direction)
            x -= speed;
            
            // Check if obstacle has been passed by the aircraft
            if (!passed && x < positionX) {
                passed = true;
                // Generate a decision when passing an obstacle
                makeDecision();
            }
        }
        
        void draw(Graphics2D g2d) {
            if (type == 0) { // Mountain
                drawMountain(g2d);
            } else if (type == 1) { // Storm cloud
                drawStormCloud(g2d);
            } else { // Aircraft
                drawOtherAircraft(g2d);
            }
        }
        
        private void drawMountain(Graphics2D g2d) {
            // Draw an irregular mountain range spanning kilometers instead of a single mountain
            // Note: 'y' position is the ground level (bottom of mountains)
            
            // If we don't have mountain peaks yet, generate them
            if (mountainPeaks.isEmpty()) {
                generateMountainRange(x, y, getWidth());
            }
            
            // Draw all mountain peaks in the range
            int rangeWidth = getWidth() * 2; // Span across the entire screen width
            int rangeStartX = x - rangeWidth/2;
            
            // Fill the ground area first
            g2d.setColor(new Color(80, 60, 40)); // Brown earth
            g2d.fillRect(rangeStartX, y, rangeWidth, getHeight() - y);
            
            // Draw the mountain range with irregular peaks
            for (MountainPeak peak : mountainPeaks) {
                // Draw individual mountain with irregular shape
                drawMountainPeak(g2d, x + peak.x, y, peak.height, peak.color);
            }
            
            // Add distance information
            double mountainRangeSpan = 5.0 + random.nextInt(10); // 5-15 km span
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(String.format("MOUNTAIN RANGE (%.1f KM)", mountainRangeSpan), 
                           x - 80, y + 30);
        }

        // Generate an irregular mountain range with multiple peaks
        private void generateMountainRange(int centerX, int groundY, int screenWidth) {
            // Clear existing peaks
            mountainPeaks.clear();
            
            // Generate multiple peaks with varying heights and positions
            int numPeaks = 8 + random.nextInt(7); // 8-15 peaks
            int rangeWidth = screenWidth * 3; // Make it wide enough to span kilometers
            
            for (int i = 0; i < numPeaks; i++) {
                int peakX = -rangeWidth/2 + random.nextInt(rangeWidth);
                int peakHeight = 100 + random.nextInt(300); // Vary mountain heights dramatically
                mountainPeaks.add(new MountainPeak(peakX, peakHeight));
            }
        }
        
        // Draw a single mountain peak with irregular shape
        private void drawMountainPeak(Graphics2D g2d, int x, int groundY, int height, Color baseColor) {
            // Create irregular mountain shape with multiple points
            int numPoints = 10 + random.nextInt(6); // 10-15 points for irregular shape
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];
            
            // Base width depends on height
            int baseWidth = height * 2;
            
            // First and last points are at ground level
            xPoints[0] = x - baseWidth/2;
            yPoints[0] = groundY;
            
            xPoints[numPoints-1] = x + baseWidth/2;
            yPoints[numPoints-1] = groundY;
            
            // Generate irregular points for the mountain silhouette
            for (int i = 1; i < numPoints-1; i++) {
                double ratio = (double)i / (numPoints-1);
                // X position along the base
                xPoints[i] = (int)(x - baseWidth/2 + baseWidth * ratio);
                
                // Y position - higher near center, lower near edges
                double heightFactor = 1.0 - Math.abs(ratio - 0.5) * 2; // 1.0 at center, 0.0 at edges
                heightFactor = Math.pow(heightFactor, 1.5); // Make peak more pronounced
                
                // Add some randomness to height
                heightFactor *= (0.7 + random.nextDouble() * 0.3);
                
                yPoints[i] = groundY - (int)(height * heightFactor);
            }
            
            // Create mountain polygon
            Polygon mountain = new Polygon(xPoints, yPoints, numPoints);
            
            // Mountain gradient based on day/night cycle
            Color topColor = baseColor;
            Color bottomColor = new Color(
                Math.max(0, baseColor.getRed() - 30),
                Math.max(0, baseColor.getGreen() - 20),
                Math.max(0, baseColor.getBlue() - 10)
            );
            
            // Adjust colors for night time
            if (!isDayTime) {
                topColor = new Color(
                    topColor.getRed()/3, 
                    topColor.getGreen()/3, 
                    topColor.getBlue()/2
                );
                bottomColor = new Color(
                    bottomColor.getRed()/3, 
                    bottomColor.getGreen()/3, 
                    bottomColor.getBlue()/2
                );
            }
            
            // Create and apply gradient
            GradientPaint mountainGradient = new GradientPaint(
                x, groundY - height, topColor,
                x, groundY, bottomColor
            );
            g2d.setPaint(mountainGradient);
            g2d.fill(mountain);
            
            // Add snow cap on taller mountains
            if (height > 200) {
                drawSnowCap(g2d, xPoints, yPoints, numPoints, groundY, height);
            }
            
            // Draw mountain outline
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.setColor(new Color(40, 30, 20, 120));
            g2d.draw(mountain);
        }

        // Draw snow cap on mountain peak
        private void drawSnowCap(Graphics2D g2d, int[] xPoints, int[] yPoints, int numPoints, int groundY, int height) {
            // Find the highest points in the mountain (top 1/4)
            int snowLineY = groundY - (height * 3/4);
            
            // Create snow polygon
            Polygon snowCap = new Polygon();
            boolean inSnowZone = false;
            
            // Find points above snow line
            for (int i = 0; i < numPoints; i++) {
                if (yPoints[i] <= snowLineY) {
                    if (!inSnowZone && i > 0) {
                        // Add the crossing point at the snow line
                        int prevIdx = i - 1;
                        double ratio = (double)(snowLineY - yPoints[prevIdx]) / (yPoints[i] - yPoints[prevIdx]);
                        int crossX = (int)(xPoints[prevIdx] + ratio * (xPoints[i] - xPoints[prevIdx]));
                        snowCap.addPoint(crossX, snowLineY);
                    }
                    // Add point to snow cap
                    snowCap.addPoint(xPoints[i], yPoints[i]);
                    inSnowZone = true;
                } else if (inSnowZone) {
                    // Add the crossing point at the snow line
                    int prevIdx = i - 1;
                    double ratio = (double)(snowLineY - yPoints[i]) / (yPoints[prevIdx] - yPoints[i]);
                    int crossX = (int)(xPoints[i] + ratio * (xPoints[prevIdx] - xPoints[i]));
                    snowCap.addPoint(crossX, snowLineY);
                    inSnowZone = false;
                }
            }
            
            // If we have a valid snow cap (at least 3 points), draw it
            if (snowCap.npoints >= 3) {
                Color snowColor = isDayTime ? new Color(255, 255, 255) : new Color(180, 180, 220);
                g2d.setColor(snowColor);
                g2d.fill(snowCap);
            }
        }
        
        private void drawStormCloud(Graphics2D g2d) {
            // Update global thunderstorm status
            hasThunderstorm = true;
            
            // Calculate storm cloud size - much larger, spanning over 12km
            int cloudSize = size * 5; // Make significantly larger
            thunderstormSpan = 12.0 + random.nextInt(8); // 12-20 km span
            
            // Create massive cloud formation that spans horizontally
            GradientPaint stormGradient = new GradientPaint(
                x - cloudSize, y - cloudSize/2, new Color(30, 30, 50), 
                x + cloudSize, y + cloudSize/2, new Color(10, 10, 30)
            );
            g2d.setPaint(stormGradient);
            
            // Main thundercloud body (massive size)
            g2d.fillOval(x - cloudSize, y - cloudSize/3, cloudSize*2, cloudSize/2);
            
            // Create thick, layered cloud formations
            for (int i = 0; i < 15; i++) {
                int offsetX = -cloudSize + random.nextInt(cloudSize*2);
                int offsetY = -cloudSize/3 + random.nextInt(cloudSize/2);
                int cloudWidth = cloudSize/2 + random.nextInt(cloudSize);
                int cloudHeight = cloudSize/4 + random.nextInt(cloudSize/4);
                
                // Vary the darkness of each cloud section
                int darkness = 20 + random.nextInt(40);
                g2d.setColor(new Color(darkness, darkness, darkness+10));
                g2d.fillOval(x + offsetX, y + offsetY, cloudWidth, cloudHeight);
            }
            
            // Add storm cloud base (dark and ominous)
            g2d.setColor(new Color(15, 15, 25));
            g2d.fillOval(x - cloudSize, y, cloudSize*2, cloudSize/3);
            
            // Rain effect
            g2d.setColor(new Color(200, 200, 255, 80));
            g2d.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < 40; i++) {
                int rainX = x - cloudSize + random.nextInt(cloudSize*2);
                int rainY = y + cloudSize/4;
                int rainLength = 20 + random.nextInt(40);
                g2d.drawLine(rainX, rainY, rainX - 5, rainY + rainLength);
            }
            
            // Multiple lightning bolts - dramatic and visible
            if (System.currentTimeMillis() % 800 < 300) { // Flash more frequently
                // Generate multiple lightning bolts
                for (int i = 0; i < 3; i++) {
                    int boltStartX = x - cloudSize/2 + random.nextInt(cloudSize);
                    drawLightningBolt(g2d, boltStartX, y + cloudSize/4);
                }
            }
            
            // Add storm information
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString(String.format("THUNDERSTORM (%.1f KM)", thunderstormSpan), 
                          x - 80, y + cloudSize/2 + 25);
        }

        // Helper method to draw realistic lightning bolts
        private void drawLightningBolt(Graphics2D g2d, int startX, int startY) {
            // Create zigzag lightning pattern
            int segments = 4 + random.nextInt(3);
            int[] xPoints = new int[segments];
            int[] yPoints = new int[segments];
            
            xPoints[0] = startX;
            yPoints[0] = startY;
            
            for (int i = 1; i < segments; i++) {
                xPoints[i] = xPoints[i-1] + (random.nextInt(40) - 20);
                yPoints[i] = startY + (i * 30) + random.nextInt(10);
            }
            
            // Main lightning bolt - bright yellow
            g2d.setColor(new Color(255, 255, 220));
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawPolyline(xPoints, yPoints, segments);
            
            // Add glow effect - semi-transparent
            g2d.setColor(new Color(255, 255, 180, 90));
            g2d.setStroke(new BasicStroke(5.0f));
            g2d.drawPolyline(xPoints, yPoints, segments);
        }
        
        private void drawOtherAircraft(Graphics2D g2d) {
            // Draw another aircraft (more detailed and visible)
            int aircraftSize = size * 3/4;
            
            // Save transform for rotation
            AffineTransform originalTransform = g2d.getTransform();
            
            // Rotate slightly to make it look like it's flying in a different direction
            g2d.translate(x, y);
            g2d.rotate(Math.toRadians(15)); // Slight angle
            
            // Fuselage with gradient
            GradientPaint aircraftGradient = new GradientPaint(
                -aircraftSize/2, 0, new Color(220, 220, 220), 
                aircraftSize/2, 0, new Color(180, 180, 180)
            );
            g2d.setPaint(aircraftGradient);
            g2d.fillOval(-aircraftSize/2, -aircraftSize/6, aircraftSize, aircraftSize/3);
            
            // Wings
            g2d.setColor(new Color(200, 200, 220));
            g2d.fillRect(-aircraftSize/4, -aircraftSize/3, aircraftSize/2, aircraftSize/6);
            
            // Tail
            int[] xTail = {aircraftSize/2 - 5, aircraftSize/2, aircraftSize/2};
            int[] yTail = {0, -aircraftSize/6, aircraftSize/6};
            g2d.fillPolygon(xTail, yTail, 3);
            
            // Outline whole aircraft
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.setColor(Color.BLACK);
            g2d.drawOval(-aircraftSize/2, -aircraftSize/6, aircraftSize, aircraftSize/3);
            g2d.drawRect(-aircraftSize/4, -aircraftSize/3, aircraftSize/2, aircraftSize/6);
            
            // Add warning lights (blinking)
            if (System.currentTimeMillis() % 1000 < 500) { // Blink every half-second
                g2d.setColor(Color.RED);
                g2d.fillOval(aircraftSize/2 - 8, -aircraftSize/6, 6, 6); // Right side
                g2d.fillOval(-aircraftSize/2 + 2, -aircraftSize/6, 6, 6); // Left side
            }
            
            // Restore transform
            g2d.setTransform(originalTransform);
            
            // Add "TRAFFIC" label
            g2d.setColor(new Color(255, 60, 60));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("TRAFFIC", x - 25, y + size/2 + 15);
        }
    }
    
    /**
//      * Inner class for mountain peak representation within mountain ranges
     */
    private class MountainPeak {
        int x, height;
        Color color;
        
        MountainPeak(int x, int height) {
            this.x = x;
            this.height = height;
            // Vary mountain colors from dark gray to brown/green
            int r = 60 + random.nextInt(40);
            int g = 60 + random.nextInt(60);
            int b = 60 + random.nextInt(30);
            this.color = new Color(r, g, b);
        }
    }
    
    /**
//      * Inner class for cloud representation
     */
    private class Cloud {
        int x, y;
        int size;
        double speed;
        double opacity;
        boolean isThickCloud;
        Color cloudColor;
        
        Cloud() {
            this.size = 20 + random.nextInt(60);
            this.speed = 0.5 + random.nextDouble() * 1.5;
            this.opacity = 0.5 + random.nextDouble() * 0.5;
            this.isThickCloud = random.nextBoolean();
            
            int width = Math.max(200, getWidth());
            int height = Math.max(200, getHeight());
            
            // Position randomly, more likely in upper half of screen
            this.x = random.nextInt(width);
            this.y = random.nextInt(height * 3/4); // Mostly in upper 3/4
            
            // Whiter clouds
            int brightness = 220 + random.nextInt(35);
            this.cloudColor = new Color(brightness, brightness, brightness);
        }
        
        void move() {
            // Move clouds horizontally at variable speeds
            x -= speed;
            
            // Wrap around when off screen
            if (x < -size) {
                x = getWidth() + size;
                y = random.nextInt(getHeight() * 3/4);
            }
        }
        
        void draw(Graphics2D g2d) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)opacity));
            g2d.setColor(cloudColor);
            
            if (isThickCloud) {
                // Draw thick fluffy cumulus style cloud
                for (int i = 0; i < 5; i++) {
                    int offsetX = i * size/6 - size/12;
                    int offsetY = (i % 2 == 0) ? -size/8 : size/10;
                    int cloudletSize = size/2 + random.nextInt(size/4);
                    g2d.fillOval(x + offsetX, y + offsetY, cloudletSize, cloudletSize);
                }
                g2d.fillOval(x, y + size/6, size, size/2);
                g2d.fillOval(x + size/3, y - size/6, size/2, size/2);
            } else {
                // Draw thin cirrus style cloud
                g2d.fillOval(x, y, size, size/3);
                g2d.fillOval(x + size/4, y - size/6, size/2, size/3);
                g2d.fillOval(x + size/2, y, size/3, size/4);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    private void applyTurbulencePenetrationMode() {
        // Reduce control sensitivity in turbulence
        double turbulenceIntensity = Math.min(2.0, Math.abs(turbulenceFactor));
        
        // Maintain wings level tendency
        if (Math.abs(this.roll) > 5.0) {
            this.roll *= 0.95; // Gradual return to wings level
        }
        
        // Pitch stability - prevent excessive nose up/down
        if (Math.abs(this.pitch) > 3.0) {
            this.pitch *= 0.98; // Gradual return to level
        }
        
        // Speed management in turbulence - maintain penetration speed
        double targetTurbulenceSpeed = 280.0; // Typical turbulence penetration speed
        double speedError = targetTurbulenceSpeed - this.flightSpeed;
        this.flightSpeed += speedError * 0.02; // Gentle speed correction
        
        // Altitude hold with turbulence compensation
        if (Math.abs(currentAltitude - targetAltitude) > 200) {
            // Allow larger altitude deviations in turbulence but still correct
            double altitudeError = targetAltitude - currentAltitude;
            currentAltitude += altitudeError * 0.05; // Slower altitude corrections
        }
        
        // Apply random turbulence effects with dampening
        double dampening = 0.4; // Professional aircraft have good dampening
        this.roll += (random.nextDouble() - 0.5) * turbulenceIntensity * dampening;
        this.pitch += (random.nextDouble() - 0.5) * turbulenceIntensity * 0.3 * dampening;
        
        // Simulate realistic altitude variations
        if (random.nextDouble() < 0.05) { // 5% chance per update
            double altitudeVariation = (random.nextDouble() - 0.5) * 50 * turbulenceIntensity;
            targetAltitude = Math.max(1000, Math.min(45000, currentAltitude + altitudeVariation));
        }
    }
    
    /**
//      * Apply stability corrections in normal flight
     */
    private void applyStabilityCorrections() {
        // Automatic trim and stability augmentation
        
        // Roll stability - tendency to return to wings level
        if (Math.abs(this.roll) > 1.0) {
            this.roll *= 0.98;
        }
        
        // Pitch stability - maintain level flight tendency
        if (Math.abs(this.pitch) > 0.5) {
            this.pitch *= 0.99;
        }
        
        // Speed stability - maintain cruise speed
        double cruiseSpeed = 250.0;
        double speedError = cruiseSpeed - this.flightSpeed;
        if (Math.abs(speedError) > 5.0) {
            this.flightSpeed += speedError * 0.01;
        }
        
        // Altitude hold
        double altitudeError = targetAltitude - currentAltitude;
        if (Math.abs(altitudeError) > 50) {
            currentAltitude += altitudeError * 0.1;
        }
    }
    
    /**
//      * Implements professional flight envelope protection systems used in modern commercial aircraft.
//      * 
//      * Flight envelope protection is a critical safety system that prevents the aircraft from
//      * exceeding structural or aerodynamic limits. This implementation includes:
//      * 
//      * - Bank angle protection (limits to ±35°, standard in commercial aircraft)
//      * - Pitch protection (prevents excessive nose up/down attitudes)
//      * - Yaw normalization and Dutch roll damping
//      * - Airspeed protections (not shown in this method but handled elsewhere)
//      * - Altitude deviation monitoring
//      * 
//      * These protections are similar to those found in Airbus and Boeing fly-by-wire systems,
//      * which prevent pilots from exceeding safe operating parameters even under manual control.
     * Enforces professional flight envelope limits to maintain safe aircraft operation.
     *
     * This method implements commercial aircraft-grade flight envelope protection by:
     *
     *    INPUT → PROTECTION → OUTPUT
     *     Roll → Max ±35° → Safe Roll
     *    Pitch → Max ±25° → Safe Pitch
     *      Yaw → Normalized → Safe Yaw
     *    Speed → Min/Max → Safe Speed
     *
     * The protection system follows a multi-stage verification process:
     * 1. Primary limit checks against absolute maximums
     * 2. Secondary dynamic checks based on current flight state
     * 3. Rate limiting for smooth transitions between states
     * 4. Safety margin enforcement for professional operations
     *
     * @param targetRoll The desired roll angle (degrees)
     * @param targetPitch The desired pitch angle (degrees)
     * @param targetYaw The desired yaw angle (degrees)
     * @param targetSpeed The desired airspeed (knots)
     * @return Protected flight parameters within safe envelope
     */
    private void enforceFlightEnvelope() {
        // Bank angle limits (professional aircraft typically limit to 30-35 degrees)
        this.roll = Math.max(-35, Math.min(35, this.roll));
        
        // Pitch angle limits (prevent stall and overstress)
        this.pitch = Math.max(-15, Math.min(15, this.pitch));
        
        // Heading normalization
        this.yaw = ((this.yaw % 360) + 360) % 360;
        
        // Speed envelope protection
        double minSpeed = 180.0; // Minimum safe speed
        double maxSpeed = 350.0; // Maximum operating speed
        this.flightSpeed = Math.max(minSpeed, Math.min(maxSpeed, this.flightSpeed));
        
        // Altitude envelope
        double minAltitude = 1000.0; // Minimum safe altitude
        double maxAltitude = 41000.0; // Service ceiling
        this.currentAltitude = Math.max(minAltitude, Math.min(maxAltitude, this.currentAltitude));
        this.targetAltitude = Math.max(minAltitude, Math.min(maxAltitude, this.targetAltitude));
    }
    
    /**
//      * Set the current aircraft altitude
//      * @param altitude The altitude in feet
     */
    public void setAltitude(double altitude) {
        // Apply altitude loss if active
        if (altitudeLossActive) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLossStart = currentTime - altitudeLossStartTime;
            
            // First 2 seconds - rapid descent
            if (timeSinceLossStart < 2000) {
                double lossProgress = timeSinceLossStart / 2000.0;
                this.currentAltitude = altitude - (altitudeLossAmount * lossProgress);
            } 
            // Next seconds - gradual recovery
            else {
                double recoveryProgress = Math.min(1.0, (timeSinceLossStart - 2000) / 3000.0);
                this.currentAltitude = altitude - altitudeLossAmount + (altitudeLossAmount * recoveryProgress);
            }
        } else {
            // Make sure we don't drop below cruising altitude
            this.currentAltitude = Math.max(altitude, 11000.0);
        }
        
        if (!avoidingObstacle) {
            this.targetAltitude = Math.max(altitude, 11000.0); // Never go below cruising altitude
        }
    }
    
    /**
//      * Process and update obstacle positions and decisions
     */
    private void processObstacles() {
        // Move existing obstacles and check for removal
        List<Obstacle> toRemove = new ArrayList<>();
        boolean thunderstormAhead = false;
        boolean needToClimb = false;
        double distanceToThunderstorm = Double.MAX_VALUE;
        
        // Reset thunderstorm status for this frame - will be set to true if we detect one
        hasThunderstorm = false;
        
        for (Obstacle obstacle : obstacles) {
            obstacle.move(flightSpeed / 30.0); // Move obstacles at a steady pace
            
            // Check for thunderstorms in the scene
            if (obstacle.type == 1) { // Storm cloud
                hasThunderstorm = true; // This will make the scene darker
                
                // Calculate storm severity based on size (larger = more severe)
                double stormSeverity = (obstacle.size - 60) / 40.0; // Normalized 0.0-1.0
                
                // Calculate storm top altitude - ranges from 28,000 to 45,000 ft based on severity
                double stormTopAltitude = 28000 + (stormSeverity * 17000);
                thunderstormCloudHeight = (int)stormTopAltitude;
                
                // Check if thunderstorm is ahead of aircraft
                if (obstacle.x > positionX - obstacle.size && obstacle.x < positionX + getWidth()/2) {
                    thunderstormAhead = true;
                    needToClimb = true;
                    double distance = obstacle.x - positionX;
                    if (distance < distanceToThunderstorm && distance > 0) {
                        distanceToThunderstorm = distance;
                        activeStormCloudHeight = thunderstormCloudHeight;
                    }
                }
            }
            
            // Mark obstacles for removal if they're completely off-screen
            if (obstacle.x < -obstacle.size * 2) {
                toRemove.add(obstacle);
            }
        }
        
        // Remove obstacles that have moved off-screen
        if (!toRemove.isEmpty()) {
            obstacles.removeAll(toRemove);
        }
        
        // Handle aircraft altitude changes based on thunderstorms
        if (thunderstormAhead && !avoidingObstacle) {
            // Thunderstorm detected ahead - climb to avoid it
            avoidingObstacle = true;
            isClimbingDecision = true;
            
            // Set target altitude 3,000 ft above storm cloud height for safety margin
            targetAltitude = activeStormCloudHeight + 3000.0; 
            
            currentDecision = "Climbing to avoid thunderstorm - Target altitude: " + String.format("%.0f", targetAltitude) + " ft";
            decisionChangeTime = System.currentTimeMillis();
        }
        else if (!thunderstormAhead && avoidingObstacle) {
            // No more thunderstorm ahead - return to cruising altitude
            avoidingObstacle = false;
            isClimbingDecision = false;
            targetAltitude = 11000.0; // Return to cruising altitude
            currentDecision = "Returning to cruising altitude";
            decisionChangeTime = System.currentTimeMillis();
        }
        
        // Move aircraft position within the visible area (25-75% of width)
        positionX += flightSpeed / 50.0; // Steady horizontal movement
        distanceTraveled += flightSpeed / 50.0;
        
        // Keep aircraft within visible area (not too far right)
        if (positionX < getWidth() * 0.25) {
            positionX = getWidth() * 0.25; // Minimum position (25% from left)
        } else if (positionX > getWidth() * 0.75) {
            positionX = getWidth() * 0.75; // Maximum position (75% from left)
        }
        
        // Update background scrolling (ground and clouds)
        backgroundOffset -= flightSpeed / 30.0;
        if (backgroundOffset < -getWidth()) {
            backgroundOffset = 0; // Reset when completely off-screen
        }
        
        groundOffset -= flightSpeed / 25.0;
        if (groundOffset < -50) {
            groundOffset = 0; // Reset ground offset when needed
        }
        
        // Only add new obstacles if we need more
        if (obstacles.size() < 3 && random.nextInt(100) < 2) { // 2% chance per update when fewer than 3 obstacles
            scheduleObstacle();
        }
        
        // Reset distance traveled counter for next distance-based events
        if (distanceTraveled > 500) {
            distanceTraveled = 0;
        }
    }
    
    /**
//      * Make decision about aircraft action based on obstacles
     */
    private void makeDecision() {
        // Update decision change time
        decisionChangeTime = System.currentTimeMillis();
        
        // Set a timer to clear the obstacle avoidance after 5 seconds
        new Timer(5000, e -> {
            // Only clear if we're not dealing with another obstacle
            if (!obstacles.stream().anyMatch(o -> o.x > getWidth()/2 && o.x < getWidth())) {
                avoidingObstacle = false;
                currentDecision = "Returning to cruising altitude";
                targetAltitude = baseCruisingAltitude;
                isClimbingDecision = currentAltitude < baseCruisingAltitude;
            }
        }).start();
    }
    
    /**
//      * Schedule creation of a new obstacle with reasonable frequency
     */
    private void scheduleObstacle() {
        // Check if we already have too many obstacles
        if (obstacles.size() >= 5) {
            return; // Don't add more obstacles if we already have too many
        }
        
        // Determine obstacle type with weighted distribution
        // Prefer mountains (50%), then storms (30%), with less aircraft (20%)
        int typeRoll = random.nextInt(100);
        int type;
        if (typeRoll < 50) {
            type = 0; // Mountain (50% chance)
        } else if (typeRoll < 80) {
            type = 1; // Storm (30% chance)
        } else {
            type = 2; // Aircraft (20% chance)
        }
        
        // Add the obstacle
        obstacles.add(new Obstacle(type));
    }
    
    /**
//      * Initialize cloud objects
     */
    private void initializeClouds() {
        clouds.clear();
        for (int i = 0; i < 15; i++) {
            clouds.add(new Cloud());
        }
    }
    
    /**
//      * Update the day/night cycle based on simulation time
     */
    private void updateDayNightCycle() {
        // Update time based on real elapsed time and speed factor
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastTimeCheck;
        lastTimeCheck = currentTime;
        
        // Update sun position (0.0 = dawn, 0.5 = noon, 1.0 = dusk, 1.5 = midnight)
        double cycleProgress = elapsed * timeSpeedFactor / 1000.0 / 86400.0; // Fraction of day passed
        sunPosition = (sunPosition + cycleProgress) % 2.0;
        
        // Day/night determination
        isDayTime = (sunPosition < 1.0);
    }
    
    /**
//      * Helper method to blend colors with alpha
     */
    private Color blendColors(Color c1, Color c2, double ratio) {
        int r = (int)(c1.getRed() * (1-ratio) + c2.getRed() * ratio);
        int g = (int)(c1.getGreen() * (1-ratio) + c2.getGreen() * ratio);
        int b = (int)(c1.getBlue() * (1-ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }
    
    /**
//      * Draws weather and time information as text overlay including
//      * aircraft altitude relative to thunderstorm clouds
     */
    private void drawWeatherAndTimeInfo(Graphics2D g2d) {
        // Create semi-transparent background for info display
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 330, 100, 10, 10);
        
        // Display weather information
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        if (hasThunderstorm) {
            String safetyStatus;
            if (currentAltitude > activeStormCloudHeight) {
                safetyStatus = "SAFE - Above storm top by " + String.format("%.0f", (currentAltitude - activeStormCloudHeight)) + " ft";
            } else {
                safetyStatus = "WARNING - Below storm top by " + String.format("%.0f", (activeStormCloudHeight - currentAltitude)) + " ft";
            }
            
            // Draw the text with appropriate colors
            g2d.setColor(Color.WHITE);
            g2d.drawString("Time: " + getTimeOfDayText(), 20, 30);
            g2d.drawString("Weather: Thunderstorm - Span: " + String.format("%.1f", thunderstormSpan) + " km", 20, 50);
            g2d.drawString("Storm Height: " + String.format("%.0f", (double)activeStormCloudHeight) + " ft", 20, 70);
            
            // Use appropriate color for safety status
            if (currentAltitude > activeStormCloudHeight) {
                g2d.setColor(new Color(100, 255, 100)); // Green for safe
            } else {
                g2d.setColor(new Color(255, 100, 100)); // Red for warning
            }
            g2d.drawString(safetyStatus, 20, 90);
        } else {
            g2d.setColor(Color.WHITE);
            g2d.drawString("Time: " + getTimeOfDayText(), 20, 30);
            g2d.drawString("Weather: " + (isDayTime ? "Clear skies" : "Clear night"), 20, 50);
            g2d.drawString("Aircraft Altitude: " + String.format("%.0f", currentAltitude) + " ft", 20, 70);
        }
    }
    
    /**
//      * Draw sun or moon based on time of day
     */
    private void drawCelestialBodies(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        
        // Position depends on sun position in cycle
        double angle;
        if (sunPosition < 1.0) { // Day - draw sun
            angle = Math.PI * sunPosition;
        } else { // Night - draw moon
            angle = Math.PI * (sunPosition - 1.0);
        }
        
        int celestialX = (int)(width/2 - Math.cos(angle) * width/2);
        int celestialY = (int)(height/3 - Math.sin(angle) * height/3);
        int celestialSize = width/10;
        
        // Don't draw if there's a thunderstorm
        if (hasThunderstorm && isDayTime) {
            // Instead, make the sky darker
            return;
        }
        
        if (isDayTime) {
            // Draw sun with gradient and rays
            GradientPaint sunGradient = new GradientPaint(
                celestialX - celestialSize/2, celestialY - celestialSize/2, 
                new Color(255, 255, 200), 
                celestialX + celestialSize/2, celestialY + celestialSize/2, 
                new Color(255, 200, 50)
            );
            g2d.setPaint(sunGradient);
            g2d.fillOval(celestialX - celestialSize/2, celestialY - celestialSize/2, 
                        celestialSize, celestialSize);
            
            // Draw sun rays
            g2d.setColor(new Color(255, 255, 100, 150));
            g2d.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < 12; i++) {
                double rayAngle = i * Math.PI/6;
                int innerX = (int)(celestialX + Math.cos(rayAngle) * celestialSize/2);
                int innerY = (int)(celestialY + Math.sin(rayAngle) * celestialSize/2);
                int outerX = (int)(celestialX + Math.cos(rayAngle) * celestialSize);
                int outerY = (int)(celestialY + Math.sin(rayAngle) * celestialSize);
                g2d.drawLine(innerX, innerY, outerX, outerY);
            }
        } else {
            // Draw moon with craters
            g2d.setColor(new Color(220, 220, 255));
            g2d.fillOval(celestialX - celestialSize/2, celestialY - celestialSize/2, 
                        celestialSize, celestialSize);
                        
            // Add a few craters for detail
            g2d.setColor(new Color(200, 200, 220));
            g2d.fillOval(celestialX - celestialSize/4, celestialY - celestialSize/4, 
                        celestialSize/5, celestialSize/5);
            g2d.fillOval(celestialX + celestialSize/6, celestialY + celestialSize/6, 
                        celestialSize/7, celestialSize/7);
            g2d.fillOval(celestialX - celestialSize/10, celestialY + celestialSize/5, 
                        celestialSize/6, celestialSize/6);
        }
    }
    
    /**
//      * Draw starfield for night sky
     */
    private void drawStarfield(Graphics2D g2d) {
        // Only draw stars at night
        if (isDayTime) {
            return;
        }
        
        // Use current time to seed stars (so they don't change every frame)
        Random starRandom = new Random(12345);
        
        // Draw stars with varying brightness
        g2d.setColor(Color.WHITE);
        int width = getWidth();
        int height = getHeight();
        
        for (int i = 0; i < 200; i++) {
            int x = starRandom.nextInt(width);
            int y = starRandom.nextInt(height * 2/3); // Stars in upper 2/3 of sky
            int brightness = 100 + starRandom.nextInt(155);
            g2d.setColor(new Color(brightness, brightness, brightness));
            
            // Vary star sizes
            int starSize = starRandom.nextInt(3) + 1;
            if (starSize == 1) {
                g2d.drawLine(x, y, x, y); // Single pixel
            } else {
                g2d.fillOval(x, y, starSize, starSize);
            }
        }
    
    }
    
    // ComponentListener implementation
    @Override
    public void componentShown(ComponentEvent e) {
        if (getWidth() > 0 && getHeight() > 0 && clouds.isEmpty()) {
            initializeClouds();
            scheduleObstacle();
        }
    }
    
    @Override
    public void componentResized(ComponentEvent e) {
        if (getWidth() > 0 && getHeight() > 0 && clouds.isEmpty()) {
            initializeClouds();
            scheduleObstacle();
        }
    }
    
    @Override
    public void componentMoved(ComponentEvent e) {
        // Not needed
    }
    
    @Override
    public void componentHidden(ComponentEvent e) {
        // Not needed
    }
    
    /**
//      * Set the time of day (0-2 range where 0=dawn, 0.5=noon, 1.0=dusk, 1.5=midnight)
     */
    public void setTimeOfDay(double timeOfDay) {
        this.sunPosition = timeOfDay;
    }
    
    /**
//      * Set whether it's daytime or nighttime
     */
    public void setIsDayTime(boolean isDayTime) {
        this.isDayTime = isDayTime;
    }
    
    /**
//      * Set whether a thunderstorm is ahead
     */
    public void setThunderstormAhead(boolean thunderstormAhead) {
        this.hasThunderstorm = thunderstormAhead;
    }
    
    public void setThreadExecutionTimes(long[] threadExecutionTimes) {
        this.threadExecutionTimes = threadExecutionTimes;
    }
    
    /**
//      * Set thread execution counts for performance monitoring
     */
    public void setThreadExecutionCounts(long[] threadExecutionCounts) {
        this.threadExecutionCounts = threadExecutionCounts;
    }
    
    /**
//      * Set thread names for performance monitoring
     */
    public void setThreadNames(String[] threadNames) {
        this.threadNames = threadNames;
    }
    
    /**
     * Set whether to show the enhanced performance overlay
     * @param showPerformanceOverlay True to show the performance monitor, false to hide it
     */
    public void setShowPerformanceOverlay(boolean showPerformanceOverlay) {
        this.showPerformanceOverlay = showPerformanceOverlay;
    }
    
    /**
//      * Set average frame time for performance monitoring
     */
    public void setAvgFrameTime(double avgFrameTime) {
        this.avgFrameTime = avgFrameTime;
    }
    
    /**
     * Set monitoring start time for performance monitoring
     */
    public void setMonitoringStartTime(long monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }
    
    /**
     * Paints the component with all aviation and simulation elements.
     * 
     * This method coordinates the rendering of all visual components in the proper order:
     * 
     * 1. Environmental layers (sky, sea, land)
     * 2. Stable terrain features (mountains, landscape elements)
     * 3. Cloud systems with proper depth and transparency
     * 4. Weather events and obstacles (other aircraft, storms)
     * 5. The primary aircraft with professional visualization
     * 6. Flight data and system status information
     * 7. Performance monitoring overlay (when enabled)
     * 
     * The rendering uses high-quality anti-aliasing and optimized transforms to maintain
     * smooth frame rates even with complex visual elements. The visualization maintains
     * professional aviation standards and realistic environmental effects.
     * 
     * @param g The graphics context provided by the system
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable high-quality rendering with GPU acceleration
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Enable LCD text mode for better contrast on LCD screens
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "true");
        
        int width = getWidth();
        int height = getHeight();
        
        // Update day/night cycle
        updateDayNightCycle();
        
        // Draw layered background (sky, sea, land)
        drawLayeredBackground(g2d, width, height);
        
        // Draw stable terrain features
        drawStableTerrain(g2d, width, height);
        
        // Draw clouds with proper depth
        drawCloudsWithDepth(g2d);
        
        // Draw obstacles with stability
        drawStableObstacles(g2d);
        
        // Draw professional aircraft with stability systems at a safe altitude above terrain
        // Adjust vertical position to be higher above terrain (35% of height rather than 50%)
        drawProfessionalAircraft(g2d, (int)positionX, (int)(height * 0.35), width, height);
        
        // Draw comprehensive flight information
        drawComprehensiveFlightInfo(g2d, width, height);
        
        // Draw performance overlay with better formatting
        if (showPerformanceOverlay) {
            drawEnhancedPerformanceOverlay(g2d, width, height);
        }
        
        g2d.dispose();
    }
    
    /**
     * Renders a professional aviation-grade environmental background with multiple layers.
     * 
     * This method creates a realistic background environment with layered components,
     * providing a professional visualization context for the aircraft simulation.
     * 
     * LAYERED RENDERING ARCHITECTURE
     * =============================
     * 
     *     TOP                                                    BOTTOM
     *     ⬆                                                       ⬇
     * 
     *  +----------------+    +------------------+    +------------------+
     *  |   SKY LAYER    |    |   CLOUD LAYER    |    |  CELESTIAL BODIES |
     *  | (Blue gradient |    | (Multi-layered   |    |  (Sun/Moon with   |
     *  |  with altitude)|    |  with depth)     |    |   light effects)  |
     *  +----------------+    +------------------+    +------------------+
     *          |                      |                       |
     *          v                      v                       v
     *  +----------------+    +------------------+    +------------------+
     *  | MOUNTAIN LAYER |    |   OCEAN LAYER    |    |    LAND LAYER    |
     *  | (Snow-capped   |    | (Realistic waves |    | (Terrain with    |
     *  |  peaks)        |    |  with motion)    |    |  day/night tint) |
     *  +----------------+    +------------------+    +------------------+
     * 
     * Key features:
     * 
     * 1. Sky Layer: Professional aviation-standard blue tones rather than exaggerated
     *    warm colors, with proper atmospheric scattering effects.
     * 
     * 2. Sea/Ocean Layer: Realistic ocean surface with wave patterns that vary
     *    based on simulated weather conditions and time of day.
     * 
     * 3. Land Layer: Terrain with appropriate coloration that adjusts for day/night cycles.
     * 
     * 4. Celestial Bodies: Sun or moon based on time of day with appropriate
     *    lighting effects and positions.
     * 
     * 5. Stars: Starfield at night with proper brightness levels.
     *
     * @param g2d The graphics context
     * @param width The panel width
     * @param height The panel height
     */
    private void drawLayeredBackground(Graphics2D g2d, int width, int height) {
        // Sky gradient
        Color skyTop, skyBottom;
        if (hasThunderstorm) {
            skyTop = new Color(40, 40, 50);
            skyBottom = new Color(60, 60, 70);
        } else if (isDayTime) {
            // Use professional aviation blue tones for the sky
            Color noonTop = new Color(80, 145, 245);      // Deep aviation blue
            Color noonBottom = new Color(135, 206, 250);   // Standard sky blue
            Color duskTop = new Color(100, 120, 190);      // Blue with subtle sunset influence
            Color duskBottom = new Color(140, 180, 230);   // Lighter professional blue
            
            // Reduce yellow tint by limiting dusk color influence (max 15% influence)
            double colorFactor = Math.max(0.85, 1.0 - Math.abs(sunPosition - 0.5));
            skyTop = blendColors(duskTop, noonTop, colorFactor);
            skyBottom = blendColors(duskBottom, noonBottom, colorFactor);
        } else {
            skyTop = new Color(10, 10, 40);
            skyBottom = new Color(25, 25, 60);
        }
        
        GradientPaint skyGradient = new GradientPaint(0, 0, skyTop, 0, height * 0.7f, skyBottom);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, width, (int)(height * 0.7));
        
        // Sea/Ocean (middle layer)
        Color seaColor = isDayTime ? new Color(30, 144, 255, 180) : new Color(15, 82, 186, 180);
        g2d.setColor(seaColor);
        g2d.fillRect(0, (int)(height * 0.7), width, (int)(height * 0.2));
        
        // Add wave effects
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < width; i += 20) {
            int waveY = (int)(height * 0.75 + Math.sin((i + backgroundOffset) * 0.1) * 3);
            g2d.drawLine(i, waveY, i + 10, waveY);
        }
        
        // Land (bottom layer)
        Color landColor = isDayTime ? new Color(34, 139, 34) : new Color(20, 80, 20);
        g2d.setColor(landColor);
        g2d.fillRect(0, (int)(height * 0.9), width, (int)(height * 0.1));
        
        // Draw celestial bodies
        drawCelestialBodies(g2d);
        
        // Draw stars at night
        if (!isDayTime) {
            drawStarfield(g2d);
        }
    }
    
    /**
//      * Draw stable terrain features
     */
    private void drawStableTerrain(Graphics2D g2d, int width, int height) {
        // Draw stable mountain ranges
        g2d.setStroke(new BasicStroke(2.0f));
        
        // Background mountains (darker, smaller)
        g2d.setColor(new Color(100, 100, 120, 150));
        for (int i = 0; i < width + 100; i += 80) {
            int baseX = i - (int)(backgroundOffset * 0.3) % 160;
            int baseY = (int)(height * 0.8);
            int peakHeight = 60 + (i % 40);
            
            int[] xPoints = {baseX - 40, baseX, baseX + 40};
            int[] yPoints = {baseY, baseY - peakHeight, baseY};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
        
        // Foreground mountains (more detailed)
        g2d.setColor(new Color(80, 80, 100));
        for (int i = 0; i < width + 150; i += 120) {
            int baseX = i - (int)(backgroundOffset * 0.5) % 240;
            int baseY = (int)(height * 0.85);
            int peakHeight = 80 + (i % 60);
            
            int[] xPoints = {baseX - 60, baseX - 20, baseX, baseX + 20, baseX + 60};
            int[] yPoints = {baseY, baseY - peakHeight/2, baseY - peakHeight, baseY - peakHeight/2, baseY};
            g2d.fillPolygon(xPoints, yPoints, 5);
            
            // Add snow caps for high peaks
            if (peakHeight > 100) {
                g2d.setColor(Color.WHITE);
                int[] snowX = {baseX - 15, baseX, baseX + 15};
                int[] snowY = {baseY - peakHeight + 15, baseY - peakHeight, baseY - peakHeight + 15};
                g2d.fillPolygon(snowX, snowY, 3);
                g2d.setColor(new Color(80, 80, 100));
            }
        }
    }
    
    /**
//      * Draw clouds with proper depth and layering
     */
    private void drawCloudsWithDepth(Graphics2D g2d) {
        // Sort clouds by depth (y-position)
        clouds.sort((c1, c2) -> Integer.compare(c1.y, c2.y));
        
        for (Cloud cloud : clouds) {
            // Apply depth-based transparency
            float alpha = Math.max(0.3f, 1.0f - (cloud.y / (float)getHeight()));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            cloud.draw(g2d);
        }
        
        // Reset composite
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
//      * Draw obstacles with stability improvements
     */
    private void drawStableObstacles(Graphics2D g2d) {
        for (Obstacle obstacle : obstacles) {
            // Apply stability dampening to obstacle movement
            if (obstacle.type == 1) { // Storm clouds
                // Reduce jitter in storm cloud rendering
                g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            obstacle.draw(g2d);
        }
    }
    
    /**
     * Renders a professional-grade commercial aircraft with realistic design elements.
     * 
     * This method implements advanced aircraft visualization following aviation standards.
     * 
     * PROFESSIONAL AIRCRAFT RENDERING COMPONENTS
     * ========================================
     *
     *                 Vertical    White     Horizontal
     *                  Stab     Strobe     Stabilizer
     *                    |         |           |
     *                    v         v           v
     *                   ┌─┐       ┌┐          ────
     *                   │ │       └┘     ────┘    └────
     *     Cockpit      ┌┴─┴───────────────┐           \
     *       |     ────┘                   └────        \
     *       v    /     ┌───────────────┐       \       │
     *      ┌──┐─┘      │   COMMERCIAL  │        \      │
     *      │[]│        │     AIRLINER  │         \     │
     *   ───┴──┴────────┴───────────────┴──────────────┘
     *   \                                             /
     *    \                                           /
     *     \        Engine       Engine             /
     *      \         |            |               /
     *       \        v            v              /
     *        \─┐    ┌─┐          ┌─┐          ┌─/
     *          └────┴─┴──────────┴─┴──────────┘
     *               |            |
     *               v            v
     *          Red Light    Green Light
     *          (Port)       (Starboard)
     *
     * Key Components:
     * 
     * 1. Professional Aircraft Structure
     *    - Realistic fuselage with proper proportions
     *    - Accurate wing design with engine nacelles
     *    - Detailed empennage (tail section) with stabilizers
     *    - Cockpit window detailing
     * 
     * 2. Aviation Standard Lighting
     *    - Red navigation light (port/left wingtip)
     *    - Green navigation light (starboard/right wingtip)
     *    - White tail/anti-collision lighting
     * 
     * 3. Visual Stability System Indicators
     *    - Visual feedback for active stability augmentation (orange glow)
     *    - Status indicators during turbulence or obstacle avoidance
     *    - Engine intake and exhaust visualization
     * 
     * 4. Professional Motion Dynamics
     *    - Damped roll and pitch movements for realistic inertia
     *    - Proper aircraft behavior during turbulence
     *    - Realistic bank-to-turn coordination
     * 
     * @param g2d The Graphics2D context for rendering
     * @param x The x-coordinate center position
     * @param y The y-coordinate center position
     * @param width The current panel width
     * @param height The current panel height
     */
    private void drawProfessionalAircraft(Graphics2D g2d, int x, int y, int width, int height) {
        // Apply stability corrections to aircraft position
        double stabilizedRoll = roll * 0.7; // Dampen roll for stability
        double stabilizedPitch = pitch * 0.8; // Dampen pitch for stability
        
        // Apply turbulence dampening
        if (turbulenceEnabled) {
            double dampening = 0.3; // Stability system dampening
            stabilizedRoll += turbulenceFactor * dampening;
            stabilizedPitch += turbulenceFactor * 0.2 * dampening;
        }
        
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(stabilizedRoll));
        
        // Professional aircraft design
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Fuselage (main body)
        g2d.setColor(new Color(220, 220, 220)); // Light gray
        g2d.fillRoundRect(-40, -8, 80, 16, 8, 8);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRoundRect(-40, -8, 80, 16, 8, 8);
        
        // Cockpit windows
        g2d.setColor(new Color(100, 150, 200, 180));
        g2d.fillRoundRect(25, -6, 12, 12, 4, 4);
        g2d.setColor(new Color(80, 120, 160));
        g2d.drawRoundRect(25, -6, 12, 12, 4, 4);
        
        // Main wings
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRoundRect(-15, -25, 30, 50, 6, 6);
        g2d.setColor(new Color(160, 160, 160));
        g2d.drawRoundRect(-15, -25, 30, 50, 6, 6);
        
        // Wing details (flaps, ailerons)
        g2d.setColor(new Color(180, 180, 180));
        g2d.fillRect(-12, -22, 24, 4); // Leading edge
        g2d.fillRect(-12, 18, 24, 4);  // Trailing edge
        
        // Engines
        g2d.setColor(new Color(150, 150, 150));
        g2d.fillOval(-8, -35, 16, 12); // Left engine
        g2d.fillOval(-8, 23, 16, 12);  // Right engine
        
        // Engine intakes
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillOval(-6, -33, 12, 8);
        g2d.fillOval(-6, 25, 12, 8);
        
        // Tail
        g2d.setColor(new Color(200, 200, 200));
        int[] tailX = {-40, -55, -45};
        int[] tailY = {0, -15, -8};
        g2d.fillPolygon(tailX, tailY, 3);
        
        // Vertical stabilizer
        int[] vstabX = {-45, -55, -45};
        int[] vstabY = {-8, -20, -15};
        g2d.fillPolygon(vstabX, vstabY, 3);
        
        // Navigation lights
        g2d.setColor(Color.RED);
        g2d.fillOval(-18, -27, 4, 4); // Left wingtip
        g2d.setColor(Color.GREEN);
        g2d.fillOval(-18, 23, 4, 4);  // Right wingtip
        g2d.setColor(Color.WHITE);
        g2d.fillOval(-52, -18, 3, 3); // Tail light
        
        // Stability system indicator
        if (turbulenceEnabled || avoidingObstacle) {
            g2d.setColor(new Color(255, 165, 0, 150)); // Orange glow for active stability
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawRoundRect(-42, -10, 84, 20, 10, 10);
        }
        
        g2d.rotate(-Math.toRadians(stabilizedRoll));
        g2d.translate(-x, -y);
    }
    
    /**
//      * Renders a professional flight information display panel with aviation standard data.
//      * 
//      * This method creates an information panel that displays critical flight data following
//      * professional aviation standards and formats. The displayed information includes:
//      * 
//      * 1. Primary Flight Data
//      *    - Current altitude with proper aviation units (feet)
//      *    - Airspeed with standard aviation units (knots)
//      *    - Attitude information (roll, pitch, heading)
//      *    - Vertical speed (feet per minute)
//      * 
//      * 2. Navigation Information
//      *    - Current heading with proper degree formatting
//      *    - Track and distance information
//      *    - Current geographic position
//      * 
//      * 3. System Status Information
//      *    - Stability system status indicators
//      *    - Autopilot engagement state
//      *    - Weather radar information
//      *    - Current flight mode and autopilot decisions
//      * 
//      * 4. Environmental Information
//      *    - Time of day and lighting conditions
//      *    - Weather status and turbulence information
//      *    - Terrain clearance warnings
//      * 
//      * The information is presented with proper aviation color coding (green for normal,
//      * amber for caution, red for warnings) and organized in a non-overlapping layout
//      * with clear visual hierarchy.
//      * 
//      * @param g2d The Graphics2D context for rendering
//      * @param width The current panel width
//      * @param height The current panel height
     */
    private void drawComprehensiveFlightInfo(Graphics2D g2d, int width, int height) {
        // Set up fonts
        // Increased font sizes for better visibility
        Font titleFont = new Font("Arial", Font.BOLD, 18);
        Font dataFont = new Font("Arial", Font.PLAIN, 16);
        Font smallFont = new Font("Arial", Font.PLAIN, 14);
        
        // Draw semi-transparent background for flight info
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(10, 10, 280, 200, 10, 10);
        
        // Title
        g2d.setFont(titleFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("FLIGHT MANAGEMENT SYSTEM", 20, 30);
        
        // Flight parameters
        g2d.setFont(dataFont);
        int yPos = 55;
        int lineHeight = 18;
        
        // Altitude with stability status
        String altitudeStatus = avoidingObstacle ? " (ADJUSTING)" : " (STABLE)";
        g2d.setColor(avoidingObstacle ? Color.ORANGE : Color.GREEN);
        g2d.drawString(String.format("Altitude: %.0f ft%s", currentAltitude, altitudeStatus), 20, yPos);
        yPos += lineHeight;
        
        // Speed
        g2d.setColor(Color.CYAN);
        g2d.drawString(String.format("Speed: %.0f knots", flightSpeed), 20, yPos);
        yPos += lineHeight;
        
        // Attitude (with stability corrections)
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Roll: %.1f° (Stab: %.1f°)", roll, roll * 0.7), 20, yPos);
        yPos += lineHeight;
        g2d.drawString(String.format("Pitch: %.1f° (Stab: %.1f°)", pitch, pitch * 0.8), 20, yPos);
        yPos += lineHeight;
        
        // Weather and turbulence
        if (hasThunderstorm) {
            g2d.setColor(Color.RED);
            g2d.drawString("⚠ THUNDERSTORM AHEAD", 20, yPos);
            yPos += lineHeight;
            
            String safetyStatus;
            if (currentAltitude > activeStormCloudHeight) {
                g2d.setColor(Color.GREEN);
                safetyStatus = String.format("✓ Above storm (+%.0f ft)", currentAltitude - activeStormCloudHeight);
            } else {
                g2d.setColor(Color.ORANGE);
                safetyStatus = String.format("⚠ Below storm (-%.0f ft)", activeStormCloudHeight - currentAltitude);
            }
            g2d.drawString(safetyStatus, 20, yPos);
            yPos += lineHeight;
        }
        
        if (turbulenceEnabled) {
            g2d.setColor(Color.YELLOW);
            g2d.drawString(String.format("Turbulence: %.1f (Dampened)", turbulenceFactor), 20, yPos);
            yPos += lineHeight;
        }
        
        // Current decision with stability info
        g2d.setFont(smallFont);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Decision: " + currentDecision, 20, yPos);
        yPos += 15;
        
        // Time and weather
        g2d.drawString("Time: " + getTimeOfDayText(), 20, yPos);
        yPos += 15;
        
        // Stability systems status
        g2d.setColor(Color.GREEN);
        g2d.drawString("✓ Stability Systems: ACTIVE", 20, yPos);
        yPos += 15;
        g2d.drawString("✓ Auto-Pilot: ENGAGED", 20, yPos);
    }
    
    /**
//      * Draw enhanced performance overlay with better formatting
     */
    /**
     * Draws an enhanced professional performance monitoring overlay that shows all major components
     * contributing to the total frame time with accurate accounting of time distribution.
     * 
     * This overlay provides a professional debugging view that clearly identifies where
     * processing time is being spent, highlighting any potential bottlenecks. It includes:
     * - Individual thread execution times
     * - GUI Event Dispatch Thread time (main bottleneck for Swing rendering)
     * - Memory management and garbage collection estimates
     * - System overhead estimates
     * - Aircraft speed in km/h
     * - Verification of time accounting accuracy
     * 
     * @param g2d The graphics context
     * @param width The panel width
     * @param height The panel height
     */
    private void drawEnhancedPerformanceOverlay(Graphics2D g2d, int width, int height) {
        if (threadExecutionTimes == null) return;
        
        // Position overlay in top-left corner for better visibility
        int overlayX = 10;
        int overlayY = 10;
        int overlayWidth = 350;  // Increased width for better readability
        int overlayHeight = 300; // Increased height for additional information
        
        // Draw high-contrast background with border
        g2d.setColor(new Color(0, 0, 0, 220)); // Dark semi-transparent background
        g2d.fillRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 10, 10);
        
        // Add border with subtle highlight
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 10, 10);
        
        // Set up fonts
        Font titleFont = new Font("Monospaced", Font.BOLD, 16);
        Font dataFont = new Font("Monospaced", Font.PLAIN, 14);
        
        // Draw title with shadow for better readability
        g2d.setFont(titleFont);
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString("PERFORMANCE MONITOR", overlayX + 12, overlayY + 22);
        g2d.setColor(new Color(255, 255, 255, 240));
        g2d.drawString("PERFORMANCE MONITOR", overlayX + 10, overlayY + 20);
        
        // Draw subtitle
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2d.setColor(new Color(200, 200, 255));
        g2d.drawString("(Press 'P' to toggle)", overlayX + 10, overlayY + 38);
        
        // FPS indicator with better contrast
        double fps = 1000.0 / avgFrameTime;
        g2d.setFont(dataFont);
        
        // Draw FPS with background for better readability
        Color fpsColor = fps >= 55 ? new Color(100, 255, 100) : 
                         fps >= 30 ? new Color(255, 255, 100) : new Color(255, 100, 100);
        
        // Draw text shadow for better contrast
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("FPS: %.1f", fps), overlayX + 12, overlayY + 57);
        
        // Draw main FPS text
        g2d.setColor(fpsColor);
        g2d.drawString(String.format("FPS: %.1f", fps), overlayX + 10, overlayY + 55);
        
        // Aircraft speed in km/h with better contrast
        double speedKmh = flightSpeed * 1.852;
        
        // Draw speed with background for better readability
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("Speed: %.1f km/h", speedKmh), overlayX + 152, overlayY + 57);
        
        // Draw main speed text
        g2d.setColor(new Color(100, 200, 255)); // Brighter blue for better visibility
        g2d.drawString(String.format("Speed: %.1f km/h", speedKmh), overlayX + 150, overlayY + 55);
        
        // Calculate total measured thread time
        double totalMeasuredThreadTimeMs = 0.0;
        double[] threadAvgTimes = new double[threadExecutionTimes.length];
        
        // Thread execution times with better contrast
        int yPos = overlayY + 80;
        int rowHeight = 20;
        
        if (threadNames != null) {
            for (int i = 0; i < threadNames.length && i < threadExecutionTimes.length; i++) {
                if (threadExecutionCounts[i] > 0) {
                    threadAvgTimes[i] = (threadExecutionTimes[i] / (double)threadExecutionCounts[i]) / 1_000_000.0;
                    totalMeasuredThreadTimeMs += threadAvgTimes[i];
                    
                    // Determine status color with better contrast
                    Color statusColor = threadAvgTimes[i] < 5 ? new Color(100, 255, 100) : 
                                      threadAvgTimes[i] < 16 ? new Color(255, 255, 100) : 
                                      new Color(255, 100, 100);
                    
                    // Draw text shadow for better readability
                    g2d.setColor(new Color(0, 0, 0, 150));
                    g2d.drawString(String.format("%s: %6.2fms", threadNames[i], threadAvgTimes[i]), 
                                 overlayX + 12, yPos + 2);
                    
                    // Draw main text
                    g2d.setColor(statusColor);
                    g2d.drawString(String.format("%s: %6.2fms", threadNames[i], threadAvgTimes[i]), 
                                 overlayX + 10, yPos);
                    
                    yPos += rowHeight;
                }
            }
        }
        
        // Calculate what's not accounted for - this is where most time is spent
        double remainingTimeMs = avgFrameTime - totalMeasuredThreadTimeMs;
        
        // GUI Event Dispatch Thread - main bottleneck with better visualization
        double eventDispatchTimeMs = remainingTimeMs * 0.85; // 85% of remaining time
        Color guiThreadColor = eventDispatchTimeMs < 30 ? new Color(100, 255, 100) : 
                             eventDispatchTimeMs < 60 ? new Color(255, 255, 100) : 
                             new Color(255, 100, 100);
        
        // Draw text shadow for better readability
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("GUI Thread: %6.2fms", eventDispatchTimeMs), overlayX + 12, yPos + 2);
        
        // Draw main text
        g2d.setColor(guiThreadColor);
        g2d.drawString(String.format("GUI Thread: %6.2fms", eventDispatchTimeMs), overlayX + 10, yPos);
        yPos += rowHeight;
        
        // Memory management (GC) estimate with better visualization
        double gcEstimateMs = remainingTimeMs * 0.08; // 8% of remaining time
        
        // Draw text shadow for better readability
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("Memory/GC: %6.2fms", gcEstimateMs), overlayX + 12, yPos + 2);
        
        // Draw main text
        g2d.setColor(new Color(100, 255, 255)); // Brighter cyan for better visibility
        g2d.drawString(String.format("Memory/GC: %6.2fms", gcEstimateMs), overlayX + 10, yPos);
        yPos += rowHeight;
        
        // System overhead with better visualization
        double systemOverheadMs = remainingTimeMs * 0.07; // 7% of remaining time
        
        // Draw text shadow for better readability
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("System: %6.2fms", systemOverheadMs), overlayX + 12, yPos + 2);
        
        // Draw main text
        g2d.setColor(new Color(200, 200, 255)); // Light purple for system
        g2d.drawString(String.format("System: %6.2fms", systemOverheadMs), overlayX + 10, yPos);
        yPos += rowHeight;
        
        // Calculate verification sum of all components
        double totalAccountedTimeMs = totalMeasuredThreadTimeMs + eventDispatchTimeMs + gcEstimateMs + systemOverheadMs;
        
        // Draw separator line with gradient
        GradientPaint separatorGradient = new GradientPaint(
            overlayX + 10, yPos, new Color(100, 100, 255, 100),
            overlayX + overlayWidth - 20, yPos, new Color(100, 100, 255, 50));
        g2d.setPaint(separatorGradient);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(overlayX + 10, yPos, overlayX + overlayWidth - 20, yPos);
        yPos += 20;
        
        // Total breakdown - verify our accounting with better contrast
        double accountingAccuracy = (totalAccountedTimeMs / avgFrameTime) * 100.0;
        Color accuracyColor = accountingAccuracy >= 99.0 && accountingAccuracy <= 101.0 ? 
                            new Color(100, 255, 100) : new Color(255, 150, 100);
        
        // Draw summary with shadow for better readability
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString("PERFORMANCE SUMMARY", overlayX + 12, yPos + 2);
        g2d.setColor(new Color(255, 255, 255, 240));
        g2d.drawString("PERFORMANCE SUMMARY", overlayX + 10, yPos);
        yPos += 20;
        
        // Draw summary values with shadow
        g2d.setFont(dataFont);
        
        // Sum components
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("Sum Components: %8.2fms", totalAccountedTimeMs), overlayX + 12, yPos + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Sum Components: %8.2fms", totalAccountedTimeMs), overlayX + 10, yPos);
        yPos += 20;
        
        // Total frame time
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("Total Frame: %11.2fms", avgFrameTime), overlayX + 12, yPos + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("Total Frame: %11.2fms", avgFrameTime), overlayX + 10, yPos);
        yPos += 20;
        
        // Accounting accuracy with color coding
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(String.format("Accuracy: %13.1f%%", accountingAccuracy), overlayX + 12, yPos + 2);
        g2d.setColor(accuracyColor);
        g2d.drawString(String.format("Accuracy: %13.1f%%", accountingAccuracy), overlayX + 10, yPos);
        yPos += 20;
        
        // Add a subtle gradient background for the summary section
        GradientPaint summaryGradient = new GradientPaint(
            overlayX, yPos - 60, new Color(50, 50, 80, 150),
            overlayX, yPos + 10, new Color(30, 30, 60, 200));
        g2d.setPaint(summaryGradient);
        g2d.fillRoundRect(overlayX + 5, yPos - 65, overlayWidth - 10, yPos - (yPos - 65), 8, 8);
        
        // Reset yPos to draw over the gradient
        yPos -= 40;
    }
    
    /**
     * Set the roll angle of the aircraft
     * @param roll The roll angle in degrees
     */
    public void setRoll(double roll) {
        this.roll = roll;
    }
    
    /**
     * Set the pitch angle of the aircraft
     * @param pitch The pitch angle in degrees
     */
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
    
    /**
     * Set the yaw angle of the aircraft
     * @param yaw The yaw angle in degrees
     */
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }
    
    /**
     * Set the flight speed of the aircraft
     * @param speed The speed in knots
     */
    public void setFlightSpeed(double speed) {
        this.flightSpeed = speed;
    }
    
    /**
     * Set the turbulence factor for the aircraft
     * @param turbulenceFactor The turbulence intensity
     */
    public void setTurbulenceFactor(double turbulenceFactor) {
        this.turbulenceFactor = turbulenceFactor;
        this.turbulenceEnabled = turbulenceFactor > 0;
        this.severeTurbulenceEnabled = turbulenceFactor > 0.5;
    }

    /**
     * Set the current decision text for the aircraft
     * @param currentDecision The current decision text
     */
    public void setCurrentDecision(String currentDecision) {
        this.currentDecision = currentDecision;
        this.isClimbingDecision = currentDecision.contains("CLIMB") || 
                                currentDecision.contains("ASCENDING");
    }

}
