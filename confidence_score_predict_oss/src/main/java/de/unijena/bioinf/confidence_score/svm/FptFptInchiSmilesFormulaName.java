/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.confidence_score.svm;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by martin on 03.08.18.
 */
public class FptFptInchiSmilesFormulaName {


    MolecularFormula formula;
    ProbabilityFingerprint fpt;
    String inchikey;
    String name;
    Fingerprint true_fpt;
    String smiles;
    String top_hit_inchikey;

    public FptFptInchiSmilesFormulaName(ProbabilityFingerprint fpt, Fingerprint true_fpt,String inchikey,String smiles, MolecularFormula formula, String name){

        this.formula=formula;
        this.fpt=fpt;
        this.inchikey=inchikey;
        this.name=name;
        this.true_fpt=true_fpt;
        this.smiles=smiles;

    }

    public String getName() {
        return name;
    }

    public Fingerprint getTrueFpt(){return this.true_fpt;}

    public MolecularFormula getFormula() {
        return formula;
    }

    public ProbabilityFingerprint getFpt() {
        return fpt;
    }
    public String getSmiles(){return smiles;}

    public String getInchikey() {
        return inchikey;
    }

    public String getTop_hit_inchikey() {
        return top_hit_inchikey;
    }

    public void setTop_hit_inchikey(String key){
        this.top_hit_inchikey=key;
    }
}
