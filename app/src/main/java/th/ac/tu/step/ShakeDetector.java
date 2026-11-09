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
        SensorManager sm = context != null ? context.getSystemService(SensorManager.class) : null;
        sensors = sm;
        accelerometer = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.changed = changed;
    }

    ShakeDetector(SensorManager sensors, Sensor accelerometer, Runnable changed) {
        this.sensors = sensors;
        this.accelerometer = accelerometer;
        this.changed = changed;
    }

    void resume() {
        if (!listening && accelerometer != null && sensors != null) {
            armed = true;
            listening = sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    void pause() {
        if (sensors != null) sensors.unregisterListener(this);
        listening = false;
    }

    void setRound(boolean active, int count) {
        setRound(active, count, SystemClock.elapsedRealtimeNanos());
    }

    void setRound(boolean active, int count, long timestamp) {
        this.active = active;
        this.count = count;
        armed = true;
        lastShake = -MIN_GAP_NS;
        roundStart = timestamp;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event == null || !listening) return;
        if (event.sensor != null && event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        if (onReading(event.values[0], event.values[1], event.values[2], event.timestamp)) {
            if (changed != null) changed.run();
        }
    }

    boolean onReading(float x, float y, float z, long timestamp) {
        if (!active || timestamp < roundStart) return false;
        double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        if (g < RELEASE_G) armed = true;
        if (g >= TRIGGER_G && armed) {
            armed = false;
            if (timestamp - lastShake >= MIN_GAP_NS) {
                count++;
                lastShake = timestamp;
                return true;
            }
        }
        return false;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    int count() { return count; }
    boolean needsPermission() { return false; }
    void setListening(boolean listening) { this.listening = listening; }
    boolean isListening() { return listening; }

    String status() {
        if (accelerometer == null) return "เครื่องนี้ไม่มีเซนเซอร์ตรวจจับการเขย่า";
        return listening ? "" : "เซนเซอร์เขย่ายังไม่พร้อม";
    }
}
