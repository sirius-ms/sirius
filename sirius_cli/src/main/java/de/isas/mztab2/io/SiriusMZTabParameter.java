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

package de.isas.mztab2.io;

import de.isas.mztab2.model.CV;
import de.isas.mztab2.model.Database;
import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder;

public enum SiriusMZTabParameter {
    //SIRIUS
    SIRIUS_SCORE("SIRIUS:sirius_score"),
    SIRIUS_ISOTOPE_SCORE("SIRIUS:isotope_score"),
    SIRIUS_TREE_SCORE("SIRIUS:tree_score"),
    ZODIAC_SCORE("SIRIUS:zodiac_score"),
    SIRIUS_EXPL_INTENSITY_OF_TOTAL_INTENSITY("SIRIUS:explained_intensity_ratio"),
    SIRIUS_EXPL_INTENSITY_OF_EXPLAINABLE_INTENSITY("SIRIUS:explained_intensity_of_explainable_peaks_ratio"),
    SIRIUS_NUM_EXPL_PEAKS_RATIO("SIRIUS:explained_peaks_ratio"),
    SIRIUS_MED_ABS_MASS_DEVIATION("SIRIUS:med_abs_mass_deviation"),

    SIRIUS_ANNOTATED_SPECTRA_LOCATION("SIRIUS:annotated_spectra_location"),
    SIRIUS_TREE_LOCATION("SIRIUS:tree_location"),
    SIRIUS_SUMMARY_LOCATION("SIRIUS:candidate_location"),

    //FingerID
    FINGERID_SCORE("CSI:FingerID:score"),
    FINGERID_CONFIDENCE("CSI:FingerID:confidence"),
    FINGERID_TANIMOTO_SIMILARITY("CSI:FingerID:tanimoto"),

    FINGERID_CANDIDATE_LOCATION("CSI:FingerID:candidate_location"),
    FINGERID_FINGERPRINT_LOCATION("CSI:FingerID:fingerprint_location"),

    //CANOPUS

    //OpenMS
    OPENMS_FEATURE_ID("OpenMS:feature_id"),
    OPENMS_CONSENSUS_ID("OpenMS:consensus_id");


    public final String cvLabel;
    public final String cvAccession;
    public final String parameterName;

    SiriusMZTabParameter(String parameterName) {
        this(null, null, parameterName);
    }

    SiriusMZTabParameter(String cvLabel, String cvAccession, String parameterName) {
        this.cvLabel = cvLabel;
        this.cvAccession = cvAccession;
        this.parameterName = parameterName;
    }

    public static Parameter newInstance(SiriusMZTabParameter p) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName);
    }

    public static Parameter newInstance(SiriusMZTabParameter p, String value) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName).value(value);
    }

    public static OptColumnMapping newOptColumn(SiriusMZTabParameter p, String value) {
        return new OptColumnMappingBuilder.GlobalOptColumnMappingBuilder().withName(p.parameterName).build(value);
    }


    public final static Parameter SOFTWARE_SIRIUS = new Parameter()
            .name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", null, "SIRIUS"))
            .value(PropertyManager.getProperty("de.unijena.bioinf.sirius.version"));

    public final static Parameter SOFTWARE_FINGER_ID = new Parameter()
            .name("CSI:FingerID")
            .value(PropertyManager.getProperty("de.unijena.bioinf.fingerid.version"));

    public final static Database NO_DATABASE = new Database().id(1).param(new Parameter().name("no database").value("null"));
    public final static Database DE_NOVO = new Database().id(2).param(new Parameter().name("de novo"));
    public final static Database PUBCHEM = new Database().id(3)
            .prefix("CID")
            .uri("https://pubchem.ncbi.nlm.nih.gov/compound/")
            .param(new Parameter().name("CSI:FingerID PubChem Copy").value("CID"));


    public final static CV DEFAULT_CV = new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary").version("4.1.16").uri("URL:https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo");
    public final static Parameter SMALL_MOLECULE_IDENTIFICATION_RELIABILITY = new Parameter().cvLabel("MS").cvAccession("MS:1002955").name("hr-ms compound identification confidence level");

    public final static Parameter CSI_FINGERID_CONFIDENCE_SCORE = new Parameter().id(1).name("CSI:FingerID Confidence Score (PP)");

    public final static Parameter SCAN_POLARITY_ITEM_POSITIVE = new Parameter().cvLabel("MS").cvAccession("MS:1000130").name("positive scan");
    public final static Parameter SCAN_POLARITY_ITEM_NEGATIVE = new Parameter().cvLabel("MS").cvAccession("MS:1000129").name("negative scan");

    public static Parameter getScanPolarity(@NotNull PrecursorIonType precursorIonType) {
        if (precursorIonType.getCharge() > 0) {
            return SCAN_POLARITY_ITEM_POSITIVE;
        } else if (precursorIonType.getCharge() < 0) {
            return SCAN_POLARITY_ITEM_NEGATIVE;
        }
        throw new IllegalArgumentException("Uncharged Ions do not exist!");
    }
}