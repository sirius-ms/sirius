package de.unijena.bioinf.ms.properties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface DefaultProperty {
    /**
     * Use this if you want the id for the given field or parameter to be
     * different from the field name. Note that the field name will be
     * ignored if a Class has only a single field/parameter. In such
     * cases only the propertyParent value will be used.
     * */
    String propertyKey() default "";

    /**
     * Use this if you wont the ID/Name of the Property to be different from the corresponding Class name.
     * One Class/Parent may hold multiple fields.
     * Note that all propertyParent values in one Class need to be the same and that the
     * propertyParent name is used as a unique ID to identify the corresponding Class at runtime.
     * */
    String propertyParent() default "";
}
