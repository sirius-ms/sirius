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

package de.unijena.bioinf.rest;

import java.io.IOException;

public class HttpErrorResponseException extends IOException {
    private final int errorCode;
    private final String bearerToken;
    private final String request;
    private final String content;

    public HttpErrorResponseException(int errorCode, String reasonPhrase, String bearerToken, String request, String content) {
        super(reasonPhrase);
        this.errorCode = errorCode;
        this.bearerToken = bearerToken;
        this.request = request;
        this.content = content;
    }

    @Override
    public String getMessage() {
        return "Error when querying REST service. Bad HTTP Response Code: "
                + getErrorCode() + " | HTTP-Message: " + super.getMessage() + " | HTTP-Request: " + getRequest();
    }

    private String getRequest() {
        return request;
    }

    public String getMessageWithBody() {
        return getMessage() + " | HTTP-Body: " + getContent();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getContent() {
        return content;
    }

    public String getReasonPhrase(){
        return super.getMessage();
    }
}
