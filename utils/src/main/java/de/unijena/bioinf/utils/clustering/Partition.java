package de.unijena.bioinf.utils.clustering;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public final class Partition<T> extends AbstractList<List<T>> {

    private final List<T> list;
    private final int chunkSize;

    public Partition(List<T> list, int chunkSize) {
        this.list = new ArrayList<>(list);
        this.chunkSize = chunkSize;
    }

    public static <T> Partition<T> ofSize(List<T> list, int chunkSize) {
        return new Partition<>(list, chunkSize);
    }

    public static <T> Partition<T> ofNumber(List<T> list, int chunkNumber) {
        return new Partition<>(list, Math.max(1, list.size() / chunkNumber));
    }

    @Override
    public List<T> get(int index) {
        int start = index * chunkSize;
        int end = Math.min(start + chunkSize, list.size());

        if (start > end) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of the list range <0," + (size() - 1) + ">");
        }

        return new ArrayList<>(list.subList(start, end));
    }

    @Override
    public int size() {
        return (int) Math.ceil((double) list.size() / (double) chunkSize);
    }
}