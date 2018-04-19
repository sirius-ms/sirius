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
package fragtreealigner.util;

import fragtreealigner.domainobjects.db.FragmentationTreeDatabase;
import fragtreealigner.ui.MainFrame;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Session implements Serializable {
	private Parameters parameters;
	private MainFrame mainFrame;
	private FragmentationTreeDatabase fragTreeDB;
	private FragmentationTreeDatabase altFragTreeDB;

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	public Parameters getParameters() {
		return parameters;
	}
	
	public void setMainFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	public MainFrame getMainFrame() {
		return mainFrame;
	}

	public void setFragTreeDB(FragmentationTreeDatabase fragTreeDB) {
		this.fragTreeDB = fragTreeDB;
	}

	public void setAltFragTreeDB(FragmentationTreeDatabase altFragTreeDB) {
		this.altFragTreeDB = altFragTreeDB;
	}
	public FragmentationTreeDatabase getFragTreeDB() {
		return fragTreeDB;
	}

	public FragmentationTreeDatabase getAltFragTreeDB() {
		return altFragTreeDB;
	}	
}
