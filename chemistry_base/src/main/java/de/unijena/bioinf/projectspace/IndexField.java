package de.unijena.bioinf.projectspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexField {

    boolean documentId() default false;

    /**
     * Optional: If not set, the actual Java field name is used.
     */
    String name() default "";

    /**
     * Whether the field shall be stored. (If false then the field will be indexed but not retrievable in search results.)
     * Lucene: Adds additional StoredValueField to lucene index.
     */
    boolean stored() default true;

    /**
     * Whether a String field shall be indexed as StringField (false) or TextField (true).
     */
    boolean fullTextSearch() default false;

    /**
     * Whether a field shall be sortable.
     * Lucene: Adds additional doc value field to lucene index.
     */
    boolean sortable() default false;

    /**
     * Whether a field shall be used as fallback search field for queries that do not specify a field to search in. Simple fulltext search.
     */
    boolean defaultSearchField() default false;

    /**
     * Optional: A custom query rewriter class used to dynamically transform query terms/phrases for this field at parse-time.
     */
    Class<? extends QueryRewriter> queryRewriter() default QueryRewriter.NoOp.class;

    /**
     * Optional: A provider for the closed vocabulary of this field, so that clients can offer its values for
     * completion instead of leaving the user to guess them. A provider class is named here rather than the values
     * themselves, so the vocabulary stays where it is defined (see {@link PossibleValueProvider}).
     * <p>
     * Fields whose values follow from their java type (enums, booleans) need no provider. A declared provider is
     * more specific and wins over those derived values.
     */
    Class<? extends PossibleValueProvider> possibleValueProvider() default PossibleValueProvider.None.class;
}
