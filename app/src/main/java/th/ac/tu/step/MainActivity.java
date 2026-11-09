package th.ac.tu.step;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class MainActivity extends Activity {
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            render();
            if (active) clock.postDelayed(this, 250);
        }
    };
    private StepCounterManager steps;
    private ShakeDetector shakes;
    private DBHelper store;
    private ActivityAdapter history;
    private SessionPanel panel;
    private List<ActivitySession> rows;
    private boolean active, historyOpen, foreground;
    private long startedAt, accumulated, runningSince = -1;
    private TextView state, stepText, shakeText, sensorText;
    private Button action;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        int pad = root.getPaddingLeft();
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom);
                return insets;
            });
        }
        state = findViewById(R.id.state);
        stepText = findViewById(R.id.steps);
        shakeText = findViewById(R.id.shakes);
        sensorText = findViewById(R.id.sensors);
        action = findViewById(R.id.action);
        steps = new StepCounterManager(this, this::render);
        shakes = new ShakeDetector(this, this::render);
        store = new DBHelper(this);
        history = new ActivityAdapter(this, findViewById(R.id.list), this::confirmDelete);
        panel = new SessionPanel(this);
        if (saved != null) {
            active = saved.getBoolean("active");
            startedAt = saved.getLong("start");
            accumulated = saved.getLong("duration");
            historyOpen = saved.getBoolean("history");
            steps.setRound(active, saved.getInt("steps"));
            shakes.setRound(active, saved.getInt("shakes"));
        }
        rows = store.all();
        history.show(rows, findViewById(R.id.empty));
        findViewById(R.id.trackTab).setOnClickListener(v -> tab(false));
        findViewById(R.id.historyTab).setOnClickListener(v -> tab(true));
        action.setOnClickListener(v -> { if (active) saveRound(); else startRound(); });
        tab(historyOpen);
        render();
    }

    private void tab(boolean showHistory) {
        historyOpen = showHistory;
        findViewById(R.id.tracking).setVisibility(showHistory ? View.GONE : View.VISIBLE);
        findViewById(R.id.history).setVisibility(showHistory ? View.VISIBLE : View.GONE);
        findViewById(R.id.trackTab).setEnabled(showHistory);
        findViewById(R.id.historyTab).setEnabled(!showHistory);
    }

    private void startRound() {
        active = true;
        accumulated = 0;
        startedAt = System.currentTimeMillis();
        runningSince = SystemClock.elapsedRealtime();
        steps.setRound(true, 0);
        shakes.setRound(true, 0);
        clock.removeCallbacks(tick);
        tick.run();
        if (Build.VERSION.SDK_INT >= 29 && steps.needsPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 41);
        }
    }

    private long elapsed() {
        return accumulated + (runningSince < 0 ? 0 : SystemClock.elapsedRealtime() - runningSince);
    }

    private ActivitySession current() {
        return new ActivitySession(0, startedAt, elapsed(), steps.count(), shakes.count());
    }

    private void saveRound() {
        try {
            store.insert(current());
        } catch (android.database.SQLException e) {
            toast("บันทึกไม่สำเร็จ กรุณาลองอีกครั้ง");
            return;
        }
        active = false;
        clock.removeCallbacks(tick);
        runningSince = -1;
        steps.setRound(false, steps.count());
        shakes.setRound(false, shakes.count());
        refresh();
        toast("บันทึกกิจกรรมแล้ว");
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this).setTitle("ลบกิจกรรมนี้?")
            .setMessage("ยอดรวมและสถิติจะคำนวณใหม่จากกิจกรรมที่เหลือ")
            .setNegativeButton("ยกเลิก", null).setPositiveButton("ลบ", (dialog, which) -> {
                try {
                    if (store.delete(id)) refresh();
                } catch (android.database.SQLException e) {
                    toast("ลบไม่สำเร็จ กรุณาลองอีกครั้ง");
                }
            }).show();
    }

    private void refresh() {
        rows = store.all();
        history.show(rows, findViewById(R.id.empty));
        render();
    }

    private void render() {
        if (rows == null) return;
        ActivitySession round = active ? current() : rows.isEmpty()
            ? new ActivitySession(0, 0, 0, 0, 0) : rows.get(0);
        state.setText(active ? "กำลังบันทึกกิจกรรม" : rows.isEmpty() ? "พร้อมเริ่มกิจกรรม" : "กิจกรรมล่าสุด");
        action.setText(active ? "จบและบันทึกกิจกรรม" : "เริ่มกิจกรรม");
        stepText.setText(Integer.toString(round.steps));
        shakeText.setText(Integer.toString(round.shakes));
        String status = (steps.status() + "  " + shakes.status()).trim();
        sensorText.setText(status);
        sensorText.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        panel.show(round, rows);
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (active) runningSince = SystemClock.elapsedRealtime();
        steps.resume();
        shakes.resume();
        tick.run();
    }

    @Override protected void onPause() {
        foreground = false;
        accumulated = elapsed();
        runningSince = -1;
        clock.removeCallbacks(tick);
        steps.pause();
        shakes.pause();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("active", active);
        out.putBoolean("history", historyOpen);
        out.putLong("start", startedAt);
        out.putLong("duration", elapsed());
        out.putInt("steps", steps.count());
        out.putInt("shakes", shakes.count());
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 41 && foreground) steps.resume();
        render();
    }

    @Override protected void onDestroy() { store.close(); super.onDestroy(); }
}
