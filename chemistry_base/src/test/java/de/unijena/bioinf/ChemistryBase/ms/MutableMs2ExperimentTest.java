package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The spectra of a mutable experiment have to stay mutable.
 * <p>
 * Callers add to them in place - every parser does - and the validator removes empty spectra through an iterator. A
 * caller that builds its list with {@code Stream.toList()} or {@code Arrays.asList} would otherwise hand over
 * something that cannot be changed, and the failure is far away from the cause: an unsupported operation inside a
 * validator, for the one experiment that happened to contain an empty spectrum.
 */
public class MutableMs2ExperimentTest {

    private static MutableMs2Spectrum ms2(double... mz) {
        return new MutableMs2Spectrum(new SimpleSpectrum(mz, new double[mz.length]), 200d, CollisionEnergy.none(), 2);
    }

    private static SimpleSpectrum ms1(double... mz) {
        return new SimpleSpectrum(mz, new double[mz.length]);
    }

    @Test
    public void anImmutableMs2ListCanStillBeRemovedFrom() {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(List.of(ms2(), ms2(100d, 150d)));
        exp.getMs2Spectra().removeIf(s -> s.size() == 0);
        assertEquals(1, exp.getMs2Spectra().size(), "the empty spectrum has to be removable");
    }

    @Test
    public void aFixedSizeMs2ListCanStillBeAddedTo() {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(Arrays.asList(ms2(100d)));
        exp.getMs2Spectra().add(ms2(200d));
        assertEquals(2, exp.getMs2Spectra().size());
    }

    @Test
    public void anImmutableMs1ListCanStillBeAddedTo() {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs1Spectra(List.of(ms1(100d)));
        exp.getMs1Spectra().add(ms1(200d));
        assertEquals(2, exp.getMs1Spectra().size());
    }

    /** an absent list is not an empty one - the validator distinguishes them and throws on the first */
    @Test
    public void anAbsentListStaysAbsent() {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(null);
        assertNull(exp.getMs2Spectra());
        exp.setMs1Spectra(null);
        assertNull(exp.getMs1Spectra());
    }

    /** the experiment owns its list afterwards, so that what the caller does with theirs cannot surprise it */
    @Test
    public void theExperimentDoesNotShareTheCallersList() {
        final List<MutableMs2Spectrum> mine = new ArrayList<>(List.of(ms2(100d)));
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setMs2Spectra(mine);
        mine.clear();
        assertEquals(1, exp.getMs2Spectra().size());
    }
}
