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
import android.util.Log;
import java.util.function.LongSupplier;

final class StepCounterManager {
    interface ReadingListener { void onReading(int total, long timestamp); }

    interface SensorSource {
        boolean available();
        boolean needsPermission();
        boolean start(ReadingListener listener);
        void stop();
    }

    private final SensorSource source;
    private final LongSupplier now;
    private final Runnable changed;
    private final StepSessionCounter session = new StepSessionCounter();
    private boolean listening;
    private long registration;

    StepCounterManager(Context context, Runnable changed) {
        this(new AndroidSensorSource(context), SystemClock::elapsedRealtimeNanos, changed);
    }

    // Use a controlled sensor in tests to verify the same lifecycle as the app.
    StepCounterManager(SensorSource source, LongSupplier now, Runnable changed) {
        this.source = source;
        this.now = now;
        this.changed = changed;
    }

    boolean needsPermission() { return source.available() && source.needsPermission(); }

    void resume() {
        if (listening || !source.available() || needsPermission()) return;
        session.resetBaseline(now.getAsLong());
        long current = ++registration;
        // Allow an initial reading even if a source delivers it during start().
        listening = true;
        try {
            boolean registered = source.start((total, timestamp) -> {
                if (!listening || registration != current) return;
                session.onReading(total, timestamp);
                changed.run();
            });
            if (registered) return;
        } catch (SecurityException e) {
            // Permission can be revoked between checking and registering.
        }
        listening = false;
        registration++;
        source.stop();
        session.resetBaseline(now.getAsLong());
    }

    void pause() {
        listening = false;
        registration++;
        source.stop();
        session.resetBaseline(now.getAsLong());
    }

    void setRound(boolean active, int count) {
        // Starting/stopping a round changes the count window, not registration.
        // Keep the sensor's latest cumulative reading as the new round's base.
        session.setRound(active, count, now.getAsLong());
    }

    int count() { return session.count(); }

    boolean readyForRound() {
        return !source.available()
            || (listening && !needsPermission() && !session.waitingForBaseline());
    }

    String status() {
        if (!source.available()) return "เครื่องนี้ไม่มีเซนเซอร์นับก้าว";
        if (needsPermission()) return "นับก้าวได้เมื่ออนุญาตการตรวจจับกิจกรรม";
        if (!listening) return "เซนเซอร์นับก้าวยังไม่พร้อม";
        return session.waitingForBaseline() ? "กำลังรอค่าตั้งต้นจากเซนเซอร์นับก้าว" : "";
    }

    private static final class AndroidSensorSource implements SensorSource {
        private static final String TAG = "StepCounter";
        private final Context context;
        private final SensorManager sensors;
        private final Sensor counter;
        private SensorEventListener listener;

        AndroidSensorSource(Context context) {
            this.context = context;
            sensors = context.getSystemService(SensorManager.class);
            counter = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        @Override public boolean available() { return counter != null; }

        @Override public boolean needsPermission() {
            return Build.VERSION.SDK_INT >= 29
                && context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED;
        }

        @Override public boolean start(ReadingListener readings) {
            listener = new SensorEventListener() {
                @Override public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
                    Log.d(TAG, "raw=" + event.values[0] + " timestamp=" + event.timestamp);
                    readings.onReading((int) event.values[0], event.timestamp);
                }

                @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };
            try {
                boolean registered = sensors.registerListener(listener, counter,
                    SensorManager.SENSOR_DELAY_NORMAL);
                Log.i(TAG, "register=" + registered + " sensor=" + counter.getName());
                return registered;
            } catch (SecurityException e) {
                Log.w(TAG, "Activity recognition permission denied", e);
                throw e;
            }
        }

        @Override public void stop() {
            if (listener != null) sensors.unregisterListener(listener);
            listener = null;
        }
    }
}
