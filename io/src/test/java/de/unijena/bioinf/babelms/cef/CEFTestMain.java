package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.MsExperimentParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CEFTestMain {
    public static void main(String[] args) throws IOException {
        final Path f = Path.of("/home/fleisch/Downloads/CEF/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef");
        final List<Ms2Experiment> exps = new ArrayList<>();
        MsExperimentParser parser = new MsExperimentParser();
        parser.getParser(f).parseIterator(Files.newBufferedReader(f), f.toUri()).forEachRemaining(exps::add);
        System.out.println(exps);
    }
}
