package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.*;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MergeMvStorage implements MergeStorage{

    private final MVStore store;
    private final MVMap<ProjectedTraceKey, ProjectedTrace> projectedTraces;
    private final MVMap<IsotopeProjKey, ProjectedTrace> projectedIsotopeTraces;

    private final TraceRectangleMap rectangleMap;
    private final AtomicInteger traceCounter;
    private final AtomicInteger mergeCounter;

    public MergeMvStorage(MVStore store) {
        this.store = store;
        rectangleMap = new TraceRectangleMapByRVMap(store, "merge");
        traceCounter = new AtomicInteger();
        mergeCounter = new AtomicInteger(2);

        this.projectedTraces = store.openMap("projectedTraces"); // todo: add custom data types
        this.projectedIsotopeTraces = store.openMap("projectedIsotopeTraces"); // todo: add custom data types
    }

    @Override
    public TraceRectangleMap getRectangleMap() {
        return rectangleMap;
    }

    @Override
    public void addProjectedTrace(int mergedTraceUiD, int sampleId, ProjectedTrace trace) {
        projectedTraces.put(new ProjectedTraceKey(mergedTraceUiD, sampleId), trace);
    }

    @Override
    public Iterator<ProjectedTrace> forEachProjectedTraceOf(int mergedTraceUid) {
        final ProjectedTraceKey key = new ProjectedTraceKey(mergedTraceUid, Integer.MIN_VALUE);
        final Cursor<ProjectedTraceKey, ProjectedTrace> cursor = projectedTraces.cursor(key);
        if (!cursor.hasNext()) return Collections.emptyIterator();
        return new Iterator<ProjectedTrace>() {
            ProjectedTraceKey current = cursor.next();
            @Override
            public boolean hasNext() {
                return current!=null && current.mergedTraceUid==mergedTraceUid;
            }

            @Override
            public ProjectedTrace next() {
                ProjectedTrace nxt = cursor.getValue();
                current = cursor.hasNext() ? cursor.next() : null;
                return nxt;
            }
        };
    }

    @Override
    public ProjectedTrace getProjectedTrace(int mergedTraceUiD, int sampleId) {
        return projectedTraces.get(new ProjectedTraceKey(mergedTraceUiD, sampleId));
    }

    @Override
    public void addIsotopeProjectedTrace(int parentTraceUiD, int isotopeId, int sampleId, ProjectedTrace trace) {
        this.projectedIsotopeTraces.put(new IsotopeProjKey(parentTraceUiD, isotopeId, sampleId), trace);
    }

    @Override
    public ProjectedTrace getIsotopeProjectedTrace(int parentTraceUiD, int isotopeId, int sampleId) {
        return projectedIsotopeTraces.get(new IsotopeProjKey(parentTraceUiD, isotopeId, sampleId));
    }

    @Override
    public ProjectedTrace[][] getIsotopePatternFor(int parentTraceUiD) {
        IsotopeProjKey key = new IsotopeProjKey(parentTraceUiD, 0, Integer.MIN_VALUE);
        final ArrayList<ArrayList<ProjectedTrace>> isotopes = new ArrayList<>();
        Cursor<IsotopeProjKey, ProjectedTrace> cursor = projectedIsotopeTraces.cursor(key);
        int lastIsotopePosition = -1;
        while (cursor.hasNext()) {
            key = cursor.next();
            if (key.parentTraceUiD != parentTraceUiD) break;
            ArrayList<ProjectedTrace> list;
            if (key.isotopeId==lastIsotopePosition) {
                list = isotopes.get(isotopes.size()-1);
            } else {
                list = new ArrayList<>();
                isotopes.add(list);
                lastIsotopePosition = key.isotopeId;
            }
            list.add(cursor.getValue());
        }
        return isotopes.stream().map(x->x.toArray(ProjectedTrace[]::new)).toArray(ProjectedTrace[][]::new);
    }

    private static class IsotopeProjKey implements Comparable<IsotopeProjKey>, Serializable {
        private final int parentTraceUiD, isotopeId, sampleId;

        public IsotopeProjKey(int parentTraceUiD, int isotopeId, int sampleId) {
            this.parentTraceUiD = parentTraceUiD;
            this.isotopeId = isotopeId;
            this.sampleId = sampleId;
        }

        @Override
        public int compareTo(@NotNull MergeMvStorage.IsotopeProjKey o) {
            int c = Integer.compare(parentTraceUiD, o.parentTraceUiD);
            if (c!=0) return c;
            c = Integer.compare(isotopeId, o.isotopeId);
            if (c!=0) return c;
            return Integer.compare(sampleId, o.sampleId);
        }
    }


    public static class ProjectedTraceKey implements Comparable<ProjectedTraceKey>, Serializable {
        private int mergedTraceUid;
        private int sampleId;

        public ProjectedTraceKey(int mergedTraceUid, int sampleId) {
            this.mergedTraceUid = mergedTraceUid;
            this.sampleId = sampleId;
        }

        @Override
        public int compareTo(@NotNull ProjectedTraceKey o) {
            int c = Integer.compare(mergedTraceUid,o.mergedTraceUid);
            if (c==0) c = Integer.compare(sampleId,o.sampleId);
            return c;
        }
    }

    public static class IsotopeTraceKey implements Comparable<IsotopeTraceKey>, Serializable {
        private final int mergedTraceUiD;
        private final int isotopePosition;

        public IsotopeTraceKey(int mergedTraceUiD, int isotopePosition) {
            this.mergedTraceUiD = mergedTraceUiD;
            this.isotopePosition = isotopePosition;
        }

        @Override
        public int compareTo(@NotNull IsotopeTraceKey o) {
            int c = Integer.compare(mergedTraceUiD, o.mergedTraceUiD);
            if (c==0) c = Integer.compare(isotopePosition, o.isotopePosition);
            return c;
        }
    }

}
