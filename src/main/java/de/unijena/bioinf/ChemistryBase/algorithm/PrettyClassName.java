package de.unijena.bioinf.ChemistryBase.algorithm;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 25.04.13
 * Time: 16:47
 * To change this template use File | Settings | File Templates.
 */
public class PrettyClassName {

    public static String getName(Class<?> klass) {
        final Called name = klass.getAnnotation(Called.class);
        if (name != null) {
            return name.value();
        } else {
            return klass.getSimpleName();
        }
    }

}
