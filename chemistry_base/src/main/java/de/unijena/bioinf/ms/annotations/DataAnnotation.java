package de.unijena.bioinf.ms.annotations;

/**
 * Any kind of annotation for input, intermediates and output.
 *
 * Note that annotations have to be:
 * - immutable
 * - low memory footprint
 * - serializable (e.g. by a toString and fromString method)
 */
public interface DataAnnotation {
    static <T extends DataAnnotation> String getIdentifier(Class<T> annotationType) {
        return annotationType.getName();
    }

    default String getIdentifier() {
        return getIdentifier(getClass());
    }
}
