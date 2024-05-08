package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.trace.Rect;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class MvBasedAlignmentStorage implements AlignmentStorage {

    private MVStore store;
    private MVMap<Long, MoI> mois;
    private AtomicInteger ids;

    // actually, we do not have to store this tiny little object...
    private AlignmentStatistics statistics;

    public MvBasedAlignmentStorage(MVStore store) {
        this.store = store;
        this.ids = new AtomicInteger();
        this.mois = store.openMap("mois", new MVMap.Builder<Long,MoI>().valueType(new MoI.DataType()));
        this.statistics = null;
    }

    @Override
    public void setStatistics(AlignmentStatistics statistics) {
        this.statistics = statistics;
    }

    @Override
    public AlignmentStatistics getStatistics() {
        return statistics;
    }

    @Override
    public List<MoI> getMoIWithin(double fromMz, double toMz) {
        // todo: storing floats instead of doubles is problematic here :/
        // maybe we should change that later. Unfortunately, the MvStore Rect stuff works with floats only...
        // for now I just add a little tolerance value
        fromMz -= Rect.FLOATING_POINT_TOLERANCE;
        toMz += Rect.FLOATING_POINT_TOLERANCE;

        long fromKey = ((long)(fromMz*1000L))<<30;
        long toKey = ((long)(toMz*1000L)+1L)<<30;
        Cursor<Long, MoI> cursor = mois.cursor(fromKey);
        final List<MoI> out = new ArrayList<>();
        while (cursor.hasNext()) {
            Long l = cursor.next();
            if (l >= toKey) break;
            MoI m = cursor.getValue();
            if (m.getMz() >= (fromMz) && m.getMz() <= (toMz)) {
                out.add(m);
            }
        }
        return out;
    }

    @Override
    public AlignedMoI mergeMoIs(AlignWithRecalibration recalibration, MoI left, MoI right) {
        AlignedMoI merge = AlignedMoI.merge(recalibration, left, right);
        if (left.getUid() >= 0) {
            mois.remove(left.getUid());
        }
        if (right.getUid() >= 0) {
            mois.remove(right.getUid());
        }
        addMoI(merge);
        return merge;
    }

    @Override
    public void addMoI(MoI moi) {
        {
            long id = ids.incrementAndGet();
            long mass = ((long) (moi.getMz() * 1000)) << 30;
            long key = mass + id;
            if (moi.getUid()>=0 && moi.getUid()!=key) {
                mois.remove(moi.getUid());
            }
            moi.setUid(key);
        }
        mois.put(moi.getUid(), moi);
    }

    public void removeMoI(long uid) {
        mois.remove(uid);
    }

    @Override
    public void removeMoIsIf(Predicate<MoI> predicate) {
        LongArrayList todel = new LongArrayList();
        Cursor<Long, MoI> cursor = mois.cursor(null);
        while (cursor.hasNext()) {
            cursor.next();
            if (predicate.test(cursor.getValue())) {
                todel.add(cursor.getKey());
            }
        }
        todel.forEach(x->mois.remove(x));
    }

    @Override
    public MoI getMoI(long uid) {
        return mois.get(uid);
    }

    @Override
    public void clearMoIs() {
        mois.clear();
    }

    @NotNull
    @Override
    public Iterator<MoI> iterator() {
        return mois.values().iterator();
    }
}
