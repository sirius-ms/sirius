package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.DeIsotope;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        final Options options = CliFactory.createCli(Options.class).parseArguments(args);

        final List<File> files = InterpretOptions.getFiles(options);
        final MeasurementProfile profile = InterpretOptions.getProfile(options);

        final FragmentationPatternAnalysis analyzer = FragmentationPatternAnalysis.defaultAnalyzer();
        final DeIsotope deIsotope = new DeIsotope(profile.getExpectedIonMassDeviation().getPpm(), profile.getExpectedIonMassDeviation().getAbsolute());

        for (File f : files) {
            try {
                final Ms2Experiment experiment = parseFile(f, profile);
                final ProcessedInput input = analyzer.preprocessing(experiment);

            } catch (IOException e) {
                System.err.println("Error while parsing " + f + ":\n" + e);
            } catch (Exception e) {
                System.err.println("Error while processing " + f + ":\n" + e);
            }
        }


    }

    private static Ms2Experiment parseFile(File f, MeasurementProfile profile) throws IOException {
        final GenericParser<Ms2Experiment> parser = new GenericParser<Ms2Experiment>(getParserFor(f));
        final Ms2Experiment experiment = parser.parseFile(f);
        final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(experiment);
        impl.setMeasurementProfile(profile);
        return impl;
    }

    private static Parser<Ms2Experiment> getParserFor(File f) {
        final String[] extName = f.getName().split("\\.");
        if (extName.length>1 && extName[1].equalsIgnoreCase("ms")){
            return new JenaMsParser();
        } else {
            throw new RuntimeException("No parser found for file " + f);
        }

    }


}
