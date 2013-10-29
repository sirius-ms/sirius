package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.recal.MzRecalibration;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

/**
 * Recommended recalibration strategy.
 */
public class ExpRek implements RecalibrationStrategy, Parameterized {

    public ExpRek() {
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum(referenceSpectrum);
        final SimpleMutableSpectrum mes = new SimpleMutableSpectrum(spectrum);
        final TDoubleList list = new TDoubleArrayList(spectrum.size());
        // 1. allow only peaks with >2.5% intensity for recalibration
        for (int i=0; i < mes.size(); ++i) {
            if (mes.getIntensityAt(i) <= 0.025) {
                ref.removePeakAt(i);
                mes.removePeakAt(i);
                --i;
            } else if (mes.getIntensityAt(i) > 0.04) {
                list.add(mes.getMzAt(i));
            }
        }
        final Deviation dev = new Deviation(4, 1e-3);
        final double[][] values = MzRecalibration.maxIntervalStabbing(mes, ref, new UnivariateFunction() {
            @Override
            public double value(double x) {
                return dev.absoluteFor(x);
            }
        });
        // 2. there have to be at least 6 peaks with > 4% intensity
        int found = 0;
        for (double x : values[0]) {
            if (list.contains(x)) ++found;
        }


        if (found<6) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }
}
