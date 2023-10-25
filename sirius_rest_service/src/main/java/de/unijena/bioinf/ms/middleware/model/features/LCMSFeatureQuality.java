/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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

package de.unijena.bioinf.ms.middleware.model.features;

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
public class LCMSFeatureQuality {

    /**
     * Quality checks and overall quality for QualityCategory PEAK
     */
    protected final LCMSQualityCheckResult peakQuality;
    /**
     * Quality checks and overall quality for QualityCategory ISOTOPE
     */
    protected final LCMSQualityCheckResult isotopeQuality;
    /**
     * Quality checks and overall quality for QualityCategory ADDUCTS
     */
    protected final LCMSQualityCheckResult adductQuality;
    /**
     * Quality checks and overall quality for QualityCategory MSMS
     */
    protected final LCMSQualityCheckResult ms2Quality;

    public LCMSFeatureQuality(LCMSCompoundSummary lcmsCompoundSummary) {
        //LCMSQualityCheck.QualityCategory.PEAK
        this.peakQuality = lcmsCompoundSummary.peakQualityResult;
        //LCMSQualityCheck.QualityCategory.ISOTOPE
        this.isotopeQuality = lcmsCompoundSummary.isotopeQualityResult;
        //LCMSQualityCheck.QualityCategory.ADDUCTS
        this.adductQuality = lcmsCompoundSummary.adductQualityResult;
        //LCMSQualityCheck.QualityCategory.MSMS
        this.ms2Quality = lcmsCompoundSummary.ms2QualityResult;
    }
}
