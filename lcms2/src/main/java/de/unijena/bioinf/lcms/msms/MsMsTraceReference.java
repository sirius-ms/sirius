package de.unijena.bioinf.lcms.msms;

import java.io.Serializable;
import java.util.Locale;

public class MsMsTraceReference implements Serializable {

    public final int ms2Uid;
    public final int traceUid;
    public final int rawScanIdxOfParent;

    public MsMsTraceReference(int ms2Uid, int traceUid, int scanIdx) {
        this.ms2Uid = ms2Uid;
        this.traceUid = traceUid;
        this.rawScanIdxOfParent = scanIdx;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MsMS at scan %d for trace %d (Ms2ID: %d)", rawScanIdxOfParent, traceUid, ms2Uid);
    }
}
