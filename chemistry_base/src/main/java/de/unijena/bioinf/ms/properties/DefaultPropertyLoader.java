package de.unijena.bioinf.ms.properties;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultPropertyLoader {
    public static String propRoot = PropertyManager.PROPERTY_BASE + ".ms";

    public static <C> C createInstanceWithDefaults(Class<C> klass) {
        return createInstanceWithDefaults(klass, propRoot);
    }

    public static <C> C createInstanceWithDefaults(Class<C> klass, @NotNull String parent) {
        if (parent == null || parent.isEmpty())
            throw new IllegalArgumentException("Some parent path is needed!");
        //search class annotation
        DefaultProperty klassAnnotation = null;
        if (klass.isAnnotationPresent(DefaultProperty.class))
            klassAnnotation = klass.getAnnotation(DefaultProperty.class);

        parent = parent + "." + (klassAnnotation != null && !klassAnnotation.propertyParent().isEmpty()
                ? klass.getAnnotation(DefaultProperty.class).propertyParent()
                : klass.getSimpleName());


        try {
            //search if an custom instance provider exists
            final C instance = klass.newInstance();
            final Method method = Arrays.stream(klass.getMethods()).filter(m -> m.isAnnotationPresent(DefaultInstanceProvider.class)).filter((m) -> Modifier.isStatic(m.getModifiers())).findFirst().orElse(null);
            if (method != null) {
                setDefaults(instance, method, parent);
            } else {
                // find all fields with @DefaultProperty annotation
                final List<Field> fields = Arrays.stream(klass.getFields()).filter(field -> field.isAnnotationPresent(DefaultProperty.class)).collect(Collectors.toList());
                if (fields.isEmpty()) { //no field annotation -> check if it is a single field wrapper class
                    if (klassAnnotation != null) {
                        final String fieldName = (klassAnnotation.propertyKey().isEmpty() ? "value" : klassAnnotation.propertyKey());
                        try {
                            return setDefaultValue(klass.newInstance(), klass.getField(fieldName), parent + "." + fieldName);
                        } catch (NoSuchFieldException e) {
                            throw new IllegalArgumentException("Input class contains no valid Field. Please Specify a valid Field na in the class annotation (@DefaultProperty), use the default name (value) por directly annotate the field as @DefaultProperty.", e);
                        }
                    } else {
                        throw new IllegalArgumentException("This class contains no @DefaultProperty annotation!");
                    }
                } else {
                    setDefaults(instance, fields, parent);
                }
            }
            return instance;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate input class by empty Constructor", e);
        }
    }

    private static <C> C setDefaults(final C instance, final Method method, String parent) {
        Arrays.stream(method.getParameters()).filter(parameter -> parameter.isAnnotationPresent(DefaultProperty.class));
        for (Parameter parameter : method.getParameters()) {
            final String fieldName = parameter.isAnnotationPresent() != null && !klassAnnotation.propertyParent().isEmpty()
                    ? klass.getAnnotation(DefaultProperty.class).propertyParent()
                    : klass.getSimpleName());

        }

        return instance;
    }

    private static <C> C setDefaults(final C instance, final Field field, String parent) throws IllegalAccessException {
        return setDefaults(instance, Collections.singletonList(field), parent);
    }

    private static <C> C setDefaults(final C instance, final List<Field> fields, String parent) throws IllegalAccessException {
        for (Field field : fields) {
            final DefaultProperty fieldAnnotation = field.getAnnotation(DefaultProperty.class);
            final String fieldName = (fieldAnnotation.propertyKey().isEmpty() ? field.getName() : fieldAnnotation.propertyKey());
            setDefaultValue(instance, field, parent + "." + fieldName);
        }
        return instance;
    }

    private static <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException {
        String stringValue = PropertyManager.PROPERTIES.getProperty(propertyName);
        Class<?> fType = field.getType();
        Type fGenericType = field.getGenericType();
        Object objectValue = null;
        if (fType.isAnnotation() || fType.isAnonymousClass() || fType.isArray() || fType.isInterface() || fType.isSynthetic() || fType.isInstance(Collection.class))
            throw new IllegalArgumentException("Only primitives, Enums or Simple Objects are allowed in Annotations");

        final Method fromString = getFromStringMethod(fType);
        if (fromString != null) {
            objectValue = fromString.invoke(null, stringValue);
        } else {
            if (fType.isPrimitive() || fType.isAssignableFrom(Boolean.class) || fType.isAssignableFrom(Byte.class) || fType.isAssignableFrom(Short.class) || fType.isAssignableFrom(Integer.class) || fType.isAssignableFrom(Long.class) || fType.isAssignableFrom(Float.class) || fType.isAssignableFrom(Double.class) || fType.isAssignableFrom(String.class) || fType.isAssignableFrom(Color.class)) {
                objectValue = convertDefaultType(fType, stringValue);
            } else if (fType.isArray()){
                Class<?> elementType = fType.getComponentType();
            } else if (fGenericType instanceof ParameterizedType && Collection.class.isAssignableFrom(fType)){
                Class<?> elementType = (Class<?>) ((ParameterizedType)fGenericType).getActualTypeArguments()[0];
            }
        }


        if (objectValue != null)
            field.set(instance, objectValue);

        return instance;
    }

    private static Method getFromStringMethod(Class<?> fType) {
        try {
            return fType.getMethod("fromString", String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /*private static <C> C loadObjectFieldFromPropery(Class<C> klass, final String annotationName) {
        try {
            C instance = klass.newInstance();
            Field[] fields = klass.getDeclaredFields();
            if (fields.length > 1) {
                for (Field field : klass.getDeclaredFields()) {
                    String propertyName = annotationName + "." + field.getName();
                    loadPrimitiveFieldFromPropery(instance, field, propertyName);
                }
            } else if (fields.length == 1) {

            }
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
*/
   /* private static <C> void loadPrimitiveFieldFromPropery(final C instance, final Field field, final String propertyName) throws IllegalAccessException {
        String value = PropertyManager.PROPERTIES.getProperty(propertyName);
        if (value != null)
            field.set(instance, convert(field.getType(), value));
    }*/
    /*
     * Default PropertyEditors will be provided for the Java primitive types
     * "boolean", "byte", "short", "int", "long", "float", and "double"; and
     * for the classes java.lang.String. java.awt.Color, and java.awt.Font.
     */
    private static Object convertDefaultType(Class<?> targetType, String value) {
        PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
        editor.setAsText(value);
        return editor.getValue();
    }

    private static Object[] convertCollection(Class<?> targetElementType, String value){
        final String[] values = Arrays.stream(value.split(",")).map(String::trim).toArray(String[]::new);

    }

    private String makePropertyString(@NotNull String parent, Field field) {

    }

    //implement fromstring
    //implement tostring
}
