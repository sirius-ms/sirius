package de.unijena.bioinf.lcms;

import de.unijena.bioinf.model.lcms.*;

import java.util.ArrayList;

public class ProcessedSample {

    public final LCMSRun run;
    public final NoiseModel ms1NoiseModel, ms2NoiseModel;
    public final ChromatogramCache chromatogramCache;
    public final SpectrumStorage storage;
    public final ChromatogramBuilder builder;

    public final ArrayList<FragmentedIon> ions;

    ProcessedSample(LCMSRun run, NoiseModel ms1NoiseModel, NoiseModel ms2NoiseModel, ChromatogramCache chromatogramCache, SpectrumStorage storage) {
        this.run = run;
        this.ms1NoiseModel = ms1NoiseModel;
        this.ms2NoiseModel = ms2NoiseModel;
        this.chromatogramCache = chromatogramCache;
        this.storage = storage;
        this.builder = new ChromatogramBuilder(this);
        this.ions = new ArrayList<>();
    }
}
