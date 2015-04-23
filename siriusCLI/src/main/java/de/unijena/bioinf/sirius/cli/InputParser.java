package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MsExperiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.MsExperimentParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

public class InputParser {

    public Iterator<Instance> parse(final InputOptions input) {

        ArrayDeque<Ms2Experiment> experiments;
        final MsExperimentParser parser = new MsExperimentParser();
        MsExperiment experiment = handleSingleMode(input, parser);
        return new Iterator<Instance>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Instance next() {
                return null;
            }

            public void fetchNext() {

            }

            @Override
            public void remove() {

            }
        }

    }

    private MsExperiment handleSingleMode(InputOptions input, MsExperimentParser parser) throws IOException {
        if (input.getMs2().isEmpty() && input.getMs1().isEmpty()) return null;
        if (input.getMs2().isEmpty() && !input.getMs1().isEmpty()) {
            throw new IllegalArgumentException("Expect at least one MS/MS spectrum");
        }
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        final MutableMeasurementProfile prof = new MutableMeasurementProfile();
        if (input.getElements()!=null) {
            prof.setFormulaConstraints(input.getElements());
        }
        if (input.getParentMz() != null) {
            exp.setIonMass(input.getParentMz());
        }
        if (input.getIon()!=null) {
            final Ionization ion = PeriodicTable.getInstance().ionByName(input.getIon());
            if (ion==null) throw new IllegalArgumentException("Unknown ion type: '" + input.getIon() + "'");
            exp.setIonization(ion);
        }
        for (File f : input.getMs1()) {
            final List<Ms2Experiment> exps = parser.getParser(f).parseFromFile(f);
            for (MsExperiment exp : exps) {

            }
        }

    }

}
