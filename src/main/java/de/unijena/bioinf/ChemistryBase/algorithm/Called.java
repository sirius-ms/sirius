package de.unijena.bioinf.ChemistryBase.algorithm;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Give the class or field a more meaningful name which is not restricted by the java variable name conventions/conditions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({FIELD, METHOD, TYPE})
public @interface Called {

    public String value();

}
