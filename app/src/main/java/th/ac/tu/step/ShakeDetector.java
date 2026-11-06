package th.ac.tu.step;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

final class ShakeDetector implements SensorEventListener {
    // Count a peak once, re-arm near gravity, and ignore closely spaced peaks.
    static final double TRIGGER_G = 2.25, RELEASE_G = 1.35;
    static final long MIN_GAP_NS = 500_000_000L;
    private final SensorManager sensors;
    private final Sensor accelerometer;
    private final Runnable changed;
    private boolean listening, active, armed;
    private long roundStart, lastShake;
    private int count;

    ShakeDetector(Context context, Runnable changed) {
        sensors = context.getSystemService(SensorManager.class);
        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.changed = changed;
    }

    void resume() {
        if (!listening && accelerometer != null) {
            armed = true;
            listening = sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    void pause() {
        sensors.unregisterListener(this);
        listening = false;
    }

    void setRound(boolean active, int count) {
        this.active = active;
        this.count = count;
        armed = true;
        lastShake = -MIN_GAP_NS;
        roundStart = SystemClock.elapsedRealtimeNanos();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!listening || !active || event.timestamp < roundStart) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        if (g < RELEASE_G) armed = true;
        if (g >= TRIGGER_G && armed) {
            armed = false;
            if (event.timestamp - lastShake >= MIN_GAP_NS) {
                count++;
                lastShake = event.timestamp;
                changed.run();
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    int count() { return count; }
    boolean needsPermission() { return false; }

    String status() {
        if (accelerometer == null) return "เครื่องนี้ไม่มีเซนเซอร์ตรวจจับการเขย่า";
        return listening ? "" : "เซนเซอร์เขย่ายังไม่พร้อม";
    }
}
