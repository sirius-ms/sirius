
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

public class ParameterHelper {

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

    private HashMap<Class, ParameterizedByAnnotation> cache;
    private HashMap<String, Class<?>> nameCache;
    private List<String> lookupPaths;
    private Pattern pattern;


    public static ParameterHelper getParameterHelper() {
        final ParameterHelper helper = new ParameterHelper();
        helper.addLookupPath("de.unijena.bioinf");
        helper.addLookupPath("de.unijena.bioinf.ChemistryBase.math");
        helper.addLookupPath("de.unijena.bioinf.ChemistryBase.chem.utils.scoring");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation.merging");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation.treepost");
        helper.addLookupPath("de.unijena.bioinf.FragmentationTreeConstruction.computation");
        helper.addLookupPath("de.unijena.bioinf.IsotopePatternAnalysis");
        return helper;
    }

    public ParameterHelper() {
        this.cache = new HashMap<Class, ParameterizedByAnnotation>();
        this.nameCache = new HashMap<String, Class<?>>();
        this.lookupPaths = new ArrayList<String>();
    }

    public void addLookupPath(String name) {
        pattern=null;
        lookupPaths.add(name);
    }

    private Pattern getPattern() {
        if (pattern!=null) return pattern;
        final StringBuilder buffer = new StringBuilder();
        final ArrayList<String> orderedLookups = new ArrayList<String>(lookupPaths);
        Collections.sort(orderedLookups, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        final Iterator<String> lookups = orderedLookups.iterator();
        if (!lookups.hasNext()) pattern = Pattern.compile("\\A");
        else {
            buffer.append("\\A(?:");
            buffer.append(Pattern.quote(lookups.next()));
            while (lookups.hasNext()) {
                buffer.append("|").append(Pattern.quote(lookups.next()));
            }
            buffer.append(")\\.");
            pattern = Pattern.compile(buffer.toString());
        }
        return pattern;
    }

    public Class<?> getFromClassName(String string) {
        final Class<?> cached = nameCache.get(string);
        if (cached!=null) return cached;
        final ClassLoader loader = this.getClass().getClassLoader();
        if (string.indexOf('.') >= 0) {
            try {
                final Class<?> c = loader.loadClass(string);
                nameCache.put(string, c);
                return c;
            } catch (ClassNotFoundException e) {

            }
        }
        for (String lookup : lookupPaths) {
            final String name = lookup + '.' + string;
            try {
                final Class<?> c = loader.loadClass(name);
                nameCache.put(string, c);
                return c;
            } catch (ClassNotFoundException e) {

            }
        }
        throw new RuntimeException("Cannot find class with name " + string);
    }

    public String toClassName(Class<?> klass) {
        final String name = klass.getName();
        return getPattern().matcher(name).replaceFirst("");
    }

    public boolean isConvertable(Object o) {
        return o==null || o instanceof Number || supportedClasses.contains(o.getClass());
    }

    public boolean isConvertableOrWrapable(Object o) {
        if (isConvertable(o)) return true;
        if (o instanceof Parameterized || o instanceof ImmutableParameterized) return true;
        return o.getClass().isAnnotationPresent(HasParameters.class);
    }

    public <G,D,L> Object unwrap(DataDocument<G,D,L> document, G value) {
        if (document.isNull(value)) return null;
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
                        Constructor<ImmutableParameterized> constructor;
                        try {
                            constructor = (Constructor<ImmutableParameterized>)klass.getDeclaredConstructor();
                        } catch (NoSuchMethodException em) {
                            throw new RuntimeException();
                        }
                        constructor.setAccessible(true);
                        final ImmutableParameterized o;
                        try {
                            o = constructor.newInstance();
                        } catch (Exception ef ) {
                            throw new RuntimeException(ef);
                        }
                        return o.readFromParameters(this, document, dictionary);
                    }
                } else {
                    throw new RuntimeException("Parameter of type " + klass + " is not importable");
                }
            }
        } else {
            return DataDocument.transform(document, jdk, value);
        }
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
