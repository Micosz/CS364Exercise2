package th.ac.tu.step;

import org.junit.Test;
import static org.junit.Assert.*;

public final class StepSessionCounterTest {
    @Test public void firstWalkingBatchIsNotDiscardedWhenRoundStarts() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.onReading(4200, 90);
        counter.setRound(true, 0, 100);
        counter.onReading(4220, 110);
        assertEquals(20, counter.count());
    }

    @Test public void cachedInitialBaselineAllowsCountingTheFirstNewStep() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(4200, 90);
        assertFalse(counter.waitingForBaseline());
        counter.onReading(4201, 110);
        assertEquals(1, counter.count());
    }

    @Test public void firstReadingIsBaselineInsteadOfBootTotal() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        assertTrue(counter.waitingForBaseline());
        counter.onReading(4200, 110);
        assertEquals(0, counter.count());
        assertFalse(counter.waitingForBaseline());
        counter.onReading(4217, 120);
        assertEquals(17, counter.count());
    }

    @Test public void nextRoundDoesNotIncludeWalkingBetweenRounds() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(1000, 110);
        counter.onReading(1010, 120);
        counter.setRound(false, counter.count(), 130);
        counter.onReading(1013, 140);
        assertEquals(10, counter.count());
        counter.setRound(true, 0, 150);
        // This cumulative reading also includes steps taken between rounds.
        counter.onReading(1018, 145);
        assertEquals(0, counter.count());
        counter.onReading(1028, 170);
        assertEquals(10, counter.count());
    }

    @Test public void orderedPreStartReadingUpdatesBaselineWithoutCounting() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 90);
        assertEquals(0, counter.count());
        counter.onReading(105, 95); // Delivered late, but all these steps predate start.
        assertEquals(0, counter.count());
        counter.onReading(107, 110);
        assertEquals(2, counter.count());
    }

    @Test public void outOfOrderReadingCannotLowerBaselineAndDoubleCount() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 110);
        counter.onReading(110, 130);
        counter.onReading(105, 120);
        counter.onReading(112, 140);
        assertEquals(12, counter.count());
    }

    @Test public void duplicateTimestampDoesNotChangeBaseline() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 110);
        counter.onReading(110, 120);
        counter.onReading(105, 120);
        counter.onReading(112, 130);
        assertEquals(12, counter.count());
    }

    @Test public void resumeKeepsCountAndExcludesBackgroundWalking() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 110);
        counter.onReading(105, 120);
        counter.resetBaseline(200);
        // Re-registration supplies the current total even if its last step was
        // before resume. That initial value excludes all background walking.
        counter.onReading(120, 180);
        assertFalse(counter.waitingForBaseline());
        counter.onReading(115, 170); // A queued older sample cannot overwrite it.
        assertEquals(5, counter.count());
        counter.onReading(123, 220);
        assertEquals(8, counter.count());
    }

    @Test public void restoredRoundKeepsSavedCount() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 17, 100);
        counter.onReading(5000, 110);
        counter.onReading(5003, 120);
        assertEquals(20, counter.count());
    }

    @Test public void sensorCounterResetDoesNotSubtractOrJumpCount() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 110);
        counter.onReading(105, 120);
        counter.onReading(0, 130);
        assertEquals(5, counter.count());
        counter.onReading(3, 140);
        assertEquals(8, counter.count());
    }

    @Test public void idleReadingsProvideTheBaselineForTheFirstActiveBatch() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.onReading(1000, 10);
        counter.onReading(1500, 20);
        assertEquals(0, counter.count());
        counter.setRound(true, 0, 100);
        counter.onReading(1505, 110);
        assertEquals(5, counter.count());
        counter.onReading(1510, 120);
        assertEquals(10, counter.count());
    }

    @Test public void stoppedRoundKeepsItsFinalCount() {
        StepSessionCounter counter = new StepSessionCounter();
        counter.setRound(true, 0, 100);
        counter.onReading(100, 110);
        counter.onReading(110, 120);
        counter.setRound(false, counter.count(), 130);
        counter.onReading(200, 140);
        counter.onReading(250, 150);
        assertEquals(10, counter.count());
    }
}
