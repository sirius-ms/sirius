package de.unijena.bioinf;
//import de.unijena.bioinf.ms.DefaultProperty;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("de.unijena.bioinf.ms.properties.DefaultProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PropertyAnnotationProcessor extends AbstractProcessor {



    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final AnnotationSet annotationSet = new AnnotationSet();
        System.out.println("Lilalu!");
        for (final Element element : roundEnv.getElementsAnnotatedWith(annotations.iterator().next())) {
            System.out.println(element);
            TypeElement m = annotations.iterator().next();
            System.out.println(m.getEnclosedElements());
            annotationSet.add(element);
        }
        return false;
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

            } else if (element.getKind().isField()) {
                addField((VariableElement)element);
            } else {
                System.err.println(element.toString() +  " cannot be processed");
            }

        }

        protected void addField(VariableElement element) {
/*
            DefaultProperty def = element.getAnnotation(DefaultProperty.class);

            System.out.println("add field '" + element.getSimpleName() +  "' as '" + def.propertyParent() + "." + def.propertyKey());
            */
        }

    }

    protected static class Field {
        private String parent, name;

        public Field(String parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }
}
