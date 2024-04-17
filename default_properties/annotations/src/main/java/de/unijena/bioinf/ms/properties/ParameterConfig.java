/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.properties;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

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

    private String localConfigName;

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
            getConfigKeys().forEachRemaining(key -> toWrite.setProperty(key, config.getString(key)));
            layout.save(toWrite, writer);
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    public Map<String, String> toMap(){
        return toMap(config);
    }

    public Map<String, String> toMap(@NotNull String layerToExport){
        return toMap(config.getConfiguration(layerToExport));
    }

    private Map<String, String> toMap(Configuration configToExport){
        final Map<String, String> toWrite = new HashMap<>(configToExport.size());
        configToExport.getKeys().forEachRemaining(key -> toWrite.put(key, configToExport.getString(key)));
        return toWrite;
    }

    public void writeModified(Writer writer) throws IOException {
        try {
            layout.save(localConfig(), writer);
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    public ParameterConfig newIndependentInstance(@NotNull final String name, String... exclusions) {
        return newIndependentInstance(name, false, exclusions);
    }

    public ParameterConfig newIndependentInstance(@NotNull final String name, boolean overrideExisting, String... exclusions) {
        return newIndependentInstance(SiriusConfigUtils.newConfiguration(), name, overrideExisting, Arrays.stream(exclusions).collect(Collectors.toSet()));
    }

    public ParameterConfig newIndependentInstance(@Nullable final Map<String,String> properties, @NotNull final String name, boolean overrideExisting, String... exclusions) {
        return newIndependentInstance(SiriusConfigUtils.makeConfigFromMap(properties), name, overrideExisting, Arrays.stream(exclusions).collect(Collectors.toSet()));

    }
    public ParameterConfig newIndependentInstance(@NotNull final ParameterConfig modificationLayer, boolean overrideExisting, String... exclusions) {
        if (!modificationLayer.isModifiable())
            throw new IllegalArgumentException("Unmodifiable \"modificationLayer\"! Only modifiable ParameterConfigs are allowed as modification layer.");

        return newIndependentInstance(modificationLayer.localConfig(), modificationLayer.localConfigName, overrideExisting, Arrays.stream(exclusions).collect(Collectors.toSet()));
    }

    public ParameterConfig newIndependentInstance(@NotNull final InputStream streamToLoad, @NotNull final String name, boolean overrideExisting, String... exclusions) throws ConfigurationException {
        return newIndependentInstance(SiriusConfigUtils.makeConfigFromStream(streamToLoad), name, overrideExisting, Arrays.stream(exclusions).collect(Collectors.toSet()));
    }

    public ParameterConfig newIndependentInstance(@NotNull final PropertiesConfiguration modifiableLayer, @NotNull final String name, boolean overrideExisting, @NotNull Set<String> excludeConfigs) {
        if (name.isEmpty())
            throw new IllegalArgumentException("Empty name is not Allowed here");
        if (excludeConfigs.remove(name))
            LoggerFactory.getLogger(getClass()).warn("Exclusion List contain name of new modification layer. Cannot exclude the new layer -> Ignoring!");

        final CombinedConfiguration nuConfig = SiriusConfigUtils.newCombinedConfiguration();
        Configuration add = this.config.getConfiguration(name);
        if (add == null || overrideExisting)
            add = modifiableLayer;

        nuConfig.addConfiguration(add, name);
        this.config.getConfigurationNameList().stream()
                .filter(Objects::nonNull).filter(n -> !n.equals(name)).filter(n -> !excludeConfigs.contains(n))
                .forEach(n -> nuConfig.addConfiguration(this.config.getConfiguration(n), n));

        return new ParameterConfig(nuConfig, classesConfig, layout, name, configRoot, classRoot);
    }

    public String getLocalConfigName() {
        return localConfigName;
    }

    public boolean containsConfiguration(@NotNull String name) {
        return this.config.getConfiguration(name) != null;
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

    public List<String> getConfigNames() {
        return config.getConfigurationNameList();
    }

    public Configuration removeConfig(String name) {
        return config.removeConfiguration(name);
    }


    public void addNewConfig(String name, InputStream input) throws ConfigurationException {
        addNewConfig(name, SiriusConfigUtils.makeConfigFromStream(input));
    }


    public void addNewConfig(String name, Configuration nuLayer) {
        List<Configuration> inner = new ArrayList<>(config.getConfigurations());
        List<String> innerNames = new ArrayList<>(config.getConfigurationNameList());
        innerNames.forEach(config::removeConfiguration);
        config.addConfiguration(nuLayer, name);
        localConfigName = name;
        Iterator<Configuration> it = inner.iterator();
        innerNames.forEach(n -> config.addConfiguration(it.next(), n));
    }

    public void updateConfig(ParameterConfig update) {
        updateConfig(update.getLocalConfigName(), update.localConfig());
    }

    public void updateConfig(String name, Configuration update) {
        setOnConfig(name, update, true);
    }

    public void addToConfig(ParameterConfig update) {
        addToConfig(update.getLocalConfigName(), update.localConfig());
    }

    public void addToConfig(String name, Configuration update) {
        setOnConfig(name, update, false);
    }

    private void setOnConfig(String name, Configuration update, final boolean overrideExistingKeys) {
        if (!containsConfiguration(name))
            throw new IllegalArgumentException("Update failed: Configuration with name '" + name + "' does not exist.");
        final Configuration toUpdate = config.getConfiguration(name);
        if (toUpdate == update)
            return;
        update.getKeys().forEachRemaining(k -> {
            if (overrideExistingKeys || !toUpdate.containsKey(k))
                toUpdate.setProperty(k, update.getProperty(k));
        });
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

    public Optional<String[]> getConfigDescription(String key) {
        return Optional.ofNullable(layout.getComment(shortKey(key)))
                .map(it -> Arrays.stream(it.split("\n"))
                        .map(it2 -> it2.replaceFirst("^\\s*#\\s*", "")).toArray(String[]::new));
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

            throw new RuntimeException(new IllegalDefaultPropertyKeyException("Default value change finished with errors! Rollback previous default value for key " + key + " if possible.", e));
        }
    }

    public boolean containsConfigKey(@NotNull String key) {
        key = shortKey(key);
        return config.containsKey(key);
    }


    public Class<?> getClassFromKeyAndThrow(@NotNull String key) {
        try {
            return getClassFromKey(key);
        } catch (IllegalDefaultPropertyKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> getClassFromKey(@NotNull String key) throws IllegalDefaultPropertyKeyException {
        try {
            final String ks = shortKey(key);
            key = ks.split("[.]")[0];
            final String value = classesConfig.getString(key);
            if (value == null)
                throw new NullPointerException("No Class value found for given key '" + key + "'");
            Class<?> clazz = Class.forName(value);
            return clazz;
        } catch (Throwable e) {
            throw new IllegalDefaultPropertyKeyException(e);
        }
    }

    public Object createInstanceWithDefaults(String key) {
        Class<?> clazz = getClassFromKeyAndThrow(key);
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
            //search if an from String method exists
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

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not instantiate Class object by its name!", e);
        }
    }

    public <A> Map<Class<A>, A> createInstancesWithDefaults(Class<A> annotationType, boolean skipIllegalKeys) {
        return Collections.unmodifiableMap(createInstancesWithDefaults(getConfigKeys(), annotationType, skipIllegalKeys));
    }


    public <A> Map<Class<A>, A> createInstancesWithModifiedDefaults(Class<A> annotationType, boolean skipIllegalKeys) {
        return Collections.unmodifiableMap(createInstancesWithDefaults(getModifiedConfigKeys(), annotationType, skipIllegalKeys));
    }

    private <A> Map<Class<A>, A> createInstancesWithDefaults(Iterator<String> keys, Class<A> annotationType, boolean skipIllegalKeys) {
        Map<Class<A>, A> defaultInstances = new ConcurrentHashMap<>();
        keys.forEachRemaining(classKey -> {
            try {
                Class<?> cls = getClassFromKey(classKey);
                if (cls == null)
                    throw new IllegalArgumentException("Could not found a class for key: " + classKey);
                if (annotationType.isAssignableFrom(cls)) {
                    A instance = (A) createInstanceWithDefaults(cls);
                    if (instance == null)
                        throw new IllegalArgumentException("Could not create instance for: " + cls.getName());

                    defaultInstances.put((Class<A>) cls, instance);
                }
            } catch (IllegalDefaultPropertyKeyException e) {
                if (skipIllegalKeys)
                    LoggerFactory.getLogger(getClass()).debug("\"" + classKey + "\" is not a valid DefaultPropertyKey and will be IGNORED!");
                else throw new RuntimeException(e);
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


    private <C> C getDefaultInstanceFromProvider(final Method providerMethod, String parent, String sourceParent) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
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


    private <C> C setDefaultValue(C instance, Field field, String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
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

    private <T> T parseProperty(@NotNull Class<T> type, @Nullable Type generic, @Nullable String fieldName, @NotNull String propertyName) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
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

    public static <T> T convertStringToType(@NotNull Class<T> fType, Type generic, @NotNull String stringValue) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        T objectValue = null;
        final Method fromString = getFromStringMethod(fType);
        if (fromString != null) {
            objectValue = (T) invokePossiblyPrivateMethod(fromString, null, stringValue);
        } else {
            if (fType.isPrimitive() || fType.isAssignableFrom(Boolean.class) || fType.isAssignableFrom(Byte.class) || fType.isAssignableFrom(Short.class) || fType.isAssignableFrom(Integer.class) || fType.isAssignableFrom(Long.class) || fType.isAssignableFrom(Float.class) || fType.isAssignableFrom(Double.class) || fType.isAssignableFrom(String.class) || fType.isAssignableFrom(Color.class)) {
                if (fType.isAssignableFrom(Color.class) && stringValue.startsWith("#"))
                    objectValue = (T) Color.decode(stringValue);
                else
                    objectValue = convertToDefaultType(fType, stringValue);
            } else if (fType.isArray()) {
                Class<?> elementType = fType.getComponentType();
                if (elementType.isPrimitive()) {
                    objectValue = (T) ArrayUtils.toPrimitive(convertToCollection(wrapPrimitive(elementType), stringValue));
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
            } else if (Class.class.isAssignableFrom(fType)) {
                objectValue = (T) Class.forName(stringValue);
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

    public static <T> T[] convertToCollection(@NotNull Class<T> targetElementType, @NotNull String values) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        TypeVariable<Class<T>> generic = targetElementType.getTypeParameters() != null && targetElementType.getTypeParameters().length > 0
                ? targetElementType.getTypeParameters()[0]
                : null;
        final String[] stringValues = Arrays.stream(values.split(",")).map(String::trim).filter(it -> !it.isEmpty()).toArray(String[]::new);
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

    public static <T> Class<T> wrapPrimitive(Class<T> type) {
        if (type == int.class) return (Class<T>) Integer.class;
        if (type == float.class) return (Class<T>) Float.class;
        if (type == byte.class) return (Class<T>) Byte.class;
        if (type == double.class) return (Class<T>) Double.class;
        if (type == long.class) return (Class<T>) Long.class;
        if (type == char.class) return (Class<T>) Character.class;
        if (type == boolean.class) return (Class<T>) Boolean.class;
        if (type == short.class) return (Class<T>) Short.class;
        if (type == void.class) return (Class<T>) Void.class;
        return type;
    }
}
