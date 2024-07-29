package de.unijena.bioinf.fingerid.blast;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.CompoundJsonMapper;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@JsonSerialize(using = MsNovelistCompoundCandidate.Serializer.class)
public class MsNovelistCompoundCandidate extends CompoundCandidate {
    @Getter
    @Setter
    private double rnnScore;


    public MsNovelistCompoundCandidate(InChI inchi) {
        super(inchi);
    }

    public MsNovelistCompoundCandidate(CompoundCandidate c, double rnnScore) {
        super(c);
        this.rnnScore = rnnScore;
    }

    public MsNovelistCompoundCandidate(String smiles, double rnnScore) {
        super(InChISMILESUtils.getInchiFromSmilesOrThrow(smiles, false));
        setSmiles(smiles);
        this.rnnScore = rnnScore;
    }

    public static class Serializer extends CompoundJsonMapper.BaseSerializer<MsNovelistCompoundCandidate> {
        @Override
        protected void serializeInternal(MsNovelistCompoundCandidate value, JsonGenerator gen) throws IOException {
            super.serializeInternal(value, gen);
            gen.writeNumberField("rnnScore", value.rnnScore);
        }
    }
}
