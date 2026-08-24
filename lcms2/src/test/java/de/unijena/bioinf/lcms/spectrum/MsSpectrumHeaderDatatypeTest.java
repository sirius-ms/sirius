package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import org.h2.mvstore.WriteBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every field of a spectrum header has to survive being written and read back.
 * <p>
 * The serializer is written by hand, so a field added to the header is not stored until someone adds it here too, and
 * nothing fails when they do not: the value simply comes back as whatever the constructor defaults to. Whether it
 * survives then depends on whether the store happens to hand back the instance that was put in or one it
 * deserialised, which is a worse failure than losing it outright because it is intermittent.
 */
public class MsSpectrumHeaderDatatypeTest {

    private static Ms1SpectrumHeader roundTrip(Ms1SpectrumHeader header) {
        final MsSpectrumHeaderDatatype datatype = new MsSpectrumHeaderDatatype();
        final WriteBuffer out = new WriteBuffer(1024);
        datatype.write(out, header);
        final ByteBuffer in = out.getBuffer().flip();
        return datatype.read(in);
    }

    @Test
    public void anMs1HeaderSurvives() {
        final Ms1SpectrumHeader header = new Ms1SpectrumHeader(7, 42, "scan=42", -1, true);
        final Ms1SpectrumHeader back = roundTrip(header);
        assertEquals(7, back.getUid());
        assertEquals("scan=42", back.getSourceId());
        assertEquals(-1, back.polarity);
        assertTrue(back.centroided);
        assertFalse(back instanceof Ms2SpectrumHeader, "an MS1 header may not come back as an MS2 one");
    }

    @Test
    public void everyFieldOfAnMs2HeaderSurvives() {
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(11, 99, "controllerType=0 scan=99", 1, 2, true,
                new CollisionEnergy(35), new IsolationWindow(0d, 2d), 88, 237.1486, 237.1486, 552.5);
        final Ms1SpectrumHeader read = roundTrip(header);
        assertInstanceOf(Ms2SpectrumHeader.class, read);
        final Ms2SpectrumHeader back = (Ms2SpectrumHeader) read;

        assertEquals(11, back.getUid());
        assertEquals(99, back.getScanId());
        assertEquals("controllerType=0 scan=99", back.getSourceId());
        assertEquals(1, back.polarity);
        assertEquals(2, back.getMsLevel());
        assertTrue(back.centroided);
        assertEquals(88, back.getParentId());
        assertEquals(237.1486, back.getPrecursorMz(), 1e-9);
        assertEquals(552.5, back.getRetentionTime(), 1e-9);
        assertTrue(back.getEnergy().isPresent());
        assertTrue(back.getIsolationWindow().isPresent());
        assertEquals(2d, back.getIsolationWindow().get().getWindowWidth(), 1e-9);
    }

    /**
     * The targeted mass is what the instrument was asked to isolate and the precursor mass is what was found in it;
     * for a wide isolation window they are far apart, so one cannot stand in for the other.
     */
    @Test
    public void theTargetedMassSurvivesSeparatelyFromThePrecursor() {
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(1, 2, "scan=2", 1, 2, true, new CollisionEnergy(35),
                new IsolationWindow(0d, 100d), 3, 214.0038, 200.0, 100d);
        final Ms2SpectrumHeader back = (Ms2SpectrumHeader) roundTrip(header);
        assertEquals(214.0038, back.getPrecursorMz(), 1e-9);
        assertEquals(200.0, back.getTargetedMz(), 1e-9, "the targeted mass is not the precursor mass");
    }

    /**
     * Whether a spectrum was acquired data independently decides which quality checks apply to it, so it has to be a
     * property of the spectrum rather than something a reader infers from the width of the isolation window.
     */
    @Test
    public void theDataIndependentFlagSurvives() {
        final Ms2SpectrumHeader dia = new Ms2SpectrumHeader(1, 2, "scan=2", 1, 2, true, new CollisionEnergy(35),
                new IsolationWindow(0d, 100d), 3, 214.0038, 214.0038, 100d).asDataIndependent();
        assertTrue(dia.isDataIndependent(), "the fixture has to be marked in the first place");
        assertTrue(((Ms2SpectrumHeader) roundTrip(dia)).isDataIndependent());

        final Ms2SpectrumHeader dda = new Ms2SpectrumHeader(1, 2, "scan=2", 1, 2, true, new CollisionEnergy(35),
                new IsolationWindow(0d, 2d), 3, 214.0038, 214.0038, 100d);
        assertFalse(dda.isDataIndependent(), "and an ordinary header may not be marked by default");
        assertFalse(((Ms2SpectrumHeader) roundTrip(dda)).isDataIndependent());
    }

    /** the uid is assigned by the store after the header exists, so it has to survive being replaced */
    @Test
    public void withUidKeepsEverythingElse() {
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(1, 2, "scan=2", 1, 2, true, new CollisionEnergy(35),
                new IsolationWindow(0d, 100d), 3, 214.0038, 200.0, 100d).asDataIndependent();
        final Ms2SpectrumHeader renumbered = header.withUid(77);
        assertEquals(77, renumbered.getUid());
        assertEquals(200.0, renumbered.getTargetedMz(), 1e-9);
        assertTrue(renumbered.isDataIndependent());
    }
}
