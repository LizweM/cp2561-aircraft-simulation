/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 *
 * Aircraft Simulation Project
 */

import javax.swing.UIManager;
import java.util.Locale;

/**
 * Small helpers for keeping the simulation portable across Ubuntu/Linux,
 * Windows, and macOS. Wraps OS detection, ANSI-escape availability, and
 * Swing look-and-feel selection so the rest of the code stays clean.
 */
public final class PlatformSupport {

    private PlatformSupport() {}

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    public static boolean isWindows() { return OS.contains("win"); }
    public static boolean isMac()     { return OS.contains("mac") || OS.contains("darwin"); }
    public static boolean isLinux()   { return OS.contains("nux") || OS.contains("nix"); }

    /**
     * Returns true if it's safe to emit ANSI escape sequences (color, cursor
     * control, screen clear) on stdout.
     *
     * Linux and macOS terminals support them natively. Windows-10+ Terminal /
     * PowerShell 7 also do, which we detect via the WT_SESSION or TERM env
     * vars. Plain cmd.exe does not - we'd just paint garbage characters - so
     * we return false there.
     */
    public static boolean supportsAnsi() {
        if (isWindows()) {
            // Modern Windows Terminal sets WT_SESSION; ConEmu sets ConEmuANSI.
            return System.getenv("WT_SESSION") != null
                    || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))
                    || System.getenv("TERM") != null;
        }
        return true;
    }

    /**
     * Asks Swing to use the host's native look-and-feel so the window blends
     * in with the OS desktop (Aqua on macOS, Windows L&F on Windows, GTK on
     * GNOME, etc.). Falls back silently if the L&F isn't available.
     */
    public static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Cross-platform default L&F is fine if the native one fails.
        }
    }

    /** OS label for diagnostic output. */
    public static String osLabel() {
        if (isWindows()) return "Windows";
        if (isMac())     return "macOS";
        if (isLinux())   return "Linux";
        return System.getProperty("os.name", "unknown");
    }
}
