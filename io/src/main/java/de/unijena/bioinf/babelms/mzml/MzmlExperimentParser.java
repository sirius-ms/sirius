package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

public class MzmlExperimentParser implements Parser<Ms2Experiment> {

    protected BufferedReader currentSource;
    protected InMemoryStorage inMemoryStorage;
    protected Iterator<FragmentedIon> ions;
    protected ProcessedSample sample;
    protected LCMSProccessingInstance instance;

    @Override
    public Ms2Experiment parse(BufferedReader reader, URL source) throws IOException {
        if (reader!=currentSource){
            currentSource = reader;
            final MzXMLParser parser = new MzXMLParser();
            instance = new LCMSProccessingInstance();
            inMemoryStorage = new InMemoryStorage();
            LCMSRun run = parser.parse(new DataSource(source), new InputSource(reader), inMemoryStorage);
            sample = instance.addSample(run, inMemoryStorage);
            instance.detectFeatures(sample);
            ions = sample.ions.iterator();
        }

        if (ions.hasNext()) {
            return instance.makeFeature(sample,ions.next(),false).toMsExperiment();
        } else {
            instance=null;inMemoryStorage=null;sample=null;ions=null;currentSource=null;
            return null;
        }
    }
}
