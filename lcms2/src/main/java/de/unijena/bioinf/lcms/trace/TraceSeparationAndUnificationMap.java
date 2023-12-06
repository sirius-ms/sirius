package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * - each trace is mapped into a rectangle of the TraceSeparationAndUnificationMap
 * - if two traces overlap in their rectangle, we can
 *      - merge them into one
 *      - separate them by making the rectangles smaller
 * - after mapping all rectangles we can map each trace to a region of
 *   the m/z and retention time plane such that it is guaranteed that no
 *   two traces overlap to each other
 *
 *   Note: This is an alternative to the quantisation of the m/z domain which is more
 *   flexible, as it allows two traces to have very similar but still separate m/z values
 *   as long as they have clearly separable retention times
 *
 *   Implementation: The default implementation uses r-trees from the MVStore to calculate if two rectangles overlap.
 *
 *   We use the TraceSeparationAndUnificationMap for two scenarios:
 *
 *   1.) Separating traces WITHIN a sample
 *     - after picking traces, we map all traces to the m/z and rt plane and join consecutive traces while
 *       separate overlapping traces. This gives us, as side-product, an m/z deviation value that clearly separates
 *       overlapping traces. Furthermore, it is guaranteed that we do not accidentally map two different traces on the
 *       same data points.
 *
 *   2.) Separating merged traces ACROSS samples
 *     - here, we have a set of mass of interests (MoIs) which are aligned across samples. Each MoI stores the m/z and
 *       rt plane its traces lie on. We want to separate and unify traces such that:
 *         - two MoIs that belong to the same trace map to only one rectangle
 *         - two rectangles that overlap can be separated, such that the corresponding traces can be merged without
 *           accidentally merging the same trace into two different merged traces
 *     - the difference to scenario 1 is that multiple MoIs can map to the same trace. So we have to use a minimal
 *       allowed m/z difference for which we always merge overlapping rectangles no matter what.
 *
 *  Implementation Note: Rectangles are stored as floating point values. This limits the precision of the stored values.
 *  For m/z values it should be possible in general to store 4 digits after the decimal point. For rt values, we
 *  have to store rt in seconds to avoid problems. Alternatively, we could store the scan index instead of the rt.
 */
public abstract class TraceSeparationAndUnificationMap implements Iterable<TraceSeparationAndUnificationMap.Rectangle> {

    public static TraceSeparationAndUnificationMap getMapFromMvStore(MVStore store, String prefix) {
        return new TraceSeparationAndUnificationMapByRTree(store,prefix);
    }

    public static class Rectangle implements Comparable<Rectangle>, Serializable {
        public float minMz, maxMz, minRt, maxRt;
        public double avgMz;
        public int id;

        private Rectangle innerNonOverlaping;

        public Rectangle(float minMz, float maxMz, float minRt, float maxRt, double avgMz) {
            this(minMz,maxMz,minRt,maxRt,avgMz, -1);
        }

        public Rectangle(float minMz, float maxMz, float minRt, float maxRt, double avgMz, int id) {
            this.minMz = minMz;
            this.maxMz = maxMz;
            this.minRt = minRt;
            this.maxRt = maxRt;
            this.avgMz = avgMz;
            this.id = id;
        }

        public void upgrade(Rectangle other) {
            minMz = Math.min(minMz, other.minMz);
            maxMz = Math.max(maxMz, other.maxMz);
            minRt = Math.min(minRt, other.minRt);
            maxRt = Math.max(maxRt, other.maxRt);
            avgMz = (avgMz+other.avgMz)/2;
        }

        public boolean contains(Rectangle other) {
            return minMz <= other.minMz && maxMz >= other.maxMz && minRt <= other.minRt && maxRt >= other.maxRt;
        }

        @Override
        public int compareTo(@NotNull TraceSeparationAndUnificationMap.Rectangle o) {
            int c = Float.compare(minMz, o.minMz);
            if (c!=0) return c;
            c = Float.compare(maxMz, o.maxMz);
            if (c!=0) return c;
            c = Float.compare(minRt, o.minRt);
            if (c!=0) return c;
            c = Float.compare(maxRt, o.maxRt);
            if (c!=0) return c;
            else return Integer.compare(id, o.id);
        }

        @Override
        public String toString() {
            return id + ":(m/z = " + minMz + ".." + maxMz + ", rt = " + minRt + ".." + maxRt + ", avgMz = " + avgMz + ")" ;
        }

        public Optional<Rectangle> getInnerArea(){
            return Optional.ofNullable(innerNonOverlaping);
        }
    }

    public interface Callback {
        public void merge(Rectangle mergedRectangle, int[] mergedIds);
    }

    /**
     * After calling this routine, no two rectangles are allowed to overlap anymore
     *
     * - consecutive rectangles without overlap are always joined if their m/z values is below
     *   the mergeEverythingBelow parameter
     * - overlapping rectangles with m/z below mergeEverythingBelow are merged ONLY if the
     *   parameter mergeOverlapping is true.
     * - overlapping rectangles with m/z above mergeEverythingBelow are updated such that they do no
     *   longer overlap
     * - whenever traces are merged, the callback is called with the merged rectangle and the merged ids.
     */
    public void resolveOverlappingRectangles(Deviation mergeEverythingBelow, boolean mergeOverlapping, Callback callback) {
        // we store all rectangles instead of using an iterator,
        // because we will update the store while iterating
        final ArrayList<Rectangle> rects = new ArrayList<>();
        for (Rectangle r : this) rects.add(r);
        for (Rectangle rectangle : rects) {
            if (rectangle.id < 0) continue;
            final List<Rectangle> overlapping = overlappingRectangle(rectangle);
            if (overlapping.size()>1) {
                // merge rectangles that have almost same m/z value
                if (mergeOverlapping) {
                    mergeRectanglesWithSimilarMz(overlapping, mergeEverythingBelow, callback);
                }
                // separate rectangles from each other in m/z plane
                if (overlapping.size()>1) {
                    separateRectanglesInMzPlane(overlapping);
                }
            }
        }
    }

    private void separateRectanglesInMzPlane(List<Rectangle> overlapping) {
        overlapping.sort(null);
        System.out.println("-------------------");
        System.out.println(overlapping);

        for (int k=0; k < overlapping.size(); ++k) {
            Rectangle r = overlapping.get(k);
            r.innerNonOverlaping = new Rectangle(r.minMz,r.maxMz,r.minRt,r.maxRt,r.avgMz);
        }

        for (int i=1; i < overlapping.size(); ++i) {
            Rectangle l = overlapping.get(i-1);
            Rectangle r = overlapping.get(i);
            float lmz = l.maxMz;
            float rmz = r.minMz;
            if (lmz > rmz) {
                float middle = (float)((l.avgMz+r.avgMz)/2);
                if (lmz > middle) {
                    lmz = Math.min(rmz, middle);
                } else {
                    rmz = Math.min(lmz, middle);
                }
                float epsilon = 1e-5f;
                while (lmz==rmz) {
                    lmz -= epsilon;
                    epsilon+=1e-5f; // -_- floating point precision problem...
                }
                l.maxMz = lmz;
                r.minMz = rmz;
            }
        }

        // separate rectangles in rt plane
        overlapping.sort(Comparator.comparingDouble(x->x.minRt));
        for (int i=1; i < overlapping.size(); ++i) {
            Rectangle l = overlapping.get(i-1).innerNonOverlaping;
            Rectangle r = overlapping.get(i).innerNonOverlaping;
            float lmz = l.maxRt;
            float rmz = r.minRt;
            if (l.maxRt > r.maxRt) continue;
            if (lmz > rmz) {
                float middle = (float)((l.maxRt-r.minRt)/2 + r.minRt);
                float epsilon = 1e-5f;
                lmz=middle;
                rmz=middle;
                while (lmz==rmz) {
                    lmz -= epsilon;
                    epsilon+=1e-5f; // -_- floating point precision problem...
                }
                l.maxRt = lmz;
                r.minRt = rmz;
            }
        }

        for (int i=0; i < overlapping.size(); ++i) {
            Rectangle r = overlapping.get(i);
            if (r.innerNonOverlaping.minRt==r.minRt&&r.innerNonOverlaping.maxRt==r.maxRt) r.innerNonOverlaping=null; // we dont need that
            removeRectangle(r.id);
            addRectangle(r);
        }
        System.out.println(overlapping);
        System.out.println("-------------------");

    }

    private void mergeRectanglesWithSimilarMz(List<Rectangle> overlapping, Deviation mergeEverythingBelow, Callback callback) {
        List<Rectangle> merge = new ArrayList<>();
        for (int i=0; i < overlapping.size(); ++i) {
            final Rectangle l = overlapping.get(i);
            merge.clear();
            merge.add(l);
            ListIterator<Rectangle> liter = overlapping.listIterator(i+1);
            while (liter.hasNext()) {
                final Rectangle r = liter.next();
                if (mergeEverythingBelow.inErrorWindowSymetric(l.avgMz,r.avgMz) || l.contains(r) || r.contains(l)) {
                    // merge l with r
                    l.upgrade(r);
                    // delete r
                    merge.add(r);
                    liter.remove();
                }
            }
            if (merge.size()>1) {
                callback.merge(l, merge.stream().mapToInt(x->x.id).toArray());
                // delete all merged rectangles
                for (Rectangle r : merge) {
                    if (r!=l) {
                        removeRectangle(r.id);
                        r.id = -1; // mark as deleted
                    }
                }
            }
        }
        overlapping.removeIf(x->x.id<0);
    }


    public abstract void addRectangle(Rectangle rectangle);

    public abstract void removeRectangle(int id);

    public void updateRectangle(Rectangle rect) {
        removeRectangle(rect.id);
        addRectangle(rect);
    }

    public abstract Optional<Rectangle> getRectangle(int id);

    public abstract List<Rectangle> overlappingRectangle(Rectangle query);

    protected static class TraceSeparationAndUnificationMapByRTree extends TraceSeparationAndUnificationMap {

        private final MVStore store;
        private final MVRTreeMap<Integer> plane;
        private final MVMap<Integer, Rectangle> entries;

        protected TraceSeparationAndUnificationMapByRTree(MVStore store, String prefix) {
            this.store = store;
            this.plane = store.openMap(prefix  + "_plane", new MVRTreeMap.Builder<>());
            this.entries = store.openMap(prefix + "_entries");
        }

        @Override
        public void addRectangle(Rectangle rectangle) {
            entries.put(rectangle.id, rectangle);
            plane.add(toKey(rectangle), rectangle.id);
        }

        @Override
        public void removeRectangle(int id) {
            Rectangle rect = entries.remove(id);
            if (rect != null) {
                plane.remove(toKey(rect));
            }
        }

        @Override
        public Optional<Rectangle> getRectangle(int id) {
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public List<Rectangle> overlappingRectangle(Rectangle query) {
            MVRTreeMap.RTreeCursor intersectingKeys = plane.findIntersectingKeys(toKey(query));
            final List<Rectangle> rectangles = new ArrayList<>();
            while (intersectingKeys.hasNext()) getRectangle((int)intersectingKeys.next().getId()).ifPresent(rectangles::add);
            return rectangles;
        }

        private static SpatialKey toKey(Rectangle query) {
            return new SpatialKey(query.id, query.minMz, query.maxMz, query.minRt, query.maxRt);
        }

        @NotNull
        @Override
        public Iterator<Rectangle> iterator() {
            Cursor<Integer, Rectangle> cursor = entries.cursor(null);
            return new Iterator<Rectangle>() {
                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                public Rectangle next() {
                    cursor.next();
                    return cursor.getValue();
                }
            };
        }
    }


}
