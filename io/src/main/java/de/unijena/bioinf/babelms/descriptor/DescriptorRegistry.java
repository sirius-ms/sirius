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

package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.*;

/**
 *
 */
public class DescriptorRegistry {

    private final HashMap<Class, DescriptorMap> registry;

    private static DescriptorRegistry SINGLETON;
    static {
        SINGLETON = new DescriptorRegistry();
        DefaultDescriptors.addAll(SINGLETON);
    }

    public static DescriptorRegistry getInstance() {
        return SINGLETON;
    }

    DescriptorRegistry() {
        this.registry = new HashMap<Class, DescriptorMap>();
    }

    public <AnnotationType, Annotation extends DataAnnotation> void put(Class<AnnotationType> annotationType, Class<Annotation> annotationClass, Descriptor<Annotation> descriptor) {
        final String[] keywords = descriptor.getKeywords();
        synchronized (registry) {
            final DescriptorMap map;
            if (registry.containsKey(annotationType)) {
                map = registry.get(annotationType);
            } else {
                map = new DescriptorMap();
                registry.put(annotationType, map);
            }
            map.put(annotationClass, descriptor, keywords);
        }
    }

    public <AnnotationType> Descriptor[] getByKeywords(Class<AnnotationType> annotationType, String[] keywords) {
        synchronized (registry) {
            final DescriptorMap map =registry.get(annotationType);
            if (map == null) return new Descriptor[0];
            else return map.getByKeyword(keywords);
        }
    }

    public <AnnotationType, Annotation extends DataAnnotation> Descriptor<Annotation> get(Class<AnnotationType> annotationType, Class<Annotation> annotoAnnotationClass) {
        synchronized (registry) {
            final DescriptorMap map =registry.get(annotationType);
            return map.get(annotoAnnotationClass);
        }
    }

    private static class DescriptorMap {

        private HashMap<Class, Descriptor<DataAnnotation>> hashmap;
        private Map<String, List<Descriptor<DataAnnotation>>> keywordMap;

        public DescriptorMap() {
            this.hashmap = new HashMap<>();
            this.keywordMap = new HashMap<>(16);
        }

        private <Annotation extends DataAnnotation> void put(Class<Annotation> key, Descriptor<Annotation> descriptor, String[] keywords) {
            hashmap.put(key, (Descriptor<DataAnnotation>)descriptor);
            for (String s : keywords) keywordMap.computeIfAbsent(s, k -> new ArrayList<>(4)).add((Descriptor<DataAnnotation>)descriptor);
        }

        private <Annotation extends DataAnnotation> Descriptor<Annotation> get(Class<Annotation> annotationClass) {
            return (Descriptor<Annotation>)hashmap.get(annotationClass);
        }

        private Descriptor[] getByKeyword(String[] keywords) {
            final HashSet<Descriptor> descriptors = new HashSet<>();
            for (String k : keywords) {
                if (keywordMap.containsKey(k))
                    descriptors.addAll(keywordMap.get(k));
            }
            return descriptors.toArray(new Descriptor[descriptors.size()]);
        }

    }

}
