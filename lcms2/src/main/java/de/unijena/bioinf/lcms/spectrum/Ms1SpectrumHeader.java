package de.unijena.bioinf.lcms.spectrum;

import lombok.Getter;

import java.io.Serializable;

public class Ms1SpectrumHeader implements Serializable {

    @Getter
    protected final int uid;

    @Getter
    protected final String sourceId;

    protected final byte polarity;

    protected final boolean centroided;

//    public Ms1SpectrumHeader(int polarity, boolean centroided) {
//        this(-1, polarity, centroided);
//    }

    public Ms1SpectrumHeader(int uid, String sourceId, int polarity, boolean centroided) {
        this.uid = uid;
        this.sourceId = sourceId;
        this.polarity = (byte) polarity;
        this.centroided = centroided;
    }

    public Ms1SpectrumHeader withUid(int uid) {
        return new Ms1SpectrumHeader(uid, sourceId, polarity, centroided);
    }
}
