package th.ac.tu.step;

/** Immutable snapshot of one round. The store assigns the ID when saved. */
final class ActivitySession {
    final long id, startedAt, durationMs;
    final int steps, shakes;

    ActivitySession(long id, long startedAt, long durationMs, int steps, int shakes) {
        this.id = id;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.steps = steps;
        this.shakes = shakes;
    }

    ActivitySession withId(long id) {
        return new ActivitySession(id, startedAt, durationMs, steps, shakes);
    }
}
