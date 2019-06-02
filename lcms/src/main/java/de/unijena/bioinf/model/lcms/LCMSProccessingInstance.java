package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.lcms.ChromatogramBuilder;

public class LCMSProccessingInstance {
    protected LCMSRun lcms;
    protected NoiseModel noiseModelMs1, noiseModelMs2;
    protected ChromatogramCache cache;
    protected SpectrumStorage storage;
    protected ChromatogramBuilder builder;

    public LCMSProccessingInstance(LCMSRun lcms, SpectrumStorage storage) {
        this.lcms = lcms;
        this.storage =  storage;
        this.cache = new ChromatogramCache();
        this.noiseModelMs1 = new GlobalNoiseModel(lcms, storage, 0.75, (x)->!x.isMsMs());
        this.noiseModelMs2 = new GlobalNoiseModel(lcms, storage, 0.85, (x)->x.isMsMs());
        this.builder = new ChromatogramBuilder(lcms, noiseModelMs1,storage);
        builder.setCache(cache);
    }

    public ChromatogramBuilder getBuilder() {
        return builder;
    }

    public LCMSRun getLcms() {
        return lcms;
    }

    public NoiseModel getNoiseModelMs1() {
        return noiseModelMs1;
    }
    public NoiseModel getNoiseModelMs2() {
        return noiseModelMs2;
    }

    public ChromatogramCache getCache() {
        return cache;
    }

    public SpectrumStorage getStorage() {
        return storage;
    }
}
