package de.unijena.bioinf.sirius.elementdetection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The model is loaded by the static initializer of {@link TransformerElementDetector}. A failure there is permanent
 * for the JVM (every later use throws NoClassDefFoundError), so loading must not depend on the state of whichever
 * thread happens to trigger it first -- e.g. a job thread whose job was just cancelled.
 */
class TransformerElementDetectorLoadingTest {

    @Test
    void modelLoadsOnAnInterruptedThread() {
        Thread.currentThread().interrupt();
        try {
            assertNotNull(TransformerElementDetector.readFromClassPath());
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt must be left for the caller to handle");
        } finally {
            Thread.interrupted();
        }
    }
}
