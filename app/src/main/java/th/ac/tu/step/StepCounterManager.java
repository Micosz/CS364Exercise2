package th.ac.tu.step;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;

final class StepCounterManager {
    private final Context context;
    private final SensorManager sensors;
    private final Sensor counter;
    private final Runnable changed;
    private final StepSessionCounter session = new StepSessionCounter();
    private boolean listening;
    private SensorEventListener listener;

    StepCounterManager(Context context, Runnable changed) {
        this.context = context;
        this.changed = changed;
        sensors = context.getSystemService(SensorManager.class);
        counter = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    boolean needsPermission() {
        return counter != null && Build.VERSION.SDK_INT >= 29
            && context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
               != PackageManager.PERMISSION_GRANTED;
    }

    void resume() {
        if (!listening && counter != null && !needsPermission()) {
            session.resetBaseline(SystemClock.elapsedRealtimeNanos());
            SensorEventListener next = new SensorEventListener() {
                private boolean initial = true;

                @Override public void onSensorChanged(SensorEvent event) {
                    // Callbacks queued for an earlier registration are obsolete.
                    if (!listening || listener != this
                            || event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
                    int total = (int) event.values[0];
                    if (initial) {
                        initial = false;
                        session.establishBaseline(total, event.timestamp);
                    } else {
                        session.onReading(total, event.timestamp);
                    }
                    changed.run();
                }

                @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };
            listener = next;
            try {
                listening = sensors.registerListener(next, counter, SensorManager.SENSOR_DELAY_NORMAL);
            } catch (SecurityException e) {
                // Permission may be revoked between checking it and registering.
                listening = false;
            }
            if (!listening) listener = null;
        }
    }

    void pause() {
        SensorEventListener previous = listener;
        listener = null;
        listening = false;
        if (previous != null) sensors.unregisterListener(previous);
        session.resetBaseline(SystemClock.elapsedRealtimeNanos());
    }

    void setRound(boolean active, int count) {
        boolean restart = listening;
        if (restart) pause();
        session.setRound(active, count, SystemClock.elapsedRealtimeNanos());
        // Re-register to request the on-change sensor's current cumulative value,
        // instead of carrying a reading taken during the previous/idle round.
        if (restart) resume();
    }

    int count() { return session.count(); }

    String status() {
        if (counter == null) return "เครื่องนี้ไม่มีเซนเซอร์นับก้าว";
        if (needsPermission()) return "นับก้าวได้เมื่ออนุญาตการตรวจจับกิจกรรม";
        if (!listening) return "เซนเซอร์นับก้าวยังไม่พร้อม";
        return session.waitingForBaseline() ? "กำลังเตรียมตัวนับก้าว กรุณารอค่าตั้งต้นก่อนเดิน" : "";
    }
}
