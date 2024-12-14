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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.AlignedFeatureFoldChange;
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

    public static final String CATEGORY_NAME = "sample type";
    public static final String CATEGORY_TYPE = "LC/MS runs";
    public static final String CATEGORY_DESC = "Sample types for LC/MS runs";
    public static final List<String> CATEGORY_VALUES = List.of(SAMPLE, BLANK, CTRL);

    public static final String SAMPLE_GRP_NAME = "sample runs";
    public static final String SAMPLE_GRP_QUERY = "category:" + CATEGORY_NAME + " AND text:" + SAMPLE;

    public static final String BLANK_GRP_NAME = "blank runs";
    public static final String BLANK_GRP_QUERY = "category:" + CATEGORY_NAME + " AND text:" + BLANK;

    public static final String CTRL_GRP_NAME = "control runs";
    public static final String CTRL_GRP_QUERY = "category:" + CATEGORY_NAME + " AND text:" + CTRL;

    private boolean blankSubtractionEnabled;
    private double blankSubtractionFoldChange;
    private boolean ctrlSubtractionEnabled;
    private double ctrlSubtractionFoldChange;

    private final SiriusGui gui;

    public boolean isEnabled() {
        return blankSubtractionEnabled || ctrlSubtractionEnabled;
    }

    public boolean matches(InstanceBean bean) {
        return gui.applySiriusClient((client, pid) -> {
            List<AlignedFeatureFoldChange> foldChanges = client.featureStatistics().getFoldChange1(pid, bean.getFeatureId());
            for (AlignedFeatureFoldChange foldChange : foldChanges) {
                if (Objects.equals(foldChange.getLeftGroup(), "sample type:sample") &&
                        Objects.equals(foldChange.getAggregation(), AlignedFeatureFoldChange.AggregationEnum.AVG) &&
                        Objects.equals(foldChange.getQuantification(), AlignedFeatureFoldChange.QuantificationEnum.APEX_INTENSITY)
                ) {
                    Double fc = foldChange.getFoldChange();
                    if (fc == null) fc = Double.POSITIVE_INFINITY;
                    if (Objects.equals(foldChange.getRightGroup(), "sample type:blank") && blankSubtractionEnabled) {
                        return fc >= blankSubtractionFoldChange;
                    } else if (Objects.equals(foldChange.getRightGroup(), "sample type:control") && ctrlSubtractionEnabled) {
                        return fc >= ctrlSubtractionFoldChange;
                    }
                }
            }
            return false;
        });
    }

}
