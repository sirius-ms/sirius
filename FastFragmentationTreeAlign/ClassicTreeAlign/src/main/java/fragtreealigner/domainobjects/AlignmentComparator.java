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
package fragtreealigner.domainobjects;

import java.io.Serializable;
import java.util.Comparator;

@SuppressWarnings("serial")
public class AlignmentComparator implements Comparator<Alignment>, Serializable {
	private int scoreNum;

	public AlignmentComparator() {
		this.scoreNum = 0;
	}
	
	public AlignmentComparator(int scoreNum) {
		this.scoreNum = scoreNum;
	}
	
	public int compare(Alignment alignment1, Alignment alignment2) {
		if (scoreNum == 0) {
			if (alignment1.getScore() < alignment2.getScore()) return -1;
			if (alignment1.getScore() > alignment2.getScore()) return 1;		
			return 0;
		} else {
			if (alignment1.getScoreList().get(scoreNum - 1) < alignment2.getScoreList().get(scoreNum - 1)) return -1;
			if (alignment1.getScoreList().get(scoreNum - 1) > alignment2.getScoreList().get(scoreNum - 1)) return 1;					
			return 0;
		}
	}
}
