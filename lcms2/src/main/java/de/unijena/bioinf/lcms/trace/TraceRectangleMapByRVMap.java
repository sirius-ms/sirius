package de.unijena.bioinf.lcms.trace;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TraceRectangleMapByRVMap implements TraceRectangleMap{

    private MVStore store;
    private MVRTreeMap<Integer> plane;

    private MVMap<Integer, Rect> entries;

    private AtomicInteger ids;

    public TraceRectangleMapByRVMap(MVStore store, String prefix) {
        this.store = store;
        this.plane = store.openMap(prefix  + "_plane", new MVRTreeMap.Builder<>());
        this.entries = store.openMap(prefix + "_entries");
        ids = new AtomicInteger();
    }

    @Override
    public void addRect(Rect rect) {
        if (rect.id<0) rect.id = ids.getAndIncrement();
        entries.put(rect.id, rect);
        plane.put(rect.toKey(), rect.id);
    }

    @Override
    public void removeRect(Rect rect) {
        Rect k = entries.remove(rect.id);
        if (k!=null) plane.remove(k.toKey());
    }

    @Override
    public void updateRect(Rect rect) {
        removeRect(rect);
        addRect(rect);
    }

    @Override
    public Optional<Rect> getRect(int id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<Rect> overlappingRectangle(Rect query) {
        MVRTreeMap.RTreeCursor intersectingKeys = plane.findIntersectingKeys(query.toKey());
        final List<Rect> rectangles = new ArrayList<>();
        while (intersectingKeys.hasNext()) getRect((int)intersectingKeys.next().getId()).ifPresent(rectangles::add);
        return rectangles;
    }

    @NotNull
    @Override
    public Iterator<Rect> iterator() {
        return entries.values().iterator();
    }
}
