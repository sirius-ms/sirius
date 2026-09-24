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
     * Whether a field shall be used as fallback search field for queries that do not specify a field to search in.
     * A text field is searched for the bare term (simple fulltext search). A numeric field is searched for a bare
     * NUMBER within a window around it - half a unit of the number's last decimal, at least a few ppm - so a mass
     * typed the way people type masses finds the feature it means; a bare term that is not a number does not reach
     * a numeric field at all.
     */
    boolean defaultSearchField() default false;

    /**
     * Optional: A custom query rewriter class used to dynamically transform query terms/phrases for this field at parse-time.
     */
    Class<? extends QueryRewriter> queryRewriter() default QueryRewriter.NoOp.class;
}
