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

package de.unijena.bioinf.ms.frontend.subtools.export.tables;

import picocli.CommandLine;

import java.text.DecimalFormat;

public class PredictionsOptions {

    protected DecimalFormat decimalFormat = null;

    //this tool is already integrated in summaries tool.
    @CommandLine.Option(names = {"--digits","--precision","-p"},
            description = "@|bold Specify number of digits used for printing floating point values. -1 -> full length Double value.  %n %n|@", defaultValue="-1", hidden = true)
    protected void setDigits(int digits) {
        if (digits == 0) {
            decimalFormat = new DecimalFormat("0");
        } else if (digits > 0) {
            decimalFormat = new DecimalFormat("0." + "#".repeat(digits));
        } else {
            decimalFormat = null;
        }
    }

    protected String float2string(double value) {
        if (decimalFormat==null) return String.valueOf(value);
        else return decimalFormat.format(value);
    }
    protected String float2string(float value) {
        if (decimalFormat==null) return String.valueOf(value);
        else return decimalFormat.format(value);
    }

    @CommandLine.Option(names = {"--classyfire"}, description = "Output predicted  classyfire probabilities by CANOPUS.")
    protected boolean classyfire;

    @CommandLine.Option(names = {"--npc"}, description = "Output predicted NPC (natural product classifier) probabilities by CANOPUS.")
    protected boolean npc;

    @CommandLine.Option(names = {"--fingerprints", "--fingerprint"}, description = "Output predicted fingerprint probabilities by CSI:FingerID.")
    protected boolean fingerprints;

    @CommandLine.Option(names = {"--pubchem"}, description = "Output predicted PubChem fingerprint probabilities by CSI:FingerID (subset of --fingerprint).")
    protected boolean pubchem;


    @CommandLine.Option(names = {"--maccs"}, description = "Output predicted MACCS fingerprint probabilities by CSI:FingerID (subset of --fingerprint).")
    protected boolean maccs;

    @CommandLine.Option(names = {"--all"}, description = "Output all predicted CSI:FingerID and CANOPUS probabilities (sets --fingerprints, --classyfire, --npc).")
    protected void setAll(boolean all){
        if (all){
            classyfire = true;
            npc = true;
            fingerprints = true;
        }
    }


    public boolean isAnyPredictionSet(){
        return classyfire || npc || fingerprints || pubchem || maccs;
    }

    public boolean requiresCanopusResult(){
        return classyfire || npc;
    }

    public boolean requiresFingerprintResult(){
        return fingerprints || pubchem || maccs;
    }
}
