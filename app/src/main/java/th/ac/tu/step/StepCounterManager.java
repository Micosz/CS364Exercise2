package th.ac.tu.step;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

final class StepCounterManager implements SensorEventListener {
    private final Context context;
    private final SensorManager sensors;
    private final Sensor counter;
    private final Runnable changed;
    private boolean listening;
    private int previous = -1, count;

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
            previous = -1;
            listening = sensors.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    void pause() {
        sensors.unregisterListener(this);
        listening = false;
        previous = -1;
    }

    void setRound(boolean active, int count) {
        this.count = count;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!listening || event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;
        int total = (int) event.values[0];
        // On first registration the hardware reports its cumulative count since boot.
        previous = total;
        changed.run();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    int count() { return count; }

    String status() {
        if (counter == null) return "เครื่องนี้ไม่มีเซนเซอร์นับก้าว";
        if (needsPermission()) return "นับก้าวได้เมื่ออนุญาตการตรวจจับกิจกรรม";
        if (!listening) return "เซนเซอร์นับก้าวยังไม่พร้อม";
        return previous < 0 ? "กำลังรอค่าเริ่มต้นจากเซนเซอร์ก้าว" : "";
    }
}
