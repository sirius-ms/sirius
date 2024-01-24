package de.unijena.bioinf.lcms.spectrum;

import lombok.Getter;

import java.io.Serializable;

public class Ms1SpectrumHeader implements Serializable {

    @Getter
    protected final long scanId;

    @Getter
    protected final int uid;
    protected final byte polarity;

    protected final boolean centroided;

    public Ms1SpectrumHeader(long scanId, int polarity, boolean centroided) {
        this(scanId,-1, polarity, centroided);
    }

    public Ms1SpectrumHeader(long scanId, int uid, int polarity, boolean centroided) {
        this.scanId = scanId;
        this.uid = uid;
        this.polarity = (byte) polarity;
        this.centroided = centroided;
    }

    public Ms1SpectrumHeader withUid(int uid) {
        return new Ms1SpectrumHeader(scanId, uid, polarity, centroided);
    }
}
