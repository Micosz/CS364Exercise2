package th.ac.tu.step;

import android.content.Context;

/** Shared counter API for the shake branch. */
final class ShakeDetector {
    private int count;

    ShakeDetector(Context context, Runnable changed) {}
    void resume() {}
    void pause() {}
    void setRound(boolean active, int count) { this.count = count; }
    int count() { return count; }
    boolean needsPermission() { return false; }
    String status() { return ""; }
}
