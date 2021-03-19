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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.sirius.Feedback;
import de.unijena.bioinf.sirius.Progress;

public class DummyProgress implements Progress{

	public DummyProgress() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void finished() {
		System.out.println("finished");
	}

	@Override
	public void info(String s) {
		System.out.println("info "+s);
	}

	@Override
	public void init(double d) {
		System.out.println("init "+d);
	}

	@Override
	public void update(double d1, double d2, String s3,Feedback f) {
		System.out.println("update "+d1+" "+d2+" "+s3);
	}

}
