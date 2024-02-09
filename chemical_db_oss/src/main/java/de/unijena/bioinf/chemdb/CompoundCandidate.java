

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonSerialize(using = CompoundCandidate.Serializer.class)
public class CompoundCandidate {
    //The 2d inchi is the UID of an CompoundCandidate
    protected final InChI inchi;
    protected String name;
    protected String smiles;
    protected int pLayer;
    protected int qLayer;
    protected double xlogp = Double.NaN;

    //database info
    protected long bitset;
    protected ArrayList<DBLink> links;

    //citation info
    protected PubmedLinks pubmedIDs;

    protected Double taxonomicScore;
    protected String taxonomicSpecies;

    @Nullable //this is the tanimoto to a matched fingerprint.
    protected Double tanimoto = null;

    protected List<String> referenceSpectraSplash;

    public CompoundCandidate(InChI inchi, String name, String smiles, int pLayer, int qLayer, double xlogp, @Nullable Double tanimoto, long bitset, DBLink[] links, PubmedLinks pubmedIDs, List<String> referenceSpectraSplash) {
        this(inchi, name, smiles, pLayer, qLayer, xlogp, tanimoto, bitset, new ArrayList<>(List.of(links)), pubmedIDs, referenceSpectraSplash);
    }

    public CompoundCandidate(InChI inchi, String name, String smiles, int pLayer, int qLayer, double xlogp, @Nullable Double tanimoto, long bitset, ArrayList<DBLink> links, PubmedLinks pubmedIDs, List<String> referenceSpectraSplash) {
        this.inchi = inchi;
        this.name = name;
        this.smiles = smiles;
        this.pLayer = pLayer;
        this.qLayer = qLayer;
        this.xlogp = xlogp;
        this.tanimoto = tanimoto;
        this.bitset = bitset;
        this.links = links;
        this.pubmedIDs = pubmedIDs;
        this.referenceSpectraSplash = referenceSpectraSplash;
    }

    public CompoundCandidate(CompoundCandidate c) {
        this.inchi = c.inchi;
        this.name = c.name;
        this.bitset = c.bitset;
        this.smiles = c.smiles;
        this.links = c.links;
        this.pLayer = c.pLayer;
        this.qLayer = c.qLayer;
        this.xlogp = c.xlogp;
        this.tanimoto = c.tanimoto;
        this.pubmedIDs = c.pubmedIDs;
        this.taxonomicScore = c.taxonomicScore;
        this.taxonomicSpecies = c.taxonomicSpecies;
        this.referenceSpectraSplash = c.referenceSpectraSplash;
    }


    public CompoundCandidate(InChI inchi) {
        this.inchi = inchi;
    }

    public PubmedLinks getPubmedIDs() {
        return pubmedIDs;
    }

    public void setPubmedIDs(PubmedLinks pubmedIDs) {
        this.pubmedIDs = pubmedIDs;
    }

    public InChI getInchi() {
        return inchi;
    }

    public String getInchiKey2D() {
        return inchi.key2D();
    }

    public long getBitset() {
        return bitset;
    }

    public void setBitset(long bitset) {
        this.bitset = bitset;
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

    public @NotNull Multimap<String, String> getLinkedDatabases() {
        return DataSources.getLinkedDataSources(this);
    }

    public String getSmiles() {
        return smiles;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getpLayer() {
        return pLayer;
    }

    public void setpLayer(int pLayer) {
        this.pLayer = pLayer;
    }

    public int getqLayer() {
        return qLayer;
    }

    public void setqLayer(int qLayer) {
        this.qLayer = qLayer;
    }

    public double getXlogp() {
        return xlogp;
    }

    public void setXlogp(double xlogp) {
        this.xlogp = xlogp;
    }

    public Double getTanimoto() {
        return tanimoto;
    }

    public void setTanimoto(Double tanimoto) {
        this.tanimoto = tanimoto;
    }

    @Nullable
    public Double getTaxonomicScore() {
        return taxonomicScore;
    }

    public void setTaxonomicScore(Double taxonomicScore) {
        this.taxonomicScore = taxonomicScore;
    }

    @Nullable
    public String getTaxonomicSpecies() {
        return taxonomicSpecies;
    }

    public void setTaxonomicSpecies(String taxonomicSpecies) {
        this.taxonomicSpecies = taxonomicSpecies;
    }

    public List<String> getReferenceSpectraSplash() {
        return referenceSpectraSplash;
    }

    public void setReferenceSpectraSplash(List<String> referenceSpectraSplashes) {
        this.referenceSpectraSplash = referenceSpectraSplashes;
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

    public boolean hasReferenceSpectra() {
        return referenceSpectraSplash != null && !referenceSpectraSplash.isEmpty();
    }


    //region Serializer
    public static class Serializer extends BaseSerializer<CompoundCandidate> {
    }

    public abstract static class BaseSerializer<C extends CompoundCandidate> extends JsonSerializer<C> {

        protected void serializeInternal(C value, JsonGenerator gen) throws IOException {
            gen.writeStringField("name", value.name);
            gen.writeStringField("inchi", value.inchi.in3D);
            gen.writeStringField("inchikey", value.getInchiKey2D());
            if (value.pLayer != 0) gen.writeNumberField("pLayer", value.pLayer);
            if (value.qLayer != 0) gen.writeNumberField("qLayer", value.qLayer);
            gen.writeNumberField("xlogp", value.xlogp);
            gen.writeStringField("smiles", value.smiles);
            gen.writeNumberField("bitset", value.bitset);
            gen.writeObjectFieldStart("links");
            final Set<String> set = new HashSet<>(3);
            for (int k = 0; k < value.links.size(); ++k) {
                final DBLink link = value.links.get(k);
                if (set.add(link.name)) {
                    gen.writeArrayFieldStart(link.name);
                    gen.writeString(link.id);
                    for (int j = k + 1; j < value.links.size(); ++j) {
                        if (value.links.get(j).name.equals(link.name)) {
                            gen.writeString(value.links.get(j).id);
                        }
                    }
                    gen.writeEndArray();
                }
            }
            gen.writeEndObject();
            if (value.pubmedIDs != null && value.pubmedIDs.getNumberOfPubmedIDs() > 0) {
                gen.writeArrayFieldStart("pubmedIDs");
                for (int id : value.pubmedIDs.getCopyOfPubmedIDs()) {
                    gen.writeNumber(id);
                }
                gen.writeEndArray();
            }
            if (value.referenceSpectraSplash != null && !value.referenceSpectraSplash.isEmpty()) {
                gen.writeArrayFieldStart("referenceSpectraSplash");
                for (String splash : value.referenceSpectraSplash) {
                    gen.writeString(splash);
                }
                gen.writeEndArray();
            }
        }

        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            serializeInternal(value, gen);
            gen.writeEndObject();
        }
    }

    public static void toJSONList(List<FingerprintCandidate> fpcs, Writer out) throws IOException {
        toJSONList(fpcs, new JsonFactory().createGenerator(out));
    }

    public static void toJSONList(List<FingerprintCandidate> fpcs, OutputStream out) throws IOException {
        toJSONList(fpcs, new JsonFactory().createGenerator(out));
    }

    public static <C extends CompoundCandidate> void toJSONList(List<C> fpcs, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("compounds");
        new ObjectMapper().writeValue(generator, fpcs);
        generator.writeEndObject();
        generator.flush();
    }

    public FormulaCandidate toFormulaCandidate(PrecursorIonType ionization){
        return new FormulaCandidate(inchi.extractFormulaOrThrow(), ionization ,bitset);
    }
}

