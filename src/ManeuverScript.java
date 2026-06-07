import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ManeuverScript {

    // One maneuver = one row in the CSV
    public static class Maneuver {
        public final int seconds;
        public final double roll;
        public final double pitch;
        public final double yaw;

        public Maneuver(int seconds, double roll, double pitch, double yaw) {
            this.seconds = seconds;
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
        }
    }

    private List<Maneuver> maneuvers = new ArrayList<>();
    private int currentIndex = 0;

    // Returns the next maneuver, looping back to start when it reaches the end
    public Maneuver next() {
        Maneuver m = maneuvers.get(currentIndex);
        currentIndex = (currentIndex + 1) % maneuvers.size();
        return m;
    }

    public int size() {
        return maneuvers.size();
    }

    public static ManeuverScript load(String filePath) {
    ManeuverScript script = new ManeuverScript();
    int lineNumber = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // Skip blank lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            // Skip the header line
            if (line.trim().startsWith("seconds")) {
                continue;
            }

            // Split by comma
            String[] fields = line.split(",");

            // Check we have exactly 4 fields
            if (fields.length != 4) {
                System.err.println("Script error on line " + lineNumber +
                    ": expected 4 fields but found " + fields.length);
                System.exit(1);
            }

            // Parse each field
            int seconds;
            double roll, pitch, yaw;

            try {
                seconds = Integer.parseInt(fields[0].trim());
            } catch (NumberFormatException e) {
                System.err.println("Script error on line " + lineNumber +
                    " field 1 (\"seconds\"): \"" + fields[0].trim() + "\" is not a number");
                System.exit(1);
                return null;
            }

            try {
                roll = Double.parseDouble(fields[1].trim());
            } catch (NumberFormatException e) {
                System.err.println("Script error on line " + lineNumber +
                    " field 2 (\"roll\"): \"" + fields[1].trim() + "\" is not a number");
                System.exit(1);
                return null;
            }

            try {
                pitch = Double.parseDouble(fields[2].trim());
            } catch (NumberFormatException e) {
                System.err.println("Script error on line " + lineNumber +
                    " field 3 (\"pitch\"): \"" + fields[2].trim() + "\" is not a number");
                System.exit(1);
                return null;
            }

            try {
                yaw = Double.parseDouble(fields[3].trim());
            } catch (NumberFormatException e) {
                System.err.println("Script error on line " + lineNumber +
                    " field 4 (\"yaw\"): \"" + fields[3].trim() + "\" is not a number");
                System.exit(1);
                return null;
            }

            // Validate ranges
            if (roll < -180 || roll > 180) {
                System.err.println("Script error on line " + lineNumber +
                    ": roll value " + roll + " is out of range (-180 to 180)");
                System.exit(1);
            }

            if (pitch < -90 || pitch > 90) {
                System.err.println("Script error on line " + lineNumber +
                    ": pitch value " + pitch + " is out of range (-90 to 90)");
                System.exit(1);
            }

            if (yaw < -180 || yaw > 180) {
                System.err.println("Script error on line " + lineNumber +
                    ": yaw value " + yaw + " is out of range (-180 to 180)");
                System.exit(1);
            }

            script.maneuvers.add(new Maneuver(seconds, roll, pitch, yaw));
        }

    } catch (IOException e) {
        System.err.println("Error reading script file: " + e.getMessage());
        System.exit(1);
    }

    if (script.maneuvers.isEmpty()) {
        System.err.println("Script error: file contains no valid maneuvers");
        System.exit(1);
    }

    return script;
}
}