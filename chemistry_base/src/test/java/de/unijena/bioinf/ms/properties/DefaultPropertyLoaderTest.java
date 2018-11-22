package de.unijena.bioinf.ms.properties;

import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidates;
import de.unijena.bioinf.ChemistryBase.ms.NumberOfCandidatesPerIon;
import de.unijena.bioinf.jjobs.JJob;
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
public class DefaultPropertyLoaderTest {

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
        PropertyManager.addPropertiesFromStream(DefaultPropertyLoader.class.getResourceAsStream("/test.annotation.properties"));
        for (int i = 0; i < propertyKey.length; i++) {
            PropertyManager.setProperty(propertyKey[i], propertyValue[i]);
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SimpleClassAnnotation.class, (Consumer<SimpleClassAnnotation>) c -> assertEquals(c.value, 25), new String[]{"de.unijena.bioinf.ms.SimpleClassAnnotation"}, new String[]{"25"}},
                {SimpleClassAnnotationParent.class, (Consumer<SimpleClassAnnotationParent>) c -> assertEquals(c.value, false), new String[]{"de.unijena.bioinf.ms.SimpleClassAnnotationBool"}, new String[]{"false"}},
                //
                {SimpleClassAnnotationParentName.class, (Consumer<SimpleClassAnnotationParentName>) c -> assertEquals(c.customName, "String value"), new String[]{"de.unijena.bioinf.ms.SimpleClassAnnotationString"}, new String[]{"String value"}},
                {SimpleClassAnnotationNoParentName.class, (Consumer<SimpleClassAnnotationNoParentName>) c -> assertEquals(c.customName, "String value"), new String[]{"de.unijena.bioinf.ms.SimpleClassAnnotationNoParentName.customName"}, new String[]{"String value"}},
                //
                {ArrayClass.class, (Consumer<ArrayClass>) c -> assertTrue(Arrays.equals(c.value, new Integer[]{1, 2, 3, 4, 5})), new String[]{"de.unijena.bioinf.ms.ArrayClass"}, new String[]{"1, 2,  3,4,   5"}},
                {PrimitiveArrayClass.class, (Consumer<PrimitiveArrayClass>) c -> assertTrue(Arrays.equals(c.value, new int[]{1, 2, 3, 4, 5})), new String[]{"de.unijena.bioinf.ms.PrimitiveArrayClass"}, new String[]{"1, 2,  3,4,   5"}},
                {ListClass.class, (Consumer<ListClass>) c -> assertEquals(c.value, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d)), new String[]{"de.unijena.bioinf.ms.ListClass"}, new String[]{"1, 2.3,  3.22,4,   5"}},
                //
                {FromInstanceProviderClass.class, (Consumer<FromInstanceProviderClass>) c -> {
                    assertEquals(c.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.integer.intValue(), 25);
                }
                        , new String[]{"de.unijena.bioinf.ms.FromInstanceProviderClass.list", "de.unijena.bioinf.ms.FromInstanceProviderClass.integer"}, new String[]{"1, 2.3,  3.22,4,   5", "25"}},
                //
                {MultiFieldClass.class, (Consumer<MultiFieldClass>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                }
                        , new String[]{"de.unijena.bioinf.ms.MMF.list1", "de.unijena.bioinf.ms.MMF.listTwo", "de.unijena.bioinf.ms.MMFCustom.customIntField"}, new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25"}},
                //
                {MultiFieldWitAnnotatedFieldClass.class, (Consumer<MultiFieldWitAnnotatedFieldClass>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                    assertEquals(c.defaultPropField.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.defaultPropField.integer.intValue(), 25);
                }
                        , new String[]{"de.unijena.bioinf.ms.MultiFieldWitAnnotatedFieldClass.list1", "de.unijena.bioinf.ms.MultiFieldWitAnnotatedFieldClass.listTwo", "de.unijena.bioinf.ms.MultiFieldWitAnnotatedFieldClass.intField", "de.unijena.bioinf.ms.MultiFieldWitAnnotatedFieldClass.defaultPropField.list", "de.unijena.bioinf.ms.MultiFieldWitAnnotatedFieldClass.defaultPropField.integer"}
                        , new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25", "1, 2.3,  3.22,4,   5", "25"}},

                //
                {AllInOne.class, (Consumer<AllInOne>) c -> {
                    assertEquals(c.list1, Arrays.asList(1d, 2.3d, 3.22d, 4d, 5d));
                    assertEquals(c.list2, Arrays.asList(2d, 4d, 6d, 8d, 10d));
                    assertEquals(c.integer.intValue(), 25);
                    assertEquals(c.defaultPropField.list, Arrays.asList(1d, 2.3d, 3.22, 4d, 5d));
                    assertEquals(c.defaultPropField.integer.intValue(), 25);
                    assertEquals(c.fromStringField.key, 1);
                    assertEquals(c.fromStringField.value, "SuperValue");
                }
                        , new String[]{"de.unijena.bioinf.ms.AllInOne.list1", "de.unijena.bioinf.ms.AllInOne.listTwo", "de.unijena.bioinf.ms.AllInOne.intField", "de.unijena.bioinf.ms.AllInOne.defaultPropField.list", "de.unijena.bioinf.ms.AllInOne.defaultPropField.integer", "de.unijena.bioinf.ms.AllInOne.fromStringField"}
                        , new String[]{"1, 2.3,  3.22,4,   5", "2d , 4, 6,   8, 10d", "25", "1, 2.3,  3.22,4,   5", "25", "1:SuperValue"}},
                //
                {FromStringObject.class, (Consumer<FromStringObject>) c -> {
                    assertEquals(c.key, 1);
                    assertEquals(c.value, "SuperValue");
                }
                        , new String[]{"de.unijena.bioinf.ms.FromStringObject"}
                        , new String[]{"1:SuperValue"}},
                //
                {EnumClass.class, (Consumer<EnumClass>) c -> assertEquals(c.value, JJob.JobState.DONE), new String[]{"de.unijena.bioinf.ms.EnumClass"}, new String[]{"dOnE"}},
                {NumberOfCandidatesPerIon.class, (Consumer<NumberOfCandidatesPerIon>) c -> assertEquals(c.value, -1), new String[]{"de.unijena.bioinf.ms.NumberOfCandidatesPerIon"}, new String[]{"-1"}},
                {NumberOfCandidates.class, (Consumer<NumberOfCandidates>) c -> assertEquals(c.value, 666), new String[]{"de.unijena.bioinf.ms.NumberOfCandidates"}, new String[]{"666"}}
        });
    }

    // This test will run 4 times since we have 5 parameters defined
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
        public boolean value;
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



