package de.unijena.bioinf.ChemistryBase.data;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterizedByAnnotation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public abstract class ParameterHelper {

    public final static String NAME = "$name";
    public final static String VERSION = "$version";

    private final static JDKDocument jdk = new JDKDocument();
    private static final HashSet<Class> supportedClasses = new HashSet<Class>();
    static {
        final Class[] supported = new Class[]{
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class,
                Double.class, String.class, List.class, Map.class, BigInteger.class, BigDecimal.class
        };
        for (Class<?> klass : supported) {
            supportedClasses.add(klass);
        }
    }

    private HashMap<Class, ParameterizedByAnnotation> cache = new HashMap<Class, ParameterizedByAnnotation>();

    public String getKeyName(Class<?> klass) {
        return klass.getCanonicalName();
    }

    public boolean isConvertable(Object o) {
        return o instanceof Number || supportedClasses.contains(o.getClass());
    }

    public <G,D,L> Object unwrap(DataDocument<G,D,L> document, G value) {
        if (document.isDictionary(value)) {
            final D dictionary = document.getDictionary(value);
            final G name = document.getFromDictionary(dictionary, NAME);
            if (document.isNull(name)) {
                // try to convert as hashmap
                return DataDocument.transform(document, jdk, value);
            } else {
                final Class<?> klass = getFromClassName(document.getString(name));
                if (klass.isAnnotationPresent(HasParameters.class)) {
                    final ParameterizedByAnnotation k;
                    if (cache.containsKey(klass)) {
                        k = cache.get(klass);
                    } else {
                        k = new ParameterizedByAnnotation(klass);
                        cache.put(klass, k);
                    }
                    final Object o = k.construct(this, document, dictionary);
                    k.delegateFor(o).importParameters(this, document, dictionary);
                    return o;
                } else if (Parameterized.class.isAssignableFrom(klass)) {
                    try {
                        final Parameterized o = (Parameterized)klass.newInstance();
                        o.importParameters(this, document, dictionary);
                        return o;
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } else if (ImmutableParameterized.class.isAssignableFrom(klass)) {
                    try {
                        final ImmutableParameterized o = (ImmutableParameterized)klass.newInstance();
                        return o.readFromParameters(this, document, dictionary);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Parameter of type " + klass + " is not importable");
                }
            }
        } else {
            return DataDocument.transform(document, jdk, value);
        }
    }

    public Class<?> getFromClassName(String string) {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(string);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String toClassName(Class<?> klass) {
        return klass.getCanonicalName();
    }

    public <G,D,L> G wrap(DataDocument<G,D,L> document, Object o) {
        if (isConvertable(o)) {
            return DataDocument.transform(jdk, document, o);
        } else {
            // use dictionary
            final D dictionary = document.newDictionary();
            document.addToDictionary(dictionary, NAME, toClassName(o.getClass()));
            if (o instanceof Parameterized) {
                ((Parameterized) o).exportParameters(this, document, dictionary );
            } else if (o instanceof ImmutableParameterized) {
                ((ImmutableParameterized) o).exportParameters(this, document, dictionary );
            } else if (o.getClass().isAnnotationPresent(HasParameters.class)) {
                final ParameterizedByAnnotation k;
                if (cache.containsKey(o.getClass())) {
                    k = cache.get(o.getClass());
                } else {
                    k = new ParameterizedByAnnotation(o.getClass());
                    cache.put(o.getClass(), k);
                }
                k.delegateFor(o).exportParameters(this, document, dictionary);
            } else {
                throw new RuntimeException("Parameter of type " + o.getClass() + " is not exportable");
            }
            return document.wrapDictionary(dictionary);
        }

    }



}
