package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import gnu.trove.list.array.TShortArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST_FPs;

public class FBCandidateFingerprintSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FBCandidateFingerprints> {
    @Nullable
    @Override
    public FBCandidateFingerprints read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        //read fingerprints from binary
        if (reader.exists(FINGERBLAST_FPs.relFilePath(id))) {
            final FingerIdData fingerIdData = reader.getProjectSpaceProperty(FingerIdDataProperty.class)
                    .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

            return reader.binaryFile(FINGERBLAST_FPs.relFilePath(id), br -> {
                List<Fingerprint> fps = new ArrayList<>();
                try (DataInputStream dis = new DataInputStream(br)) {
                    TShortArrayList shorts = new TShortArrayList(2000); //use it to reconstruct the array
                    while (dis.available() > 0) {
                        short value = dis.readShort();
                        if (value < 0) {
                            fps.add(new ArrayFingerprint(fingerIdData.getFingerprintVersion(), shorts.toArray()));
                            shorts.clear();
                        } else {
                            shorts.add(value);
                        }
                    }
                }
                return new FBCandidateFingerprints(fps);
            });
        }
        return null; // no fingerprints file
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FBCandidateFingerprints> component) throws IOException {
        final FBCandidateFingerprints candidatefps = component.orElseThrow(() -> new IllegalArgumentException("Could not find CandidateFingerprints to write for ID: " + id));

        writer.binaryFile(FINGERBLAST_FPs.relFilePath(id), (w) -> {
            try (DataOutputStream da = new DataOutputStream(w)) {
                List<short[]> fpIdxs = candidatefps.getFingerprints().stream()
                        .map(Fingerprint::toIndizesArray)
                        .collect(Collectors.toList());
                for (short[] fpIdx : fpIdxs) {
                    for (short idx : fpIdx) {
                        da.writeShort(idx);
                    }
                    da.writeShort(-1); //separator
                }
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(FINGERBLAST_FPs.relFilePath(id));
    }
}
