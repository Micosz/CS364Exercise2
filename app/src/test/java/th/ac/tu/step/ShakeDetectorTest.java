package th.ac.tu.step;

import android.hardware.SensorManager;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ShakeDetectorTest {
    private static final float G = SensorManager.GRAVITY_EARTH;
    private static final long SEC_NS = 1_000_000_000L;

    @Test public void rhythmicShakesIncrementCountSequentially() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 0, time);

        // Perform 5 rhythmic shakes spaced 1 second apart
        for (int i = 1; i <= 5; i++) {
            time += SEC_NS;
            // Peak exceeding TRIGGER_G (2.25g)
            assertTrue("Peak " + i + " should trigger shake",
                detector.onReading(0f, 0f, 3.0f * G, time));
            assertEquals(i, detector.count());

            // Return to near gravity (< RELEASE_G = 1.35g)
            time += 200_000_000L;
            assertFalse(detector.onReading(0f, 0f, 1.0f * G, time));
        }
        assertEquals(5, detector.count());
    }

    @Test public void singleShakeWithMultipleSamplesCountsOnlyOnce() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 0, time);

        // Multiple samples during a single 200ms shake impulse
        assertTrue(detector.onReading(0f, 0f, 2.5f * G, time + 20_000_000L));
        assertFalse(detector.onReading(0f, 0f, 3.2f * G, time + 40_000_000L));
        assertFalse(detector.onReading(0f, 0f, 2.8f * G, time + 60_000_000L));
        assertFalse(detector.onReading(0f, 0f, 2.3f * G, time + 80_000_000L));

        assertEquals(1, detector.count());
    }

    @Test public void reboundWithinMinGapDoesNotDoubleCount() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 0, time);

        // First shake
        assertTrue(detector.onReading(0f, 0f, 3.0f * G, time));
        assertEquals(1, detector.count());

        // Quick release at 150ms
        detector.onReading(0f, 0f, 1.0f * G, time + 150_000_000L);

        // Rebound peak at 300ms (< 500ms MIN_GAP_NS)
        assertFalse("Rebound within 500ms must not be counted",
            detector.onReading(0f, 0f, 3.0f * G, time + 300_000_000L));
        assertEquals(1, detector.count());

        // Release at 400ms
        detector.onReading(0f, 0f, 1.0f * G, time + 400_000_000L);

        // Next valid shake at 600ms (>= 500ms MIN_GAP_NS)
        assertTrue("Shake after 500ms gap should be counted",
            detector.onReading(0f, 0f, 3.0f * G, time + 600_000_000L));
        assertEquals(2, detector.count());
    }

    @Test public void stillDeviceAtOneGDoesNotTrigger() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 0, time);

        // Still on flat surface: 1.0g for 15 seconds (sampled every 20ms)
        for (int i = 0; i < 750; i++) {
            time += 20_000_000L;
            assertFalse(detector.onReading(0f, 0f, 1.0f * G, time));
        }
        assertEquals(0, detector.count());
    }

    @Test public void normalMovementBelowTriggerDoesNotTrigger() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 0, time);

        // Walking / handling phone: acceleration fluctuates between 1.1g and 1.9g (< 2.25g)
        float[] sampleGs = {1.2f, 1.5f, 1.8f, 1.6f, 1.3f, 1.9f, 1.4f};
        for (float g : sampleGs) {
            time += 100_000_000L;
            assertFalse(detector.onReading(g * G, 0f, 0f, time));
        }
        assertEquals(0, detector.count());
    }

    @Test public void inactiveRoundIgnoresShakes() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(false, 0, time);

        assertFalse(detector.onReading(0f, 0f, 3.0f * G, time + SEC_NS));
        assertEquals(0, detector.count());
    }

    @Test public void sessionDeltaResetRoundStartsAtZero() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;

        // Round 1
        detector.setRound(true, 0, time);
        time += SEC_NS;
        detector.onReading(0f, 0f, 3.0f * G, time);
        time += 200_000_000L;
        detector.onReading(0f, 0f, 1.0f * G, time);
        assertEquals(1, detector.count());

        // Stop Round 1
        time += SEC_NS;
        detector.setRound(false, detector.count(), time);
        assertEquals(1, detector.count());

        // Shaking between rounds should not change previous round's count
        time += SEC_NS;
        detector.onReading(0f, 0f, 3.5f * G, time);
        assertEquals(1, detector.count());

        // Round 2 starts at 0
        time += SEC_NS;
        detector.setRound(true, 0, time);
        assertEquals(0, detector.count());

        // New shake in Round 2
        time += SEC_NS;
        assertTrue(detector.onReading(0f, 0f, 3.0f * G, time));
        assertEquals(1, detector.count());
    }

    @Test public void staleSamplesBeforeRoundStartAreIgnored() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long roundStart = 5 * SEC_NS;
        detector.setRound(true, 0, roundStart);

        // Event timestamp is older than round start
        assertFalse(detector.onReading(0f, 0f, 4.0f * G, roundStart - 1));
        assertEquals(0, detector.count());

        // Event timestamp at or after round start is accepted
        assertTrue(detector.onReading(0f, 0f, 4.0f * G, roundStart + 100));
        assertEquals(1, detector.count());
    }

    @Test public void restoredSessionKeepsSavedCountAndContinues() {
        ShakeDetector detector = new ShakeDetector(null, null, null);
        detector.setListening(true);
        long time = SEC_NS;
        detector.setRound(true, 7, time);
        assertEquals(7, detector.count());

        time += SEC_NS;
        assertTrue(detector.onReading(0f, 0f, 3.0f * G, time));
        assertEquals(8, detector.count());
    }

    @Test public void statusReportsSensorAvailability() {
        ShakeDetector noSensor = new ShakeDetector(null, null, null);
        assertEquals("เครื่องนี้ไม่มีเซนเซอร์ตรวจจับการเขย่า", noSensor.status());
        assertFalse(noSensor.needsPermission());

        noSensor.setListening(false);
        assertEquals("เครื่องนี้ไม่มีเซนเซอร์ตรวจจับการเขย่า", noSensor.status());
    }
}
