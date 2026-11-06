package th.ac.tu.step;

import java.util.List;

final class SessionStats {
    long totalSteps, totalShakes;
    int bestSteps, bestShakes;
    boolean newStepRecord, newShakeRecord;

    // Storage returns the most recently saved session first.
    static SessionStats from(List<ActivitySession> rows) {
        SessionStats stats = new SessionStats();
        if (rows.isEmpty()) return stats;
        int olderBestSteps = 0, olderBestShakes = 0;
        for (int i = 0; i < rows.size(); i++) {
            ActivitySession row = rows.get(i);
            stats.totalSteps += row.steps;
            stats.totalShakes += row.shakes;
            stats.bestSteps = Math.max(stats.bestSteps, row.steps);
            stats.bestShakes = Math.max(stats.bestShakes, row.shakes);
            if (i > 0) {
                olderBestSteps = Math.max(olderBestSteps, row.steps);
                olderBestShakes = Math.max(olderBestShakes, row.shakes);
            }
        }
        stats.newStepRecord = rows.get(0).steps > olderBestSteps;
        stats.newShakeRecord = rows.get(0).shakes > olderBestShakes;
        return stats;
    }

    static double perMinute(int count, long elapsedMs) {
        return elapsedMs <= 0 ? 0 : count * 60_000d / elapsedMs;
    }
}
