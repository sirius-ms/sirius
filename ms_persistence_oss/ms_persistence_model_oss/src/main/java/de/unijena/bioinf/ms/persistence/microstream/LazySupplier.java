package de.unijena.bioinf.ms.persistence.microstream;

import one.microstream.reference.Lazy;

import java.util.function.Function;

public class LazySupplier<I, T> implements Lazy<T> {

    public static<I, T> LazySupplier<I, T> from(Lazy<I> input, Function<I, T> converter){
        return new LazySupplier<>(input, converter);
    }

    private final Function<I, T> converter;
    private final Lazy<I> input;

    public LazySupplier(Lazy<I> input, Function<I, T> converter) {
        this.converter = converter;
        this.input = input;
    }

    @Override
    public T get() {
        return converter.apply(input.get());
    }

    @Override
    public T peek() {
        I it = input.peek();
        if (it == null)
            return null;
        return converter.apply(it);
    }

    @Override
    public T clear() {
        throw new UnsupportedOperationException("Lazy mapper does not support clearing!");
        //todo Highly unsure if this is working ->  maybe jus unsupported exception
//        input.clear();
//        return peek();
    }

    @Override
    public boolean isStored() {
        return input.isStored();
    }

    @Override
    public boolean isLoaded() {
        return input.isLoaded();
    }

    @Override
    public long lastTouched() {
        return input.lastTouched();
    }

    @Override
    public boolean clear(ClearingEvaluator clearingEvaluator) {
        return input.clear(clearingEvaluator);
    }
}
