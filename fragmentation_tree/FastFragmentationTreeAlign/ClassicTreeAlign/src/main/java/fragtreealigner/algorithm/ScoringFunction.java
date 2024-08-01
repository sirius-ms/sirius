
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package fragtreealigner.algorithm;

import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

import java.io.Serializable;

@SuppressWarnings("serial")
abstract public class ScoringFunction implements Serializable {
	protected Session session;
	protected float scoreNullNull;
	protected float scoreUnion;
	
	public ScoringFunction() {
		initialize();
	}
	
	public ScoringFunction(Session session) {
		this.session = session;
		initialize();
	}
	
	public void initialize() {
		if (session == null) {
			scoreNullNull =  0;
			scoreUnion    = -9;
		} else {
			Parameters parameters = session.getParameters();
			scoreNullNull = parameters.scoreNullNull;
			scoreUnion    = parameters.scoreUnion;
		}
	}
	
	abstract public float score(AlignmentTreeNode node1, AlignmentTreeNode node2);
	abstract public float score(AlignmentTreeNode node1p1, AlignmentTreeNode node1p2, AlignmentTreeNode node2);
	
	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public float getScoreNullNull() {
		return scoreNullNull;
	}
	public void setScoreNullNull(float scoreNullNull) {
		this.scoreNullNull = scoreNullNull;
	}
	public float getScoreUnion() {
		return scoreUnion;
	}

	public void setScoreUnion(float scoreUnion) {
		this.scoreUnion = scoreUnion;
	}

}
