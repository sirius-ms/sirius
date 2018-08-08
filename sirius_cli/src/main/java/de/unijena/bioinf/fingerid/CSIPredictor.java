package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;
import de.unijena.bioinf.fingerid.db.CachedRESTDB;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;

public class CSIPredictor {
    protected final PredictorType predictorType;
    protected CachedRESTDB database;
    protected MaskedFingerprintVersion fpVersion;
    protected Fingerblast blaster;
    protected PredictionPerformance[] performances;
    protected boolean initialized;

    public CSIPredictor(PredictorType predictorType) {
        this.predictorType = predictorType;
    }

    public PredictorType getPredictorType() {
        return predictorType;
    }

    public CachedRESTDB getDatabase() {
        return database;
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fpVersion;
    }

    public Fingerblast getBlaster() {
        return blaster;
    }

    public PredictionPerformance[] getPerformances() {
        return performances;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public void initialize() throws IOException {
//        try (final WebAPI webAPI = WebAPI.newInstance()) {
            final TIntArrayList list = new TIntArrayList(4096);
            PredictionPerformance[] perf = WebAPI.INSTANCE.getStatistics(predictorType, list);

            final CdkFingerprintVersion version = (CdkFingerprintVersion) WebAPI.getFingerprintVersion();

            final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(version);
            v.disableAll();

            int[] fingerprintIndizes = list.toArray();

            for (int index : fingerprintIndizes) {
                v.enable(index);
            }

            MaskedFingerprintVersion fingerprintVersion = v.toMask();

            FingerblastScoringMethod method = (predictorType == PredictorType.CSI_FINGERID_NEGATIVE) ? new ScoringMethodFactory.CSIFingerIdScoringMethod(perf) : WebAPI.INSTANCE.getCovarianceScoring(fingerprintVersion, 1d / perf[0].withPseudoCount(0.25).numberOfSamples());
            synchronized (this) {
                performances = perf;
                fpVersion = fingerprintVersion;
                blaster = new Fingerblast(method, null);
                initialized = true;
                refreshCacheDir();
            }
//        }
    }


    public void refreshCacheDir() throws IOException {
//        try (final WebAPI webAPI = WebAPI.newInstance()) {
            VersionsInfo versionsInfo = WebAPI.INSTANCE.getVersionInfo();
            database = SearchableDatabases.makeCachedRestDB(versionsInfo, fpVersion);
            database.checkCache();
//        }
    }
}
