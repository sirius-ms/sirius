package de.unijena.bioinf.ms.rest.model.worker;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkerList /*extends AbstractList<WorkerInfo>*/ {
    private int pendingJobs = Integer.MIN_VALUE;
    private final ArrayList<WorkerInfo> workerList;

    public WorkerList(int initialCapacity) {
        workerList = new ArrayList<>(initialCapacity);
    }

    public WorkerList(Collection<? extends WorkerInfo> c, int pendingJobs) {
        this(c);
        this.pendingJobs = pendingJobs;
    }

    public WorkerList() {
        workerList = new ArrayList<>();
    }


    public WorkerList(Collection<? extends WorkerInfo> c) {
        workerList = new ArrayList<>(c);
    }

    public WorkerList(int initialCapacity, int pendingJobs) {
        this(initialCapacity);
        this.pendingJobs = pendingJobs;
    }

    public int getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(int pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public Stream<WorkerInfo> getWorkerActiveWithinAsStrean(Instant slot) {
        final long time = System.currentTimeMillis();
        return workerList.stream().filter((w) -> w.getPulseAsLong() + slot.toEpochMilli() >= time);
    }

    public List<WorkerInfo> getWorkerActiveWithin(Instant slot) {
        return getWorkerActiveWithinAsStrean(slot).collect(Collectors.toList());
    }

    public long getNumWorkerActiveWithin(Instant slot) {
        return getWorkerActiveWithinAsStrean(slot).count();
    }

    public EnumSet<PredictorType> getSupportedTypes() {
        return workerList.stream().map(WorkerInfo::getPredictorsAsEnums).flatMap(Collection::stream).collect(Collectors.toCollection(() -> EnumSet.noneOf(PredictorType.class)));
    }

    public EnumSet<PredictorType> getActiveSupportedTypes(Instant slot) {
        return getWorkerActiveWithinAsStrean(slot).map(WorkerInfo::getPredictorsAsEnums).flatMap(Collection::stream).collect(Collectors.toCollection(() -> EnumSet.noneOf(PredictorType.class)));
    }

    public boolean supportsAllPredictorTypes(EnumSet<PredictorType> neededTypes, Instant activeWithin) {
        return getActiveSupportedTypes(activeWithin).containsAll(neededTypes);
    }

    public boolean supportsAllPredictorTypes(EnumSet<PredictorType> neededTypes) {
        return getActiveSupportedTypes(Instant.ofEpochSecond(600/*10 min*/)).containsAll(neededTypes);
    }


    //region ArrayList Delegation
    public int size() {
        return workerList.size();
    }

    public boolean isEmpty() {
        return workerList.isEmpty();
    }

    public boolean contains(Object o) {
        return workerList.contains(o);
    }

    public int indexOf(Object o) {
        return workerList.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return workerList.lastIndexOf(o);
    }

    public WorkerInfo get(int index) {
        return workerList.get(index);
    }

    public WorkerInfo set(int index, WorkerInfo element) {
        return workerList.set(index, element);
    }

    public boolean add(WorkerInfo workerInfo) {
        return workerList.add(workerInfo);
    }

    public void add(int index, WorkerInfo element) {
        workerList.add(index, element);
    }

    public WorkerInfo remove(int index) {
        return workerList.remove(index);
    }

    public boolean remove(Object o) {
        return workerList.remove(o);
    }

    public void clear() {
        workerList.clear();
    }

    public boolean addAll(Collection<? extends WorkerInfo> c) {
        return workerList.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends WorkerInfo> c) {
        return workerList.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
        return workerList.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return workerList.retainAll(c);
    }

    @NotNull
    public ListIterator<WorkerInfo> listIterator(int index) {
        return workerList.listIterator(index);
    }

    @NotNull
    public ListIterator<WorkerInfo> listIterator() {
        return workerList.listIterator();
    }

    @NotNull
    public Iterator<WorkerInfo> iterator() {
        return workerList.iterator();
    }

    @NotNull
    public List<WorkerInfo> subList(int fromIndex, int toIndex) {
        return workerList.subList(fromIndex, toIndex);
    }

    public void forEach(Consumer<? super WorkerInfo> action) {
        workerList.forEach(action);
    }

    public Spliterator<WorkerInfo> spliterator() {
        return workerList.spliterator();
    }

    public boolean removeIf(Predicate<? super WorkerInfo> filter) {
        return workerList.removeIf(filter);
    }

    public void replaceAll(UnaryOperator<WorkerInfo> operator) {
        workerList.replaceAll(operator);
    }

    public boolean containsAll(Collection<?> c) {
        return workerList.containsAll(c);
    }

    public Stream<WorkerInfo> stream() {
        return workerList.stream();
    }

    public Stream<WorkerInfo> parallelStream() {
        return workerList.parallelStream();
    }
    //endregion
}
