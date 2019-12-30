package de.unijena.bioinf.ChemistryBase.utils;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.io.IOException;
import java.util.Optional;

public abstract class IOFunctions {

    public interface IOCallable<T> {
        public T call() throws IOException;
    }

    public interface IORunnable {
        public void run() throws IOException;
    }

    public interface IOFunction<A,B> {
        public B apply(A a) throws IOException;
    }

    public interface IOConsumer<A> {
        public void accept(A a) throws IOException;
    }

    public interface IOSupplier<A> {
        public A get() throws IOException;
    }

    public static interface ClassValueProducer {
        public <T extends DataAnnotation> Optional<T> apply(Class<T> klass) throws IOException;
    }

    public static interface ClassValueConsumer {
        public <T extends DataAnnotation> void apply(Class<T> klass, T value) throws IOException;
    }

}
