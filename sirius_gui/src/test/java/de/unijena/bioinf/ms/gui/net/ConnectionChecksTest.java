/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

import io.sirius.ms.sdk.model.ConnectionError;
import io.sirius.ms.sdk.model.ConnectionErrorClass;
import io.sirius.ms.sdk.model.ConnectionErrorType;
import io.sirius.ms.sdk.model.Term;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static io.sirius.ms.sdk.model.ConnectionErrorClass.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ConnectionChecks#selectMainError(List)} decides which of several connection errors the user is
 * told about. A failing internet probe outranks the errors that actually explain the problem, so it is
 * skipped whenever the login and license servers are still reachable - those two answering proves there
 * is internet, whatever the probe says.
 */
public class ConnectionChecksTest {

    private static ConnectionError error(ConnectionErrorClass klass, int siriusErrorCode) {
        return new ConnectionError()
                .errorKlass(klass)
                .siriusErrorCode(siriusErrorCode)
                .siriusMessage(klass.getValue() + " " + siriusErrorCode)
                .errorType(ConnectionErrorType.ERROR);
    }

    @Test
    public void testInternetPlusTwoOtherErrorsPicksTheMostRelevantOne() {
        // the reported case: a blocked internet probe alongside unaccepted terms and a rejected web service
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(TERMS, 62),
                error(APP_SERVER, 91));

        assertEquals(TERMS, ConnectionChecks.selectMainError(errors).getErrorKlass(),
                "terms outrank the app server error and both outrank the internet probe");
    }

    @Test
    public void testInternetPlusOneOtherErrorPicksTheOtherOne() {
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(APP_SERVER, 91));

        assertEquals(APP_SERVER, ConnectionChecks.selectMainError(errors).getErrorKlass());
    }

    @Test
    public void testInternetAloneStaysInternet() {
        List<ConnectionError> errors = List.of(error(INTERNET, 1));

        assertEquals(INTERNET, ConnectionChecks.selectMainError(errors).getErrorKlass(),
                "nothing else to report, so the internet error stands");
    }

    @Test
    public void testOnlyInternetErrorsStayInternet() {
        // nothing to fall back to once the internet errors are filtered out
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(INTERNET, 102));

        assertEquals(INTERNET, ConnectionChecks.selectMainError(errors).getErrorKlass());
        assertEquals(1, ConnectionChecks.selectMainError(errors).getSiriusErrorCode(),
                "the first internet error is kept");
    }

    @Test
    public void testUnreachableLoginAndLicenseServerKeepInternetAsMainError() {
        // login and license server unreachable too: the internet really is down, so say so
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(LOGIN_SERVER, 2),
                error(LICENSE_SERVER, 3),
                error(APP_SERVER, 91));

        assertEquals(INTERNET, ConnectionChecks.selectMainError(errors).getErrorKlass());
    }

    @Test
    public void testUnreachableLoginServerAloneDoesNotKeepInternetAsMainError() {
        // only one of the two is unreachable, so the internet probe is not corroborated
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(LOGIN_SERVER, 2),
                error(APP_SERVER, 91));

        assertEquals(LOGIN_SERVER, ConnectionChecks.selectMainError(errors).getErrorKlass());
    }

    @Test
    public void testFirstErrorIsKeptWhenItIsNotAnInternetError() {
        List<ConnectionError> errors = List.of(
                error(TERMS, 61),
                error(APP_SERVER, 91));

        assertEquals(TERMS, ConnectionChecks.selectMainError(errors).getErrorKlass(),
                "the list is already ordered by relevance, only the internet error needs skipping");
    }

    @Test
    public void testSameClassIsBrokenByErrorCode() {
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                error(APP_SERVER, 93),
                error(APP_SERVER, 91));

        assertEquals(91, ConnectionChecks.selectMainError(errors).getSiriusErrorCode(),
                "the lower error code wins within the same class");
    }

    @Test
    public void testMissingErrorCodeDoesNotBreakSelection() {
        // the error code is optional in the API model, and a missing one must not abort the whole panel
        List<ConnectionError> errors = List.of(
                error(INTERNET, 1),
                new ConnectionError().errorKlass(APP_SERVER).siriusMessage("no code")
                        .errorType(ConnectionErrorType.ERROR),
                error(APP_SERVER, 91));

        assertEquals(91, ConnectionChecks.selectMainError(errors).getSiriusErrorCode(),
                "an error without a code sorts last rather than throwing");
    }

    @Test
    public void testTermsSentenceNamesTheTermsWithoutClaimingTheyAreUnaccepted() {
        String sentence = ConnectionChecks.termsSentence(List.of(
                new Term().name("Terms of Service").link(URI.create("https://example.org/tos/")),
                new Term().name("Privacy Policy").link(URI.create("https://example.org/pp/"))));

        assertNotNull(sentence);
        assertTrue(sentence.contains("Terms of Service"));
        assertTrue(sentence.contains("Privacy Policy"));
        assertFalse(sentence.contains("have not been accepted"),
                "only the error message knows which term is missing, the link list must not claim it");
    }

    @Test
    public void testTermsSentenceIsEmptyWithoutTerms() {
        assertEquals("", ConnectionChecks.termsSentence(null), "no terms, no sentence");
        assertEquals("", ConnectionChecks.termsSentence(List.of()));
    }
}
