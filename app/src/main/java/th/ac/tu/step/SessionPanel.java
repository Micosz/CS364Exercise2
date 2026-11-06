package th.ac.tu.step;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

final class SessionPanel {
    private final TextView elapsed, rates, totals, record;
    private List<ActivitySession> lastRows;
    private SessionStats stats;

    SessionPanel(Activity screen) {
        elapsed = screen.findViewById(R.id.elapsed);
        rates = screen.findViewById(R.id.rates);
        totals = screen.findViewById(R.id.totals);
        record = screen.findViewById(R.id.record);
    }

    void show(ActivitySession round, List<ActivitySession> rows) {
        // The activity replaces the list after each insert or delete.
        if (rows != lastRows) {
            lastRows = rows;
            stats = SessionStats.from(rows);
            totals.setText("รวม " + rows.size() + " รอบ  ·  " + stats.totalSteps + " ก้าว  ·  เขย่า "
                + stats.totalShakes + " ครั้ง\nสูงสุดต่อรอบ  " + stats.bestSteps + " ก้าว  ·  เขย่า "
                + stats.bestShakes + " ครั้ง");
        }
        long sec = round.durationMs / 1000;
        elapsed.setText(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60));
        rates.setText(String.format(Locale.getDefault(), "เฉลี่ย %.1f ก้าว/นาที  ·  เขย่า %.1f ครั้ง/นาที",
            SessionStats.perMinute(round.steps, round.durationMs),
            SessionStats.perMinute(round.shakes, round.durationMs)));
        boolean newRecord = round.id > 0 && (stats.newStepRecord || stats.newShakeRecord);
        record.setVisibility(newRecord ? View.VISIBLE : View.GONE);
        if (newRecord) {
            String metric = stats.newStepRecord && stats.newShakeRecord ? "ก้าวเดินและการเขย่า"
                : stats.newStepRecord ? "ก้าวเดิน" : "การเขย่า";
            record.setText("New Record · " + metric);
        }
    }
}
