/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.projectspace.summaries;

public interface SummaryLocations {
    String
            FORMULA_CANDIDATES = "formula_candidates.tsv", //all formula candidates
            STRUCTURE_CANDIDATES = "structure_candidates.tsv", //all structure candidates
//            CANOPUS_CANDIDATES_ANNOTATIONS = "canopus_summary.tsv", //todo canopus annotation for all formula candidates
            //project level summaries
            FORMULA_SUMMARY = "formula_identifications.tsv",
            FORMULA_SUMMARY_ADDUCTS = "formula_identifications_adducts.tsv",
            COMPOUND_SUMMARY = "compound_identifications.tsv",
            COMPOUND_SUMMARY_ADDUCTS = "compound_identifications_adducts.tsv", // does this make much sense???
            CANOPUS_FORMULA_SUMMARY = "canopus_formula_summary.tsv",
            CANOPUS_FOMRULA_SUMMARY_ADDUCTS = "canopus_formula_summary_adducts.tsv",
            CANOPUS_COMPOUND_SUMMARY = "canopus_compound_summary.tsv",
//            CANOPUS_COMPOUND_SUMMARY_ADDUCTS = "canopus_compound_summary_adducts.tsv", // does this make not much sense???
            MZTAB_SUMMARY = "report.mztab";
}
