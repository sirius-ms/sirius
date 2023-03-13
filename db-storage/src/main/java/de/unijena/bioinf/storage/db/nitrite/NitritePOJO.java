package de.unijena.bioinf.storage.db.nitrite;

import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.InheritIndices;

@InheritIndices
public abstract class NitritePOJO {

    // TODO handle indices, mapping, toString

    public enum IndexType {

    }

    @Id
    protected NitriteId id;

    public NitritePOJO() {}

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
        this.id = id;
    }

}
