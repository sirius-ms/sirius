package de.unijena.bioinf.ms.properties;

import com.google.gson.internal.Primitives;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.stream.Collectors;


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

        try {
            Method method = getFromStringMethod(klass);
            if (method != null) {
                return parseProperty(klass, null, null, parent);
            }

            //search if an custom instance provider exists
            method = getDefaultInstaceProviderMethod(klass);
            if (method != null) {
                return getDefaultInstanceFromProvider(method, parent, sourceParent);
            }

                // find all fields with @DefaultProperty annotation
                final List<Field> fields = Arrays.stream(klass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(DefaultProperty.class)).collect(Collectors.toList());
                if (fields.isEmpty()) { //no field annotation -> check if it is a single field wrapper class
                    if (klassAnnotation != null) {
                        if (klass.isEnum()) {
                            return parseProperty(klass, null, null, parent);
                        } else {
                            try {
                                final String fieldName = (klassAnnotation.propertyKey().isEmpty() ? "value" : klassAnnotation.propertyKey());
                                return setDefaultValue(invokePossiblyPrivateConstructor(klass), klass.getDeclaredField(fieldName), parent);
                            } catch (NoSuchFieldException e) {
                                throw new IllegalArgumentException("Input class contains no valid Field. Please Specify a valid Field name in the class annotation (@DefaultProperty), use the default name (value) por directly annotate the field as @DefaultProperty.", e);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("This class contains no @DefaultProperty annotation!");
                    }
                } else {
                    final C instance = invokePossiblyPrivateConstructor(klass);
                    for (Field field : fields) {
                        final DefaultProperty fieldAnnotation = field.getAnnotation(DefaultProperty.class);
                        final String fieldParent = (fieldAnnotation.propertyParent().isEmpty() ? parent : sourceParent + "." + fieldAnnotation.propertyParent());
                        final String fieldName = (fieldAnnotation.propertyKey().isEmpty() ? field.getName() : fieldAnnotation.propertyKey());
                        setDefaultValue(instance, field, fieldParent + "." + fieldName);
                    }
                    return instance;
                }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate input class by empty Constructor", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method does not contain a non parameter Constructor", e);

        }
    }

    private <C> C invokePossiblyPrivateConstructor(Class<C> klass) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        Constructor<C> constr = klass.getDeclaredConstructor();
        if (Modifier.isPublic(constr.getModifiers())) return constr.newInstance();
        else {
            constr.setAccessible(true);
            C instance = constr.newInstance();
            constr.setAccessible(false);
            return instance;
        }
    }


    private <C> C getDefaultInstanceFromProvider(final Method providerMethod, String parent, String sourceParent) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        final Parameter[] parameters = providerMethod.getParameters();
        final Object[] args = new Object[parameters.length];


        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(DefaultProperty.class) /* kaidu: why?  && !parameter.getAnnotation(DefaultProperty.class).propertyKey().isEmpty() */) {
                final String fieldParent = !parameter.getAnnotation(DefaultProperty.class).propertyParent().isEmpty()
                        ? sourceParent + "." + parameter.getAnnotation(DefaultProperty.class).propertyParent()
                        : parent;
                final String fieldName = parameter.getAnnotation(DefaultProperty.class).propertyKey().isEmpty() ? parameter.getName() : parameter.getAnnotation(DefaultProperty.class).propertyKey();
//                if (parameters.length == 1)
                args[i] = parseProperty(parameter.getType(), parameter.getParameterizedType(), fieldName, fieldParent);
//                else
//                    args[i] = parseProperty(parameter.getType(), parameter.getParameterizedType(), fieldName, fieldParent + "." + fieldName);
            } else if (parameters.length == 1) {
                args[0] = parseProperty(parameter.getType(), parameter.getParameterizedType(), "arg0", parent);
            } else {
                throw new IllegalArgumentException("Parameter need to be annotated With @DefaultProperty and the property key is mandatory!");
            }
        }
        return (C) providerMethod.invoke(null, args);
    }

    private <C> C getDefaultInstanceFromString(final Method providerMethod, String stringValue) throws InvocationTargetException, IllegalAccessException {
        return (C) providerMethod.invoke(null, stringValue);
    }


    private <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
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

    private <T> T parseProperty(@NotNull Class<T> type, @Nullable Type generic, @Nullable String fieldName, @NotNull String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        String stringValue = properties.getProperty(propertyName);
        if (stringValue == null && fieldName != null && !propertyName.endsWith(fieldName))
            stringValue = properties.getProperty(propertyName + "." + fieldName);
        if (stringValue == null)
            return null;
        return convertStringToType(type, generic, stringValue);
    }

    //// static util methods
    private static Method getFromStringMethod(@NotNull final Class<?> fType) {
        try {
            Method m = fType.getDeclaredMethod("fromString", String.class);
            if (m != null && Modifier.isStatic(m.getModifiers()) && fType.isAssignableFrom(m.getReturnType()))
                return m;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private static Method getDefaultInstaceProviderMethod(@NotNull final Class<?> klass) {
        return Arrays.stream(klass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DefaultInstanceProvider.class)
                        && Modifier.isStatic(m.getModifiers())
                        && klass.isAssignableFrom(m.getReturnType())
                )
                .findFirst().orElse(null);
    }

    public static <T> T convertStringToType(@NotNull Class<T> fType, Type generic, @NotNull String stringValue) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        T objectValue = null;
        final Method fromString = getFromStringMethod(fType);
        if (fromString != null) {
            objectValue = (T) fromString.invoke(null, stringValue);
        } else {
            if (fType.isPrimitive() || fType.isAssignableFrom(Boolean.class) || fType.isAssignableFrom(Byte.class) || fType.isAssignableFrom(Short.class) || fType.isAssignableFrom(Integer.class) || fType.isAssignableFrom(Long.class) || fType.isAssignableFrom(Float.class) || fType.isAssignableFrom(Double.class) || fType.isAssignableFrom(String.class) || fType.isAssignableFrom(Color.class)) {
                objectValue = convertToDefaultType(fType, stringValue);
            } else if (fType.isArray()) {
                Class<?> elementType = fType.getComponentType();
                if (elementType.isPrimitive()) {
                    objectValue = (T) ArrayUtils.toPrimitive(convertToCollection(Primitives.wrap(elementType), stringValue));
                } else {
                    objectValue = (T) convertToCollection(elementType, stringValue);
                }
            } else if (Collection.class.isAssignableFrom(fType)) {
                Class<?> elementType = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                Object[] objectValueAsArray = convertToCollection(elementType, stringValue);
                objectValue = createCollectionInstance(fType, elementType);
                ((Collection) objectValue).addAll(Arrays.asList(objectValueAsArray));
            } else if (fType.isEnum()) {
                objectValue = (T) Enum.valueOf((Class<Enum>) fType, stringValue.toUpperCase());
            } else {
                throw new IllegalArgumentException("Class of type " + fType.toString() + "cannot be instantiated from String values. For non standard classes you need to define an \"fromString\" Method.");
            }
        }
        return objectValue;
    }

    public static <T, E> T createCollectionInstance(final @NotNull Class<T> fType, final @NotNull Class<E> emementType) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (fType.isInterface() || Modifier.isAbstract(fType.getModifiers())) {
            if (Queue.class.isAssignableFrom(fType)) {
                return (T) new LinkedList<E>();
            } else if (Set.class.isAssignableFrom(fType)) {
                return (T) new HashSet<E>();
            } else {
                return (T) new ArrayList<E>();
            }
        } else {
            return fType.getDeclaredConstructor().newInstance();
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

    public static <T> T[] convertToCollection(@NotNull Class<T> targetElementType, @NotNull String values) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        TypeVariable<Class<T>> generic = targetElementType.getTypeParameters() != null && targetElementType.getTypeParameters().length > 0
                ? targetElementType.getTypeParameters()[0]
                : null;
        final String[] stringValues = Arrays.stream(values.split(",")).map(String::trim).toArray(String[]::new);
        final T[] objectValues = (T[]) Array.newInstance(targetElementType, stringValues.length);
        for (int i = 0; i < stringValues.length; i++)
            objectValues[i] = convertStringToType(targetElementType, generic, stringValues[i]);
        return objectValues;
    }

    public static <T> Constructor<T> getConstructor(Class<T> klass) throws NoSuchMethodException {
        Constructor<T> c = klass.getDeclaredConstructor();
        c.setAccessible(true);
        return c;
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
