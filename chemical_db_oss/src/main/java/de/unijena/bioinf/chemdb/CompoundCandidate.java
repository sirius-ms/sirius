

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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompoundCandidate {
    //The 2d inchi key is the UUID of an CompoundCandidate, field need to exist fot proper object based serialization
    @Id
    @Getter
    @NotNull
    final String inchikey;
    @Getter
    @NotNull
    protected final InChI inchi;
    @Getter
    @Setter
    protected String name;
    @Getter
    @Setter
    protected String smiles;
    @Getter
    @Setter
    protected int pLayer;
    @Getter
    @Setter
    protected int qLayer;
    @Getter
    @Setter
    protected double xlogp = Double.NaN;
    //database info
    @Setter
    protected long bitset;

    public long getBitset() {
        if (bitset <= 0)
            bitset = CustomDataSources.getDBFlagsFromNames(getLinkedDatabases().keySet());
        return bitset;
    }

    protected ArrayList<DBLink> links;
    //citation info
    @Getter
    @Setter
    protected PubmedLinks pubmedIDs;


    //todo the following fields are results and should be in an extended class. In the meantime they should not be part of the constructor
    /**
     * Maximum Common Edge Subgraph (MCES) distance to the top scoring hit (CSI:FingerID) in a candidate list.
     *
     * @see <a href="https://doi.org/10.1101/2023.03.27.534311">Small molecule machine learning: All models are wrong, some may not even be useful</a>
     */
    @Nullable
    @Getter
    @Setter
    protected Double mcesToTopHit = null;

    /**
     * Tanimoto distance to a predicted molecular fingerprint (CSI:FingerID).
     */
    @Nullable //this is the tanimoto to a matched fingerprint.
    @Getter
    @Setter
    protected Double tanimoto = null;

    public CompoundCandidate(@NotNull InChI inchi, String name, String smiles, int pLayer, int qLayer, double xlogp, long bitset, DBLink[] links, PubmedLinks pubmedIDs) {
        this(inchi, name, smiles, pLayer, qLayer, xlogp, bitset, new ArrayList<>(List.of(links)), pubmedIDs);
    }

    public CompoundCandidate(@NotNull InChI inchi, String name, String smiles, int pLayer, int qLayer, double xlogp, long bitset, ArrayList<DBLink> links, PubmedLinks pubmedIDs) {
        this(inchi);
        this.name = name;
        this.smiles = smiles;
        this.pLayer = pLayer;
        this.qLayer = qLayer;
        this.xlogp = xlogp;
        this.bitset = bitset;
        this.links = links;
        this.pubmedIDs = pubmedIDs;

    }

    public CompoundCandidate(CompoundCandidate c) {
        this.inchi = c.inchi;
        this.inchikey = c.inchikey;
        this.name = c.name;
        this.bitset = c.bitset;
        this.smiles = c.smiles;
        this.links = c.links;
        this.pLayer = c.pLayer;
        this.qLayer = c.qLayer;
        this.xlogp = c.xlogp;
        this.tanimoto = c.tanimoto;
        this.mcesToTopHit = c.mcesToTopHit;
        this.pubmedIDs = c.pubmedIDs;
    }


    public CompoundCandidate(InChI inchi) {
        this.inchi = inchi;
        this.inchikey = (inchi != null) ? inchi.key2D() : null;
    }

    public String getInchiKey2D() {
        return inchikey;
    }


    public List<DBLink> getMutableLinks() {
        return links;
    }

    public List<DBLink> getLinks() {
        return links == null ? null
                : Collections.unmodifiableList(links);
    }

    public void setLinks(Collection<DBLink> links) {
        if (links instanceof ArrayList)
            this.links = (ArrayList<DBLink>) links;
        else this.links = new ArrayList<>(links);
    }

    public @NotNull Map<String, List<String>> getLinkedDatabases() {
        if (links != null) {
            Map<String, List<String>> databases = new HashMap<>();
            for (DBLink link : links)
                databases.computeIfAbsent(link.getName(), k -> new ArrayList<>()).add(link.getId());

            return databases;
        }

        return Map.of();
    }

    @Deprecated
    public boolean canBeNeutralCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEUTRAL_CHARGE);
    }

    @Deprecated
    public boolean canBePositivelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.POSITIVE_CHARGE);
    }

    @Deprecated
    public boolean canBeNegativelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEGATIVE_CHARGE);
    }

    @Deprecated
    public boolean hasChargeState(CompoundCandidateChargeState chargeState) {
        return (hasChargeState(pLayer, chargeState.getValue()) || hasChargeState(qLayer, chargeState.getValue()));
    }

    @Deprecated
    public boolean hasChargeState(CompoundCandidateChargeLayer chargeLayer, CompoundCandidateChargeState chargeState) {
        return (chargeLayer == CompoundCandidateChargeLayer.P_LAYER ?
                hasChargeState(pLayer, chargeState.getValue()) :
                hasChargeState(qLayer, chargeState.getValue())
        );
    }

    private boolean hasChargeState(int chargeLayer, int chargeState) {
        return ((chargeLayer & chargeState) == chargeState);
    }

    public String toString() {
        return getInchiKey2D() + " (dbflags=" + bitset + ")";
    }

    public void mergeDBLinks(List<DBLink> links) {
        this.links = (ArrayList<DBLink>) Stream.concat(this.links.stream(), links.stream()).distinct().collect(Collectors.toList());
    }

    public void mergeBits(long bitset) {
        this.bitset |= bitset;
    }

    public void mergeCompoundName(@Nullable String name) {
        if (name == null || name.isBlank())
            return;
        if (this.name == null || this.name.isBlank() || this.name.length() > name.length())
            this.name = name;
    }

    public FormulaCandidate toFormulaCandidate(PrecursorIonType ionization) {
        return new FormulaCandidate(inchi.extractFormulaOrThrow(), ionization, bitset);
    }
}

