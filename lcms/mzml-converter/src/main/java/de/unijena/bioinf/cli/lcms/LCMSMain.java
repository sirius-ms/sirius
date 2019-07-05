package de.unijena.bioinf.cli.lcms;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.Feature;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class LCMSMain {

    public static void main(String[] args) {
        for (String arg : args) {
            LCMSProccessingInstance instance = new LCMSProccessingInstance();
            InMemoryStorage storage = new InMemoryStorage();
            final File f = new File(arg);
            String nameWithoutEnding = f.getName();
            nameWithoutEnding = nameWithoutEnding.substring(0,nameWithoutEnding.lastIndexOf('.'));
            try (final BufferedWriter bw = FileUtils.getWriter(new File(f.getParent(), nameWithoutEnding + ".ms"))){
                LCMSRun parse = new MzXMLParser().parse(f, storage);
                ProcessedSample sample = instance.addSample(parse, storage);
                instance.detectFeatures(sample);
                for (FragmentedIon ion : sample.ions) {
                    Feature feature = instance.makeFeature(sample, ion, false);
                    bw.write(feature.toMsExperiment());

                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

        }
    }

}
