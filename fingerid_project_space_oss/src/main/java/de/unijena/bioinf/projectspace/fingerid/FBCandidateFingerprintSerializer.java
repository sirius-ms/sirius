/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.fingerid.blast.AbstractFBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;
import gnu.trove.list.array.TShortArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST_FPs;

public class FBCandidateFingerprintSerializer<FB extends AbstractFBCandidateFingerprints> implements ComponentSerializer<FormulaResultId, FormulaResult, FB> {
    private final Function<List<Fingerprint>, FB> creator;
    private final Location<FormulaResultId> location;

    public FBCandidateFingerprintSerializer(Location<FormulaResultId> location, Function<List<Fingerprint>, FB> creator) {
        this.location = location;
        this.creator = creator;
    }

    protected List<Fingerprint> readFingerprints(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        //read fingerprints from binary
        if (reader.exists(location.relFilePath(id))) {
            final FingerIdData fingerIdData = reader.getProjectSpaceProperty(FingerIdDataProperty.class)
                    .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

            final FBCandidateNumber numC = id.getAnnotationOrNull(FBCandidateNumber.class);
            return reader.binaryFile(location.relFilePath(id), br -> {
                List<Fingerprint> fps = new ArrayList<>();
                try (DataInputStream dis = new DataInputStream(br)) {
                    TShortArrayList shorts = new TShortArrayList(2000); //use it to reconstruct the array
                    while (dis.available() > 0 && (numC == null || numC.value <= 0 || fps.size() < numC.value)) {
                        short value = dis.readShort();
                        if (value < 0) {
                            fps.add(new ArrayFingerprint(fingerIdData.getFingerprintVersion(), shorts.toArray()));
                            shorts.clear();
                        } else {
                            shorts.add(value);
                        }
                    }
                }
                return fps;
            });
        }
        return null; // no fingerprints file

    }


    @Nullable
    @Override
    public FB read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final List<Fingerprint> fps = readFingerprints(reader, id, container);
        return fps == null ? null : creator.apply(fps);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FB> component) throws IOException {
        final FB candidatefps = component.orElseThrow(() -> new IllegalArgumentException("Could not find CandidateFingerprints to write for ID: " + id));

        writer.binaryFile(location.relFilePath(id), (w) -> {
            try (DataOutputStream da = new DataOutputStream(w)) {
                List<short[]> fpIdxs = candidatefps.getFingerprints()
                        .stream().map(Fingerprint::toIndizesArray).toList();
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
        writer.deleteIfExists(location.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(location.relDir());
    }
}
