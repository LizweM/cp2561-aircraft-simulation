/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 *
 * Aircraft Simulation Project
 */

/**
 * Ultra-stable variant of DirectionControl. Overrides the default physics
 * parameters (inertia / dampening / tolerance) so the axis responds smoothly
 * and resists oscillation.
 */
public class DirectionControlStable extends DirectionControl {

    public DirectionControlStable(String name, double min, double max, ConfigLoader config) {
        super(name, min, max, config);

        switch (name) {
            case "Roll":
            case "Pitch":
                setPhysicsParameters(2.5, 0.98, 2.5);
                break;
            case "Yaw":
                setPhysicsParameters(3.0, 0.99, 3.0);
                break;
            default:
                // Keep values from the parent constructor.
                break;
        }
    }

    private void setPhysicsParameters(double inertia, double dampening, double tolerance) {
        setInertia(inertia);
        setDampening(dampening);
        setTolerance(tolerance);
    }
}
