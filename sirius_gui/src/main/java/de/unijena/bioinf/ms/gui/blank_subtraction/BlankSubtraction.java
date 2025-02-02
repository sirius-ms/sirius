/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.blank_subtraction;

import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.FoldChange;
import io.sirius.ms.sdk.model.QuantMeasure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
public class BlankSubtraction {

    public static final String SAMPLE = "sample";
    public static final String BLANK = "blank";
    public static final String CTRL = "control";

    public static final String TAG_NAME = "sample type";
    public static final String TAG_TYPE = "LC/MS runs";
    public static final String TAG_DESC = "Sample types for LC/MS runs";
    public static final List<Object> POSSIBLE_VALUES = List.of(SAMPLE, BLANK, CTRL);

    public static final String SAMPLE_GRP_NAME = "sample runs";
    public static final String SAMPLE_GRP_QUERY = "category:\"" + TAG_NAME + "\" AND text:" + SAMPLE;

    public static final String BLANK_GRP_NAME = "blank runs";
    public static final String BLANK_GRP_QUERY = "category:\"" + TAG_NAME + "\" AND text:" + BLANK;

    public static final String CTRL_GRP_NAME = "control runs";
    public static final String CTRL_GRP_QUERY = "category:\"" + TAG_NAME + "\" AND text:" + CTRL;

    private boolean blankSubtractionEnabled;
    private double blankSubtractionFoldChange;
    private boolean ctrlSubtractionEnabled;
    private double ctrlSubtractionFoldChange;

    public boolean isEnabled() {
        return blankSubtractionEnabled || ctrlSubtractionEnabled;
    }

    public boolean matches(InstanceBean bean) {
        String pid = bean.getProjectManager().getProjectId();
        List<FoldChange> foldChanges = bean.getClient().featureStatistics()
                .getFoldChangesByAlignedFeatureExperimental(pid, bean.getFeatureId());
        for (FoldChange foldChange : foldChanges) {
            if (Objects.equals(foldChange.getLeftGroup(), SAMPLE_GRP_NAME) &&
                    Objects.equals(foldChange.getAggregation(), AggregationType.AVG) &&
                    Objects.equals(foldChange.getQuantification(), QuantMeasure.APEX_INTENSITY)
            ) {
                Double fc = foldChange.getFoldChange();
                if (fc == null) fc = Double.POSITIVE_INFINITY;
                if (Objects.equals(foldChange.getRightGroup(), BLANK_GRP_NAME) && blankSubtractionEnabled) {
                    return fc >= blankSubtractionFoldChange;
                } else if (Objects.equals(foldChange.getRightGroup(), CTRL_GRP_NAME) && ctrlSubtractionEnabled) {
                    return fc >= ctrlSubtractionFoldChange;
                }
            }
        }
        return true;
    }
}
