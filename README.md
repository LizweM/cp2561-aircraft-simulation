# Aircraft Direction Control Simulation
# =================================

```
                                      ✈️
                   +-------------------+-------------------+
                   |                 / | \                 |
                   |                /  |  \                |
              ROLL |               /   |   \               | YAW
            <------|------------->o    |    o<-------------|------>
                   |                   |                   |
                   |                   |                   |
                   |                   v                   |
                   |                 PITCH                 |
                   +-------------------+-------------------+
```

## Table of Contents
- [Introduction](#introduction)
- [System Architecture](#system-architecture)
- [Class Diagram](#class-diagram)
- [Data Flow](#data-flow)
- [Control Mechanism](#control-mechanism)
- [User Interaction](#user-interaction)
- [Configuration](#configuration)
- [Logging and Analysis](#logging-and-analysis)
- [Running the Simulation](#running-the-simulation)

## Introduction

This aircraft direction control simulation models the behavior of an aircraft's orientation control systems for roll, pitch, and yaw axes. The simulation implements a physics-based approach with inertia, dampening, and tolerance factors to create realistic control responses.

```
     ROLL                PITCH                YAW
     ----                -----                ---
      |                    |                   |
  [wing tilt]         [nose up/down]      [left/right turn]
      |                    |                   |
      v                    v                   v
  <------>              ^     v            <----->
```

## System Architecture

The simulation is built using a modular architecture with separate components for control mechanisms, configuration, display, and logging. These components interact in real-time to create a dynamic simulation.

```
+-----------------------------------------------------------+
|                   SIMULATION ARCHITECTURE                  |
+-----------------------------------------------------------+
|                                                           |
|  +---------------+      +----------------+                |
|  | Configuration |----->| DirectionControl|<--+           |
|  |    Loader     |      | (Roll/Pitch/Yaw)|   |           |
|  +---------------+      +----------------+   |           |
|         |                       |            |           |
|         |                       |            |           |
|         v                       v            |           |
|  +---------------+      +----------------+   |           |
|  |   Log System  |<-----| Display System |---+           |
|  |   (CSV/TXT)   |      | (ASCII Charts) |               |
|  +---------------+      +----------------+               |
|         |                       ^                        |
|         |                       |                        |
|         v                       |                        |
|  +---------------+      +----------------+               |
|  |  Data Analysis|      |   User Input   |               |
|  |  & Statistics |      |    System      |               |
|  +---------------+      +----------------+               |
|                                                           |
+-----------------------------------------------------------+
```

## Class Diagram

The simulation consists of the following main classes:

```
+---------------+    +---------------+    +---------------+
| ConfigLoader  |    | Main          |    | ShowValues    |
+---------------+    +---------------+    +---------------+
| -properties   |    | -logWriter    |    | -axis         |
+---------------+    | -csvLogWriter |    | -expected     |
| +getDouble()  |    | -statistics   |    | -actual       |
| +getInt()     |    | -rollControl  |    | -min/max      |
| +getString()  |    | -pitchControl |    | -colorize     |
| +getBoolean() |    | -yawControl   |    | -displayWidth |
+---------------+    +---------------+    +---------------+
        ^             | +main()      |    | +update()     |
        |             | +logToFile() |    | +display()    |
        |             | +logToCSV()  |    +-------+-------+
        |             +-------+-------+            |
        |                     |                    |
        |                     v                    |
        |             +-------+-------+            |
        +------------>| DirectionControl |<--------+
                      +---------------+
                      | -name         |
                      | -currentValue |
                      | -targetValue  |
                      | -velocity     |
                      | -inertia      |
                      | -dampening    |
                      +---------------+
                      | +update()     |
                      | +getters      |
                      | +setters      |
                      +---------------+
```

## Data Flow

The simulation's data flows from user input and configuration through the control mechanisms and eventually to the display and logging systems.

```
                  +-------------+
                  | User Input  |
                  +------+------+
                         |
                         v
+----------+      +------+------+     +------------+
| Config   |----->| Direction   |<----| Turbulence |
| Settings |      | Controllers |     | Simulation |
+----------+      +------+------+     +------------+
                         |
                         v
              +----------+---------+
              |                    |
              v                    v
    +---------+-------+   +--------+--------+
    | Display System  |   | Logging System  |
    +-----------------+   +-----------------+
```

## Control Mechanism

Each direction control (Roll, Pitch, Yaw) is modeled using a physics-based approach:

```
TARGET VALUE         PHYSICS MODEL          CURRENT VALUE
    |                     |                      |
    v                     v                      v
    O------------------->[+]------------------->[#]
    |                    /|\                     |
    |      +------------+ | +------------+       |
    |      |             \|/             |       |
    |      |         +----+----+         |       |
    |      |         | INERTIA  |         |       |
    +------+         +----+----+         +-------+
           |              |              |
           |         +----v----+         |
           +-------->| DAMPENING|<--------+
                     +----+----+
                          |
                     +----v----+
                     |  UPDATE  |
                     +---------+
```

## Control Parameters

Each control axis uses specific parameters to simulate realistic aircraft behavior:

```
ROLL AXIS                 PITCH AXIS                YAW AXIS
[-180° to +180°]          [-90° to +90°]           [-180° to +180°]
    Inertia: 1.0              Inertia: 1.2             Inertia: 1.5
    Dampening: 0.98           Dampening: 0.95          Dampening: 0.92
    |                         |                        |
    v                         v                        v
+---+--+                  +---+--+                 +---+--+
|      |<------ 0° ------>|      |<---- 0° ------>|      |
+------+                  +------+                 +------+
```

## User Interaction

The simulation provides a simple text-based interface for user interaction:

```
+----------------------------------------------+
|                                              |
|  Plane Direction Controls:                   |
|  ----------------------                      |
|                                              |
|  Roll:  [----|------|-#---|------|-] -12.4°  |
|  Pitch: [-----|----#|-----|------|-]  +3.7°  |
|  Yaw:   [-----|-----|---#-|------|-]  +8.2°  |
|                                              |
|  Enter commands (r=roll, p=pitch, y=yaw, q=quit) |
|  Example: r 30 (sets roll to 30 degrees)     |
|                                              |
+----------------------------------------------+
```

## Configuration System

The simulation uses a properties file for configuration:

```
+--------------------+     +--------------------+     +--------------------+
|  DEFAULT CONFIG    |---->|  USER CONFIG FILE  |---->|  RUNTIME CONFIG    |
|  (Hardcoded)       |     |  (Properties)      |     |  (In Memory)       |
+--------------------+     +--------------------+     +--------------------+
        |                           |                          |
        |                           |                          |
        v                           v                          v
+--------------------+     +--------------------+     +--------------------+
| - Default inertia  |     | - Custom inertia   |     | - Active settings  |
| - Default dampening|     | - Custom dampening |     | - Applied to       |
| - Default display  |     | - Custom display   |     |   simulation       |
+--------------------+     +--------------------+     +--------------------+
```

## Logging and Analysis

The simulation logs data in two formats:

```
+-------------------+                    +-------------------+
| HUMAN-READABLE LOG|                    |     CSV DATA      |
+-------------------+                    +-------------------+
| Timestamp: Event  |                    | Time,Axis,Expected|
| Timestamp: Event  |                    | Time,Axis,Expected|
| Timestamp: Event  |                    | Time,Axis,Expected|
+-------------------+                    +-------------------+
        |                                         |
        v                                         v
+-------------------+                    +-------------------+
|  PlaneLog.txt     |                    |  PlaneData.csv    |
+-------------------+                    +-------------------+
```

## Running the Simulation

The simulation provides real-time feedback on aircraft control dynamics:

```
RUNNING SIMULATION
-----------------

 t=0s  STARTUP             t=5s  USER INPUT           t=10s  TURBULENCE
 ------                    ------                      ------
  R: 0°                     R: +30° (target)           R: +28.3° (adjusting)
  P: 0°                     P: 0°                      P: -2.5° (turbulence)
  Y: 0°                     Y: 0°                      Y: +1.7° (roll effect)


                   TIME
     ----------------------------------------------->
     
     +30° ·                    ******
         |                   **      ****
         |                  *            ****
         |                 *                 ****
     +15° ·               *                      *****
         |               *                           *****
         |              *                                 ******
      0° ·*************                                         *******
         +----------+----------+----------+----------+----------+------->
         0s         5s        10s        15s        20s        25s    t
```

## Simulation Statistics

Upon completion, the simulation provides performance statistics:

```
+------------------------------------------+
|      SIMULATION STATISTICS               |
+------------------------------------------+
|                                          |
| Roll Statistics:                         |
|   Samples: 1000                          |
|   Average Deviation: 2.37                |
|   Maximum Deviation: 15.42               |
|                                          |
| Pitch Statistics:                        |
|   Samples: 1000                          |
|   Average Deviation: 1.89                |
|   Maximum Deviation: 9.74                |
|                                          |
| Yaw Statistics:                          |
|   Samples: 1000                          |
|   Average Deviation: 3.12                |
|   Maximum Deviation: 12.33               |
|                                          |
+------------------------------------------+
```

## System Requirements

- Java Development Kit (JDK) 17 or higher
- Terminal with ANSI color support for full visualization
- Minimum 2MB disk space for logs and configuration

## Advanced Features

The simulation includes several advanced features:

1. Real-time physics-based control dynamics
2. Simulated turbulence with random jitter
3. Cross-axis coupling (roll affects yaw)
4. Configurable control parameters
5. CSV data logging for post-simulation analysis
6. ANSI color-coded console visualization
7. Automatic configuration file detection

## Developer Information

Last updated: July 07, 2025
© Aircraft Control Simulation Project
