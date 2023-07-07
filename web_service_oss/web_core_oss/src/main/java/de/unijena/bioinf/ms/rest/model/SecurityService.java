

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model;


import de.unijena.bioinf.babelms.utils.Base64;

import java.io.IOException;
import java.security.SecureRandom;

public class SecurityService {

    public static final String ERROR_CODE_SEPARATOR = ":;:";
    public static final String TERMS_MISSING =  "terms_missing";
    public static final String EMAIL_VERIFICATION_MISSING =  "email_verification_missing";
    public static final String SUB_EXPIRED =  "subscription_expired";
    public static final String LIMIT_REACHED =  "limit_reached";

    public static String generateSecurityToken() {
        final SecureRandom rand = new SecureRandom();
        final byte[] bytes = new byte[48];
        rand.nextBytes(bytes);
        try {
            return Base64.encodeBytes(bytes, Base64.URL_SAFE); // not url safe: DatatypeConverter.printBase64Binary(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }
    }
}
