/*
 * Sirius MassSpec Tool
 * based on the Epos Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Sirius.
 *
 * Sirius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sirius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Sirius.  If not, see <http://www.gnu.org/licenses/>;.
*/
package de.unijena.bioinf.babelms.mzxml.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.MSnTreeBuilder;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectraTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class MsRun implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = -91305684835607102L;

    protected List<ParentFile> parentFiles;
    protected List<MsInstrument> msInstruments;
    protected List<DataProcessing> dataProcessings;
    protected List<Scan> scans;
    private Spotting spotting;
    private Integer scanCount;
    private Double startTime;
    private Double endTime;
    private String sha1;

    /**
     * Property for DefinitionHelper to skip all scan nodes
     */
    public final static String NOSCANS = "NO_SCANS";


    public MsRun() {
        this.parentFiles = new ArrayList<ParentFile>();
        this.msInstruments = new ArrayList<MsInstrument>();
        this.dataProcessings = new ArrayList<DataProcessing>();
        this.scans = new ArrayList<Scan>();
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
        		.append("<table>")
                .def("scan count", scanCount)
                .def("start time", startTime)
                .def("end time", endTime)
                .append("</table>")
                .append("<table>")
                .defEnumOf("parent files", parentFiles)
                .defEnumOf("MS instruments", msInstruments)
                .defEnumOf("data processings", dataProcessings)
                .append("</table>")
                .skipIf(NOSCANS)
                	.append("<table>")
                    .defEnumOf("scans", scans)
                    .append("</table>")
                .endCondition()
                .append("<table>")
                .def("spotting", spotting)
                .def("sha1", sha1)
                .append("</table>")
                .endList();
    }

    public Iterator<Scan> recursiveIterator() {
        return new Scan.RecursiveScanIterator(scans);
    }

    public Spotting getSpotting() {
        return spotting;
    }

    public void setSpotting(Spotting spotting) {
        this.spotting = spotting;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public List<ParentFile> getParentFiles() {
        return parentFiles;
    }

    public List<MsInstrument> getMsInstruments() {
        return msInstruments;
    }

    public List<DataProcessing> getDataProcessings() {
        return dataProcessings;
    }

    public Integer getScanCount() {
        return scanCount;
    }

    public void setScanCount(Integer scanCount) {
        this.scanCount = scanCount;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
             /*
    public SimpleSpectraTree<Scan, Peak> buildSpectraTree() throws InvalidInputData {
    	final MSnTreeBuilder<Scan, Peak> builder = new MSnTreeBuilder<Scan, Peak>();
    	final Iterator<Scan> scans = recursiveIterator();
    	final HashMap<Integer, Scan> map = new HashMap<Integer, Scan>();
    	while (scans.hasNext()) {
    		final Scan scan = scans.next();
    		map.put(scan.getNum(), scan);
    	}
    	for (Scan scan: map.values()) {
    		final List<PrecursorIon> precursors = scan.precursorIons;
    		if (precursors.isEmpty() || scan.getMsLevel() == 1) {
    			builder.addRoot(scan);
    		} else {
    			final PrecursorIon ion = precursors.get(0);
    			final Scan precursorSpectrum = map.get(ion.getPrecursorScanNum());
    			if (precursorSpectrum != null) {
    				final int pos = Spectrums.search(scan, ion.getPrecursorMz(), new Deviation(1, 1e-3));
    				builder.add(scan, precursorSpectrum, pos, scan.getCollisionEnergy(), scan.getMsLevel());
    			} else {
    				builder.add(scan, ion.getPrecursorMz(), scan.getCollisionEnergy(), scan.getMsLevel());
    			}
    		}
    	}
    	return builder.buildTree(new Deviation(1, 1e-3));
    }
    */

    public SimpleSpectraTree buildSpectraTree() throws InvalidInputData {
        throw new RuntimeException("Does not exist!"); // TODO add
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MsRun msRun = (MsRun) o;

        if (dataProcessings != null ? !dataProcessings.equals(msRun.dataProcessings) : msRun.dataProcessings != null)
            return false;
        if (endTime != null ? !endTime.equals(msRun.endTime) : msRun.endTime != null) return false;
        if (msInstruments != null ? !msInstruments.equals(msRun.msInstruments) : msRun.msInstruments != null)
            return false;
        if (parentFiles != null ? !parentFiles.equals(msRun.parentFiles) : msRun.parentFiles != null) return false;
        if (scanCount != null ? !scanCount.equals(msRun.scanCount) : msRun.scanCount != null) return false;
        if (scans != null ? !scans.equals(msRun.scans) : msRun.scans != null) return false;
        if (sha1 != null ? !sha1.equals(msRun.sha1) : msRun.sha1 != null) return false;
        if (spotting != null ? !spotting.equals(msRun.spotting) : msRun.spotting != null) return false;
        if (startTime != null ? !startTime.equals(msRun.startTime) : msRun.startTime != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = parentFiles != null ? parentFiles.hashCode() : 0;
        result = 31 * result + (msInstruments != null ? msInstruments.hashCode() : 0);
        result = 31 * result + (dataProcessings != null ? dataProcessings.hashCode() : 0);
        result = 31 * result + (scans != null ? scans.hashCode() : 0);
        result = 31 * result + (spotting != null ? spotting.hashCode() : 0);
        result = 31 * result + (scanCount != null ? scanCount.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        return result;
    }

}
