/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import com.google.common.primitives.Shorts;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import org.dizitart.no2.Document;

import java.util.List;

public class Deserializer {

    public static FormulaCandidate deserializeFormula(Document document, PrecursorIonType ionType) {
        String formula = (String) document.getOrDefault("formula", null);
        long flags = (long) document.getOrDefault("flags", 0);

        return new FormulaCandidate(MolecularFormula.parseOrThrow(formula), ionType, flags);
    }

    @SuppressWarnings("unchecked")
    public static CompoundCandidate deserializeCompound(Document document) {
        String inchi = (String) document.getOrDefault("inchi", null);
        String inchiKey = (String) document.getOrDefault("inchikey", null);
        String smiles = (String) document.getOrDefault("smiles", null);
        String name = (String) document.getOrDefault("name", null);
        int pLayer = (int) document.getOrDefault("player", 0);
        int qLayer = (int) document.getOrDefault("qlayer", 0);
        long bitset = (long) document.getOrDefault("bitset", 0);
        double xLogP = (int) document.getOrDefault("xlogp", 0);
        List<Integer> pubmidIds = (List<Integer>) document.getOrDefault("pubmedIds", null);
        List<Document> linkDocs =  (List<Document>) document.getOrDefault("links", null);
        DBLink[] links;
        if (linkDocs != null) {
            links = linkDocs.stream().map((d) -> new DBLink((String) d.get("name"), (String) d.get("id"))).toArray(DBLink[]::new);
        } else {
            links = new DBLink[0];
        }

        return new CompoundCandidate(
                new InChI(inchiKey, inchi), name, smiles, pLayer, qLayer, xLogP, null, bitset, links,
                pubmidIds == null ? null : new PubmedLinks(pubmidIds)
        );
    }

    public static InChI deserializeInchi(Document document) {
        String inchi = (String) document.getOrDefault("inchi", null);
        String inchiKey = (String) document.getOrDefault("inchikey", null);
        return new InChI(inchi, inchiKey);
    }

    @SuppressWarnings("unchecked")
    public static FingerprintCandidate deserializeFingerprint(Document document, FingerprintVersion version) {
        CompoundCandidate c = deserializeCompound(document);
        List<Short> indizes = (List<Short>) document.getOrDefault("indizes", null);
        return new FingerprintCandidate(c, new ArrayFingerprint(version, Shorts.toArray(indizes)));
    }


}
