package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;

/*
Zwei Strategien:
    1.) Rekalibriere jedes MS2 Spektrum einzeln und merge danach zusammen
    2.) Merge erst zusammen und rekalibriere dann alle Peaks

    Wenn wir nur ein Spektrum haben wird 2) genommen. Wenn wir mehrere Spektren haben wird
    zuerst geprüft wie viele Referenz-Peaks wir pro Spektrum haben. Wenn wir in jedem Spektrum
    mindestens
 */
public class HypothesenDrivenRecalibration2 {


    protected AbstractRecalibrationStrategy method;

    public HypothesenDrivenRecalibration2() {
        this(new MedianSlope(new Deviation(2, 5e-4),8,0.01));
    }

    public HypothesenDrivenRecalibration2(AbstractRecalibrationStrategy method) {
        this.method = method;
    }

    public SpectralRecalibration collectPeaksFromMs2(ProcessedInput input, FTree tree) {
        final MutableMs2Experiment exp = input.getExperimentInformation();
        final int N = exp.getMs2Spectra().size(), M = tree.numberOfVertices();
        final MutableMs2Spectrum[] spectras = new MutableMs2Spectrum[N];
        final SimpleMutableSpectrum[] refs = new SimpleMutableSpectrum[N], collected = new SimpleMutableSpectrum[N];
        // ensure that scan id is ordered
        int k=0;
        for (MutableMs2Spectrum m : exp.getMs2Spectra()) {
            spectras[k] = m;
            m.setScanNumber(k);
            refs[k] = new SimpleMutableSpectrum(M);
            collected[k] = new SimpleMutableSpectrum(M);
            ++k;
        }
        final SimpleMutableSpectrum mergedRef = new SimpleMutableSpectrum(), merged = new SimpleMutableSpectrum();
        for (Fragment f : tree) {
            final ProcessedPeak peak = input.getMergedPeaks().get(f.getPeakId());
            final double mz = f.getIonization().addToMass(f.getFormula().getMass());
            for (MS2Peak pk : peak.getOriginalPeaks()) {
                final int sc = ((MutableMs2Spectrum)pk.getSpectrum()).getScanNumber();
                collected[sc].addPeak(pk.getMz(), pk.getIntensity());
                refs[sc].addPeak(mz, peak.getRelativeIntensity());
            }
            mergedRef.addPeak(mz, peak.getRelativeIntensity());
            merged.addPeak(peak.getMass(), peak.getRelativeIntensity());
        }
        UnivariateFunction[] recalibrationFunctions = new UnivariateFunction[spectras.length];
        for (int i=0; i < spectras.length; ++i) {
            if (refs[i].size() >= method.getMinNumberOfPeaks()) {
                recalibrationFunctions[i] = method.recalibrate(collected[i], refs[i]);
            }
        }
        final UnivariateFunction mergedFunc = method.recalibrate(merged, mergedRef);
        return new SpectralRecalibration(spectras, recalibrationFunctions, mergedFunc);
    }

}
