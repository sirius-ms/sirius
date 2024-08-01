/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public class ConnectionError implements Comparable<ConnectionError> {
    private static final Comparator<ConnectionError> c = Comparator.comparing(ConnectionError::getErrorKlass)
            .thenComparingInt(ConnectionError::getSiriusErrorCode);

    @Override
    public int compareTo(@NotNull ConnectionError o) {
        return c.compare(this, o);
    }

    public enum Type {WARNING, ERROR}

    /**
     * ######### error classes ##########
     * 0. Unknown Error
     * 1. Internet
     * 2. login server
     * 3. license server
     * 4. Token error
     * 5. logged in
     * 6. license available and active
     * 7. Terms not accepted
     * 8. Service not reachable
     */
    public enum Klass {UNKNOWN, INTERNET, LOGIN_SERVER, LICENSE_SERVER, TOKEN, LOGIN, LICENSE, TERMS, APP_SERVER}

    @NotNull
    @Schema(nullable = false, requiredMode = REQUIRED)
    private final Type errorType;
    @NotNull
    @Schema(nullable = false, requiredMode = REQUIRED)
    private final Klass errorKlass;

    @Schema(nullable = false, requiredMode = REQUIRED)
    private final int siriusErrorCode;
    @NotNull
    @Schema(nullable = false, requiredMode = REQUIRED)
    private final String siriusMessage;

    @JsonIgnore
    private final @Nullable Throwable exception;

    @Schema(nullable = true)
    private @Nullable Integer serverResponseErrorCode;
    @Schema(nullable = true)
    private @Nullable String serverResponseErrorMessage;

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @NotNull Klass errorKlass) {
        this(siriusErrorCode, siriusMessage, errorKlass, null);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @NotNull Klass errorKlass, @Nullable Throwable exception) {
        this(siriusErrorCode, siriusMessage, errorKlass, exception, Type.ERROR);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @NotNull Klass errorKlass, @Nullable Throwable exception, @NotNull Type type) {
        this(siriusErrorCode, siriusMessage, errorKlass, exception, type, null, null);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @NotNull Klass errorKlass, @Nullable Throwable exception, @NotNull Type type, @Nullable Integer serverResponseErrorCode, @Nullable String serverResponseErrorMessage) {
        this.serverResponseErrorCode = serverResponseErrorCode;
        this.serverResponseErrorMessage = serverResponseErrorMessage;
        this.siriusErrorCode = siriusErrorCode;
        this.siriusMessage = siriusMessage;
        this.errorKlass = errorKlass;
        this.exception = exception;
        this.errorType = type;

        if (exception instanceof HttpErrorResponseException) {
            if (this.serverResponseErrorCode == null)
                this.serverResponseErrorCode = ((HttpErrorResponseException) exception).getErrorCode();
            if (this.serverResponseErrorMessage == null)
                this.serverResponseErrorMessage = ((HttpErrorResponseException) exception).getReasonPhrase();
        }
    }

    @Override
    public String toString() {
        return errorKlass.name() + " " + errorType.name() + ": " + siriusErrorCode + " | " + siriusMessage;
    }

    public int getSiriusErrorCode() {
        return siriusErrorCode;
    }

    @NotNull
    public String getSiriusMessage() {
        return siriusMessage;
    }

    @NotNull
    @JsonIgnore
    public Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }


    @Nullable
    public Integer getServerResponseErrorCode() {
        return serverResponseErrorCode;
    }

    @NotNull
    @JsonIgnore
    public Optional<Integer> serverResponseErrorCode() {
        return Optional.ofNullable(serverResponseErrorCode);
    }

    public void setServerResponseErrorCode(@Nullable Integer serverResponseErrorCode) {
        this.serverResponseErrorCode = serverResponseErrorCode;
    }

    @Nullable
    public String getServerResponseErrorMessage() {
        return serverResponseErrorMessage;
    }

    @NotNull
    @JsonIgnore
    public Optional<String> serverResponseErrorMessage() {
        return Optional.ofNullable(serverResponseErrorMessage);
    }

    public void setServerResponseErrorMessage(@Nullable String serverResponseErrorMessage) {
        this.serverResponseErrorMessage = serverResponseErrorMessage;
    }

    public boolean isError() {
        return errorType == Type.ERROR;
    }

    public boolean isWarning() {
        return errorType == Type.WARNING;
    }

    @NotNull
    public Type getErrorType() {
        return errorType;
    }

    public Klass getErrorKlass() {
        return errorKlass;
    }

    public ConnectionError withNewMessage(int siriusErrorCode, String siriusMessage, Klass errorKlass) {
        return new ConnectionError(siriusErrorCode, siriusMessage, errorKlass, exception, errorType, serverResponseErrorCode, serverResponseErrorMessage);
    }
}
