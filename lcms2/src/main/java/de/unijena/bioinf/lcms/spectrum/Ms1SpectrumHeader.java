package de.unijena.bioinf.lcms.spectrum;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class Ms1SpectrumHeader implements Serializable {

    @Getter
    protected final int uid;

    protected final String sourceId;

    @Getter
    protected final int scanId;

    protected final byte polarity;

    protected final boolean centroided;

//    public Ms1SpectrumHeader(int polarity, boolean centroided) {
//        this(-1, polarity, centroided);
//    }

    public Ms1SpectrumHeader(int uid, int scanIndex, @Nullable String sourceId, int polarity, boolean centroided) {
        this.uid = uid;
        this.scanId = scanIndex;
        this.sourceId = sourceId;
        this.polarity = (byte) polarity;
        this.centroided = centroided;
    }

    public Ms1SpectrumHeader withUid(int uid) {
        return new Ms1SpectrumHeader(uid, scanId, sourceId, polarity, centroided);
    }

    public String getSourceId() {
        if (sourceId==null) return "scan="+scanId;
        else return sourceId;
    }

}
