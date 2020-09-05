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

public class CSVDialogReturnContainer {

	private double minEnergy, maxEnergy;
	private int massIndex, intIndex, msLevel;
	
	public CSVDialogReturnContainer() {
		minEnergy = -1;
		maxEnergy = -1;
		massIndex = -1;
		intIndex =-1;
		msLevel = -1;
	}
	
	public double getMinEnergy() {
		return minEnergy;
	}
	public void setMinEnergy(double minEnergy) {
		this.minEnergy = minEnergy;
	}
	public double getMaxEnergy() {
		return maxEnergy;
	}
	public void setMaxEnergy(double maxEnergy) {
		this.maxEnergy = maxEnergy;
	}
	public int getMassIndex() {
		return massIndex;
	}
	public void setMassIndex(int massIndex) {
		this.massIndex = massIndex;
	}
	public int getIntIndex() {
		return intIndex;
	}
	public void setIntIndex(int intIndex) {
		this.intIndex = intIndex;
	}
	public int getMsLevel() {
		return msLevel;
	}
	public void setMsLevel(int msLevel) {
		this.msLevel = msLevel;
	}
	
	

}
