package de.unijena.bioinf.ms.properties;

import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIonization;
import de.unijena.bioinf.jjobs.JJob;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(Parameterized.class)
public class ParameterConfigTest {

    @Parameterized.Parameter(0)
    public Class<?> classToCreate;
    @Parameterized.Parameter(1)
    public Consumer<Object> resultValidator;
    @Parameterized.Parameter(2)
    public String[] propertyKey;
    @Parameterized.Parameter(3)
    public String[] propertyValue;

    @Before
    public void init() throws IOException {
        for (int i = 0; i < propertyKey.length; i++) {
            PropertyManager.DEFAULTS.setConfigProperty(propertyKey[i], propertyValue[i]);
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //0
                {SimpleClassAnnotation.class, (Consumer<SimpleClassAnnotation>) c -> assertEquals(c.value, 25), new String[]{"SimpleClassAnnotation"}, new String[]{"25"}},
                //1
                {SimpleClassAnnotationParent.class, (Consumer<SimpleClassAnnotationParent>) c -> assertEquals(c.value, false), new String[]{"SimpleClassAnnotationBool"}, new String[]{"false"}},
                //2
                {SimpleClassAnnotationParentName.class, (Consumer<SimpleClassAnnotationParentName>) c -> assertEquals(c.customName, "String value"), new String[]{"SimpleClassAnnotationString"}, new String[]{"String value"}},
                //3
                {SimpleClassAnnotationNoParentName.class, (Consumer<SimpleClassAnnotationNoParentName>) c -> assertEquals(c.customName, "String value"), new String[]{"SimpleClassAnnotationNoParentName.customName"}, new String[]{"String value"}},
                //4
                {ArrayClass.class, (Consumer<ArrayClass>) c -> assertTrue(Arrays.equals(c.value, new Integer[]{1, 2, 3, 4, 5})), new String[]{"ArrayClass"}, new String[]{"1, 2,  3,4,   5"}},
                //5
                {PrimitiveArrayClass.class, (Consumer<PrimitiveArrayClass>) c -> assertTrue(Arrays.equals(c.value, new int[]{1, 2, 3, 4, 5})), new String[]{"PrimitiveArrayClass"}, new String[]{"1, 2,  3,4,   5"}},
                //6
                {ListClass.class, (Consumer<ListClass>) c -> assertEquals(c.value, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d)), new String[]{"ListClass"}, new String[]{"1, 2.3,  3.22,4,   5"}},
                //7
                {FromInstanceProviderClass.class, (Consumer<FromInstanceProviderClass>) c -> {
                    assertEquals(c.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.integer.intValue(), 25);
                }
                        , new String[]{"FromInstanceProviderClass.list", "FromInstanceProviderClass.integer"}, new String[]{"1, 2.3,  3.22,4,   5", "25"}},
                //8
                {MultiFieldClass.class, (Consumer<MultiFieldClass>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                }
                        , new String[]{"MMF.list1", "MMF.listTwo", "MMFCustom.customIntField"}, new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25"}},
                //9
                {MultiFieldWitAnnotatedFieldClass.class, (Consumer<MultiFieldWitAnnotatedFieldClass>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                    assertEquals(c.defaultPropField.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.defaultPropField.integer.intValue(), 25);
                }
                        , new String[]{"MultiFieldWitAnnotatedFieldClass.list1", "MultiFieldWitAnnotatedFieldClass.listTwo", "MultiFieldWitAnnotatedFieldClass.intField", "MultiFieldWitAnnotatedFieldClass.defaultPropField.list", "MultiFieldWitAnnotatedFieldClass.defaultPropField.integer"}
                        , new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25", "1, 2.3,  3.22,4,   5", "25"}},

                //10
                {AllInOne.class, (Consumer<AllInOne>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                    assertEquals(c.defaultPropField.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.defaultPropField.integer.intValue(), 25);
                    assertEquals(c.fromStringField.key, 1);
                    assertEquals(c.fromStringField.value, "SuperValue");
                }
                        , new String[]{"AllInOne.list1", "AllInOne.listTwo", "AllInOne.intField", "AllInOne.defaultPropField.list", "AllInOne.defaultPropField.integer", "AllInOne.fromStringField"}
                        , new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25", "1, 2.3,  3.22,4,   5", "25", "1:SuperValue"}},
                //11
                {FromStringObject.class, (Consumer<FromStringObject>) c -> {
                    assertEquals(c.key, 1);
                    assertEquals(c.value, "SuperValue");
                }
                        , new String[]{"FromStringObject"}
                        , new String[]{"1:SuperValue"}},
                //12
                {EnumClass.class, (Consumer<EnumClass>) c -> assertEquals(c.value, JJob.JobState.DONE), new String[]{"EnumClass"}, new String[]{"dOnE"}},
                //13
                {NumberOfCandidatesPerIonization.class, (Consumer<NumberOfCandidatesPerIonization>) c -> TestCase.assertEquals(c.value, -1), new String[]{"NumberOfCandidatesPerIonization"}, new String[]{"-1"}},
                //14
                {NumberOfCandidates.class, (Consumer<NumberOfCandidates>) c -> TestCase.assertEquals(c.value, 666), new String[]{"NumberOfCandidates"}, new String[]{"666"}}
        });
    }

    @Test
    public void testCreateInstanceWithDefault() {
        Object r = PropertyManager.DEFAULTS.createInstanceWithDefaults(classToCreate);
        resultValidator.accept(classToCreate.cast(r));
    }

    @DefaultProperty
    public static class SimpleClassAnnotation {
        public int value;
    }

    @DefaultProperty(propertyParent = "SimpleClassAnnotationBool")
    public static class SimpleClassAnnotationParent {
        public final boolean value;

        public SimpleClassAnnotationParent(boolean value) {
            this.value = value;
        }

        private SimpleClassAnnotationParent() {
            this(false);
        }
    }

    @DefaultProperty(propertyParent = "SimpleClassAnnotationString", propertyKey = "customName")
    public static class SimpleClassAnnotationParentName {
        public String customName;
    }

    @DefaultProperty(propertyKey = "customName")
    public static class SimpleClassAnnotationNoParentName {
        public String customName;
    }

    @DefaultProperty
    public static class ArrayClass {
        public Integer[] value;
    }

    @DefaultProperty
    public static class PrimitiveArrayClass {
        public int[] value;
    }

    @DefaultProperty
    public static class ListClass {
        public List<Double> value;
    }

    public static class FromInstanceProviderClass {
        public List<Double> list;
        public Integer integer;


        @DefaultInstanceProvider
        public static FromInstanceProviderClass setDefaults(@DefaultProperty(propertyKey = "list") List<Double> list, @DefaultProperty(propertyKey = "integer") Integer value) {
            FromInstanceProviderClass c = new FromInstanceProviderClass();
            c.list = list;
            c.integer = value;
            return c;
        }
    }

    @DefaultProperty(propertyParent = "MMF")
    public static class MultiFieldClass {
        @DefaultProperty
        public List<Double> list1;
        @DefaultProperty(propertyKey = "listTwo")
        public List<Double> list2;
        @DefaultProperty(propertyParent = "MMFCustom", propertyKey = "customIntField")
        public Integer integer;
    }


    public static class MultiFieldWitAnnotatedFieldClass {
        @DefaultProperty
        private List<Double> list1;
        @DefaultProperty(propertyKey = "listTwo")
        private List<Double> list2;
        @DefaultProperty(propertyKey = "intField")
        private Integer integer;

        @DefaultProperty
        private FromInstanceProviderClass defaultPropField;
    }

    public static class AllInOne {
        @DefaultProperty
        private List<Double> list1;
        @DefaultProperty(propertyKey = "listTwo")
        private List<Double> list2;
        @DefaultProperty(propertyKey = "intField")
        private Integer integer;

        @DefaultProperty
        private FromInstanceProviderClass defaultPropField;

        @DefaultProperty
        private FromStringObject fromStringField;
    }

    @DefaultProperty
    public static class FromStringObject {
        int key;
        String value;

        public FromStringObject(int key, String value) {
            this.key = key;
            this.value = value;
        }

        public static FromStringObject fromString(String input) {
            String[] values = input.split(":");
            return new FromStringObject(Integer.valueOf(values[0]), values[1]);
        }

    }

    @DefaultProperty
    public static class EnumClass {
        private JJob.JobState value;
    }
}



