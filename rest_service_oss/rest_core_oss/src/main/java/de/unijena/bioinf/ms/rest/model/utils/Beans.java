package de.unijena.bioinf.ms.rest.model.utils;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;

import java.lang.reflect.InvocationTargetException;

public class Beans extends BeanUtils {
    private static final BeanUtilsNotNull instance = new BeanUtilsNotNull();

    public static void copyPropertiesNotNull(final Object dest, final Object orig) {
        try {
            instance.copyProperties(dest, orig);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    private static class BeanUtilsNotNull extends BeanUtilsBean {
        @Override
        public void copyProperty(Object dest, String name, Object value)
                throws IllegalAccessException, InvocationTargetException {
            if (value == null) return;
            super.copyProperty(dest, name, value);
        }
    }
}
