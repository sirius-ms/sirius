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

package de.unijena.bioinf.ms.gui.io;

import de.unijena.bioinf.ms.gui.ms_viewer.data.MSViewerDataModel;
import de.unijena.bioinf.ms.gui.ms_viewer.data.PeakInformation;

public class DummySpectrumContainer implements MSViewerDataModel {

	public DummySpectrumContainer() {
		
	}

	@Override
	public int findIndexOfPeak(double arg0, double arg1) {
		return -1;
	}

	@Override
	public String getIonization(int index) {
		return null;
	}

	@Override
	public double getAbsoluteIntensity(int arg0) {
		return 0;
	}

	@Override
	public int getIndexWithMass(double arg0) {
		return 0;
	}

	@Override
	public PeakInformation getInformations(int arg0) {
		return null;
	}

	@Override
	public int[] getIsotopePeaks(int arg0) {
		return null;
	}

	@Override
	public String getLabel() {
		return "";
	}

	@Override
	public double getMass(int arg0) {
		return 0;
	}

	@Override
	public double getRelativeIntensity(int arg0) {
		return 0;
	}

	@Override
	public double minMz() {
		return 0d;
	}

	@Override
	public double maxMz() {
		return 400d;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public boolean isIsotope(int arg0) {
		return false;
	}

	@Override
	public boolean isMarked(int arg0) {
		return false;
	}

	@Override
	public boolean isPlusZeroPeak(int arg0) {
		return false;
	}

	@Override
	public String getMolecularFormula(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isImportantPeak(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUnimportantPeak(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
