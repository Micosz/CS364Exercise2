package th.ac.tu.step;

import org.junit.Test;
import static org.junit.Assert.*;

public final class StepCounterManagerTest {
    private static final class FakeSensor implements StepCounterManager.SensorSource {
        boolean available = true, permission = true, registers = true, throwsSecurity;
        boolean initialDuringStart;
        int starts, stops;
        StepCounterManager.ReadingListener listener;

        @Override public boolean available() { return available; }
        @Override public boolean needsPermission() { return !permission; }
        @Override public boolean start(StepCounterManager.ReadingListener listener) {
            starts++;
            this.listener = listener;
            if (throwsSecurity) throw new SecurityException("revoked");
            if (initialDuringStart) listener.onReading(4200, 90);
            return registers;
        }
        @Override public void stop() { stops++; }
        void emit(int total, long timestamp) { listener.onReading(total, timestamp); }
    }

    private static final class Clock {
        long value;
        long now() { return value; }
    }

    @Test public void startRoundKeepsLiveBaselineAndCountsTheEntireFirstWalkingBatch() {
        FakeSensor sensor = new FakeSensor();
        Clock clock = new Clock();
        StepCounterManager manager = new StepCounterManager(sensor, clock::now, () -> {});
        manager.resume();
        sensor.emit(4200, 90);
        clock.value = 100;
        manager.setRound(true, 0);
        assertEquals(1, sensor.starts);
        assertEquals(0, sensor.stops);
        assertEquals("", manager.status());
        sensor.emit(4220, 110);
        assertEquals(20, manager.count());
    }

    @Test public void firstRoundWaitsForPermissionAndAnActualBaseline() {
        FakeSensor sensor = new FakeSensor();
        sensor.permission = false;
        StepCounterManager manager = new StepCounterManager(sensor, () -> 100, () -> {});
        assertFalse(manager.readyForRound());
        sensor.permission = true;
        manager.resume();
        assertFalse(manager.readyForRound());
        sensor.emit(4200, 90);
        assertTrue(manager.readyForRound());
        manager.setRound(true, 0);
        sensor.emit(4220, 110);
        assertEquals(20, manager.count());
    }

    @Test public void startingANewRoundDoesNotWaitForAnotherInitialEvent() {
        FakeSensor sensor = new FakeSensor();
        Clock clock = new Clock();
        StepCounterManager manager = new StepCounterManager(sensor, clock::now, () -> {});
        manager.resume();
        sensor.emit(100, 10);
        clock.value = 20;
        manager.setRound(true, 0);
        sensor.emit(110, 30);
        clock.value = 40;
        manager.setRound(false, manager.count());
        sensor.emit(115, 50); // Walking between rounds updates the live baseline.
        clock.value = 60;
        manager.setRound(true, 0);
        sensor.emit(125, 70);
        assertEquals(10, manager.count());
        assertEquals(1, sensor.starts);
    }

    @Test public void callbackFromBeforePauseCannotOverwriteTheResumedBaseline() {
        FakeSensor sensor = new FakeSensor();
        Clock clock = new Clock();
        StepCounterManager manager = new StepCounterManager(sensor, clock::now, () -> {});
        manager.resume();
        sensor.emit(100, 10);
        manager.setRound(true, 0);
        sensor.emit(105, 20);
        StepCounterManager.ReadingListener old = sensor.listener;
        manager.pause();
        clock.value = 100;
        manager.resume();
        sensor.emit(120, 90); // Current cached value: exclude background steps.
        old.onReading(1000, 110);
        sensor.emit(123, 120);
        assertEquals(8, manager.count());
    }

    @Test public void initialReadingDeliveredDuringRegistrationIsAccepted() {
        FakeSensor sensor = new FakeSensor();
        sensor.initialDuringStart = true;
        Clock clock = new Clock();
        clock.value = 100;
        StepCounterManager manager = new StepCounterManager(sensor, clock::now, () -> {});
        manager.resume();
        manager.setRound(true, 0);
        sensor.emit(4201, 110);
        assertEquals(1, manager.count());
    }

    @Test public void deniedPermissionDoesNotRegisterAndGrantCanResumeSameRound() {
        FakeSensor sensor = new FakeSensor();
        sensor.permission = false;
        StepCounterManager manager = new StepCounterManager(sensor, () -> 100, () -> {});
        manager.setRound(true, 0);
        manager.resume();
        assertTrue(manager.needsPermission());
        assertEquals(0, sensor.starts);
        assertTrue(manager.status().contains("อนุญาต"));
        sensor.permission = true;
        manager.resume();
        sensor.emit(100, 90);
        sensor.emit(103, 110);
        assertEquals(3, manager.count());
    }

    @Test public void unavailableSensorDoesNotRegisterOrRequestPermission() {
        FakeSensor sensor = new FakeSensor();
        sensor.available = false;
        sensor.permission = false;
        StepCounterManager manager = new StepCounterManager(sensor, () -> 100, () -> {});
        manager.resume();
        manager.setRound(true, 0);
        assertEquals(0, sensor.starts);
        assertFalse(manager.needsPermission());
        assertTrue(manager.readyForRound()); // Other activity features remain usable.
        assertTrue(manager.status().contains("ไม่มีเซนเซอร์"));
    }

    @Test public void failedRegistrationIgnoresCallbacksAndCanRetry() {
        FakeSensor sensor = new FakeSensor();
        sensor.registers = false;
        StepCounterManager manager = new StepCounterManager(sensor, () -> 100, () -> {});
        manager.resume();
        StepCounterManager.ReadingListener failed = sensor.listener;
        sensor.registers = true;
        manager.resume();
        sensor.emit(100, 90);
        manager.setRound(true, 0);
        failed.onReading(1000, 110);
        sensor.emit(102, 120);
        assertEquals(2, manager.count());
        assertEquals(2, sensor.starts);
    }

    @Test public void permissionRevokedDuringRegistrationDoesNotCrash() {
        FakeSensor sensor = new FakeSensor();
        sensor.throwsSecurity = true;
        StepCounterManager manager = new StepCounterManager(sensor, () -> 100, () -> {});
        manager.resume();
        assertTrue(manager.status().contains("ยังไม่พร้อม"));
        assertEquals(0, manager.count());
    }
}
