package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IdentificationResults implements ResultAnnotation, Iterable<IdentificationResult> {
    private final List<IdentificationResult> results;

    public Optional<IdentificationResult> getResultFor(MolecularFormula formula, PrecursorIonType ionType) {
        for (IdentificationResult ir : results) {
            if (ir.getMolecularFormula().equals(formula) && ir.getPrecursorIonType().equals(ionType))
                return Optional.of(ir);
        }
        return Optional.empty();
    }


    public IdentificationResults(@NotNull Iterable<IdentificationResult> c) {
        results = new ArrayList<>();
        c.forEach(results::add);
    }

    public void setRankingScoreType(@NotNull final Class<? extends ResultScore> scoreType) {
        results.forEach(r -> r.setRankingScore(scoreType));
        Collections.sort(results);
        int rank = 1;
        for (IdentificationResult result : results) result.rank = rank++;
    }

    public IdentificationResult[] toArray() {
        IdentificationResult[] arr = new IdentificationResult[results.size()];
        return results.toArray(arr);
    }

    public List<IdentificationResult> toList() {
        return new ArrayList<>(results);
    }

    public Stream<IdentificationResult> stream() {
        return results.stream();
    }

    public Stream<IdentificationResult> parallelStream() {
        return results.parallelStream();
    }

    public IdentificationResults addToNewInstance(IdentificationResult result) {
        return addAllToNewInstance(Collections.singletonList(result));
    }

    public IdentificationResults addAllToNewInstance(Iterable<IdentificationResult> results) {
        final IdentificationResults nuInstance = clone();
        results.forEach(nuInstance.results::add);
        return nuInstance;
    }


    public IdentificationResults removeFromNewInstance(IdentificationResult result) {
        return removeAllFromNewInstance(Collections.singletonList(result));
    }

    public IdentificationResults removeAllFromNewInstance(Iterable<IdentificationResult> results) {
        final IdentificationResults nuInstance = clone();
        results.forEach(nuInstance.results::remove);
        return nuInstance;
    }

    @NotNull
    @Override
    public Iterator<IdentificationResult> iterator() {
        return results.iterator();
    }

    public ListIterator<IdentificationResult> listIterator() {
        return results.listIterator();
    }

    public ListIterator<IdentificationResult> listIterator(int index) {
        return results.listIterator(index);
    }

    @Override
    public Spliterator<IdentificationResult> spliterator() {
        return results.spliterator();
    }

    @Override
    public void forEach(Consumer<? super IdentificationResult> action) {
        results.forEach(action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentificationResults that = (IdentificationResults) o;
        return results.equals(that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }

    @Override
    public String toString() {
        return "IdentificationResults: " + results;
    }

    @Override
    protected IdentificationResults clone() {
        return new IdentificationResults(results);
    }

    public int size() {
        return results.size();
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean contains(IdentificationResult o) {
        return results.contains(o);
    }

    public boolean containsAll(@NotNull Collection<IdentificationResult> c) {
        return results.containsAll(c);
    }

    public IdentificationResult get(int index) {
        return results.get(index);
    }

    public int indexOf(IdentificationResult o) {
        return results.indexOf(o);
    }

    public int lastIndexOf(IdentificationResult o) {
        return results.lastIndexOf(o);
    }
}
