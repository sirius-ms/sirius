package de.unijena.bioinf.ChemistryBase.algorithm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {

    public String value() default "";

}
