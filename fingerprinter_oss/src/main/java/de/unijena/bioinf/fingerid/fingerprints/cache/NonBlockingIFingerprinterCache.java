package de.unijena.bioinf.fingerid.fingerprints.cache;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.fingerid.Fingerprinter;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IFingerprinter;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NonBlockingIFingerprinterCache implements IFingerprinterCache {

    private final Map<CdkFingerprintVersion.USED_FINGERPRINTS, Queue<IFingerprinter>> fingerprinterCache;

    public NonBlockingIFingerprinterCache(int cacheSize) {
        this.fingerprinterCache = initCacheMap(cacheSize);
    }

    public NonBlockingIFingerprinterCache() {
        this(-1);
    }

    private Map<CdkFingerprintVersion.USED_FINGERPRINTS, Queue<IFingerprinter>> initCacheMap(int cacheSize) {
        Map<CdkFingerprintVersion.USED_FINGERPRINTS, Queue<IFingerprinter>> tmp = new ConcurrentHashMap<>();
        for (CdkFingerprintVersion.USED_FINGERPRINTS usedFps : CdkFingerprintVersion.USED_FINGERPRINTS.values())
            tmp.put(usedFps, cacheSize < 1 ? new ConcurrentLinkedQueue<>() : new ArrayBlockingQueue<>(cacheSize));
        return Collections.unmodifiableMap(tmp);
    }

    @Override
    public <R> R applyFingerprinter(CdkFingerprintVersion.USED_FINGERPRINTS fp, Function<IFingerprinter, R> function) throws CDKException {
        IFingerprinter fprinter = fingerprinterCache.get(fp).poll();
        if (fprinter == null)
            fprinter = Fingerprinter.getFingerprinter(fp);
        try {
            return function.apply(fprinter);
        } finally {
            fingerprinterCache.get(fp).offer(fprinter);
        }
    }
}
