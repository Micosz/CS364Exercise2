package th.ac.tu.step;

import android.content.Context;

/** Shared counter API for the step branch. */
final class StepCounterManager {
    private int count;

    StepCounterManager(Context context, Runnable changed) {}
    void resume() {}
    void pause() {}
    void setRound(boolean active, int count) { this.count = count; }
    int count() { return count; }
    boolean needsPermission() { return false; }
    String status() { return ""; }
}
