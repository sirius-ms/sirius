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

package de.unijena.bioinf;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("de.unijena.bioinf.ms.properties.DefaultProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class PropertyAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            final AnnotationSet annotationSet = new AnnotationSet();
            for (final Element element : roundEnv.getElementsAnnotatedWith(DefaultProperty.class)) {
                annotationSet.add(element);
            }
            if (annotationSet.elements.isEmpty()) return true;

            annotationSet.sort();

            try {
                FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "tmp");
                Path moduleRoot = Paths.get(resource.toUri()).getParent().getParent().getParent().getParent().getParent();
                Path resourcePath = moduleRoot.resolve("src/main/resources/de.unijena.bioinf.ms.defaults").resolve(moduleRoot.getFileName() + ".auto.config");
                Path configsMap = resourcePath.getParent().resolve(moduleRoot.getFileName() + ".class.map");

                System.out.println("#####################");
                System.out.println(resourcePath);
                System.out.println("#####################");

                Properties existingConfig = new Properties();
                if (Files.isReadable(resourcePath)) {
                    try (BufferedReader r = Files.newBufferedReader(resourcePath)) {
                        existingConfig.load(r);
                    }
                }

                Files.createDirectories(resourcePath.getParent());
                Files.deleteIfExists(resourcePath);
                Files.deleteIfExists(configsMap);

                resource.delete();

                try (BufferedWriter w = Files.newBufferedWriter(resourcePath)) {
                    final List<FieldGroup> grps = new ArrayList<>(annotationSet.fieldGroups.values());
                    System.out.println(grps.stream().map(it -> it.groupName).collect(Collectors.joining(",")));
                    grps.sort(Comparator.comparing(u -> u.groupName));
                    for (FieldGroup e : grps) {
                        if (!e.fields.isEmpty() && !e.comment.isEmpty()) {
                            w.write(e.beautifiedComment());
                        }

                        if (e.fields.size() == 1)
                            e.fields.get(0).name = "";

                        for (Field f : e.fields) {
                            if (!f.comment.isEmpty() || !f.possibleValues.isEmpty())
                                w.write(f.beautifiedComment());
                            w.write(f.paramString());
                            if (existingConfig.containsKey(f.paramKey()))
                                w.write(existingConfig.getProperty(f.paramKey()));
                            w.write('\n');
                        }
                        w.write("\n");
                    }
                }

                try (BufferedWriter w = Files.newBufferedWriter(configsMap)) {
                    for (Map.Entry<String,String> entry : annotationSet.keyToKlassName.entrySet()) {
                        w.write(entry.getKey() + "=" + entry.getValue());
                        w.newLine();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }
    protected static Pattern TAGMATCH = Pattern.compile("^\\s+@"), PARAMMATCH = Pattern.compile("^\\s+@param (\\S+) (.+)$");

    protected class AnnotationSet {
        private List<Field> elements;
        private HashMap<String, FieldGroup> fieldGroups;
        private HashMap<String, String> keyToKlassName;

        protected AnnotationSet() {
            this.elements = new ArrayList<>();
            fieldGroups = new HashMap<>();
            keyToKlassName = new HashMap<>();
        }

        protected void addFieldInGroup(TypeElement enclosed, Field field) {
            fieldGroups.computeIfAbsent(enclosed.getQualifiedName().toString(), k->initializeFieldGroup(enclosed)).fields.add(field);
            elements.add(field);
        }

        private void addFieldsInGroup(TypeElement enclosed, List<Field> fields) {
            if (fields.isEmpty()) return;
            fieldGroups.computeIfAbsent(enclosed.getQualifiedName().toString(), k->initializeFieldGroup(enclosed)).fields.addAll(fields);
            elements.addAll(fields);
        }

        private FieldGroup initializeFieldGroup(TypeElement klass) {
            FieldGroup g = new FieldGroup(klass.getQualifiedName().toString());
            g.comment = getCommentWithoutTags(klass);
            return g;
        }

        protected String getParamTagFor(ExecutableElement method, Element parameter) {
            String c= getComment(method);
            StringBuilder buf = new StringBuilder();
            for (String line : c.split("\n")) {
                final Matcher m = PARAMMATCH.matcher(line);
                if (m.find() && m.group(1).matches(parameter.getSimpleName().toString())) {
                    buf.append(m.group(2).trim());
                }
            }
            return buf.toString();
        }

        protected String getCommentWithoutTags(Element elem) {
            String c= getComment(elem);
            StringBuilder buf = new StringBuilder();
            for (String line : c.split("\n")) {
                if (TAGMATCH.matcher(line).find()) return buf.toString();
                buf.append(line.trim()).append("\n");
            }
            return buf.toString();
        }

        protected String getComment(Element elem) {
            String c= processingEnv.getElementUtils().getDocComment(elem);
            if (c==null) return "";
            return c;
        }


        protected void add(Element element) {
            if (element.getKind().isClass()) {
                if (element.getKind()==ElementKind.ENUM) {
                    addEnum((TypeElement)element);
                } else {
                    addClass((TypeElement)element);
                }
            } else if (element.getKind().isField()) {
                addField((VariableElement)element);
            } else if (element.getKind()==ElementKind.PARAMETER) {
                addInstanceProvider((VariableElement) element);
            } else {
                System.err.println("Warning: @DefaultProperty on " + element.toString() + " which is a " + element.getKind() + " is currently not supported. (in " + element.getEnclosingElement().toString() + ")" );
            }

        }

        private void addInstanceProvider(VariableElement parameter) {
            final ExecutableElement method = (ExecutableElement) parameter.getEnclosingElement();
            final TypeElement klass = (TypeElement) method.getEnclosingElement();
            if (method.getAnnotation(DefaultInstanceProvider.class) == null) {
                System.err.println("Warning: @DefaultProperty is used for a parameter of a method which does not contain an @DefaultInstanceProvider annotation!");
                return;
            }
            final String comment = getParamTagFor(method,parameter);
            addFieldInGroup(klass, new Field(resolveParentName(klass, parameter), resolveKeyName(parameter), comment));
        }

        private void addEnum(TypeElement element) {
            final String parentKey = resolveParentName(element);
            final List<String> possibleValues = new ArrayList<>();
            for (Element e : element.getEnclosedElements()) {
                if (e.getKind()==ElementKind.ENUM_CONSTANT) {
                    possibleValues.add(e.getSimpleName().toString());
                }
            }
            addFieldInGroup(element, new Field(parentKey, "", "", possibleValues));
        }

        private String resolveParentName(TypeElement enclosingType) {
            DefaultProperty outerDef = enclosingType.getAnnotation(DefaultProperty.class);
            String kay = enclosingType.getSimpleName().toString();
            if (outerDef!=null && !outerDef.propertyParent().isEmpty())
                kay = outerDef.propertyParent();

            keyToKlassName.put(kay, enclosingType.getQualifiedName().toString());
            return kay;
        }

        private String resolveParentName(TypeElement enclosingType, Element element)  {
            DefaultProperty innerDef = element.getAnnotation(DefaultProperty.class);
            if (innerDef != null && !innerDef.propertyParent().isEmpty()) {
                String kay = innerDef.propertyParent();
                keyToKlassName.put(kay, enclosingType.getQualifiedName().toString());
                return kay;
            }
            return resolveParentName(enclosingType);
        }

        private String resolveKeyName(Element element) {
            DefaultProperty innerDef = element.getAnnotation(DefaultProperty.class);
            if (innerDef != null && !innerDef.propertyKey().isEmpty())
                return innerDef.propertyKey();
            return element.getSimpleName().toString();
        }

        private void addClass(TypeElement element) {
            DefaultProperty def = element.getAnnotation(DefaultProperty.class);
            // if class contains an instance provider, we do not add all elements as properties

            for (Element e : element.getEnclosedElements()) {
                if (e.getAnnotation(DefaultInstanceProvider.class)!=null) {
                    System.out.println("class " + element.getSimpleName() + " contains already an instance provider");
                    return;
                }
            }

            final List<Field> innerFields = new ArrayList<>();
            boolean hasAno = false;
            for (Element e : element.getEnclosedElements()) {
                if (e.getKind().isField()) {
                    if (e.getModifiers().contains(Modifier.STATIC)) continue;
                    innerFields.add(new Field(resolveParentName(element,e),resolveKeyName(e)));
                    if (e.getAnnotation(DefaultProperty.class)!=null) hasAno=true;
                }
            }
            if (innerFields.size()==1 && !hasAno) {
                // its a single field wrapper
                addFieldInGroup(element, new Field(innerFields.get(0).parent, ""));
            } else {
                addFieldsInGroup(element, innerFields);
            }
        }

        protected void addField(VariableElement element) {
            if (element.getModifiers().contains(Modifier.STATIC)) return;
            final Element e = element.getEnclosingElement();
            if (e.getKind().isClass()) {
                addFieldInGroup((TypeElement) e, new Field(resolveParentName((TypeElement) e,element), resolveKeyName(element), getComment(element)));
            } else {
                System.err.println("Ignore " + element.toString() + ". is a parameter of a method.");
            }
        }

        public void sort() {
            elements.sort((u,v)->{
                if (u.parent.equals(v.parent)) return u.name.compareTo(v.name);
                else return u.parent.compareTo(v.parent);
            });
        }
    }

    static Pattern wordSplit = Pattern.compile("\\s");
    protected static String splitLongComment(String comment) {
        StringBuilder buffer = new StringBuilder();
        for (String line : comment.split("\n")) {
            line = line.trim()+"\n";
            Matcher m = wordSplit.matcher(line);
            int offset = 0; int l=1;
            buffer.append("# ");
            while (m.find(offset)) {
                l += m.start()-offset;
                if (l > 80 ) {
                    buffer.append("\n# ");
                    l=m.start()-offset+2;
                    buffer.append(line, offset, m.start());
                    buffer.append(line, m.start(), m.end());
                    offset = m.end();
                } else {
                    buffer.append(line, offset, m.start());
                    buffer.append(line, m.start(), m.end());
                    offset = m.end();
                }
            }
        }
        return buffer.toString();
    }

    protected static class FieldGroup {
        private String groupName;
        private List<Field> fields;
        private String comment;

        public FieldGroup(String groupName) {
            this.groupName = groupName;
            this.comment = "";
            this.fields = new ArrayList<>();
        }

        public String beautifiedComment() {
            return splitLongComment(comment);
        }
    }

    protected static class Field {
        private String parent, name, comment;
        List<? extends Object> possibleValues;

        public Field(String parent, String name, String comment, List<? extends Object> possibleValues) {
            this.parent = parent;
            this.name = name;
            this.comment = comment;
            this.possibleValues = possibleValues;
        }

        public Field(String parent, String name, String comment) {
            this(parent,name,comment, Collections.emptyList());
        }


        public Field(String parent, String name) {
            this(parent,name,"");
        }

        public Field withPossibleValues(List<? extends Object> xs) {
            return new Field(parent,name,comment,xs);
        }

        public String beautifiedComment() {
            if (!possibleValues.isEmpty()) {
                return splitLongComment((comment.trim() + "\n" + possibleValueComment()).trim());
            } else return splitLongComment(comment.trim());
        }

        private String possibleValueComment() {
            if (possibleValues.isEmpty()) return "";
            if (possibleValues.size()==1) return "Must be '" + possibleValues.get(0).toString() + "'";
            StringBuilder buf = new StringBuilder();
            buf.append("Must be "+ (possibleValues.size()==2 ? "either '" : "one of '"));
            buf.append(possibleValues.get(0).toString());
            buf.append("'");
            for (int k=1; k < possibleValues.size()-1; ++k) {
                buf.append(", ");
                buf.append("'").append(possibleValues.get(k).toString()).append("'");
            }
            buf.append(" or ").append(possibleValues.get(possibleValues.size() - 1).toString()).append("'");
            return buf.toString();
        }

        public String paramString() {
            return paramKey() + " = ";
        }

        public String paramKey() {
            if (name.isEmpty()) return parent;
            else return parent + "." + name;
        }
    }
}
