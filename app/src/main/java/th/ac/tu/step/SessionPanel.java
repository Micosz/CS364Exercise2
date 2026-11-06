package th.ac.tu.step;

import android.app.Activity;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

final class SessionPanel {
    private final TextView elapsed, totals;

    SessionPanel(Activity screen) {
        elapsed = screen.findViewById(R.id.elapsed);
        totals = screen.findViewById(R.id.totals);
    }

    void show(ActivitySession round, List<ActivitySession> rows) {
        long seconds = round.durationMs / 1000;
        elapsed.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
        totals.setText("บันทึกแล้ว " + rows.size() + " รอบ");
    }
}
