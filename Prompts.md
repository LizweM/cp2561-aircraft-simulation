#AI Interaction. Log for Github copilot interaction for CP2561

## Task 1 – CSV maneuver script loading and validation

For Task 1 we used the assignment instructions as the main body of the prompt and asked AI to explain what the task was asking for in simpler terms. We wanted to understand how scripted aircraft maneuvers should replace the existing hard-coded automated demo behavior.

Task 1 asks me to add scripted aircraft maneuvers to the aircraft simulation. Explain what this task is asking for in simple terms. The project currently has an automated demo thread that changes roll, pitch, and yaw values directly. I need to use a CSV file instead. Tell me what files likely need to change and what the implementation steps should be.

After that, we asked for implementation help:

Provide the exact steps to implement Task 1. I need a ManeuverScript class that loads maneuvers from a CSV file. Each maneuver should contain roll, pitch, yaw, and duration/seconds values. The class should validate the CSV data, skip invalid or blank/comment lines where appropriate, and provide the next maneuver to the automated demo thread. Do not change anything unrelated to Task 1.

Then we asked how to apply it to the existing project:

Here is my Main.java. Show me exactly where to load default_maneuvers.csv, where to create the ManeuverScript, and how to update the automated demo thread so it uses the script instead of hard-coded values. Return an explanation of your reasoning on what you changed.

We also asked for help checking whether it worked:

How do I know if I did Task 1 right? What should I see when the simulation runs, and how can I test that the CSV maneuver script is actually controlling the aircraft instead of the old hard-coded demo?

What changed

We added ManeuverScript.java. This class loads maneuver instructions from a CSV file and stores them as maneuver objects. Each maneuver contains aircraft control values such as roll, pitch, yaw, and the number of seconds the maneuver should run.

We added support for loading a maneuver script from default_maneuvers.csv. The simulation now loads the script path using a command-line argument when provided, or falls back to default_maneuvers.csv by default.

The automated demo worker in Main.java was changed so that it calls script.next() to get the next maneuver. It then applies the maneuver values to the roll, pitch, and yaw controls and sleeps for the maneuver duration.

This replaced the old approach where the automated demo used hard-coded maneuver values directly in the thread.

Testing

To test Task 1, we ran the simulation with the default maneuver CSV file. The automated demo started and the aircraft changed roll, pitch, and yaw based on the CSV values.

We also checked that changing values in default_maneuvers.csv changed the aircraft behavior when the program was run again. This confirmed that the simulation was reading from the script file instead of only using hard-coded values.

We also checked that invalid CSV data did not silently break the simulation and that the program still had a clear default script path.

## Task 2 – Observer/listener pattern for aircraft state updates


For Task 2 we used the assignment instructions as the main body of the prompt and asked AI to explain the observer pattern in the context of our aircraft simulation. The goal was to understand how the GUI and aircraft controls should communicate without relying on fragile or unrelated update logic.

Task 2 asks me to apply the observer pattern to the aircraft simulation. Explain what this means in simple terms using my project. The simulation has direction controls for roll, pitch, and yaw, and the GUI needs to stay updated when those values change. Tell me what files likely need to change and what the observer/listener relationship should look like.

After that, we asked for implementation help:

Provide the exact steps to implement Task 2. I need a listener interface for direction control updates and changes to DirectionControl so it can register listeners and notify them when values change. Show how this should connect to the GUI without changing unrelated behavior.

Then we asked how to apply it to the existing project:

Here is DirectionControl.java, Main.java, and AircraftGUI.java. Show me exactly where to add the listener interface, where to notify listeners, and how to make the GUI receive the roll, pitch, and yaw controls correctly. Do not change anything unrelated to Task 2. Return an explanation of your reasoning on what you did.

We also asked for help checking whether it worked:

How do I know if I did Task 2 right? What should I see in the GUI and console? How can I test that the aircraft display is using the direction controls correctly?

What changed

We added DirectionControlListener.java. This interface represents something that wants to be notified when a direction control changes.

We updated the direction control logic so that roll, pitch, and yaw can be observed by other parts of the program. This follows the observer pattern because the control object is the subject being watched, and the listener is notified when the control state changes.

We updated the GUI connection so that AircraftGUI receives the real DirectionControl objects for roll, pitch, and yaw. This lets the GUI read the current aircraft orientation directly from the simulation controls instead of relying on unrelated or duplicated state.

The GUI now reflects the actual simulation state more clearly because roll, pitch, and yaw come from the same control objects used by the main simulation loop.

Testing

To test Task 2, we ran the simulation and watched the aircraft display while the automated demo changed roll, pitch, and yaw. The GUI values updated with the same direction control values being used by the simulation.

We checked that the GUI still launched correctly, that the aircraft display continued updating, and that the direction controls were not duplicated into a separate disconnected state.

We also checked that the program still shut down normally when quitting the simulation.
## Task 3 – Self-healing worker threads


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