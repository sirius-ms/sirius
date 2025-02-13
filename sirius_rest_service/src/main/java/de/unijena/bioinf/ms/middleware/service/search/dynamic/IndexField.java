package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import java.lang.annotation.*;

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
     */
    boolean stored() default false;

    /**
     * Whether a String field shall be indexed as StringField (false) or TextField (true).
     */
    boolean fullTextSearch() default false;

    /**
     * Whether a field shall be used as fallback search field for queries that do not specify a field to search in. Simple fulltext search.
     */
    boolean defaultSearchField() default false;
}
