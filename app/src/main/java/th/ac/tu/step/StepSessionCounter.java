package th.ac.tu.step;

/** Converts timestamped cumulative readings into the current foreground round. */
final class StepSessionCounter {
    private boolean active;
    private int count, previous = -1;
    private long acceptFrom = Long.MIN_VALUE, lastTimestamp = Long.MIN_VALUE;

    void setRound(boolean active, int count, long timestamp) {
        this.active = active;
        this.count = count;
        acceptFrom = timestamp;
        // Keep the live sensor's latest cumulative value. Resetting it here would
        // discard the first walking batch if no new initial event is delivered.
    }

    void resetBaseline(long timestamp) {
        previous = -1;
        acceptFrom = timestamp;
        lastTimestamp = Long.MIN_VALUE;
    }

    void onReading(int total, long timestamp) {
        if (total < 0 || timestamp <= lastTimestamp) return;
        if (active && timestamp >= acceptFrom && previous >= 0 && total > previous) {
            count += total - previous;
        }
        // An ordered pre-round sample delivered late is still a newer baseline,
        // but contributes no steps. Older/duplicate samples were rejected above.
        previous = total;
        lastTimestamp = timestamp;
    }

    int count() { return count; }
    boolean waitingForBaseline() { return previous < 0; }
}
