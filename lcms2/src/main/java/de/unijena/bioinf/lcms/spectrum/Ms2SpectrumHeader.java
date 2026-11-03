package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;

public class Ms2SpectrumHeader extends Ms1SpectrumHeader implements Serializable {
    @Nullable
    protected final CollisionEnergy energy;

    @Nullable protected final IsolationWindow isolationWindow;
    @Getter protected final int parentId; // index
    @Getter  protected final double retentionTime;
    @Getter protected final double precursorMz;
    @Getter protected final double targetedMz;
    @Getter protected final int msLevel;

    /**
     * Whether this spectrum was acquired data independently, i.e. everything in a wide isolation window was
     * fragmented together and the assignment of fragments to a precursor is a computation rather than a measurement.
     * <p>
     * A property of the spectrum rather than something a reader should infer from the width of the isolation window:
     * the width happens to differ today, and a quality check that keyed on it would silently change meaning on an
     * instrument that narrows its windows. It decides which checks apply at all - the peaks of a data independent
     * spectrum can be a co-eluting neighbour's, which is not a failure mode a targeted spectrum has.
     */
    @Getter protected final boolean dataIndependent;

    public Ms2SpectrumHeader(String sourceId, int scanId, int polarity, int mslevel, boolean centroided, CollisionEnergy energy, IsolationWindow window, int parentId, double precursorMz, double targetedMz, double retentionTime) {
        this(-1, scanId, sourceId, polarity,mslevel,centroided,energy,window,parentId,precursorMz,targetedMz,retentionTime);
    }

    /** the same header, marked as data independent, see {@link #isDataIndependent()} */
    public Ms2SpectrumHeader asDataIndependent() {
        return new Ms2SpectrumHeader(uid, scanId, sourceId, polarity, msLevel, centroided, energy, isolationWindow,
                parentId, precursorMz, targetedMz, retentionTime, true);
    }

    public Optional<CollisionEnergy> getEnergy() {
        return Optional.ofNullable(energy);
    }

    public Optional<IsolationWindow> getIsolationWindow() {
        return Optional.ofNullable(isolationWindow);
    }

    public Ms2SpectrumHeader(int uid, int index, String sourceId, int polarity, int msLevel, boolean centroided,  CollisionEnergy energy, IsolationWindow window, int parentId, double precursorMz, double targetedMz, double retentionTime) {
        this(uid, index, sourceId, polarity, msLevel, centroided, energy, window, parentId, precursorMz, targetedMz,
                retentionTime, false);
    }

    public Ms2SpectrumHeader(int uid, int index, String sourceId, int polarity, int msLevel, boolean centroided,  CollisionEnergy energy, IsolationWindow window, int parentId, double precursorMz, double targetedMz, double retentionTime, boolean dataIndependent) {
        super(uid, index, sourceId, polarity, centroided);
        this.dataIndependent = dataIndependent;
        this.energy = energy;
        this.parentId = parentId;
        this.isolationWindow = window==null || window.isUndefined() ? null : window;
        this.retentionTime = retentionTime;
        this.msLevel = msLevel;
        if (precursorMz<0 && targetedMz<0) {
            throw new IllegalArgumentException("Neither precursor nor targeted mz is known");
        }
        if (window!=null) {
            this.precursorMz = precursorMz>=0 ? precursorMz : targetedMz + window.getWindowOffset();
            this.targetedMz = targetedMz>=0 ? targetedMz : precursorMz - window.getWindowOffset();
        } else {
            this.precursorMz = precursorMz;
            this.targetedMz = targetedMz;
        }
    }

    public Ms2SpectrumHeader withUid(int uid) {
        return new Ms2SpectrumHeader(uid, scanId, sourceId, polarity, msLevel, centroided, energy, isolationWindow, parentId, precursorMz, targetedMz, retentionTime, dataIndependent);
    }

    @Override
    public String toString() {
        return "Ms2SpectrumHeader{" +
                "parentId=" + parentId +
                ", retentionTime=" + retentionTime +
                ", precursorMz=" + precursorMz +
                '}';
    }

    public Ms2SpectrumHeader withParentId(int parentId) {
        return new Ms2SpectrumHeader(uid, scanId, sourceId, polarity, msLevel, centroided, energy, isolationWindow, parentId, precursorMz, targetedMz, retentionTime);
    }
}
