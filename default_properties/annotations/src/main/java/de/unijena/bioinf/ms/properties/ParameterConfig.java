package de.unijena.bioinf.ms.properties;

import com.google.gson.internal.Primitives;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public final class ParameterConfig {
    public final String configRoot;
    public final String classRoot;
    private final PropertiesConfigurationLayout layout;
    private final CombinedConfiguration config;
    private final CombinedConfiguration classesConfig;

    private final String localConfigName;

    ParameterConfig(CombinedConfiguration config, CombinedConfiguration classesConfig, String layoutConfigName, String localConfigName, String configRoot, String classRoot) {
        this(config, classesConfig,
                ((PropertiesConfiguration) config.getConfiguration(layoutConfigName)).getLayout(),
                localConfigName, configRoot, classRoot
        );
    }

    ParameterConfig(CombinedConfiguration config, CombinedConfiguration classesConfig, PropertiesConfigurationLayout layout, String localConfigName, String configRoot, String classRoot) {
        this.config = config;
        this.classesConfig = classesConfig;
        this.localConfigName = localConfigName;
        this.layout = layout;
        this.configRoot = configRoot;
        this.classRoot = classRoot;
    }

    public void write(Writer writer) throws IOException {
        try {
            final PropertiesConfiguration toWrite = SiriusConfigUtils.newConfiguration();
            config.getKeys().forEachRemaining(key -> toWrite.setProperty(key, config.getString(key)));
            layout.save(toWrite, writer);
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    public void writeModified(Writer writer) throws IOException {
        try {
            layout.save(localConfig(), writer);
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    public ParameterConfig newIndependentInstance(@NotNull final String name) {
        return newIdependendInstance(SiriusConfigUtils.newConfiguration(), name);
    }

    public ParameterConfig newIndependentInstance(@NotNull final ParameterConfig modificationLayer) {
        if (!modificationLayer.isModifiable())
            throw new IllegalArgumentException("Unmodifiable \"modificationLayer\"! Only modifiable ParameterConfigs are allowed as modification layer.");

        return newIdependendInstance(modificationLayer.localConfig(), modificationLayer.localConfigName);
    }

    public ParameterConfig newIndependentInstance(@NotNull final InputStream streamToLoad, @NotNull final String name) throws ConfigurationException {
        return newIdependendInstance(SiriusConfigUtils.makeConfigFromStream(streamToLoad), name);
    }

    private ParameterConfig newIdependendInstance(@NotNull final PropertiesConfiguration modifiableLayer, @NotNull final String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("Empty name is not Allowed here");

        final CombinedConfiguration nuConfig = SiriusConfigUtils.newCombinedConfiguration();
        nuConfig.addConfiguration(modifiableLayer, name);
        this.config.getConfigurationNames().forEach(n -> nuConfig.addConfiguration(config.getConfiguration(n), n));

        return new ParameterConfig(nuConfig, classesConfig, layout, name, configRoot, classRoot);
    }


    public String shortKey(@NotNull String key) {
        return key.replaceFirst(classRoot + ".", "")
                .replaceFirst(configRoot + ".", "");
    }

    public Iterator<String> getConfigKeys() {
        return config.getKeys();
    }

    public Iterator<String> getModifiedConfigKeys() {
        return localConfig().getKeys();
    }

    public Iterator<String> getClassConfigKeys() {
        return classesConfig.getKeys();
    }


    public ImmutableConfiguration getModifiedConfigs() {
        return localConfig();
    }

    public ImmutableConfiguration getConfigs() {
        return config;
    }

    public ImmutableConfiguration getClassConfigs() {
        return classesConfig;
    }

    public boolean isModifiable() {
        return localConfigName != null && !localConfigName.isEmpty();
    }
    private PropertiesConfiguration localConfig() {
        if (!isModifiable())
            throw new UnsupportedOperationException("This is an unmodifiable config. Please use newIndependentInstance(name) to create a modifiable child instance.");
        return (PropertiesConfiguration) config.getConfiguration(localConfigName);
    }

    public String getConfigDescription(String key) {
        return layout.getComment(shortKey(key));
    }

    public String getConfigValue(@NotNull String key) {
        return config.getString(shortKey(key));
    }

    public <C> boolean isInstantiatableWithDefaults(Class<C> klass) {
        return klass.isAnnotationPresent(DefaultProperty.class)
                || Arrays.stream(klass.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(DefaultInstanceProvider.class))
                || Arrays.stream(klass.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(DefaultProperty.class));
    }


    void setConfigProperty(@NotNull String key, @NotNull String value) {
        localConfig().setProperty(key, value);
    }

    public Class<?> changeConfig(@NotNull String key, @NotNull String value) {
        String backup = null;
        try {
            key = shortKey(key);

            // find actual value
            backup = localConfig().getString(key);
            if (config.getString(key) == null)
                throw new IllegalDefaultPropertyKeyException("No Default value found for given key.");


            // set new property
            localConfig().setProperty(key, value);

            // check new property
            Object nuDefault = createInstanceWithDefaults(key);
            if (nuDefault == null)
                throw new NullPointerException("Test default instance is NULL");

            return nuDefault.getClass();
        } catch (Throwable e) {
            // rollback property
            if (backup == null)
                localConfig().clearProperty(backup);
            else
                localConfig().setProperty(key, backup);

            throw new IllegalDefaultPropertyKeyException("Default value change finished with errors! Rollback previous default value for key " + key + " if possible.", e);
        }
    }

    public boolean containsConfigKey(@NotNull String key) {
        key = shortKey(key);
        return config.containsKey(key);
    }


    public Class<?> getClassFromKey(@NotNull String key) {
        try {
            final String ks = shortKey(key);
            key = ks.split("[.]")[0];
            final String value = classesConfig.getString(key);
            if (value == null)
                throw new NullPointerException("No Class value found for given key.");
            Class<?> clazz = Class.forName(value);
            return clazz;
        } catch (Throwable e) {
            throw new IllegalDefaultPropertyKeyException(e);
        }
    }

    public Object createInstanceWithDefaults(String key) {
        Class<?> clazz = getClassFromKey(key);
        return createInstanceWithDefaults(clazz);
    }

    public <C> C createInstanceWithDefaults(Class<C> klass) {
        return createInstanceWithDefaults(klass, "");
    }

    public <C> C createInstanceWithDefaults(Class<C> klass, @NotNull final String sourceParent) {
        return createInstanceWithDefaults(klass, sourceParent, true);
    }

    private <C> C createInstanceWithDefaults(Class<C> klass, @NotNull final String sourceParent, boolean useClassParent) {
        if (!sourceParent.isEmpty() && !sourceParent.endsWith("."))
            throw new IllegalArgumentException("Parent path has either to be empty or end with a \".\".");


        //search class annotation
        DefaultProperty klassAnnotation = null;
        if (klass.isAnnotationPresent(DefaultProperty.class))
            klassAnnotation = klass.getAnnotation(DefaultProperty.class);

        final String parent;
        if (useClassParent)
            parent = sourceParent + (klassAnnotation != null && !klassAnnotation.propertyParent().isEmpty()
                    ? klass.getAnnotation(DefaultProperty.class).propertyParent()
                    : klass.getSimpleName());
        else parent = sourceParent.substring(0, sourceParent.length() - 1); //remove dot

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
                    final String fieldParent = (fieldAnnotation.propertyParent().isEmpty() ? parent : sourceParent + fieldAnnotation.propertyParent());
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

    public <A> Map<Class<A>, A> createInstancesWithDefaults(Class<A> annotationType) {
        return Collections.unmodifiableMap(createInstancesWithDefaults(getConfigKeys(), annotationType));
    }


    public <A> Map<Class<A>, A> createInstancesWithModifiedDefaults(Class<A> annotationType) {
        return Collections.unmodifiableMap(createInstancesWithDefaults(getModifiedConfigKeys(), annotationType));
    }

    private <A> Map<Class<A>, A> createInstancesWithDefaults(Iterator<String> keys, Class<A> annotationType) {
        Map<Class<A>, A> defaultInstances = new ConcurrentHashMap<>();
        keys.forEachRemaining(classKey -> {
            Class<?> cls = getClassFromKey(classKey);
            if (cls == null)
                throw new IllegalArgumentException("Could not found a class for key: " + classKey);
            if (annotationType.isAssignableFrom(cls)) {
                A instance = (A) createInstanceWithDefaults(cls);
                if (instance == null)
                    throw new IllegalArgumentException("Could not create instance for: " + cls.getName());

                defaultInstances.put((Class<A>) cls, instance);
            }
        });
        return defaultInstances;
    }

    private static <C> C invokePossiblyPrivateConstructor(Class<C> klass) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        Constructor<C> constr = klass.getDeclaredConstructor();
        if (Modifier.isPublic(constr.getModifiers())) return constr.newInstance();
        else {
            constr.setAccessible(true);
            C instance = constr.newInstance();
            constr.setAccessible(false);
            return instance;
        }
    }

    private static Object invokePossiblyPrivateMethod(Method provideMethod, Object obj, Object... args) throws IllegalAccessException, InvocationTargetException {
        if (Modifier.isPublic(provideMethod.getModifiers()))
            return provideMethod.invoke(obj, args);
        else {
            provideMethod.setAccessible(true);
            Object instance = provideMethod.invoke(obj, args);
            provideMethod.setAccessible(false);
            return instance;
        }
    }


    private <C> C getDefaultInstanceFromProvider(final Method providerMethod, String parent, String sourceParent) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        final Parameter[] parameters = providerMethod.getParameters();
        final Object[] args = new Object[parameters.length];


        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(DefaultProperty.class)) {
                final String fieldParent = !parameter.getAnnotation(DefaultProperty.class).propertyParent().isEmpty()
                        ? sourceParent + parameter.getAnnotation(DefaultProperty.class).propertyParent()
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
        return (C) invokePossiblyPrivateMethod(providerMethod, null, args);
    }

    private <C> C getDefaultInstanceFromString(final Method providerMethod, String stringValue) throws InvocationTargetException, IllegalAccessException {
        return (C) invokePossiblyPrivateMethod(providerMethod, null, stringValue);
    }


    private <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        final boolean accessible = field.isAccessible();
        try {
            final Object objectValue;
            if (isInstantiatableWithDefaults(field.getType())) {
                objectValue = createInstanceWithDefaults(field.getType(), propertyName + ".", false);
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
        String stringValue = config.getString(propertyName);
        if (stringValue == null && fieldName != null && !propertyName.endsWith(fieldName))
            stringValue = config.getString(propertyName + "." + fieldName);
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
            objectValue = (T) invokePossiblyPrivateMethod(fromString, null, stringValue);
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
