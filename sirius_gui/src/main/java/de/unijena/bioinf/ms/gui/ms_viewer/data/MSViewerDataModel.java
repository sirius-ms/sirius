/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.ms_viewer.data;


public interface MSViewerDataModel {

    double minMz();

    double maxMz();

    int getSize();

    double getMass(int index);

    double getRelativeIntensity(int index);

    double getAbsoluteIntensity(int index);

    String getMolecularFormula(int index);

    PeakInformation getInformations(int index);

    boolean isMarked(int index);

    boolean isImportantPeak(int index);

    boolean isUnimportantPeak(int index);

    boolean isPlusZeroPeak(int index);

    boolean isIsotope(int index);

    int[] getIsotopePeaks(int index);

    String getLabel();

    int getIndexWithMass(double mass);

    int findIndexOfPeak(double mass, double tolerance);

    String getIonization(int index);
}
