/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FTAnalysis;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;
import java.util.List;

public interface LearnOptions {

    @Unparsed
    public List<File> getTrainingdata();

    @Option(shortName = "e", defaultValue = "80", description = "expected percentage of explained peaks")
    public double getExplain();

    @Option(shortName = "p", defaultToNull = true, description = "initial profile to start learning")
    public String getProfile();

    @Option(shortName = "w", description = "write trees and profiles for each iteration step")
    public boolean isWriting();

    @Option(shortName = "i", defaultValue = "4", description = "number of iterations")
    public int getIterations();

    @Option(defaultToNull = true)
    public String getMedianNoiseIntensity();

    @Option(defaultToNull = true)
    public String getIntensityCutoff();

    @Option(shortName = "I", defaultValue = "3", description = "number of iterations for common loss detection and loss size distribution estimation")
    public int getLossSizeIterations();

    @Option(shortName = "t", defaultValue = ".", description = "target directory")
    public File getTarget();

    @Option(shortName = "f", description = "use frequencies instead of intensities for common loss estimation")
    public boolean isFrequencyCounting();

    @Option(shortName = "l", defaultToNull = true, description = "limit number of peaks to the n-th most intensive peaks. This makes computation much faster")
    public Integer getPeakLimit();

    @Option(shortName = "P", longName = "skipAllPosteriori", description = "if set, posteriori parameters (mass deviation and noise distribution) are not learned from data")
    public boolean isSkipPosteriori();

    @Option(shortName = "S", defaultValue = "BOTH", description = "which posteriori methods should be used: MASSDEV, NOISE, BOTH or NONE")
    public PosteriorMethod getPosteriori();

    @Option(shortName = "L", description = "common loss analysis. Available options: SKIP (analysis), " +
            "REPLACE (previous common losses), ADD (losses to previous common losses), " +
            "MERGE (losses with previous common losses", defaultValue = "MERGE")
    public LearnMethod getCommonLosses();

    @Option(shortName = "F", description = "common fragment analysis. Available options: SKIP (analysis), " +
            "REPLACE (previous common fragments), ADD (fragments to previous common fragments), " +
            "MERGE (fragments with previous common fragments", defaultValue = "MERGE")
    public LearnMethod getCommonFragments();

    @Option
    public boolean isRecombinateLosses();

    @Option(shortName = "X", description = "start with expert loss annotations and old sirius 2 loss size distribution instead of using the scorers given in the profile")
    public boolean isStartWithExpertLosses();

    @Option(shortName = "x", description = "like -X but expert losses are kept as common losses")
    public boolean isKeepExpertLosses();

    @Option(shortName = "K", description = "keeep old loss list with the given frequency", defaultValue = "0")
    public double getKeepOldLosses();

    @Option
    public boolean isExponentialDistribution();

    @Option(defaultToNull = true)
    public Double getMaximalCommonLossScore();

    @Option(shortName = "m", defaultToNull = true)
    public Double getLossCountThreshold();

    @Option(shortName = "M", defaultToNull = true)
    public Double getLossRatioThreshold();

    @Option(shortName = "v")
    public boolean isVerbose();

    @Option(shortName = "h", helpRequest = true)
    public boolean getHelp();

    @Option
    public boolean getVersion();

    @Option
    public boolean getCite();


}
