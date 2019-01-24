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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeLayer;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.db.custom.CustomDataSourceService;
import net.sf.jniinchi.INCHI_RET;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compound {
    private static final Logger logger = LoggerFactory.getLogger(Compound.class);

    private static Compound PrototypeCompound;

    protected static Compound getPrototypeCompound() {
        if (PrototypeCompound == null) {
            final FingerprintCandidate candidate = new FingerprintCandidate(
                    new InChI("WQZGKKKJIJFFOK-GASJEMHNSA-N", "InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2/t2-,3-,4+,5-,6?/m1/s1"),
                    new ArrayFingerprint(CdkFingerprintVersion.getDefault(), new short[]{
                            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 34, 35, 38, 80, 120
                    })
            );
            candidate.setSmiles(new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O").smiles);
            candidate.setName("Glucose");

            PrototypeCompound = new Compound(candidate);
            PrototypeCompound.addDatabase("PubChem", "5793");

        }
        return PrototypeCompound;
    }

    private volatile IAtomContainer molecule;

    private final FingerprintCandidate candidate;

    protected Compound(@NotNull final FingerprintCandidate candidate) {
        this.candidate = candidate;
    }


    public IAtomContainer getMolecule() {
        if (molecule == null) {
            molecule = parseMoleculeFromInChi();
        }
        return molecule;
    }

    public boolean hasAtomContainer() {
        return molecule != null;
    }

    private IAtomContainer parseMoleculeFromInChi() {
        try {
            final InChIGeneratorFactory f = InChIGeneratorFactory.getInstance();
            final InChIToStructure s = f.getInChIToStructure(getInchi().in2D, SilentChemObjectBuilder.getInstance());
            if (s.getReturnStatus() == INCHI_RET.OKAY && (s.getReturnStatus() == INCHI_RET.OKAY || s.getReturnStatus() == INCHI_RET.WARNING)) {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(s.getAtomContainer());

                return s.getAtomContainer();
            } else {
                logger.warn("Cannot parse InChI: " + getInchi().in2D + " due to the following error: " + s.getMessage() + " Return code: " + s.getReturnStatus() + ", Return status: " + s.getReturnStatus().toString());
                // try to parse smiles instead
                return parseMoleculeFromSmiles();
            }
            // calculate xlogP
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return parseMoleculeFromSmiles();
        }
    }

    private IAtomContainer parseMoleculeFromSmiles() {
        try {
            final IAtomContainer c = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(getSmiles());
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(c);
            return c;
        } catch (CDKException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public InChI getInchi() {
        return candidate.getInchi();
    }

    public String getSmiles() {
        return candidate.getSmiles();
    }

    public String getName() {
        return candidate.getName();
    }

    public double getXlogP() {
        return candidate.getXlogp();
    }

    public Fingerprint getFingerprint() {
        return candidate.getFingerprint();
    }

    public FingerprintCandidate getCandidate() {
        return candidate;
    }

    public boolean canBeNeutralCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEUTRAL_CHARGE);
    }

    public boolean canBePositivelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.POSITIVE_CHARGE);
    }

    public boolean canBeNegativelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEGATIVE_CHARGE);
    }

    public boolean hasChargeState(CompoundCandidateChargeState chargeState) {
        return (hasChargeState(candidate.getpLayer(), chargeState.getValue()) || hasChargeState(candidate.getqLayer(), chargeState.getValue()));
    }

    public boolean hasChargeState(CompoundCandidateChargeLayer chargeLayer, CompoundCandidateChargeState chargeState) {
        return (chargeLayer == CompoundCandidateChargeLayer.P_LAYER ?
                hasChargeState(candidate.getpLayer(), chargeState.getValue()) :
                hasChargeState(candidate.getqLayer(), chargeState.getValue())
        );
    }

    private boolean hasChargeState(int chargeLayer, int chargeState) {
        return ((chargeLayer & chargeState) == chargeState);
    }

    public void addDatabase(String name, String id) {
        CustomDataSourceService.Source c = CustomDataSourceService.getSourceFromName(name);
        if (c == null) {
            System.out.println("SCHOULD NOT BE ADDED");
        }
        long bit = c.flag();
        candidate.getLinkedDatabases().put(name, id);
        candidate.setBitset(candidate.getBitset() | bit);
    }
}
