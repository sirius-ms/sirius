package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class CompoundContainerId extends ProjectSpaceContainerId {
    public static final String RANKING_KEY = "rankingScoreType";


    protected Lock containerLock;

    // ID defining fields
    private String directoryName;
    private String compoundName;
    private int compoundIndex;

    // fields for fast compound filtering
    @Nullable
    private Double ionMass = null;
    @Nullable
    private PrecursorIonType ionType = null;
    @Nullable
    private DetectedAdducts possibleAdducts = null;
    @NotNull
    private List<Class<? extends FormulaScore>> rankingScores = Collections.emptyList();


    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex) {
        this(directoryName, compoundName, compoundIndex, null, null);
    }

    protected CompoundContainerId(String directoryName, String compoundName, int compoundIndex, @Nullable Double ionMass, @Nullable PrecursorIonType ionType) {
        this.directoryName = directoryName;
        this.compoundName = compoundName;
        this.compoundIndex = compoundIndex;
        this.containerLock = new ReentrantLock();
        this.ionMass = ionMass;
        this.ionType = ionType;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public int getCompoundIndex() {
        return compoundIndex;
    }

    public Optional<Double> getIonMass() {
        return Optional.ofNullable(ionMass);
    }

    public void setIonMass(@Nullable Double ionMass) {
        this.ionMass = ionMass;
    }

    public Optional<PrecursorIonType> getIonType() {
        return Optional.ofNullable(ionType);
    }

    public void setIonType(@Nullable PrecursorIonType ionType) {
        this.ionType = ionType;
    }

    public List<Class<? extends FormulaScore>> getRankingScoreTypes() {
        return rankingScores;
    }

    public Optional<DetectedAdducts> getDetectedAdducts() {
        return Optional.ofNullable(possibleAdducts);
    }

    public void setDetectedAdducts(@Nullable DetectedAdducts possibleAdducts) {
        this.possibleAdducts = possibleAdducts;
    }

    @SafeVarargs
    public final void setRankingScoreTypes(@NotNull Class<? extends FormulaScore>... rankingScores) {
        setRankingScoreTypes(Arrays.asList(rankingScores));
    }

    public void setRankingScoreTypes(@NotNull List<Class<? extends FormulaScore>> rankingScores) {
        this.rankingScores = new ArrayList<>(rankingScores);
    }

    /**
     * This operation is only allowed to be called with careful synchronization within the project space
     */
    void rename(String newName, String newDirName) {
        this.compoundName = newName;
        this.directoryName = newDirName;
    }

    @Override
    public String toString() {
        return directoryName + "@" + getIonMass().map(Math::round).map(String::valueOf).orElse("N/A") + "m/z";
    }

    public Map<String, String> asKeyValuePairs() {
        Map<String, String> kv = new LinkedHashMap<>(3);
        kv.put("index", String.valueOf(getCompoundIndex()));
        kv.put("name", getCompoundName());
        getIonMass().ifPresent(im -> kv.put("ionMass", String.valueOf(im)));
        getIonType().ifPresent(it -> kv.put("ionType", it.toString()));
        getDetectedAdducts().ifPresent(pa -> kv.put("detectedAdducts", pa.toString()));

        if (!rankingScores.isEmpty())
            kv.put(RANKING_KEY, rankingScores.stream().map(Score::simplify).collect(Collectors.joining(",")));

        return kv;
    }

    public void setAllNonFinal(final CompoundContainerId cid) {
        if (cid == null || cid == this)
            return;
        setRankingScoreTypes(cid.rankingScores);
        setIonMass(cid.ionMass);
        setIonType(cid.ionType);
        setDetectedAdducts(cid.possibleAdducts);
    }
}
