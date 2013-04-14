package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ObjectFormatter implements ParameterFormatter {

    /*
        Formats an parameter object
     */
    @Override
    @SuppressWarnings("unchecked")
    public <G, D, L> G format(DataDocument<G, D, L> document, Object value) {
        final Class<? extends Object> klass = value.getClass();
        final Parameter klassParameter = klass.getAnnotation(Parameter.class);
        if (klassParameter != null) {
            final D dictionary = document.newDictionary();
            for (Field f : klass.getFields()) {
                if (!f.isAnnotationPresent(Parameter.class)) continue;
                try {
                    document.addToDictionary(dictionary, getNameFromField(f, f.getName()), formatField(document, f, f.get(value)));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }
            for (Method f : klass.getMethods()) {
                if (!f.isAnnotationPresent(Parameter.class)) continue;
                try {
                    document.addToDictionary(dictionary, getNameFromField(f, f.getName()), formatField(document, f, f.invoke(value)));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

            }
            return document.wrapDictionary(dictionary);
        } else if (klass.isAssignableFrom(Iterable.class)) {
            final Iterable<? extends Object> iter = (Iterable<? extends Object>)value;
            final L list = document.newList();
            for (Object v : iter) {
                document.addToList(list, format(document, v));
            }
            return document.wrapList(list);

        } else if (klass.isAssignableFrom(Map.class)) {
            final D dictionary = document.newDictionary();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>)value).entrySet()) {
                document.addToDictionary(dictionary, entry.getKey().toString(), format(document, entry.getValue()));
            }
            return document.wrapDictionary(dictionary);
        } else {
            return document.wrap(value.toString());
        }
    }


    private <G, D, L> G formatWithParameter(DataDocument<G, D, L> document, Object value, Parameter parameter) {
        if (parameter.format().length() > 0 && parameter.formatter() == Parameter.Default.class) {
            return document.wrap(String.format(parameter.format(), value));
        } else try {
            return parameter.formatter().newInstance().format(document, value);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <G, D, L> G formatField(DataDocument<G, D, L> document, AnnotatedElement field, Object value) {
        final Parameter p = field.getAnnotation(Parameter.class);
        return formatWithParameter(document, value, mergeParameters(p, value.getClass().getAnnotation(Parameter.class)));
    }

    private String getNameFromField(AnnotatedElement field, String fieldName) {
        String name;
        final Called c = (field.getAnnotation(Called.class));
        if (c != null) name = c.value(); else {
            name = fieldName;
            if (name.startsWith("get") && name.length() > 3) name = Character.toLowerCase(name.charAt(3)) + (name.length() > 4 ? name.substring(4) : "");
            else if (name.startsWith("is") && name.length() > 2) name = Character.toLowerCase(name.charAt(2)) + (name.length() > 3 ? name.substring(3) : "");
        }
        return name;
    }
    /*
    private String getNameFromClass(Class<? extends Object> klass) {
        String name = klass.getSimpleName();
        final Called c = klass.getAnnotation(Called.class);
        final Parameter p = klass.getAnnotation(Parameter.class);
        if (c != null) name = c.value();
        if (p != null && !p.version().isEmpty()) name = name + ":" + p.version();
        return name;
    }
    */

    private Parameter mergeParameters(final Parameter fieldDefinition, final Parameter klassDefinition) {
        if (klassDefinition == null) return fieldDefinition;
        return new Parameter(){

            @Override
            public String version() {
                return fieldDefinition.version().isEmpty() ? klassDefinition.version() : fieldDefinition.version();
            }

            @Override
            public boolean inline() {
                return fieldDefinition.inline() || klassDefinition.inline();
            }

            @Override
            public Class<? extends ParameterFormatter> formatter() {
                return fieldDefinition.formatter().equals(Parameter.Default.class) ? klassDefinition.formatter() : fieldDefinition.formatter();
            }

            @Override
            public String format() {
                return fieldDefinition.format().isEmpty() ? klassDefinition.format() : fieldDefinition.format();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return fieldDefinition.annotationType();
            }
        };
    }
}
