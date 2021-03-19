/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import picocli.CommandLine;

public class Provide {


    public static class Defaults implements CommandLine.IDefaultValueProvider {
        public static final String PROPERTY_BASE = "de.unijena.bioinf.sirius.parameters";

        @Override
        public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
            final String l = argSpec.paramLabel(); //this should be the field name per default
            if (l == null || l.isEmpty()) return null;
            return PropertyManager.getProperty(PROPERTY_BASE + "." + l);
        }
    }

    public static class Versions implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            return new String[]{ApplicationCore.VERSION_STRING(), "SIRIUS lib: " + FingerIDProperties.siriusVersion(), "CSI:FingerID lib: " + FingerIDProperties.fingeridVersion()};
        }
    }
}
