package de.unijena.bioinf.babelms.mzml;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.Feature;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractMzParser implements Parser<Ms2Experiment> {
    protected InMemoryStorage inMemoryStorage;
    protected Iterator<FragmentedIon> ions;
    protected ProcessedSample sample;
    protected LCMSProccessingInstance instance;


    protected abstract boolean setNewSource(BufferedReader sourceReader, URL sourceURL);

    protected abstract LCMSRun parseToLCMSRun(BufferedReader sourceReader, URL sourceURL) throws IOException;

    @Override
    public Ms2Experiment parse(BufferedReader sourceReader, URL sourceURL) throws IOException {
        if (setNewSource(sourceReader, sourceURL)) {
            instance = new LCMSProccessingInstance();
            inMemoryStorage = new InMemoryStorage();
            final LCMSRun run = parseToLCMSRun(sourceReader, sourceURL);
            sample = instance.addSample(run, inMemoryStorage);
            instance.detectFeatures(sample);
            ions = Iterators.filter(sample.ions.iterator(), i -> Math.abs(i.getChargeState()) <= 1);
        }

        if (ions.hasNext()) {
            Feature feature = instance.makeFeature(sample, ions.next(), false);
            Ms2Experiment experiment = feature.toMsExperiment();
            // TODO: =/
            final Set<PrecursorIonType> ionTypes = feature.getPossibleAdductTypes();
            if (!ionTypes.isEmpty()) {
                ParameterConfig parameterConfig = PropertyManager.DEFAULTS.newIndependentInstance("LCMS-" + experiment.getName());
                parameterConfig.changeConfig("AdductSettings.enforced", Joiner.on(',').join(ionTypes));
                parameterConfig.changeConfig("PossibleAdducts", Joiner.on(',').join(ionTypes));
                final MsFileConfig config = new MsFileConfig(parameterConfig);
                experiment.setAnnotation(MsFileConfig.class, config);
            }

            return experiment;
        } else {
            instance = null;
            inMemoryStorage = null;
            sample = null;
            ions = null;
            return null;
        }
    }
}
