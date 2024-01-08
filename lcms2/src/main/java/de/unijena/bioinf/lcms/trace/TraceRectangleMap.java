package de.unijena.bioinf.lcms.trace;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface TraceRectangleMap extends Iterable<Rect> {

    public void addRect(Rect rect);

    public void removeRect(Rect rect);

    public void updateRect(Rect rect);

    public Optional<Rect> getRect(int id);
    public List<Rect> overlappingRectangle(Rect query);


}
