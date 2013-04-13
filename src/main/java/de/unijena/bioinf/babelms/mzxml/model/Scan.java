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
import java.util.*;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.babelms.ConsistencyValidator;

public class Scan implements Serializable, DefinitionListHelper.Applicable, ConsistencyValidator, Spectrum<Peak> {

    private static final long serialVersionUID = -9030470476506802899L;

    protected SortedMap<Integer, Reference<String, ParentFile>> scanOrigins;
    protected List<PrecursorIon> precursorIons;
    private MaldiExperiment maldi;
    protected NameValueSet nameValueSet;
    protected List<String> comments;
    protected List<Scan> scans;
    private boolean useDoublePrecision;
    private double[] peakMass;
    private double[] peakIntensity;
    private Integer num;
    private Integer msLevel;
    private Integer peaksCount;
    private Polarity polarity;
    private String scanType;
    private String filterLine;
    private Boolean centroided;
    private Boolean deisotoped;
    private boolean chargeDeconvoluted;
    private Double retentionTime;
    private Float ionisationEnergy;
    private Float collisionEnergy;
    private Float cidGasPressure;
    private Float startMz;
    private Float endMz;
    private Float lowMz;
    private Float highMz;
    private Float basePeakMz;
    private Float basePeakIntensity;
    private Float totIonCurrent;
    private Reference<Integer, MsInstrument> msInstrument;
    private Float compensationVoltage;
    private boolean useZlib;
    private int compressedLen;

    public Scan() {
        this.scanOrigins = new TreeMap<Integer, Reference<String, ParentFile>>();
        this.precursorIons = new ArrayList<PrecursorIon>();
        this.nameValueSet = new NameValueSet();
        this.comments = new ArrayList<String>();
        this.scans = new ArrayList<Scan>();
        this.chargeDeconvoluted = false;
        this.useZlib = false;
        this.compressedLen = -1;
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        return helper.startList()
                .def("num", num)
                .def("MS level", msLevel)
                .def("peaks count", peaksCount)
                .def("polarity", polarity)
                .def("scan type", scanType)
                .def("filter line", filterLine)
                .def("centroided", centroided)
                .def("deisotoped", deisotoped)
                .def("chargeDeconvoluted", chargeDeconvoluted)
                .def("retentionTime", retentionTime)
                .def("ionisationEnergy", ionisationEnergy)
                .def("collisionEnergy", collisionEnergy)
                .def("cidGasPressure", cidGasPressure)
                .def("start m/z", startMz).def("end m/z", endMz).def("low m/z", lowMz).def("high m/z", highMz)
                .def("base peak m/z", basePeakMz).def("base peak intensity", basePeakIntensity)
                .def("tot ion current", totIonCurrent)
                .def("compensation voltage", compensationVoltage)
                .def("uses zlib compression", useZlib)
                .def("uses double precision", useDoublePrecision)
                .defListOf("scan origins", scanOrigins)
                .defEnumOf("precursor ions", precursorIons)
                .def("maldi experiment", maldi)
                .append(nameValueSet)
                .defEnumOf("comments", comments)
                .skipIf(MsRun.NOSCANS)
                .defEnumOf("scans", scans)
                .endCondition()
                .endList();
    }

    public int getCompressedLen() {
        return compressedLen;
    }

    public void setCompressedLen(int compressedLen) {
        this.compressedLen = compressedLen;
    }

    public SortedMap<Integer, Reference<String, ParentFile>> getScanOrigins() {
        return scanOrigins;
    }

    public void setPeakMass(double[] peakMass) {
        this.peakMass = peakMass;
    }

    public void setPeakIntensity(double[] peakIntensity) {
        this.peakIntensity = peakIntensity;
    }

    public List<PrecursorIon> getPrecursorIons() {
        return precursorIons;
    }

    public NameValueSet getNameValueSet() {
        return nameValueSet;
    }

    public List<String> getComments() {
        return comments;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public double[] getPeakMass() {
        return peakMass;
    }

    public double[] getPeakIntensity() {
        return peakIntensity;
    }

    public MaldiExperiment getMaldi() {
        return maldi;
    }

    public void setMaldi(MaldiExperiment maldi) {
        this.maldi = maldi;
    }

    public boolean isUsingDoublePrecision() {
        return useDoublePrecision;
    }

    public void setUseDoublePrecision(boolean useDoublePrecision) {
        this.useDoublePrecision = useDoublePrecision;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Integer getMsLevel() {
        return msLevel;
    }

    public void setMsLevel(Integer msLevel) {
        this.msLevel = msLevel;
    }

    public Integer getPeaksCount() {
        return peaksCount;
    }

    public void setPeaksCount(Integer peaksCount) {
        this.peaksCount = peaksCount;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getFilterLine() {
        return filterLine;
    }

    public void setFilterLine(String filterLine) {
        this.filterLine = filterLine;
    }

    public Boolean getCentroided() {
        return centroided;
    }

    public void setCentroided(Boolean centroided) {
        this.centroided = centroided;
    }

    public Boolean getDeisotoped() {
        return deisotoped;
    }

    public void setDeisotoped(Boolean deisotoped) {
        this.deisotoped = deisotoped;
    }

    public Boolean getChargeDeconvoluted() {
        return chargeDeconvoluted;
    }

    public void setChargeDeconvoluted(Boolean chargeDeconvoluted) {
        this.chargeDeconvoluted = (chargeDeconvoluted == null) ? false : chargeDeconvoluted.booleanValue();
    }

    public Double getRetentionTime() {
        return retentionTime;
    }

    public void setRetentionTime(Double retentionTime) {
        this.retentionTime = retentionTime;
    }

    public Float getIonisationEnergy() {
        return ionisationEnergy;
    }

    public void setIonisationEnergy(Float ionisationEnergy) {
        this.ionisationEnergy = ionisationEnergy;
    }

    public Float getCollisionEnergy() {
        return collisionEnergy;
    }

    public void setCollisionEnergy(Float collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    public Float getCidGasPressure() {
        return cidGasPressure;
    }

    public void setCidGasPressure(Float cidGasPressure) {
        this.cidGasPressure = cidGasPressure;
    }

    public Float getStartMz() {
        return startMz;
    }

    public void setStartMz(Float startMz) {
        this.startMz = startMz;
    }

    public Float getEndMz() {
        return endMz;
    }

    public void setEndMz(Float endMz) {
        this.endMz = endMz;
    }

    public Float getLowMz() {
        return lowMz;
    }

    public void setLowMz(Float lowMz) {
        this.lowMz = lowMz;
    }

    public Float getHighMz() {
        return highMz;
    }

    public void setHighMz(Float highMz) {
        this.highMz = highMz;
    }

    public Float getBasePeakMz() {
        return basePeakMz;
    }

    public void setBasePeakMz(Float basePeakMz) {
        this.basePeakMz = basePeakMz;
    }

    public Float getBasePeakIntensity() {
        return basePeakIntensity;
    }

    public void setBasePeakIntensity(Float basePeakIntensity) {
        this.basePeakIntensity = basePeakIntensity;
    }

    public Float getTotIonCurrent() {
        return totIonCurrent;
    }

    public void setTotIonCurrent(Float totIonCurrent) {
        this.totIonCurrent = totIonCurrent;
    }

    public Reference<Integer, MsInstrument> getMsInstrument() {
        return msInstrument;
    }

    public void setMsInstrument(Reference<Integer, MsInstrument> msInstrument) {
        this.msInstrument = msInstrument;
    }

    public Float getCompensationVoltage() {
        return compensationVoltage;
    }

    public void setCompensationVoltage(Float compensationVoltage) {
        this.compensationVoltage = compensationVoltage;
    }

    public boolean isUseZlib() {
        return useZlib;
    }

    public void setUseZlib(boolean useZlib) {
        this.useZlib = useZlib;
    }

    public Iterator<Scan> recursiveIterator() {
        return new RecursiveScanIterator(scans);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scan scan = (Scan) o;

        if (chargeDeconvoluted != scan.chargeDeconvoluted) return false;
        if (useDoublePrecision != scan.useDoublePrecision) return false;
        if (basePeakIntensity != null ? !basePeakIntensity.equals(scan.basePeakIntensity) : scan.basePeakIntensity != null)
            return false;
        if (basePeakMz != null ? !basePeakMz.equals(scan.basePeakMz) : scan.basePeakMz != null) return false;
        if (centroided != null ? !centroided.equals(scan.centroided) : scan.centroided != null) return false;
        if (cidGasPressure != null ? !cidGasPressure.equals(scan.cidGasPressure) : scan.cidGasPressure != null)
            return false;
        if (collisionEnergy != null ? !collisionEnergy.equals(scan.collisionEnergy) : scan.collisionEnergy != null)
            return false;
        if (comments != null ? !comments.equals(scan.comments) : scan.comments != null) return false;
        if (compensationVoltage != null ? !compensationVoltage.equals(scan.compensationVoltage) : scan.compensationVoltage != null)
            return false;
        if (deisotoped != null ? !deisotoped.equals(scan.deisotoped) : scan.deisotoped != null) return false;
        if (endMz != null ? !endMz.equals(scan.endMz) : scan.endMz != null) return false;
        if (filterLine != null ? !filterLine.equals(scan.filterLine) : scan.filterLine != null) return false;
        if (highMz != null ? !highMz.equals(scan.highMz) : scan.highMz != null) return false;
        if (ionisationEnergy != null ? !ionisationEnergy.equals(scan.ionisationEnergy) : scan.ionisationEnergy != null)
            return false;
        if (lowMz != null ? !lowMz.equals(scan.lowMz) : scan.lowMz != null) return false;
        if (maldi != null ? !maldi.equals(scan.maldi) : scan.maldi != null) return false;
        if (msInstrument != null ? !msInstrument.equals(scan.msInstrument) : scan.msInstrument != null) return false;
        if (msLevel != null ? !msLevel.equals(scan.msLevel) : scan.msLevel != null) return false;
        if (nameValueSet != null ? !nameValueSet.equals(scan.nameValueSet) : scan.nameValueSet != null) return false;
        if (num != null ? !num.equals(scan.num) : scan.num != null) return false;
        if (!Arrays.equals(peakIntensity, scan.peakIntensity)) return false;
        if (!Arrays.equals(peakMass, scan.peakMass)) return false;
        if (peaksCount != null ? !peaksCount.equals(scan.peaksCount) : scan.peaksCount != null) return false;
        if (polarity != scan.polarity) return false;
        if (precursorIons != null ? !precursorIons.equals(scan.precursorIons) : scan.precursorIons != null)
            return false;
        if (retentionTime != null ? !retentionTime.equals(scan.retentionTime) : scan.retentionTime != null)
            return false;
        if (scanOrigins != null ? !scanOrigins.equals(scan.scanOrigins) : scan.scanOrigins != null) return false;
        if (scanType != null ? !scanType.equals(scan.scanType) : scan.scanType != null) return false;
        if (scans != null ? !scans.equals(scan.scans) : scan.scans != null) return false;
        if (startMz != null ? !startMz.equals(scan.startMz) : scan.startMz != null) return false;
        if (totIonCurrent != null ? !totIonCurrent.equals(scan.totIonCurrent) : scan.totIonCurrent != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = scanOrigins != null ? scanOrigins.hashCode() : 0;
        result = 31 * result + (precursorIons != null ? precursorIons.hashCode() : 0);
        result = 31 * result + (maldi != null ? maldi.hashCode() : 0);
        result = 31 * result + (nameValueSet != null ? nameValueSet.hashCode() : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        result = 31 * result + (scans != null ? scans.hashCode() : 0);
        result = 31 * result + (useDoublePrecision ? 1 : 0);
        result = 31 * result + (peakMass != null ? Arrays.hashCode(peakMass) : 0);
        result = 31 * result + (peakIntensity != null ? Arrays.hashCode(peakIntensity) : 0);
        result = 31 * result + (num != null ? num.hashCode() : 0);
        result = 31 * result + (msLevel != null ? msLevel.hashCode() : 0);
        result = 31 * result + (peaksCount != null ? peaksCount.hashCode() : 0);
        result = 31 * result + (polarity != null ? polarity.hashCode() : 0);
        result = 31 * result + (scanType != null ? scanType.hashCode() : 0);
        result = 31 * result + (filterLine != null ? filterLine.hashCode() : 0);
        result = 31 * result + (centroided != null ? centroided.hashCode() : 0);
        result = 31 * result + (deisotoped != null ? deisotoped.hashCode() : 0);
        result = 31 * result + (chargeDeconvoluted ? 1 : 0);
        result = 31 * result + (retentionTime != null ? retentionTime.hashCode() : 0);
        result = 31 * result + (ionisationEnergy != null ? ionisationEnergy.hashCode() : 0);
        result = 31 * result + (collisionEnergy != null ? collisionEnergy.hashCode() : 0);
        result = 31 * result + (cidGasPressure != null ? cidGasPressure.hashCode() : 0);
        result = 31 * result + (startMz != null ? startMz.hashCode() : 0);
        result = 31 * result + (endMz != null ? endMz.hashCode() : 0);
        result = 31 * result + (lowMz != null ? lowMz.hashCode() : 0);
        result = 31 * result + (highMz != null ? highMz.hashCode() : 0);
        result = 31 * result + (basePeakMz != null ? basePeakMz.hashCode() : 0);
        result = 31 * result + (basePeakIntensity != null ? basePeakIntensity.hashCode() : 0);
        result = 31 * result + (totIonCurrent != null ? totIonCurrent.hashCode() : 0);
        result = 31 * result + (msInstrument != null ? msInstrument.hashCode() : 0);
        result = 31 * result + (compensationVoltage != null ? compensationVoltage.hashCode() : 0);
        return result;
    }

    public static class RecursiveScanIterator implements Iterator<Scan> {

        final Deque<Iterator<Scan>> stack;

        public RecursiveScanIterator(Iterable<Scan> iterables) {
            stack = new ArrayDeque<Iterator<Scan>>();
            stack.push(iterables.iterator());
        }

        public boolean hasNext() {
            while (!stack.isEmpty() && !stack.peek().hasNext()) {
                stack.pop();
            }
            return !stack.isEmpty();
        }

        public Scan next() {
            if (!hasNext()) throw new NoSuchElementException();
            final Scan scan = stack.peek().next();
            if (!scan.getScans().isEmpty())
                stack.push(scan.getScans().iterator());
            return scan;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

	@Override
	public boolean isConsistent() {
		return peakMass.length == peakIntensity.length && peakMass.length == peaksCount;
	}

	@Override
	public boolean forceConsistency() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Peak> iterator() {
		return new Iterator<Peak>() {
			int index=0;
			@Override
			public boolean hasNext() {
				return index < peakMass.length;
			}

			@Override
			public Peak next() {
				if (!hasNext()) throw new NoSuchElementException();
				return getPeakAt(index++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public double getMzAt(int index) {
		return peakMass[index];
	}

	@Override
	public double getIntensityAt(int index) {
		return peakIntensity[index];
	}

	@Override
	public Peak getPeakAt(int index) {
		return new Peak(peakMass[index], peakIntensity[index]);
	}

	@Override
	public int size() {
		return peakMass.length;
	}

	@Override
	public <E> E getProperty(String name) {
		throw new UnsupportedOperationException(); // TODO: implement
	}

	@Override
	public <E> E getProperty(String name, E defaultValue) {
		throw new UnsupportedOperationException(); // TODO: implement
	}
}
