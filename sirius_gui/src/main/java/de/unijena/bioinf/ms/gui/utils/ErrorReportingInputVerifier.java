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

package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Input verifier that colors the component pink and overrides its tooltip with an error message.
 * An instance of this verifier should be added to only one JComponent.
 */
public abstract class ErrorReportingInputVerifier extends InputVerifier {

    private String originalTooltip;
    private Color originalBackground;

    private void initComponent(JComponent component) {
        originalTooltip = component.getToolTipText();
        originalBackground = component.getBackground();
    }

    /**
     * Verify the input and produce a suitable error message
     * @return error message or null if there are no errors
     */
    public abstract String getErrorMessage(JComponent input);

    @Override
    public boolean verify(JComponent input) {
        return getErrorMessage(input) == null;
    }

    @Override
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        modifyIfError(source);
        return true;
    }

    public void modifyIfError(JComponent source){
        if (originalBackground == null) {
            initComponent(source);
        }
        String error = getErrorMessage(source);
        if (error != null) {
            source.setBackground(Color.PINK);
            source.setToolTipText(error);
        } else if (!Objects.equals(originalTooltip, source.getToolTipText())) {
            source.setBackground(originalBackground);
            source.setToolTipText(originalTooltip);
        }
    }


}
