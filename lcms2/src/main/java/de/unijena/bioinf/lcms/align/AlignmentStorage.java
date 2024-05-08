package de.unijena.bioinf.lcms.align;

import java.util.List;
import java.util.function.Predicate;

public interface AlignmentStorage extends Iterable<MoI> {

    public void setStatistics(AlignmentStatistics statistics);

    public AlignmentStatistics getStatistics();

    public List<MoI> getMoIWithin(double fromMz, double toMz);

    AlignedMoI mergeMoIs(AlignWithRecalibration recalibration, MoI left, MoI right);

    public void addMoI(MoI moi);

    public MoI getMoI(long uid);
    void clearMoIs();

    void removeMoI(long x);

    void removeMoIsIf(Predicate<MoI> predicate);
}
