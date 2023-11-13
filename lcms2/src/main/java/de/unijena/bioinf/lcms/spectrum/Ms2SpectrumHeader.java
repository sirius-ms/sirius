package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;

public class Ms2SpectrumHeader extends Ms1SpectrumHeader implements Serializable {
    @Nullable protected final CollisionEnergy energy;

    @Nullable protected final IsolationWindow isolationWindow;
    protected final int parentId;
    protected final double retentionTime;

    protected final double precursorMz;

    public Ms2SpectrumHeader(int polarity, boolean centroided, CollisionEnergy energy, IsolationWindow window, int parentId, double precursorMz, double retentionTime) {
        super(-1, polarity, centroided);
        this.precursorMz = precursorMz;
        this.energy = energy;
        this.parentId = parentId;
        this.isolationWindow = window;
        this.retentionTime = retentionTime;
    }

    @Nullable
    public Optional<CollisionEnergy> getEnergy() {
        return Optional.ofNullable(energy);
    }

    @Nullable
    public Optional<IsolationWindow> getIsolationWindow() {
        return Optional.ofNullable(isolationWindow);
    }

    public int getUid() {
        return uid;
    }

    public int getParentId() {
        return parentId;
    }

    public double getPrecursorMz() {
        return precursorMz;
    }

    public Ms2SpectrumHeader(int uid, int polarity, boolean centroided, CollisionEnergy energy, IsolationWindow window, int parentId, double precursorMz, double retentionTime) {
        super(uid, polarity, centroided);
        this.energy = energy;
        this.parentId = parentId;
        this.isolationWindow = window;
        this.precursorMz = precursorMz;
        this.retentionTime = retentionTime;
    }

    public Ms2SpectrumHeader withUid(int uid) {
        return new Ms2SpectrumHeader(uid, polarity, centroided, energy, isolationWindow, parentId, precursorMz, retentionTime);
    }

}
