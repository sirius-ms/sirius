package de.unijena.bioinf.storage.db.nitrite;

import com.esotericsoftware.reflectasm.FieldAccess;

import java.lang.reflect.Field;
import java.util.Arrays;

public interface NitriteWriteString {

    default String writeString() {
        Class<? extends NitriteWriteString> clazz = getClass();
        StringBuilder tos = new StringBuilder(clazz.getSimpleName() + "{");
        FieldAccess access = FieldAccess.get(getClass());
        try {
            Field[] fields = access.getFields();
            Arrays.stream(fields).limit(fields.length - 1).forEach((Field field) -> {
                field.setAccessible(true);
                try {
                    tos.append(field.getName()).append("=").append(field.get(this)).append(",");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            tos.append(fields[fields.length - 1].getName()).append("=").append(fields[fields.length - 1].get(this));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        tos.append("}");
        return tos.toString();
    }

}
