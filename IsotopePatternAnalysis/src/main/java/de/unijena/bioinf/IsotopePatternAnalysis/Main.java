package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MsExperiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.util.MutableMsExperiment;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        final SimpleMutableSpectrum ms1 = new SimpleMutableSpectrum();
        ms1.addPeak(new Peak(174.0883, 16304.0));
        ms1.addPeak(new Peak(175.0901, 1192.0));
        ms1.addPeak(new Peak(176.0929, 170.0));
        final MsExperiment exp = new MutableMsExperiment(new MutableMeasurementProfile(), PeriodicTable.getInstance().ionByName("[M-H]-"), Arrays.asList(ms1), ms1);
        final IsotopePatternAnalysis iso = IsotopePatternAnalysis.defaultAnalyzer();
        iso.setCutoff(0.005);
        iso.setIntensityOffset(0d);
        List<IsotopePattern> deisotope = iso.deisotope(exp);
        System.out.println(deisotope.get(0).getCandidates().get(0).getScore());

    }

}
