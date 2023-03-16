package de.unijena.bioinf.storage.db.nitrite;

import com.esotericsoftware.reflectasm.FieldAccess;
import de.unijena.bioinf.storage.db.NoSQLPOJO;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.mapper.Mappable;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.InheritIndices;

import java.lang.reflect.Field;
import java.util.Arrays;

@InheritIndices
public abstract class NitritePOJO extends NoSQLPOJO implements Mappable {

    @Id
    protected NitriteId id;

    public NitritePOJO() {}

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
        this.id = id;
    }

    @Override
    public Document write(NitriteMapper mapper) {
        Document document = new Document();
        FieldAccess access = FieldAccess.get(getClass());
        try {
            for (Field field : access.getFields()) {
                field.setAccessible(true);
                document.put(field.getName(), field.get(this));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    @Override
    public void read(NitriteMapper mapper, Document document) {
        if (document != null) {
            FieldAccess access = FieldAccess.get(getClass());
            try {
                this.id = NitriteId.createId((Long) document.get("id"));
                for (Field field : access.getFields()) {
                    if (field.getName().equals("id")) {
                        continue;
                    }
                    field.setAccessible(true);
                    field.set(this, document.get(field.getName()));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        Class<? extends NitritePOJO> clazz = getClass();
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
