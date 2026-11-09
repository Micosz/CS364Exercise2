package th.ac.tu.step;

/** Converts timestamped cumulative readings into the current foreground round. */
final class StepSessionCounter {
    private boolean active;
    private int count, previous = -1;
    private long acceptFrom = Long.MIN_VALUE, lastTimestamp = Long.MIN_VALUE;

    void setRound(boolean active, int count, long timestamp) {
        this.active = active;
        this.count = count;
        resetBaseline(timestamp);
    }

    void resetBaseline(long timestamp) {
        previous = -1;
        acceptFrom = timestamp;
        lastTimestamp = Long.MIN_VALUE;
    }

    void establishBaseline(int total, long timestamp) {
        if (total < 0) return;
        // An on-change sensor supplies its current value at registration. The
        // cached sample can predate registration when the user is standing still.
        previous = total;
        lastTimestamp = timestamp;
    }

    void onReading(int total, long timestamp) {
        // Queued readings from before this round/resume must not change its baseline.
        if (total < 0 || timestamp < acceptFrom || timestamp <= lastTimestamp) return;
        lastTimestamp = timestamp;
        if (active && previous >= 0 && total > previous) count += total - previous;
        // The first reading establishes a baseline. A sensor reset also re-bases here.
        previous = total;
    }

    int count() { return count; }
    boolean waitingForBaseline() { return previous < 0; }
}
