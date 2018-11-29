package de.unijena.bioinf;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("de.unijena.bioinf.ms.properties.DefaultProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PropertyAnnotationProcessor extends AbstractProcessor {



    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final AnnotationSet annotationSet = new AnnotationSet();
        for (final Element element : roundEnv.getElementsAnnotatedWith(DefaultProperty.class)) {
            annotationSet.add(element);
        }
        if (annotationSet.elements.isEmpty()) return true;
        annotationSet.sort();
        try(final Writer w = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "de.unijena.bioinf", "properties.txt").openWriter()) {
            for (Field e : annotationSet.elements) {
                w.write(e.toString()+ "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    protected class AnnotationSet {

        private List<Field> elements;
        private String possibleValues;
        private String comment;

        protected AnnotationSet() {
            this.elements = new ArrayList<>();
            possibleValues = "";
            comment = "";
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
            if (method.getAnnotation(DefaultInstanceProvider.class)==null) {
                System.err.println("Warning: @DefaultProperty is used for a parameter of a method which does not contain an @DefaultInstanceProvider annotation!");
                return;
            }
            elements.add(new Field(resolveParentName(klass, parameter), resolveKeyName(parameter)));
        }

        private void addEnum(TypeElement element) {
            final String parentKey = resolveParentName(element);
            final List<String> possibleValues = new ArrayList<>();
            for (Element e : element.getEnclosedElements()) {
                if (e.getKind()==ElementKind.ENUM_CONSTANT) {
                    possibleValues.add(e.getSimpleName().toString());
                }
            }
            elements.add(new Field(parentKey, "", Joiner.on('|').join(possibleValues)));
        }

        private String resolveParentName(TypeElement enclosingType) {
            DefaultProperty outerDef = enclosingType.getAnnotation(DefaultProperty.class);
            if (outerDef!=null && !outerDef.propertyParent().isEmpty())
                return outerDef.propertyParent();
            return enclosingType.getSimpleName().toString();
        }

        private String resolveParentName(TypeElement enclosingType, Element element)  {
            DefaultProperty innerDef = element.getAnnotation(DefaultProperty.class);
            if (innerDef != null && !innerDef.propertyParent().isEmpty())
                return innerDef.propertyParent();
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
                elements.add(new Field(innerFields.get(0).parent, ""));
            } else {
                elements.addAll(innerFields);
            }
        }

        protected void addField(VariableElement element) {
            if (element.getModifiers().contains(Modifier.STATIC)) return;
            final Element e = element.getEnclosingElement();
            if (e.getKind().isClass()) {
                elements.add(new Field(resolveParentName((TypeElement) e,element), resolveKeyName(element)));
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

    protected static class Field {
        private String parent, name, comment;

        public Field(String parent, String name, String comment) {
            this.parent = parent;
            this.name = name;
            this.comment = comment;
        }
        public Field(String parent, String name) {
            this(parent,name,"");
        }

        public String toString() {
            StringBuilder buf = new StringBuilder(parent.length()+name.length()+comment.length()+5);
            buf.append(parent);
            if (!name.isEmpty()) buf.append('.').append(name);
            buf.append('=');
            if (!comment.isEmpty()) {
                buf.append(" # ").append(comment);
            }
            return buf.toString();
        }
    }
}
