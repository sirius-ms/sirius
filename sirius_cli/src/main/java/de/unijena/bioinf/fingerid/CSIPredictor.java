package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDbOnDisc;
import de.unijena.bioinf.fingerid.net.CachedRESTDB;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CSIPredictor {
    protected final PredictorType predictorType;
    protected CachedRESTDB database;
    protected MaskedFingerprintVersion fpVersion;
    protected Fingerblast blaster;
    protected PredictionPerformance[] performances;
    protected boolean initialized;

    protected SearchableDatabase bio, pubchem;

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

    public SearchableDatabase getBio() {
        return bio;
    }

    public SearchableDatabase getPubchem() {
        return pubchem;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public void initialize() throws IOException {
        try (final WebAPI webAPI = WebAPI.newInstance()) {
            final TIntArrayList list = new TIntArrayList(4096);
            PredictionPerformance[] perf = webAPI.getStatistics(predictorType, list);

            final CdkFingerprintVersion version = (CdkFingerprintVersion) WebAPI.getFingerprintVersion();

            final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(version);
            v.disableAll();

            int[] fingerprintIndizes = list.toArray();

            for (int index : fingerprintIndizes) {
                v.enable(index);
            }

            MaskedFingerprintVersion fingerprintVersion = v.toMask();

            FingerblastScoringMethod method = (predictorType==PredictorType.CSI_FINGERID_NEGATIVE) ? new ScoringMethodFactory.CSIFingerIdScoringMethod(perf) : webAPI.getCovarianceScoring(fingerprintVersion, 1d / perf[0].withPseudoCount(0.25).numberOfSamples());

            final List<CustomDatabase> cds = CustomDatabase.customDatabases(true);
            synchronized(this) {
                performances = perf;
                fpVersion = fingerprintVersion;
                blaster = new Fingerblast(method, null);
                initialized = true;
                refreshCacheDir();
            }
        }
    }


    public void refreshCacheDir() throws IOException {
        try (final WebAPI webAPI = WebAPI.newInstance()) {
            final File directory = getDefaultDirectory();
            VersionsInfo versionsInfo = webAPI.getVersionInfo();
            database = new CachedRESTDB(versionsInfo, fpVersion, directory);
            bio = new SearchableDbOnDisc("biological database", new File(directory, "bio"), false, true, false);
            pubchem = new SearchableDbOnDisc("PubChem", new File(directory, "not-bio"), true, true, false);
            database.checkCache();
        }

    }


    public List<SearchableDatabase> getAvailableDatabases() {
        final List<SearchableDatabase> db = new ArrayList<>();
        db.add(pubchem);
        db.add(bio);
        db.addAll(CustomDatabase.customDatabases(true));
        return db;
    }

    public File getDefaultDirectory() {
        final String val = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
    }
}
