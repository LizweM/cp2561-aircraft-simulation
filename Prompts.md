#AI Interaction. Log for Github copilot interaction for CP2561
## Task 3 – Self-healing worker threads

### Prompt used

For Task 3 we used the assignment instructions as the main body of the prompt then asked to explain what was needed and the steps to take. Using AI to re-iterate over what was unclear to solidify our knowledge . 

> Task 3 asks me to make the aircraft simulation self-healing. The simulation has background worker threads like turbulence, automated demo, resource monitor, and internal AircraftGUI workers. If one crashes, it currently dies silently. Explain what this task is asking for in simple terms and tell me what files I need to change.

After that,we asked for implementation help:

> Provide the exact steps to implement Task 3. I need a `SupervisedRunner` class that takes a worker name, a `Runnable`, and a `BooleanSupplier`. It should catch worker exceptions, log the worker name and stack trace, restart the work with exponential backoff starting at 100 ms and capped at 5 seconds, reset after 10 seconds of successful running, and stop permanently after 5 restarts in 30 seconds return an explanation of your reasoning on what you did.

Then we asked how to apply it to my existing project:

> Here is `Main.java`, `ResourceMonitor.java`, and `AircraftGUI.java`. Show me exactly where to apply `SupervisedRunner` to the turbulence thread, automated demo thread, and resource monitor. Do not change anything unrelated to Task 3 return an explanation of your reasoning on what you did.

We also asked for help checking whether it worked:

> How do I know if I did Task 3 right? What should I see in the console, and how can I test that the supervisor catches a crash and restarts the worker?

### What changed

We added `SupervisedRunner.java`. This class runs a worker inside a loop while the simulation is still running. If the worker throws an exception, it logs the worker name and stack trace, waits using exponential backoff, and then restarts the work. The backoff starts at 100 ms, doubles after each crash, and caps at 5 seconds.

We also added the restart budget from the task. If a worker crashes 5 times inside 30 seconds, the supervisor stops restarting that worker and logs:

`worker "turbulence" exceeded restart budget; will not be restarted.`

the supervisor was applied to:

- the turbulence worker in `Main.java`
- the automated demo worker in `Main.java`
- the resource monitor in `ResourceMonitor.java`

We did not apply it to the input thread or Swing EDT because the task says those do not need to be supervised.

### Testing

To test the supervisor, I temporarily forced a `RuntimeException` inside the turbulence worker. The console showed the crash message, the worker name, the stack trace, retries, and then the restart budget message after 5 crashes. The rest of the simulation kept running after the turbulence worker gave up. I removed the test crash before committing.

## Documentation

We also used AI to construct the format of this prompt document 