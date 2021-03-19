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

package de.unijena.bioinf.ms.middleware.compounds;

/**
 * The CompoundId contains the ID of a compound together with some read-only information that might be displayed in
 * some summary view.
 */
public class CompoundId {

    // identifier
    protected String id;

    // identifier source
    protected String name;
    protected long index;

    // additional attributes
    protected double ionMass;
    protected String ionType;

    //Summary of the results of the compounds
    protected CompoundSummary summary;
    protected CompoundMsData msData;


    public CompoundId(String id, String name, long index, double ionmass, String ionType) {
        this.id = id;
        this.name = name;
        this.index = index;
        this.ionMass = ionmass;
        this.ionType = ionType;
        this.summary = null;
    }

    public String getName() {
        return name;
    }

    public long getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public double getIonMass() {
        return ionMass;
    }

    public String getIonType() {
        return ionType;
    }

    public CompoundSummary getSummary() {
        return summary;
    }

    public void setSummary(CompoundSummary summary) {
        this.summary = summary;
    }

    public CompoundMsData getMsData() {
        return msData;
    }

    public void setMsData(CompoundMsData msData) {
        this.msData = msData;
    }
}
