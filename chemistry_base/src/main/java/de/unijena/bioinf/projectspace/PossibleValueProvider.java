package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Supplies the closed vocabulary of an indexed field, i.e. the values it can take, exactly as they are indexed.
 * <p>
 * Fields whose values come from a fixed domain (a compound class ontology, a controlled list of instrument
 * settings, ...) are searchable but not guessable: a client can only offer them for completion if it is told
 * what they are. Declaring a provider is how a field says "these are my values" without the values themselves
 * ending up as string literals in an annotation - the vocabulary stays where it is defined and stays a single
 * source of truth.
 * <p>
 * Implementations must be stateless and have a public no-arg constructor; they are instantiated once and shared.
 *
 * @see IndexField#possibleValueProvider()
 */
public interface PossibleValueProvider {

    /**
     * @param fieldName the full path of the field as used in queries (e.g.
     *                  {@code topAnnotations.compoundClassAnnotation.npcPathway}). One provider can serve
     *                  several fields, which is why the vocabulary is requested per field.
     * @return the values the field can take, exactly as they are indexed, or null if this provider has no
     * vocabulary for the given field (values derived from the java type, e.g. enum constants, then still apply)
     */
    @Nullable
    List<String> getPossibleValues(@NotNull String fieldName);

    /**
     * Combines providers that each know the vocabulary of different fields: the first one with an answer wins.
     */
    static PossibleValueProvider firstOf(@NotNull PossibleValueProvider... providers) {
        return fieldName -> {
            for (PossibleValueProvider provider : providers) {
                List<String> values = provider.getPossibleValues(fieldName);
                if (values != null)
                    return values;
            }
            return null;
        };
    }

    /**
     * The default: the field has no closed vocabulary and accepts free text.
     */
    class None implements PossibleValueProvider {
        @Override
        public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
            return null;
        }
    }
}
