/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The TSV export must be re-importable: the custom database structure import reads each row as
 * structure (SMILES/InChI), id, name - in that column order.
 */
public class TsvExporterTest {

    private static FingerprintCandidateWrapper entry(String inchiKey, String inchi, String smiles, String formula, double mass,
                                                     String name, List<DBLink> links) {
        CompoundCandidate candidate = new CompoundCandidate(new InChI(inchiKey, inchi));
        candidate.setSmiles(smiles);
        candidate.setName(name);
        candidate.setLinks(links);
        return new FingerprintCandidateWrapper(inchiKey, formula, mass, candidate, null);
    }

    private static List<String> export(FingerprintCandidateWrapper... entries) throws Exception {
        StringWriter out = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(out)) {
            TsvExporter exporter = new TsvExporter(writer, null, false);
            for (FingerprintCandidateWrapper e : entries)
                exporter.write(e);
            exporter.close();
        }
        return out.toString().lines().toList();
    }

    @Test
    public void rowsStartWithStructureIdAndName() throws Exception {
        List<String> lines = export(
                entry("LFQSCWFLJHTTHZ-UHFFFAOYSA-N", "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3", "CCO", "C2H6O", 46.04,
                        "Ethanol", List.of(new DBLink("PUBCHEM", "702"), new DBLink(null, "MY-ID-1"))),
                entry("OKKJLVBELUTLKV-UHFFFAOYSA-N", "InChI=1S/CH4O/c1-2/h2H,1H3", "CO", "CH4O", 32.03,
                        null, List.of()));

        assertEquals("SMILES\tid\tname", String.join("\t", List.of(lines.getFirst().split("\t")).subList(0, 3)));

        // the import's column contract: structure, id, name
        String[] ethanol = lines.get(1).split("\t", -1);
        assertEquals(List.of("CCO", "MY-ID-1", "Ethanol"), List.of(ethanol).subList(0, 3));

        String[] methanol = lines.get(2).split("\t", -1);
        assertEquals(List.of("CO", "", ""), List.of(methanol).subList(0, 3));
    }
}
