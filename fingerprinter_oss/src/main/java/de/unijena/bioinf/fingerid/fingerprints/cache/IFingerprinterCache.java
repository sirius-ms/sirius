package de.unijena.bioinf.fingerid.fingerprints.cache;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.fingerid.Fingerprinter;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IFingerprinter;

/**
 * Caches fingerprinter
 */
public interface IFingerprinterCache {
    IFingerprinterCache NOOP_CACHE = new IFingerprinterCache() {
        @Override
        public <R> R applyFingerprinter(CdkFingerprintVersion.USED_FINGERPRINTS fp,  Function<IFingerprinter, R> function) throws CDKException {
            return function.apply(Fingerprinter.getFingerprinter(fp));
        }
    };

    default void consumeFingerprinter(CdkFingerprintVersion.USED_FINGERPRINTS fp, Consumer<IFingerprinter> consumer) throws CDKException {
        applyFingerprinter(fp, fprinter -> {
            consumer.accept(fprinter);
            return null;
        });
    }

    <R> R applyFingerprinter(CdkFingerprintVersion.USED_FINGERPRINTS fp,  Function<IFingerprinter, R> function) throws CDKException;

    interface Function<A,B> {
        B apply(A a) throws CDKException;
    }

    interface Consumer<A> {
        void accept(A a) throws CDKException;
    }

}
