package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;

public class Ms2SpectrumHeader extends Ms1SpectrumHeader implements Serializable {
    @Nullable protected final CollisionEnergy energy;

    @Nullable protected final IsolationWindow isolationWindow;
    @Getter
    protected final int parentId;
    protected final double retentionTime;

    @Getter
    protected final double precursorMz;

    public Ms2SpectrumHeader(long scanId, int polarity, boolean centroided, @Nullable CollisionEnergy energy, @Nullable IsolationWindow window, int parentId, double precursorMz, double retentionTime) {
        super(scanId,-1, polarity, centroided);
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

    public Ms2SpectrumHeader(long scanId, int uid, int polarity, boolean centroided, @Nullable CollisionEnergy energy, @Nullable IsolationWindow window, int parentId, double precursorMz, double retentionTime) {
        super(scanId, uid, polarity, centroided);
        this.energy = energy;
        this.parentId = parentId;
        this.isolationWindow = window;
        this.precursorMz = precursorMz;
        this.retentionTime = retentionTime;
    }

    public Ms2SpectrumHeader withUid(int uid) {
        return new Ms2SpectrumHeader(scanId, uid, polarity, centroided, energy, isolationWindow, parentId, precursorMz, retentionTime);
    }



}
