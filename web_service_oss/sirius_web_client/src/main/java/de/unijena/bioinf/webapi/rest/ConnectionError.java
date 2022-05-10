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

package de.unijena.bioinf.webapi.rest;

import de.unijena.bioinf.ms.rest.client.HttpErrorResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ConnectionError {

    public enum Type {WARNING, ERROR}

    @NotNull
    private final Type type;

    private final int siriusErrorCode;
    @NotNull
    private final String siriusMessage;

    private final @Nullable Throwable exception;

    private @Nullable Integer serverResponseErrorCode;
    private @Nullable String serverResponseErrorMessage;

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage) {
        this(siriusErrorCode, siriusMessage, null);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @Nullable Throwable exception) {
        this(siriusErrorCode, siriusMessage, exception, Type.ERROR);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @Nullable Throwable exception, @NotNull Type type) {
        this(siriusErrorCode, siriusMessage, exception, type, null, null);
    }

    public ConnectionError(int siriusErrorCode, @NotNull String siriusMessage, @Nullable Throwable exception, @NotNull Type type, @Nullable Integer serverResponseErrorCode, @Nullable String serverResponseErrorMessage) {
        this.serverResponseErrorCode = serverResponseErrorCode;
        this.serverResponseErrorMessage = serverResponseErrorMessage;
        this.siriusErrorCode = siriusErrorCode;
        this.siriusMessage = siriusMessage;
        this.exception = exception;
        this.type = type;

        if (exception instanceof HttpErrorResponseException){
            if (this.serverResponseErrorCode == null)
                this.serverResponseErrorCode = ((HttpErrorResponseException) exception).getErrorCode();
            if (this.serverResponseErrorMessage == null)
                this.serverResponseErrorMessage = ((HttpErrorResponseException) exception).getReasonPhrase();
        }
    }

    @Override
    public String toString() {
        return type.name() + ": " + siriusErrorCode + " | " + siriusMessage;
    }

    public int getSiriusErrorCode() {
        return siriusErrorCode;
    }

    @NotNull
    public String getSiriusMessage() {
        return siriusMessage;
    }

    @NotNull
    public Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    @NotNull
    public Optional<Integer> getServerResponseErrorCode() {
        return Optional.ofNullable(serverResponseErrorCode);
    }

    public void setServerResponseErrorCode(@Nullable Integer serverResponseErrorCode) {
        this.serverResponseErrorCode = serverResponseErrorCode;
    }

    @NotNull
    public Optional<String> getServerResponseErrorMessage() {
        return Optional.ofNullable(serverResponseErrorMessage);
    }

    public void setServerResponseErrorMessage(@Nullable String serverResponseErrorMessage) {
        this.serverResponseErrorMessage = serverResponseErrorMessage;
    }

    public boolean isError() {
        return type == Type.ERROR;
    }

    public boolean isWarning() {
        return type == Type.WARNING;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    public ConnectionError withNewMessage(int siriusErrorCode, String siriusMessage){
        return new ConnectionError(siriusErrorCode, siriusMessage, exception ,type, serverResponseErrorCode, serverResponseErrorMessage);
    }
}
