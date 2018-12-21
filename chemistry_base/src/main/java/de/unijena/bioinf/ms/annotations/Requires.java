package de.unijena.bioinf.ms.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Indicates that a certain annotation has to be set in the input such that this class can be applied
 */

@Repeatable(Requires.Requirements.class)
@Target({ METHOD,CONSTRUCTOR, TYPE})
@Retention(SOURCE)
public @interface Requires {

    /**
     * The annotation which is required.
     */
    public Class<? extends DataAnnotation> value();

    @Target({ METHOD,CONSTRUCTOR, TYPE})
    @Retention(SOURCE)
    @interface Requirements {
        Requires[] value();
    }

}
