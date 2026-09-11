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


import io.sirius.ms.sdk.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class ConnectionChecks {
    /**
     * Order of the API model errors by relevance. {@link io.sirius.ms.sdk.model.ConnectionError} is a
     * generated model without a natural order, so the order of its core counterpart is spelled out here.
     */
    private static final Comparator<ConnectionError> BY_RELEVANCE =
            Comparator.comparing(ConnectionError::getErrorKlass)
                    .thenComparing(ConnectionError::getSiriusErrorCode,
                            Comparator.nullsLast(Comparator.naturalOrder()));

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

    public static boolean isInternet(ConnectionCheck check) {
        if (check == null)
            return false;
        return isInternet(check.getErrors());
    }

    public static boolean isInternet(@NotNull List<ConnectionError> errors) {
        return isConnected(errors) || errors.stream()
                .filter(e -> e.getErrorKlass().equals(ConnectionErrorClass.INTERNET))
                .findAny().isEmpty();
    }

    public static boolean isWarningOnly(@NotNull ConnectionCheck check) {
        return isWarningOnly(check.getErrors());
    }

    public static boolean isWarningOnly(@NotNull List<ConnectionError> errors) {
        return !errors.isEmpty() && errors.stream()
                .map(ConnectionError::getErrorType)
                .filter(e -> !e.equals(ConnectionErrorType.WARNING))
                .findAny().isEmpty();
    }


    /**
     * The error to tell the user about, out of all errors of a connection check.
     * <p>
     * The errors are already ordered by relevance, so this is the first one - except for a failing
     * internet probe, which is skipped in favour of the errors that actually explain the problem. It is
     * only trusted when the login or the license server is unreachable as well; those two answering
     * proves there is internet, whatever the probe says.
     *
     * @param errors errors of a connection check, must not be empty
     */
    @NotNull
    public static ConnectionError selectMainError(@NotNull List<ConnectionError> errors) {
        ConnectionError first = errors.getFirst();
        if (first.getErrorKlass() != ConnectionErrorClass.INTERNET || errors.size() <= 1)
            return first;

        boolean internetConfirmed = errors.stream().map(ConnectionError::getErrorKlass)
                .anyMatch(e -> e == ConnectionErrorClass.LOGIN_SERVER)
                && errors.stream().map(ConnectionError::getErrorKlass)
                .anyMatch(e -> e == ConnectionErrorClass.LICENSE_SERVER);
        if (internetConfirmed)
            return first;

        return errors.stream()
                .filter(e -> e.getErrorKlass() != ConnectionErrorClass.INTERNET)
                .min(BY_RELEVANCE)
                .orElse(first);
    }

    /**
     * Names the terms of the selected web service as links, without saying anything about whether they
     * have been accepted - only the error message knows which one is missing.
     *
     * @return the sentence, or an empty string if the subscription carries no terms
     */
    @NotNull
    public static String termsSentence(@Nullable List<Term> terms) {
        String links = toLinks(terms);
        if (links == null)
            return "";
        return "Terms for the selected Webservice: " + links + ".";
    }

    @Nullable
    public static String toLinks(@Nullable List<Term> terms){
        if (terms == null || terms.isEmpty())
            return null;
        StringBuilder builder = new StringBuilder()
                .append("<a href=").append(terms.getFirst().getLink())
                .append(">").append(terms.getFirst().getName()).append("</a>");
        for (int i = 1; i < terms.size(); i++) {
            builder.append(" and ")
                    .append("<a href=").append(terms.get(i).getLink())
                    .append(">").append(terms.get(i).getName()).append("</a>");
        }
        return builder.toString();
    }

}
