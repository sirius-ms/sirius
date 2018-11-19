package de.unijena.bioinf.ms.properties;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DefaultPropertyLoader {
    //PropertyManager.PROPERTY_BASE + ".ms"
    private String propertyRoot;
    private Properties properties;


    public DefaultPropertyLoader(Properties properties, String propertyRoot) {
        this.propertyRoot = propertyRoot;
        this.properties = properties;
    }

    public <C> C createInstanceWithDefaults(Class<C> klass) {
        return createInstanceWithDefaults(klass, propertyRoot);
    }

    public <C> C createInstanceWithDefaults(Class<C> klass, @NotNull String parent) {
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
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate input class by empty Constructor", e);
        }
    }


    private <C> C setDefaults(final C instance, final Method method, String parent) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        final Parameter[] parameters = method.getParameters();
        final Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final String fieldName = !parameter.isAnnotationPresent(DefaultProperty.class) && !parameter.getAnnotation(DefaultProperty.class).propertyParent().isEmpty()
                    ? parameter.getAnnotation(DefaultProperty.class).propertyParent()
                    : parameter.getName();
            args[i] = parseProperty(parameter.getType(), parent + "." + fieldName);
        }

        method.invoke(instance, args);
        return instance;
    }

    private <C> C setDefaults(final C instance, final List<Field> fields, String parent) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Field field : fields) {
            final DefaultProperty fieldAnnotation = field.getAnnotation(DefaultProperty.class);
            final String fieldName = (fieldAnnotation.propertyKey().isEmpty() ? field.getName() : fieldAnnotation.propertyKey());
            setDefaultValue(instance, field, parent + "." + fieldName);
        }
        return instance;
    }

    private <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final Object objectValue = parseProperty(field.getType(), propertyName);
        field.set(instance, objectValue);
        return instance;
    }

    private Object parseProperty(Class<?> type, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final String stringValue = properties.getProperty(propertyName);
        return convertStringToType(type, stringValue);
    }


    //// static util methods
    private static Method getFromStringMethod(Class<?> fType) {
        try {
            return fType.getMethod("fromString", String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static <T> T convertStringToType(@NotNull Class<T> fType, @NotNull String stringValue) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        T objectValue = null;
        final Method fromString = getFromStringMethod(fType);
        if (fromString != null) {
            if (fType.isAssignableFrom(fromString.getReturnType()))
                objectValue = (T) fromString.invoke(null, stringValue);
            else
                throw new IllegalArgumentException("fromString method has wrong return type! Expected: " + fType + "Found: " + fromString.getReturnType());
        } else {
            if (fType.isPrimitive() || fType.isAssignableFrom(Boolean.class) || fType.isAssignableFrom(Byte.class) || fType.isAssignableFrom(Short.class) || fType.isAssignableFrom(Integer.class) || fType.isAssignableFrom(Long.class) || fType.isAssignableFrom(Float.class) || fType.isAssignableFrom(Double.class) || fType.isAssignableFrom(String.class) || fType.isAssignableFrom(Color.class)) {
                objectValue = convertToDefaultType(fType, stringValue);
            } else if (fType.isArray()) {
                Class<?> elementType = fType.getComponentType();
                objectValue = (T) convertToCollection(elementType, stringValue);
            } else if (Collection.class.isAssignableFrom(fType)) {
                Class<?> elementType = fType.getTypeParameters()[0].getGenericDeclaration();
                Object[] objectValueAsArray = convertToCollection(elementType, stringValue);
                objectValue = fType.newInstance();
                ((Collection) objectValue).addAll(Arrays.asList(objectValueAsArray));
            } else {
                throw new IllegalArgumentException("Class of type " + fType.toString() + "cannot be instantiated from String values. For non standard classes you need to define an \"fromString\" Method.");
            }
        }
        return objectValue;
    }

    /*
     * Default PropertyEditors will be provided for the Java primitive types
     * "boolean", "byte", "short", "int", "long", "float", and "double"; and
     * for the classes java.lang.String. java.awt.Color, and java.awt.Font.
     */
    public static <T> T convertToDefaultType(@NotNull Class<T> targetType, @NotNull String stringValue) {
        PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
        editor.setAsText(stringValue);
        return (T) editor.getValue();
    }

    public static <T> T[] convertToCollection(@NotNull Class<T> targetElementType, @NotNull String values) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        final String[] stringValues = Arrays.stream(values.split(",")).map(String::trim).toArray(String[]::new);
        final T[] objectValues = (T[]) Array.newInstance(targetElementType, stringValues.length);
        for (int i = 0; i < stringValues.length; i++)
            objectValues[i] = convertStringToType(targetElementType, stringValues[i]);
        return objectValues;
    }
}
