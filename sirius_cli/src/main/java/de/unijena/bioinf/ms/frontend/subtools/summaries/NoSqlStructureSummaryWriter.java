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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureMatch;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiStructureSearchResult;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class NoSqlStructureSummaryWriter extends SummaryTable {

    final static List<String> HEADER = List.of(
            "structurePerIdRank",
            "formulaRank",
            "ConfidenceScoreExact",
            "ConfidenceScoreApproximate",
            "CSI:FingerIDScore",
            "ZodiacScore",
            "SiriusScore",
            "molecularFormula",
            "adduct",
            "precursorFormula",
            "InChIkey2D",
            "InChI",
            "name",
            "smiles",
            "xlogp",
            "pubchemids",
            "links",
            "dbflags",
            // metadata for mapping
            "ionMass",
            "retentionTimeInSeconds",
            "retentionTimeInMinutes",
            "formulaId",
            "alignedFeatureId",
            "mappingFeatureId",
            "overallFeatureQuality");

    NoSqlStructureSummaryWriter(SummaryTableWriter writer) {
        super(writer);
    }

    public void writeHeader() throws IOException {
        writer.writeHeader(HEADER);
    }

    public void writeStructureCandidate(AlignedFeatures f, FormulaCandidate fc, CsiStructureMatch match, CsiStructureSearchResult searchResult) throws IOException {
        List<Object> row = new ArrayList<>();

        row.add(match.getStructureRank());
        row.add(fc.getFormulaRank());

        row.add(searchResult.getConfidenceExact());
        row.add(searchResult.getConfidenceApprox());
        row.add(match.getCsiScore());
        row.add(fc.getZodiacScore());
        row.add(fc.getSiriusScore());
        row.add(fc.getMolecularFormula().toString());
        row.add(fc.getAdduct().toString());
        row.add(fc.getPrecursorFormulaWithCharge());

        row.add(match.getCandidateInChiKey());
        row.add(match.getCandidate().getInchi().in2D);
        row.add(match.getCandidate().getName());
        row.add(match.getCandidate().getSmiles());
        row.add(match.getCandidate().getXlogp());

        final Map<String, List<String>> dbMap = match.getCandidate().getLinkedDatabases();
        row.add(dbMap.getOrDefault(DataSource.PUBCHEM.name(), List.of("")).stream().filter(Objects::nonNull).collect(Collectors.joining(";")));
        row.add(links(dbMap));
        row.add(String.valueOf(match.getCandidate().getBitset()));
        row.add(f.getAverageMass());
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> Math.round(rt.getMiddleTime())).orElse(null));
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> rt.getMiddleTime() / 60d).orElse(null));

        row.add(String.valueOf(fc.getFormulaId()));
        row.add(String.valueOf(f.getAlignedFeatureId()));
        row.add(getMappingIdOrFallback(f));
        row.add(f.getDataQuality());

        writer.writeRow(row);
    }

    public static String links(Map<String, List<String>> databases) throws IOException {
        return databases.entrySet().stream().map(e -> e.getKey() + joinDBLinks(e.getValue())).collect(Collectors.joining(";"));
    }

    private static String joinDBLinks(List<String> links) {
        String joined = links.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        return joined.isEmpty() ? "" : ":(" + joined + ")";
    }
}
