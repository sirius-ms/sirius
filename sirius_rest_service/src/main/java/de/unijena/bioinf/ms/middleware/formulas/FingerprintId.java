package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.fingerid.FingerprintResult;

public class FingerprintId {

    protected FingerprintVersion fingerprintVersion;
    protected double[] fingerprint;

    public FingerprintId(FingerprintResult fpResult, boolean asDeterministic){
        this.fingerprintVersion = fpResult.fingerprint.getFingerprintVersion();
        if(asDeterministic){
            boolean[] boolFingerprint = fpResult.fingerprint.asDeterministic().toBooleanArray();
            this.fingerprint = new double[boolFingerprint.length];

            for(int idx = 0; idx < this.fingerprint.length; idx++){
                this.fingerprint[idx] = boolFingerprint[idx] ? 1 : 0;
            }
        }else{
            this.fingerprint = fpResult.fingerprint.toProbabilityArray();
        }
    }

    public double[] getFingerprint(){
        return this.fingerprint;
    }

    public FingerprintVersion getFingerprintVersion(){
        return this.fingerprintVersion;
    }
}
