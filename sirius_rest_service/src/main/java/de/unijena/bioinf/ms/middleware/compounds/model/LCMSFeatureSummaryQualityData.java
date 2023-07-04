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
import de.unijena.bioinf.lcms.quality.LCMSQualityCheckResult;
import lombok.Getter;
import lombok.Setter;

/**
 * This quality information is available for LC-MS data and includes quality measures for
 * - the chromatographic peak
 * - isotope peaks
 * - detected adducts
 * - MS/MS peaks
 * This information is the same as provided in the LCMS-view in the GUI
 */
@Getter
@Setter
public class LCMSFeatureSummaryQualityData {

    /*
    Quality checks and overall quality for QualityCategory PEAK
     */
    protected final LCMSQualityCheckResult peakQualityResult;
    /*
    Quality checks and overall quality for QualityCategory ISOTOPE
     */
    protected final LCMSQualityCheckResult isotopeQualityResult;
    /*
    Quality checks and overall quality for QualityCategory ADDUCTS
     */
    protected final LCMSQualityCheckResult adductQualityResult;
    /*
    Quality checks and overall quality for QualityCategory MSMS
     */
    protected final LCMSQualityCheckResult ms2QualityResult;

    public LCMSFeatureSummaryQualityData(LCMSCompoundSummary lcmsCompoundSummary) {
        //LCMSQualityCheck.QualityCategory.PEAK
        this.peakQualityResult = lcmsCompoundSummary.peakQualityResult;
        //LCMSQualityCheck.QualityCategory.ISOTOPE
        this.isotopeQualityResult = lcmsCompoundSummary.isotopeQualityResult;
        //LCMSQualityCheck.QualityCategory.ADDUCTS
        this.adductQualityResult = lcmsCompoundSummary.adductQualityResult;
        //LCMSQualityCheck.QualityCategory.MSMS
        this.ms2QualityResult = lcmsCompoundSummary.ms2QualityResult;
    }
}
