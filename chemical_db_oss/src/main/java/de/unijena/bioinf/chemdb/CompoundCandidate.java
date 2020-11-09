

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class CompoundCandidate {
    protected final InChI inchi;
    protected String name;
    protected String smiles;
    protected int pLayer;
    protected int qLayer;
    protected double xlogp = Double.NaN;
    @Nullable //this is the tanimoto to a matched fingerprint.
    protected Double tanimoto = null;

    //database info
    protected long bitset;
    protected DBLink[] links;

    //citation info
    protected PubmedLinks pubmedIDs = null;

    public CompoundCandidate(InChI inchi, String name, String smiles, int pLayer, int qLayer, double xlogp, @Nullable Double tanimoto, long bitset, DBLink[] links, PubmedLinks pubmedIDs) {
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
        if (c.pubmedIDs != null)
            this.pubmedIDs = c.pubmedIDs;
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

    public DBLink[] getLinks() {
        return links;
    }

    public void setLinks(DBLink[] links) {
        this.links = links;
    }

    public @NotNull Multimap<String, String> getLinkedDatabases() {
//        if (linkedDatabases == null)
        return DataSources.getLinkedDataSources(this);
//        return linkedDatabases;
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
        return (hasChargeState(pLayer, chargeState.getValue()) || hasChargeState(qLayer, chargeState.getValue()));
    }

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

    public void mergeDBLinks(DBLink[] links){
        this.links = Stream.concat(Arrays.stream(this.links),Arrays.stream(links)).distinct().toArray(DBLink[]::new);
    }

    public void mergeBits(long bitset) {
        this.bitset |= bitset;
    }

    public static class CompoundCandidateSerializer {

        public void serialize(CompoundCandidate value, JsonGenerator gen) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.name);
            gen.writeStringField("inchi", value.inchi.in3D);
            gen.writeStringField("inchikey",value.getInchiKey2D());
            if (value.pLayer!=0) gen.writeNumberField("pLayer",value.pLayer);
            if (value.qLayer!=0) gen.writeNumberField("qLayer",value.qLayer);
            gen.writeNumberField("xlogp",value.xlogp);
            gen.writeStringField("smiles", value.smiles);
            gen.writeNumberField("bitset", value.bitset);
            gen.writeObjectFieldStart("links");
            final Set<String> set = new HashSet<>(3);
            for (int k=0; k < value.links.length; ++k) {
                final DBLink link = value.links[k];
                if (set.add(link.name)) {
                    gen.writeArrayFieldStart(link.name);
                    gen.writeString(link.id);
                    for (int j=k+1; j < value.links.length; ++j) {
                        if (value.links[j].name.equals(link.name)) {
                            gen.writeString(link.id);
                        }
                    }
                    gen.writeEndArray();
                }
            }
            gen.writeEndObject();
            if (value.pubmedIDs!=null && value.pubmedIDs.getNumberOfPubmedIDs()>0) {
                gen.writeArrayFieldStart("pubmedIDs");
                for (int id : value.pubmedIDs.getCopyOfPubmedIDs()) {
                    gen.writeNumber(id);
                }
                gen.writeEndArray();
            }
            // quickn dirty
            if (value instanceof FingerprintCandidate) {
                FingerprintCandidate fpc = (FingerprintCandidate)value;
                gen.writeArrayFieldStart("fingerprint");
                for (FPIter iter : fpc.fingerprint.presentFingerprints()) {
                    gen.writeNumber(iter.getIndex());
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }

    public static class CompoundCandidateDeserializer {

        private final FingerprintVersion version;

        protected CompoundCandidateDeserializer(FingerprintVersion version) {
            this.version = version;
        }

        public CompoundCandidate deserialize(JsonParser p) throws IOException, JsonProcessingException {
            String inchi = null, inchikey = null, smiles=null,name=null;
            int player=0,qlayer=0;
            long bitset=0;
            double xlogp=0;
            TShortArrayList indizes = null;
            TIntArrayList pubmedIds= null;
            JsonToken jsonToken = p.nextToken();
            ArrayList<DBLink> links = new ArrayList<>();
            while (true) {
                if (jsonToken.isStructEnd()) break;

                // expect field name

                final String fieldName = p.currentName();
                switch (fieldName) {
                    case "inchi":
                        inchi = p.nextTextValue();
                        break;
                    case "inchikey":
                        inchikey = p.nextTextValue();
                        break;
                    case "name":
                        name = p.nextTextValue();
                        break;
                    case "pLayer":
                        player = p.nextIntValue(0);
                        break;
                    case "qLayer":
                        qlayer = p.nextIntValue(0);
                        break;
                    case "xlogp":
                        if (p.nextToken().isNumeric()) {
                            xlogp = p.getNumberValue().doubleValue();
                        } else {
                            LoggerFactory.getLogger("Warning: xlogp is invalid value for " + String.valueOf(inchikey) );
                        }
                        break;
                    case "smiles":
                        smiles = p.nextTextValue();
                        break;
                    case "bitset":
                        bitset = p.nextLongValue(0L);
                        break;
                    case "links":
                        if (p.nextToken() != JsonToken.START_OBJECT)
                            throw new IOException("malformed json. expected object"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_OBJECT) break;
                            else {
                                String linkName = p.currentName();
                                if (p.nextToken() != JsonToken.START_ARRAY)
                                    throw new IOException("malformed json. expected array"); // array start
                                do {
                                    jsonToken = p.nextToken();
                                    if (jsonToken == JsonToken.END_ARRAY) break;
                                    else links.add(new DBLink(linkName, p.getText()));
                                } while (true);
                            }
                        } while (true);
                        break;
                    case "pubmedIDs":
                        pubmedIds = new TIntArrayList();
                        if (p.nextToken() != JsonToken.START_ARRAY)
                            throw new IOException("malformed json. expected array"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_ARRAY) break;
                            else pubmedIds.add(Integer.parseInt(p.getText()));
                        } while (true);
                        break;
                    case "fingerprint":
                        indizes = new TShortArrayList();
                        if (p.nextToken() != JsonToken.START_ARRAY)
                            throw new IOException("malformed json. expected array"); // array start
                        do {
                            jsonToken = p.nextToken();
                            if (jsonToken == JsonToken.END_ARRAY) break;
                            else indizes.add(Short.parseShort(p.getText()));
                        } while (true);

                        break;
                    default:
                        p.nextToken();
                        break;
                }
                jsonToken = p.nextToken();
            }
            final CompoundCandidate C = new CompoundCandidate(
                    new InChI(inchikey, inchi), name, smiles, player,qlayer,xlogp,null,bitset,links.toArray(DBLink[]::new),
                    pubmedIds==null ? null : new PubmedLinks(pubmedIds.toArray())
            );
            if (indizes==null) {
                return C;
            } else {
                return new FingerprintCandidate(C, new ArrayFingerprint(version,indizes.toArray()));
            }
        }
    }

}

