package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.projectspace.QueryRewriter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexFieldWithMapper {
    /**
     * Optional: If not set, the actual Java field name is used.
     */
    String name() default "";

    /**
     * A mapper class that maps between pojos and documents
     * @return class of the FieldMapper.
     */
    Class<? extends FieldMapper<?>> mapper();

    /**
     * Optional: A custom query rewriter used to transform query terms/phrases for the fields this mapper
     * contributes, at parse time.
     * <p>
     * Declared here rather than inside the mapper so that reading the field tells the whole story, the same way
     * {@code @IndexField} does. A mapper usually contributes several fields and the rewriter is registered for
     * all of them, so one that only means to act on some decides that by the field name it is handed - as the
     * vocabulary of such a field does.
     */
    Class<? extends QueryRewriter> queryRewriter() default QueryRewriter.NoOp.class;
}
