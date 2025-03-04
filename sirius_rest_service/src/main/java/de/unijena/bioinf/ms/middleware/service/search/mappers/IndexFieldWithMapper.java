package de.unijena.bioinf.ms.middleware.service.search.mappers;

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
}
