/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.net;


import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionCheck;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionError;
import de.unijena.bioinf.ms.nightsky.sdk.model.LicenseInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConnectionChecks {
    private ConnectionChecks() {
    }

    public static boolean isLoggedIn(@NotNull ConnectionCheck check) {
        return isLoggedIn(check.getLicenseInfo());
    }

    public static boolean isLoggedIn(@NotNull LicenseInfo licenseInfo) {
        return licenseInfo.getUserEmail() != null;
    }

    public static boolean isConnected(@Nullable ConnectionCheck check) {
        if (check == null)
            return false;
        return isConnected(check.getErrors());
    }

    public static boolean isConnected(@NotNull List<ConnectionError> errors) {
        return errors.isEmpty();
    }

    public static boolean isInternet(@NotNull ConnectionCheck check) {
        return isInternet(check.getErrors());
    }

    public static boolean isInternet(@NotNull List<ConnectionError> errors) {
        return isConnected(errors) || errors.stream()
                .filter(e -> e.getErrorKlass().equals(ConnectionError.ErrorKlassEnum.INTERNET))
                .findAny().isEmpty();
    }

    public static boolean isWarningOnly(@NotNull ConnectionCheck check) {
        return isWarningOnly(check.getErrors());
    }

    public static boolean isWarningOnly(@NotNull List<ConnectionError> errors) {
        return !errors.isEmpty() && errors.stream()
                .map(ConnectionError::getErrorType)
                .filter(e -> !e.equals(ConnectionError.ErrorTypeEnum.WARNING))
                .findAny().isEmpty();
    }


    @Nullable
    public static String toLinks(@Nullable List<Term> terms){
        if (terms == null || terms.isEmpty())
            return null;
        StringBuilder builder = new StringBuilder()
                .append("<a href=").append(terms.get(0).getLink())
                .append(">").append(terms.get(0).getName()).append("</a>");
        for (int i = 1; i < terms.size(); i++) {
            builder.append(" and ")
                    .append("<a href=").append(terms.get(i).getLink())
                    .append(">").append(terms.get(i).getName()).append("</a>");
        }
        return builder.toString();
    }

}
