package de.unijena.bioinf.ms.properties;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultPropertyLoader {
    public static String propRoot = PropertyManager.PROPERTY_BASE + ".ms";

    public static <C> C createInstanceWithDefaults(Class<C> klass) {
        return createInstanceWithDefaults(klass,
                propRoot + "." + (klass.isAnnotationPresent(DefaultProperty.class) && !klass.getAnnotation(DefaultProperty.class).propertyParent().isEmpty()
                        ? klass.getAnnotation(DefaultProperty.class).propertyParent()
                        : klass.getSimpleName()));
    }

    public static <C> C createInstanceWithDefaults(Class<C> klass, @NotNull final String parent) {
        if (parent == null || parent.isEmpty())
            throw new IllegalArgumentException("Some parent path is needed!");

        final List<Field> fields = Arrays.stream(klass.getFields()).filter(field -> field.isAnnotationPresent(DefaultProperty.class)).collect(Collectors.toList());

        try {
            if (fields.isEmpty()) {
                if (klass.isAnnotationPresent(DefaultProperty.class)) {
                    final DefaultProperty ann = klass.getAnnotation(DefaultProperty.class);
                    final String fieldName = (ann.propertyKey().isEmpty() ? "value" : ann.propertyKey());
                    try {
                        return loadField(klass.newInstance(), klass.getField(fieldName), parent + "." + fieldName);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Input class contains no valid Field. Please Specify a valid Field na in the class annotation (@DefaultProperty), use the default name (value) por directly annotate the field as @DefaultProperty.", e);
                    }
                } else {
                    throw new IllegalArgumentException("This class contains no @DefaultProperty annotation!");
                }
            } else {
                final C instance = klass.newInstance();
                for (Field field : fields) {
                    final DefaultProperty ann = klass.getAnnotation(DefaultProperty.class);
                    final String fieldName = (ann.propertyKey().isEmpty() ? field.getName() : ann.propertyKey());
                    loadField(instance, field, parent + "." + fieldName);
                }
                return instance;
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate input class by empty Constructor", e);
        }


        Method method = Arrays.stream(klass.getMethods()).filter(m -> m.isAnnotationPresent(DefaultInstanceProvider.class)).filter((m) -> Modifier.isStatic(m.getModifiers())).findFirst().orElse(null);
        if (method != null)
            return createInstanceByCustomProvider(method);


        if (fieldProperties.length > 1) {

        } else {
            //special case for primitive wrapper classes
        }
        loadObjectFieldFromPropery(klass, annotationName);
    }

    private static <C> C createInstanceByCustomProvider(Method method) {
        return null;
    }

    private static <C> C loadObjectFieldFromPropery(Class<C> klass, final String annotationName) {
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


    private static <C> C loadField(C instance, Field field, String propertyName) throws IllegalAccessException {
        Class<?> fType = field.getType();
        if (fType.isAnnotation() || fType.isAnonymousClass() || fType.isArray() || fType.isInterface() || fType.isSynthetic() || fType.isInstance(Collection.class))
            throw new IllegalArgumentException("Only primitives, Enums or Simple Objects are allowed in Annotations");
        if (fType.isPrimitive() || fType.isInstance(String.class)) {
            loadPrimitiveFieldFromPropery(instance, field, propertyName);
        } else if (fType.getMethod("fromString", String.class)){

        } else if (field.getType().isMemberClass()){

        }
    }

    private static <C> void loadPrimitiveFieldFromPropery(final C instance, final Field field, final String propertyName) throws IllegalAccessException {
        String value = PropertyManager.PROPERTIES.getProperty(propertyName);
        if (value != null)
            field.set(instance, convert(field.getType(), value));
    }

    private static Object convert(Class<?> targetType, String text) {
        PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
        editor.setAsText(text);
        return editor.getValue();
    }

    private String makePropertyString(@NotNull String parent, Field field) {

    }

    //implement fromstring
    //implement tostring
}
