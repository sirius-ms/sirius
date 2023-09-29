package de.unijena.bioinf.chemdb.nitrite.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;

import java.io.IOException;

public class FingerprintDbSerializer extends StdSerializer<Fingerprint> {

    public FingerprintDbSerializer() {
        super(Fingerprint.class);
    }

    @Override
    public void serialize(Fingerprint fp, JsonGenerator gen, SerializerProvider provider) throws IOException {
        short[] indices = fp.toIndizesArray();
        int[] indicesInt = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indicesInt[i] = indices[i];
        }
        gen.writeArray(indicesInt, 0, indicesInt.length);
    }
}
