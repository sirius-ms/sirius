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

package de.unijena.bioinf.ms.middleware.compounds.model;

import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheck;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheckResult;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * This quality information is available for LC-MS data and includes quality measures for
 * - the chromatographic peak
 * - isotope peaks
 * - detected adducts
 * - MS/MS peaks
 *
 * This information is the same as provided in the LCMS-view in the GUI
 */
@Getter
@Setter
public class LCMSFeatureSummaryQualityData {

    protected Map<LCMSQualityCheck.QualityCategory, LCMSQualityCheckResult> qualityCategories;
    public LCMSFeatureSummaryQualityData(LCMSCompoundSummary lcmsCompoundSummary) {
        qualityCategories = new HashMap<>();
        qualityCategories.put(LCMSQualityCheck.QualityCategory.PEAK, lcmsCompoundSummary.peakQualityResult);
        qualityCategories.put(LCMSQualityCheck.QualityCategory.ISOTOPE, lcmsCompoundSummary.isotopeQualityResult);
        qualityCategories.put(LCMSQualityCheck.QualityCategory.ADDUCTS, lcmsCompoundSummary.adductQualityResult);
        qualityCategories.put(LCMSQualityCheck.QualityCategory.MSMS, lcmsCompoundSummary.ms2QualityResult);
    }
}
