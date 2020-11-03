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

package de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Created by fleisch on 29.05.17.
 */
public enum BioTransformation {

    //MyCompoundID and Rogers
    C6H12N4O ("Arginine",new String[]{"Rogers"}),
    C4H6N2O2 ("Asparagine",new String[]{"Rogers"}),
    C4H5NO3 ("Aspartic Acid",new String[]{"Rogers"}),
    C3H5NOS ("Cysteine",new String[]{"Rogers", "MyCompoundID"}),
    C6H10N2O3S2 ("Cystine",new String[]{"Rogers"}),
    C5H7NO3 ("Glutamic Acid",new String[]{"Rogers"}),
    C5H8N2O2 ("Glutamine",new String[]{"Rogers"}),
    C2H3NO ("Glycine",new String[]{"Rogers", "MyCompoundID"}),
    C6H7N3O ("Histidine",new String[]{"Rogers"}),
    C6H11NO ("Leucine, Isoleucine",new String[]{"Rogers"}),
    C6H12N2O ("Lysine",new String[]{"Rogers"}),
    C5H9NOS ("Methionine",new String[]{"Rogers"}),
    C9H9NO ("Phenylalanine",new String[]{"Rogers"}),
    C5H7NO ("Proline",new String[]{"Rogers"}),
    C3H5NO2 ("Serine",new String[]{"Rogers"}),
    C4H7NO2 ("Threonine",new String[]{"Rogers"}),
    C11H10N2O ("Tryptophan",new String[]{"Rogers"}),
    C9H9NO2 ("Tyrosine",new String[]{"Rogers"}),
    C5H9NO ("Valine",new String[]{"Rogers"}),
    C4H4O2 ("acetotacetate (-H2O)",new String[]{"Rogers"}),
    C3H5O ("acetone (-H)",new String[]{"Rogers"}),
    C10H15N2O3S ("biotinyl (-H)",new String[]{"Rogers"}),
    C10H14N2O2S ("biotinyl (-H2O)",new String[]{"Rogers"}),
    CH2ON ("carbamoyl P transfer (-H2PO4)",new String[]{"Rogers"}),
    C21H34N7O16P3S ("co-enzyme A (-H)",new String[]{"Rogers"}),
    C21H33N7O15P3S ("co-enzyme A (-H2O)",new String[]{"Rogers"}),
    C10H15N3O5S ("glutathione (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C5H7 ("isoprene addition (-H)",new String[]{"Rogers"}),
    C3H2O3 ("malonyl group (-H2O)",new String[]{"Rogers"}),
    C16H30O ("palmitoylation (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C8H8NO5P ("pyridoxal phosphate (-H2O)",new String[]{"Rogers"}),
    CH3N2O ("urea addition (-H)",new String[]{"Rogers"}),
    C5H4N5 ("adenine (-H)",new String[]{"Rogers"}),
    C10H11N5O3 ("adenosine (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C10H13N5O9P2 ("Adenosine 5'-diphosphate (-H2O)",new String[]{"Rogers"}),
    C10H12N5O6P ("Adenosine 5'monophosphate (-H2O), adenylate (-H2O)",new String[]{"Rogers"}),
    C9H13N3O10P2 ("cytidine 5' diphosphate (-H2O)",new String[]{"Rogers"}),
    C9H12N3O7P ("cytidine 5' monophsophate (-H2O)",new String[]{"Rogers"}),
    C4H4N3O ("cytosine (-H)",new String[]{"Rogers"}),
    C10H13N5O10P2 ("Guanosine 5- diphosphate (-H2O)",new String[]{"Rogers"}),
    C10H12N5O7P ("Guanosine 5- monophosphate (-H2O)",new String[]{"Rogers"}),
    C5H4N5O ("guanine (-H)",new String[]{"Rogers"}),
    C10H11N5O4 ("guanosine (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C10H14N2O10P2 ("deoxythymidine 5' diphosphate (-H2O)",new String[]{"Rogers"}),
    C10H12N2O4 ("thymidine (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C5H5N2O2 ("thymine (-H)",new String[]{"Rogers"}),
    C10H13N2O7P ("thymidine 5' monophosphate (-H2O)",new String[]{"Rogers"}),
    C9H12N2O11P2 ("uridine 5' diphosphate (-H2O)",new String[]{"Rogers"}),
    C9H11N2O8P ("uridine 5' monophosphate (-H2O)",new String[]{"Rogers"}),
    C4H3N2O2 ("uracil (-H)",new String[]{"Rogers"}),
    C9H10N2O5 ("uridine (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C2H3O2 ("acetylation (-H)",new String[]{"Rogers"}),
    C2H2 ("C2H2",new String[]{"Rogers"}),
    CO2 ("Carboxylation",new String[]{"Rogers", "MyCompoundID"}),
    CHO2 ("CHO2",new String[]{"Rogers"}),
    H2O ("condensation/dehydration",new String[]{"Rogers", "MyCompoundID"}),
    H3O6P2 ("diphosphate",new String[]{"Rogers"}),
    C2H4 ("ethyl addition (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    CO ("Formic Acid (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C2O2 ("glyoxylate (-H2O)",new String[]{"Rogers"}),
    H2 ("hydrogenation/dehydrogenation",new String[]{"Rogers", "MyCompoundID"}),
    O ("hydroxylation (-H)",new String[]{"Rogers", "MyCompoundID"}),
    P ("Inorganic Phosphate",new String[]{"Rogers"}),
    C2H2O ("ketol group (-H2O), acetylation (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    CH2 ("methanol (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    HPO3 ("phosphate",new String[]{"Rogers", "MyCompoundID"}),
    NH2 ("primary amine",new String[]{"Rogers"}),
    PP ("pyrophosphate",new String[]{"Rogers"}),
    NH ("secondary amine",new String[]{"Rogers", "MyCompoundID"}),
    SO3 ("sulfate (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    N ("tertiary amine",new String[]{"Rogers"}),
    C6H10O6 ("C6H10O6",new String[]{"Rogers"}),
    C5H8O4 ("D-Ribose (-H2O) (ribosylation)",new String[]{"Rogers", "MyCompoundID"}),
    C12H20O11 ("disaccharide (-H2O)",new String[]{"Rogers"}),
    C6H11O8P ("glucose-N-Phosphate (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C6H8O6 ("Glucuronic Acid (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C6H10O5 ("monosaccharide (-H2O)",new String[]{"Rogers", "MyCompoundID"}),
    C18H30O15 ("trisaccharide (-H2O)",new String[]{"Rogers", "MyCompoundID"}),

    //MyCompoundID only
    NH3 ("loss of C2H4 ",new String[]{"MyCompoundID"}),
    C4H3N3 ("loss of taurine ",new String[]{"MyCompoundID"}),
    C4H2N2O ("loss of Thymine ",new String[]{"MyCompoundID"}),
    C2H5NO2S ("loss of D-ribose ",new String[]{"MyCompoundID"}),
    C5H4N2O ("loss of Guanine ",new String[]{"MyCompoundID"}),
    C3H5NO2S ("loss of Carnitine ",new String[]{"MyCompoundID"}),
    C5H3N5 ("loss of Hexose ",new String[]{"MyCompoundID"}),
    C7H13NO2 ("loss of glucuronic acid",new String[]{"MyCompoundID"}),
    C5H7NO3S ("loss of Thymidine ",new String[]{"MyCompoundID"}),
    C9H11N3O4 ("loss of Glucose-6-phosphate ",new String[]{"MyCompoundID"}),
    C10H15N3O6S ("",new String[]{"MyCompoundID"}),
    C12H20O10 ("",new String[]{"MyCompoundID"}),

    //symmetric
    SH("Sulfonic acid <-> Thiol (-O3)",new String[]{"MyCompoundID"},"SO3H",true),
    C5H5N5("Adenine loss",new String[]{"MyCompoundID"},"H2O",true);



    private final MolecularFormula formula;
    private final MolecularFormula condition;
    public final String[] source;
    public final String chemName;
    private final boolean symmetric;


    BioTransformation(String chemName, String[] source) {
        this.source = source;
        this.chemName = chemName;
        formula = MolecularFormula.parseOrThrow(name());
        condition = MolecularFormula.emptyFormula();
        symmetric = true;
    }

    BioTransformation(String chemName, String[] source,  String condition, boolean symmetric) {
        this.source = source;
        this.chemName = chemName;
        this.formula = MolecularFormula.parseOrThrow(name());
        this.condition = MolecularFormula.parseOrThrow(condition);
        this.symmetric = symmetric;
    }

    BioTransformation(String chemName, String[] source, String formula, String condition, boolean symmetric) {
        this.source = source;
        this.chemName = chemName;
        this.formula = MolecularFormula.parseOrThrow(formula);
        this.condition = MolecularFormula.parseOrThrow(condition);
        this.symmetric = symmetric;
    }

    public boolean isConditional(){
        return !getCondition().equals(MolecularFormula.emptyFormula());
    }

    public boolean isSymmetric(){
        return symmetric ;
    }

    public MolecularFormula getCondition() {
        return condition;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    
}
