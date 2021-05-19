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

package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

import java.util.Properties;

public class AccountSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private Properties props;

    public AccountSettingsPanel(Properties properties) {
        super();
        this.props = properties;
        buildPanel();
        refreshValues();
    }

    private void buildPanel() {
        //todo register, login and clear button
        // save and cancel via parent panel
        // Server url, login state (Account info??) ->  user image =)
    }

    @Override
    public void saveProperties() {

    }

    @Override
    public String name() {
        return "Account";
    }
}
