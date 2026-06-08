import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

public class SupervisedRunner implements Runnable {

    private static final long INITIAL_BACKOFF_MS = 100;
    private static final long MAX_BACKOFF_MS = 5_000;
    private static final long SUCCESS_RESET_MS = 10_000;
    private static final long RESTART_WINDOW_MS = 30_000;
    private static final int MAX_RESTARTS = 5;

    private final String name;
    private final Runnable worker;
    private final BooleanSupplier running;

    private long backoffMs = INITIAL_BACKOFF_MS;
    private long stableSince = System.currentTimeMillis();

    private final Deque<Long> restartTimes = new ArrayDeque<>();
    private boolean permanentlyStopped = false;

    public SupervisedRunner(String name, Runnable worker, BooleanSupplier running) {
        this.name = name;
        this.worker = worker;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.getAsBoolean() && !permanentlyStopped) {
            try {
                worker.run();

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                long now = System.currentTimeMillis();

                if (now - stableSince >= SUCCESS_RESET_MS) {
                    backoffMs = INITIAL_BACKOFF_MS;
                    restartTimes.clear();
                }

            } catch (Exception ex) {
                logCrash(ex);

                long now = System.currentTimeMillis();

                restartTimes.addLast(now);

                while (!restartTimes.isEmpty()
                        && now - restartTimes.peekFirst() > RESTART_WINDOW_MS) {
                    restartTimes.removeFirst();
                }

                if (restartTimes.size() >= MAX_RESTARTS) {
                    String message = "worker \"" + name
                            + "\" exceeded restart budget; will not be restarted.";
                    System.err.println(message);
                    Main.logToFile(message);
                    permanentlyStopped = true;
                    break;
                }

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }

                backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                stableSince = System.currentTimeMillis();
            }
        }
    }

    private void logCrash(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        String message =
                "\n=== WORKER CRASH ===\n" +
                "Worker: " + name + "\n" +
                sw;

        System.err.println(message);
        Main.logToFile(message);
    }
}