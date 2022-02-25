/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class LipidSpecies implements ProcessedInputAnnotation {

    private final LipidChain[] chains;
    private final LipidClass type;

    public LipidSpecies sortChains() {
        final LipidChain[] clone = chains.clone();
        Arrays.sort(clone);
        return new LipidSpecies(type, clone);
    }

    public Optional<String> generateHypotheticalStructure() {
        if (chainsUnknown()) return Optional.empty();
        return type.getSmiles().map(smiles-> {
            if (getLipidClass().isSphingolipid()) {
                final Optional<LipidChain> mightSphingoChain = Arrays.stream(chains).filter(x->x.type== LipidChain.Type.SPHINGOSIN).findFirst();
                if (mightSphingoChain.isEmpty()) return null;
                LipidChain sphingoChain = mightSphingoChain.get();
                StringBuilder chainBuilder = new StringBuilder();
                int bondsToAdd = sphingoChain.numberOfDoubleBonds;
                chainBuilder.append("OCC(NR1)C(O)");
                int dboffset = ((sphingoChain.chainLength/2 - sphingoChain.numberOfDoubleBonds)/2) * 2;
                for (int j=3, n = sphingoChain.chainLength; j < n; ++j) {
                    chainBuilder.append('C');
                    if (bondsToAdd > 0 && j>=dboffset && j % 2 == 0) {
                        chainBuilder.append('=');
                        --bondsToAdd;
                    }
                }
                smiles = smiles.replace("X",chainBuilder.toString());
            }
            int chainId = 1;
            for (int i=0; i < chains.length; ++i) {
                int dboffset = ((chains[i].chainLength/2 - chains[i].numberOfDoubleBonds)/2) * 2;
                StringBuilder chainBuilder = new StringBuilder();
                int bondsToAdd = chains[i].numberOfDoubleBonds;
                if (chains[i].type == LipidChain.Type.ACYL) {
                    chainBuilder.append("C(=O)");
                } else if (chains[i].type == LipidChain.Type.ALKYL) {
                    chainBuilder.append("C");
                } else {
                    continue;
                }
                for (int j=1, n = chains[i].chainLength; j < n; ++j) {
                    chainBuilder.append('C');
                    if (bondsToAdd > 0 && j>=dboffset && j % 2 == 0) {
                        chainBuilder.append('=');
                        --bondsToAdd;
                    }
                }
                smiles = smiles.replace("R" + (chainId++),chainBuilder.toString());
            }
            return smiles;
        });
    }

    public static LipidSpecies fromString(String lipid) {
        LipidClass klasse = null;

        int splitIdx = lipid.indexOf('(');
        for (LipidClass c : LipidClass.values()) {
            if (lipid.substring(0, splitIdx).equalsIgnoreCase(c.abbr()) || lipid.substring(0, splitIdx).equalsIgnoreCase(c.name())) {
                klasse = c;
                break;
            }
        }
        if (klasse==null) throw new IllegalArgumentException("Unknown lipid: " + lipid);
        int chainIndex = splitIdx + 1;
        String chain = lipid.substring(chainIndex, lipid.lastIndexOf(')'));
        if (chain.isEmpty()) throw new IllegalArgumentException("Unknown lipid: " + lipid);
        String[] subchains = chain.split("_");
        if (subchains.length==1 && !chain.contains("_") && chain.indexOf("/")>0) {
            subchains = chain.split("/");
        }
        final LipidChain[] chains = Arrays.stream(subchains).map(x->LipidChain.fromString(x)).toArray(LipidChain[]::new);
        if (chains.length==1 && klasse.chains > 1) {
            chains[0] = new LipidChain(LipidChain.Type.MERGED, chains[0].chainLength, chains[0].numberOfDoubleBonds);
        }
        return new LipidSpecies(klasse, chains);
    }

    public LipidSpecies(LipidClass type, LipidChain[] chains) {
        this.chains = chains;
        this.type = type;
    }

    public LipidSpecies(LipidClass type) {
        this.chains = new LipidChain[0];
        this.type = type;
    }

    public Optional<MolecularFormula> getHypotheticalMolecularFormula() {
        if (chainsUnknown()) return Optional.empty();
        else {
            MolecularFormula f = type.headgroup.getMolecularFormula();
            for (LipidChain c : chains) f = f.add(c.getFormula());
            return Optional.of(f);
        }
    }

    public boolean chainsUnknown() {
        return chains.length==0 || chains[0].isMerged();
    }

    public HeadGroup getHeadGroup() {
        return type.headgroup;
    }

    public LipidClass getLipidClass() {
        return type;
    }

    public LipidChain[] getChains() {
        return chains;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LipidSpecies that = (LipidSpecies) o;
        return Arrays.equals(chains, that.chains) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(chains);
        return result;
    }

    @Override
    public String toString() {
        return type.abbr() + "("+ (chains.length>0 ? Joiner.on('_').join(chains) : "?")  + ")";
    }

    public LipidSpecies makeGeneric() {
        if (this.chains.length==1 && this.chains[0].isMerged()) return this;
        if (this.chains.length==0) return this;
        return new LipidSpecies(this.type, new LipidChain[]{LipidChain.merge(this.chains)});
    }
}
