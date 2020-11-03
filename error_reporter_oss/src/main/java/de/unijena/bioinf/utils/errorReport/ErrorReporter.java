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

package de.unijena.bioinf.utils.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ErrorReporter implements Runnable, Callable<String> {
    public static Properties properties = System.getProperties();
    public final String SOFTWARE_NAME;
    private ErrorReport report = null;


    public static boolean INIT_PROPS(Properties props) {
        if (props != null) {
            ErrorReporter.properties = props;
            return true;
        }
        return false;
    }

    public ErrorReporter() {
        String name = ErrorReporter.properties.getProperty("de.unijena.bioinf.utils.errorReport.softwareName");
        SOFTWARE_NAME = (name != null && !name.isEmpty()) ? name : "Software";
    }

    public ErrorReporter(String SOFTWARE_NAME, ErrorReport report) {
        this.SOFTWARE_NAME = SOFTWARE_NAME;
        setReport(report);
    }

    public ErrorReporter(ErrorReport report) {
        this();
        setReport(report);
    }

    public void setReport(ErrorReport report) {
        this.report = report;
    }

    public ErrorReport getReport() {
        return report;
    }

    protected void initReporter() {

    }

    protected abstract String reportError(ErrorReport report) throws Exception;

    @Override
    public String call() throws Exception {
        if (report == null) {
            String m = "Nothing to report! Process finished";
            LoggerFactory.getLogger(this.getClass()).warn(m);
            return m;
        } else {
            return reportError(report);
        }
    }

    @Override
    public void run() {
        try {
            LoggerFactory.getLogger(this.getClass()).info(call());
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Sending error report Failed", e);
        }
    }

}
