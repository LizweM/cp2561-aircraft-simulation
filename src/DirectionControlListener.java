/*
 * Observer interface for DirectionControl value changes.
 * Implemented by any class that wants to be notified when
 * a DirectionControl updates its current value.
 */
public interface DirectionControlListener {
    void onDirectionChanged(DirectionControl control);
}