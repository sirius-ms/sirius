package de.unijena.bioinf.lcms.msms;

import java.io.Serializable;
import java.util.Locale;

public class MsMsTraceReference implements Serializable {

    public final int ms2Uid;
    public final int traceUid;
    public final int rawScanIdxOfParent;
    public final int ms2scanid;

    public MsMsTraceReference(int ms2Uid, int traceUid, int scanIdx, int ms2scanid) {
        this.ms2Uid = ms2Uid;
        this.traceUid = traceUid;
        this.rawScanIdxOfParent = scanIdx;
        this.ms2scanid = ms2scanid;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[MsMs scan id=%d parent scan id=%d]" ,ms2scanid,rawScanIdxOfParent);
    }
}
