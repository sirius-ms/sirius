package de.unijena.bioinf.ms.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An indicator that this class add a certain annotation to its input
 */
@Repeatable(de.unijena.bioinf.ms.annotations.Provides.ProvidedAnnotations.class)
@Target({ METHOD,CONSTRUCTOR, TYPE})
@Retention(SOURCE)
public @interface Provides {

    /**
     * The annotation which is provided.
     */
    public Class<? extends DataAnnotation> value();

    public Class<?> in() default Object.class;

    @Target({ METHOD,CONSTRUCTOR, TYPE})
    @Retention(SOURCE)
    @interface ProvidedAnnotations {
        de.unijena.bioinf.ms.annotations.Provides[] value();
    }

}
