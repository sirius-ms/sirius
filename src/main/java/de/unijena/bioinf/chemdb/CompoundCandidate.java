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

package de.unijena.bioinf.chemdb;

import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

public class CompoundCandidate {
    protected final InChI inchi;
    protected String name;
    protected String smiles;
    protected int pLayer;
    protected int qLayer;
    protected double xlogp = Double.NaN;

    //database info
    protected long bitset;
    protected DBLink[] links;
    protected Multimap<String, String> linkedDatabases = null;

    //citation info
    protected PubmedLinks pubmedIDs = null;

    public CompoundCandidate(CompoundCandidate c) {
        this.inchi = c.inchi;
        this.name = c.name;
        this.bitset = c.bitset;
        this.smiles = c.smiles;
        this.links = c.links;
        this.pLayer = c.pLayer;
        this.qLayer = c.qLayer;
        this.xlogp = c.xlogp;
        if (c.pubmedIDs != null)
            this.pubmedIDs = c.pubmedIDs;
    }

    public CompoundCandidate(InChI inchi) {
        this.inchi = inchi;
    }

    public static CompoundCandidate fromJSON(JsonObject o) {
        final CompoundCandidate c = new CompoundCandidate(inchiFromJson(o));
        c.readCompoundCandidateFromJson(o);
        return c;
    }

    protected final void readCompoundCandidateFromJson(JsonObject o) {
        this.name = o.getString("name", null);
        this.bitset = o.getJsonNumber("bitset").longValue();
        this.smiles = o.getString("smiles", null);
        this.pLayer = o.getInt("pLayer", 0);
        this.qLayer = o.getInt("qLayer", 0);
        this.xlogp = Double.NaN;
        try {//todo HACK:  sometimes this is not parsable
            if (o.containsKey("xlogp") && !o.isNull("xlogp")) {
                this.xlogp = o.getJsonNumber("xlogp").doubleValue();
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Could not parse xlogp from String.",e);
        }
        final JsonObject map = o.getJsonObject("links");
        if (map != null) {
            final ArrayList<DBLink> links = new ArrayList<>();
            for (final String dbName : map.keySet()) {
                final JsonArray ary = map.getJsonArray(dbName);
                for (int k = 0; k < ary.size(); ++k) {
                    links.add(new DBLink(dbName, ary.getString(k)));
                }
            }
            this.links = links.toArray(new DBLink[links.size()]);
        }

        final JsonArray cites = o.getJsonArray("pubmedIDs");
        if (cites != null) {
            TIntSet pubmedIDs = new TIntHashSet(cites.size());
            for (int i = 0; i < cites.size(); i++) {
                pubmedIDs.add(cites.getInt(i));
            }
            this.pubmedIDs = new PubmedLinks(pubmedIDs);
        }
    }


    public PubmedLinks getPubmedIDs() {
        return pubmedIDs;
    }

    public void setPubmedIDs(PubmedLinks pubmedIDs) {
        this.pubmedIDs = pubmedIDs;
    }

    protected static InChI inchiFromJson(JsonObject obj) {
        final String inchikey = obj.getString("inchikey");
        final String inchi = obj.getString("inchi");
        return new InChI(inchikey, inchi);
    }

    public final String toJSON() {
        final StringWriter sw = new StringWriter();
        try (final JsonGenerator w = Json.createGenerator(sw)) {
            writeToJSON(w);
        }
        return sw.toString();
    }

    public final void writeToJSON(JsonGenerator writer) {
        writeToJSON(writer, true);
    }

    public final void writeToJSON(JsonGenerator writer, boolean encloseInBrackets) {
        if (encloseInBrackets) writer.writeStartObject();
        writeContent(writer);
        if (encloseInBrackets) writer.writeEnd();
    }

    protected void writeContent(JsonGenerator writer) {
        writer.write("inchi", inchi.in3D);
        writer.write("inchikey", inchi.key);
        writer.write("pLayer", pLayer);
        writer.write("qLayer", qLayer);

        if (Double.isNaN(xlogp)) {
            writer.write("xlogp", JsonNumber.NULL);
        } else {
            writer.write("xlogp", xlogp);
        }
        if (name != null) writer.write("name", name);
        if (smiles != null) writer.write("smiles", smiles);
        writer.write("bitset", bitset);
        if (links != null) {
            final HashMap<String, ArrayList<DBLink>> grouped = new HashMap<>();
            for (DBLink l : links) {
                if (!grouped.containsKey(l.name)) grouped.put(l.name, new ArrayList<DBLink>());
                grouped.get(l.name).add(l);
            }
            writer.writeStartObject("links");
            for (Map.Entry<String, ArrayList<DBLink>> map : grouped.entrySet()) {
                writer.writeStartArray(map.getKey());
                for (DBLink l : map.getValue()) {
                    if (l.id != null) writer.write(l.id);
                }
                writer.writeEnd();
            }
            writer.writeEnd();
        }

        if (pubmedIDs != null) {
            writer.writeStartArray("pubmedIDs");
            pubmedIDs.iterator().forEachRemaining((IntConsumer) writer::write);
            writer.writeEnd();
        }
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
        if (linkedDatabases == null)
            linkedDatabases = DatasourceService.getLinkedDataSources(this);
        return linkedDatabases;
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
}

