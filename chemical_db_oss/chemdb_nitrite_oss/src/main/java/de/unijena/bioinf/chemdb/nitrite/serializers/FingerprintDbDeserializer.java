package de.unijena.bioinf.chemdb.nitrite.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;

import java.io.IOException;

public class FingerprintDbDeserializer extends StdDeserializer<Fingerprint> {

    private final FingerprintVersion version;

    public FingerprintDbDeserializer(FingerprintVersion version) {
        super(Fingerprint.class);
        this.version = version;
    }

    @Override
    public Fingerprint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        short[] indices = p.readValueAs(short[].class);
        return new ArrayFingerprint(version, indices);
    }
}
