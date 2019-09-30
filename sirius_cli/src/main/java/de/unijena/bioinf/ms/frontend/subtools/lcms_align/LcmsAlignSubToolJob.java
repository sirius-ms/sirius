package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MultipleSources;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.babelms.ms.MsFileConfig;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.apache.commons.math3.analysis.function.Add;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LcmsAlignSubToolJob extends PreprocessingJob {

    public LcmsAlignSubToolJob(@Nullable List<File> input, @Nullable ProjectSpaceManager space) {
        super(input, space);
    }

    @Override
    protected ProjectSpaceManager compute() throws Exception {
        final ArrayList<BasicJJob> jobs = new ArrayList<>();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        i.setDetectableIonTypes(PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
        for (File f : input) {
            jobs.add(SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<>() {
                @Override
                protected Object compute() throws Exception {
                    try {
                        MemoryFileStorage storage = new MemoryFileStorage();
                        final LCMSRun parse = new MzXMLParser().parse(f, storage);
                        final ProcessedSample sample = i.addSample(parse, storage);
                        i.detectFeatures(sample);
                        storage.backOnDisc();
                        storage.dropBuffer();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return "";
                }
            }));
        }
        for (BasicJJob j : jobs) j.takeResult();
        i.getMs2Storage().backOnDisc();
        i.getMs2Storage().dropBuffer();
        Cluster alignment = i.alignAndGapFilling();
        i.detectAdductsWithGibbsSampling(alignment);
        final ConsensusFeature[] consensusFeatures = i.makeConsensusFeatures(alignment);
        LOG().info("Gapfilling Done.");

        //save to project space
        for (ConsensusFeature feature : consensusFeatures) {
            final Ms2Experiment experiment = feature.toMs2Experiment();

            // set name to common prefix

            MultipleSources sourcelocation = MultipleSources.leastCommonAncestor(input.toArray(File[]::new));
            experiment.setAnnotation(SpectrumFileSource.class, new SpectrumFileSource(sourcelocation.value));

            // if we found some adduct types in LCMS, set them into the config
            final Set<PrecursorIonType> ionTypes = feature.getPossibleAdductTypes();
            if (!ionTypes.isEmpty()) {
                ParameterConfig parameterConfig = PropertyManager.DEFAULTS.newIndependentInstance("LCMS-" + experiment.getName());
                parameterConfig.changeConfig("AdductSettings.enforced", Joiner.on(',').join(ionTypes));
                parameterConfig.changeConfig("PossibleAdducts", Joiner.on(',').join(ionTypes));
                final MsFileConfig config = new MsFileConfig(parameterConfig);
                experiment.setAnnotation(MsFileConfig.class, config);
            }

            CompoundContainerId compoundContainerId = space.newUniqueCompoundId(experiment);
            CompoundContainer container = new CompoundContainer(compoundContainerId);
            container.setAnnotation(Ms2Experiment.class, experiment);
            space.projectSpace().updateCompound(container, Ms2Experiment.class);

        }
        return space;
    }
}
