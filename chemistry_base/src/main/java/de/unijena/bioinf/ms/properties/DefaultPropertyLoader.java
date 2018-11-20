package de.unijena.bioinf.ms.properties;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

//todo fix primitive arrays
//todo integrate Enums

public class DefaultPropertyLoader {
    private String propertyRoot;
    private Properties properties;


    public DefaultPropertyLoader(Properties properties, String propertyRoot) {
        this.propertyRoot = propertyRoot;
        this.properties = properties;
    }

    public <C> boolean isInstantiatableWithDefaults(Class<C> klass) {
        return klass.isAnnotationPresent(DefaultProperty.class)
                || Arrays.stream(klass.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(DefaultInstanceProvider.class))
                || Arrays.stream(klass.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(DefaultProperty.class));
    }

    public <C> C createInstanceWithDefaults(Class<C> klass) {
        return createInstanceWithDefaults(klass, propertyRoot);
    }

    public <C> C createInstanceWithDefaults(Class<C> klass, @NotNull final String sourceParent) {
        return createInstanceWithDefaults(klass, sourceParent, true);
    }

    private <C> C createInstanceWithDefaults(Class<C> klass, @NotNull final String sourceParent, boolean useClassParent) {
        String parent = sourceParent;
        if (parent.isEmpty())
            throw new IllegalArgumentException("Some parent path is needed!");
        //search class annotation
        DefaultProperty klassAnnotation = null;
        if (klass.isAnnotationPresent(DefaultProperty.class))
            klassAnnotation = klass.getAnnotation(DefaultProperty.class);

        if (useClassParent)
            parent = parent + "." + (klassAnnotation != null && !klassAnnotation.propertyParent().isEmpty()
                    ? klass.getAnnotation(DefaultProperty.class).propertyParent()
                    : klass.getSimpleName());

        System.out.println(parent);


        try {
            //search if an custom instance provider exists
            final C instance = klass.newInstance();
            final Method method = Arrays.stream(klass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(DefaultInstanceProvider.class)).findFirst().orElse(null);
            if (method != null) {
                setDefaults(instance, method, parent, sourceParent);
            } else {
                // find all fields with @DefaultProperty annotation
                final List<Field> fields = Arrays.stream(klass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(DefaultProperty.class)).collect(Collectors.toList());
                if (fields.isEmpty()) { //no field annotation -> check if it is a single field wrapper class
                    if (klassAnnotation != null) {
                        final String fieldName = (klassAnnotation.propertyKey().isEmpty() ? "value" : klassAnnotation.propertyKey());
                        try {
                            return setDefaultValue(klass.newInstance(), klass.getField(fieldName), parent);
                        } catch (NoSuchFieldException e) {
                            throw new IllegalArgumentException("Input class contains no valid Field. Please Specify a valid Field name in the class annotation (@DefaultProperty), use the default name (value) por directly annotate the field as @DefaultProperty.", e);
                        }
                    } else {
                        throw new IllegalArgumentException("This class contains no @DefaultProperty annotation!");
                    }
                } else {
                    for (Field field : fields) {
                        final DefaultProperty fieldAnnotation = field.getAnnotation(DefaultProperty.class);
                        final String fieldParent = (fieldAnnotation.propertyParent().isEmpty() ? parent : sourceParent + "." + fieldAnnotation.propertyParent());
                        final String fieldName = (fieldAnnotation.propertyKey().isEmpty() ? field.getName() : fieldAnnotation.propertyKey());
                        setDefaultValue(instance, field, fieldParent + "." + fieldName);
                    }
                }
            }
            return instance;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate input class by empty Constructor", e);
        }
    }


    private <C> C setDefaults(final C instance, final Method method, String parent, String sourceParent) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        final Parameter[] parameters = method.getParameters();
        final Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(DefaultProperty.class) && !parameter.getAnnotation(DefaultProperty.class).propertyKey().isEmpty()) {
                final String fieldParent = !parameter.getAnnotation(DefaultProperty.class).propertyParent().isEmpty()
                        ? sourceParent + "." + parameter.getAnnotation(DefaultProperty.class).propertyParent()
                        : parent;
                final String fieldName = parameter.getAnnotation(DefaultProperty.class).propertyKey();
                args[i] = parseProperty(parameter.getType(), parameter.getParameterizedType(), fieldName, fieldParent + "." + fieldName);
            } else {
                throw new IllegalArgumentException("Parameter need to be annotated With @DefaultProperty and the property key is mandatory!");
            }
        }

        method.invoke(instance, args);
        return instance;
    }


    private <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final boolean accessible = field.isAccessible();
        try {
            final Object objectValue;
            if (isInstantiatableWithDefaults(field.getType())) {
                objectValue = createInstanceWithDefaults(field.getType(), propertyName, false);
            } else {
                objectValue = parseProperty(field.getType(), field.getGenericType(), field.getName(), propertyName);
            }

            field.setAccessible(true);
            field.set(instance, objectValue);
            return instance;
        } finally {
            field.setAccessible(accessible);
        }
    }

    private Object parseProperty(@NotNull Class<?> type, Type generic, @NotNull String fieldName, @NotNull String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        String stringValue = properties.getProperty(propertyName);
        if (stringValue == null && !propertyName.endsWith(fieldName))
            stringValue = properties.getProperty(propertyName + "." + fieldName);
        if (stringValue == null)
            return null;
        return convertStringToType(type, generic, stringValue);
    }


    //// static util methods
    private static Method getFromStringMethod(Class<?> fType) {
        try {
            Method m = fType.getDeclaredMethod("fromString", String.class);
            if (m != null && Modifier.isStatic(m.getModifiers()))
                return m;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    public static <T> T convertStringToType(@NotNull Class<T> fType, Type generic, @NotNull String stringValue) throws InvocationTargetException, IllegalAccessException, InstantiationException {
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
                Class<?> elementType = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                Object[] objectValueAsArray = convertToCollection(elementType, stringValue);
                objectValue = createCollectionInstance(fType, elementType);
                ((Collection) objectValue).addAll(Arrays.asList(objectValueAsArray));
            } else {
                throw new IllegalArgumentException("Class of type " + fType.toString() + "cannot be instantiated from String values. For non standard classes you need to define an \"fromString\" Method.");
            }
        }
        return objectValue;
    }

    public static <T, E> T createCollectionInstance(final @NotNull Class<T> fType, final @NotNull Class<E> emementType) throws IllegalAccessException, InstantiationException {
        if (fType.isInterface() || Modifier.isAbstract(fType.getModifiers())) {
            if (Queue.class.isAssignableFrom(fType)) {
                return (T) new LinkedList<E>();
            } else if (Set.class.isAssignableFrom(fType)) {
                return (T) new HashSet<E>();
            } else {
                return (T) new ArrayList<E>();
            }
        } else {
            return fType.newInstance();
        }
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
        TypeVariable<Class<T>> generic = targetElementType.getTypeParameters() != null && targetElementType.getTypeParameters().length > 0
                ? targetElementType.getTypeParameters()[0]
                : null;
        final String[] stringValues = Arrays.stream(values.split(",")).map(String::trim).toArray(String[]::new);
        final T[] objectValues = (T[]) Array.newInstance(targetElementType, stringValues.length);
        for (int i = 0; i < stringValues.length; i++)
            objectValues[i] = convertStringToType(targetElementType, generic, stringValues[i]);
        return objectValues;
    }

    private static Class<?> findSubClassParameterType(Class<?> inputClass, Class<?> classOfInterest, int parameterIndex) {
        Map<Type, Type> typeMap = new HashMap<>();
        Class<?> instanceClass = inputClass;
        while (classOfInterest != instanceClass.getSuperclass()) {
            extractTypeArguments(typeMap, instanceClass);
            instanceClass = instanceClass.getSuperclass();
            if (instanceClass == null) throw new IllegalArgumentException();
        }
        ParameterizedType parameterizedType = (ParameterizedType) instanceClass.getGenericSuperclass();
        Type actualType = parameterizedType.getActualTypeArguments()[parameterIndex];
        if (typeMap.containsKey(actualType)) {
            actualType = typeMap.get(actualType);
        }
        if (actualType instanceof Class) {
            return (Class<?>) actualType;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static void extractTypeArguments(Map<Type, Type> typeMap, Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) {
            return;
        }
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] typeParameter = ((Class<?>) parameterizedType.getRawType()).getTypeParameters();
        Type[] actualTypeArgument = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < typeParameter.length; i++) {
            if (typeMap.containsKey(actualTypeArgument[i])) {
                actualTypeArgument[i] = typeMap.get(actualTypeArgument[i]);
            }
            typeMap.put(typeParameter[i], actualTypeArgument[i]);
        }
    }
}
