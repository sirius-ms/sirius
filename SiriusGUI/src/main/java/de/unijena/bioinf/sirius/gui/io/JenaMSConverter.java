package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JenaMSConverter {

    public ExperimentContainer convert(File file) {
        MS2FormatSpectraReader reader = new MS2FormatSpectraReader();
        CompactExperiment cexp = reader.read(file);
        return convert(cexp, file.getName());
    }

    public ExperimentContainer convert(CompactExperiment cexp, String fileName) {
        ExperimentContainer ec = new ExperimentContainer();

        String ion = cexp.getIonization();
        if (ion != null && !ion.isEmpty()) {
            if (ion.contains("[M+H]+")) {
                ec.setIonization(Ionization.MPlusH);
            } else if (ion.contains("[M+Na]+")) {
                ec.setIonization(Ionization.MPlusNa);
            } else if (ion.contains("[M-H]-")) {
                ec.setIonization(Ionization.MMinusH);
            } else if (ion.contains("M+")) {
                ec.setIonization(Ionization.M);
            } else {
                ec.setIonization(Ionization.Unknown);
            }
        }

        String name = cexp.getCompoundName();
        if (name != null && !name.isEmpty()) {
            ec.setName(name);
        } else {
            ec.setName(fileName.substring(0, fileName.length() - 4));
        }


        CompactSpectrum ms1 = cexp.getMS1Spectrum();
        List<CompactSpectrum> ms1Spectra = new ArrayList<>();
        if (ms1 != null) {
            ms1.setMSLevel(1);
            ms1Spectra.add(ms1);
        }
        ec.setMs1Spectra(ms1Spectra);

        List<CompactSpectrum> ms2Spectra = new ArrayList<>();
        if (cexp.getMS2Spectra() != null) {
            for (CompactSpectrum sp : cexp.getMS2Spectra()) {
                sp.setMSLevel(2);
                ms2Spectra.add(sp);
            }
        }
        ec.setMs2Spectra(ms2Spectra);

        ec.setDataFocusedMass(cexp.getFocusedMass() > 0 ? cexp.getFocusedMass() : -1);
        return ec;
    }

}
