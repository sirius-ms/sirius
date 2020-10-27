/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */


package de.unijena.bioinf.ms.gui.ms_viewer.data;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms2IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

public class SpectraJSONWriter{
	
	class PeakMatch {
		// Peak matches *between* two spectra

		int index1, index2;
		JsonObject matchMetadata;
		
		public PeakMatch(int index1, int index2, JsonObject matchMetadata){
			this.index1 = index1;
			this.index2 = index2;
			this.matchMetadata = matchMetadata;
		}

		public PeakMatch(int index1, int index2){
			this(index1, index2, new JsonObject());
		}
	}
   
	// DEBUG: for testing, MSViewerDataModel class should probably be removed
	@Deprecated	
	public String spectrumJSONString(MSViewerDataModel dmodel){
		JsonObject spectrum = spectrum2json(dmodel);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectrum);
	}

	// MS1 vs. simulated MS1 isotope pattern (mirror)
	public String ms1MirrorJSON(SimpleSpectrum pattern1, SimpleSpectrum pattern2, FTree ftree){
		JsonObject spectra = ms1MirrorIsotope(pattern1, pattern2);
		if (ftree != null)
			annotatePeakMatches(spectra.get("spectra").getAsJsonArray(), matchPeaks(ftree, pattern1));
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectra);
	}

	// MS1 spectrum (single)
	public String ms1JSON(SimpleSpectrum pattern1){
		JsonObject spectra = ms1Spectrum(pattern1);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectra);
	}
	
	// MS2 Spectrum with FragmentationTree (single)
	public String ms2JSON(MutableMs2Spectrum spectrum, FTree tree) {
		Fragment[] fragments = annotate(spectrum, tree);
		JsonObject jSpectrum = ms2Annotated(spectrum, fragments);
		annotatePeakPairs(jSpectrum, tree, fragments);
		final JsonObject j = new JsonObject();
		j.addProperty("massDeviation", 0);					   // TODO:
		JsonArray spectra = new JsonArray();
		spectra.add(jSpectrum);
		j.add("spectra", spectra);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(j);
	}

	protected JsonObject ms1MirrorIsotope(SimpleSpectrum pattern1, SimpleSpectrum pattern2) {
		final JsonObject j = new JsonObject();
		j.addProperty("massDeviation", 0);					   // TODO:
		JsonObject spectrum1 = spectrum2json(pattern1);
		JsonObject spectrum2 = spectrum2json(pattern2);
		spectrum1.addProperty("name", "MS1");
		spectrum2.addProperty("name", "MS1 simulated isotope pattern");
		JsonArray spectra = new JsonArray();
		spectra.add(spectrum1);
		spectra.add(spectrum2);
		j.add("spectra", spectra);
		return j;
	}

	protected JsonObject ms1Spectrum(SimpleSpectrum pattern1){
		final JsonObject j = new JsonObject();
		j.addProperty("massDeviation", 0);					   // TODO:
		JsonObject spectrum1 = spectrum2json(pattern1);
		spectrum1.addProperty("name", "MS1");
		JsonArray spectra = new JsonArray();
		spectra.add(spectrum1);
		j.add("spectra", spectra);
		return j;
	}

	protected JsonObject spectrum2json(SimpleSpectrum spectrum){
		final JsonObject jSpectrum = new JsonObject();
		jSpectrum.addProperty("name", "");					  // TODO
		final JsonObject spectrumMetadata = new JsonObject(); // TODO	   
		jSpectrum.add("spectrumMetadata", spectrumMetadata);
		double scale  = spectrum.getMaxIntensity();
		final JsonArray peaks = new JsonArray();
		for (int i = 0; i < spectrum.size(); i++){
			JsonObject peak = new JsonObject();
			peak.addProperty("mz", spectrum.getMzAt(i));
			double intensity = spectrum.getIntensityAt(i);
			peak.addProperty("intensity", intensity / scale);
			peak.addProperty("structure", "SMILES");	// TODO
			JsonObject peakMetadata = new JsonObject(); // TODO
			peakMetadata.addProperty("absoluteIntensity", intensity);
			peak.add("peakMetadata", peakMetadata);
			JsonArray peakPairs = new JsonArray(); // TODO
			peak.add("peakPairs", peakPairs);
			JsonObject peakMatches = new JsonObject();
			peak.add("peakMatches", peakMatches);
			peaks.add(peak);
		}
		jSpectrum.add("peaks", peaks);
		return jSpectrum;
	}

	@Deprecated
	protected JsonObject spectrum2json(MSViewerDataModel dmodel){
		final JsonObject jSpectrum = new JsonObject();
		jSpectrum.addProperty("name", "");					  // TODO
		final JsonObject spectrumMetadata = new JsonObject(); // TODO	   
		jSpectrum.add("spectrumMetadata", spectrumMetadata);
		final JsonArray peaks = new JsonArray();
		for (int i = 0; i < dmodel.getSize(); i++){
			JsonObject peak = new JsonObject();
			peak.addProperty("mz", dmodel.getMass(i));
			peak.addProperty("intensity", dmodel.getRelativeIntensity(i));
			peak.addProperty("structure", "SMILES");	// TODO
			JsonObject peakMetadata = new JsonObject(); // TODO
			peakMetadata.addProperty("absoluteIntensity", dmodel.getAbsoluteIntensity(i));
			peakMetadata.addProperty("formula", dmodel.getMolecularFormula(i));
			peakMetadata.addProperty("ionization", dmodel.getIonization(i));
			peak.add("peakMetadata", peakMetadata);
			JsonArray peakPairs = new JsonArray(); // TODO
			peak.add("peakPairs", peakPairs);
			JsonObject peakMatches = new JsonObject();
			peak.add("peakMatches", peakMatches);
			peaks.add(peak);
		}
		jSpectrum.add("peaks", peaks);
		return jSpectrum;
	}

	
	protected JsonObject ms2Annotated(MutableMs2Spectrum spectrum, Fragment[] fragments) {
		final JsonObject jSpectrum = new JsonObject();
		jSpectrum.addProperty("name", getSpectrumName(spectrum, "MS2"));
		final JsonObject spectrumMetadata = new JsonObject(); // TODO
		jSpectrum.add("spectrumMetadata", spectrumMetadata);
		double scale  = spectrum.getMaxIntensity();
		final JsonArray peaks = new JsonArray();
		for (int i = 0; i < spectrum.size(); i++){
			JsonObject peak = new JsonObject();
			peak.addProperty("mz", spectrum.getMzAt(i));
			double intensity = spectrum.getIntensityAt(i);
			peak.addProperty("intensity", intensity / scale);
			Fragment fragment = fragments[i];
			MolecularFormula formula;
			if (fragment != null && (formula = fragment.getFormula()) != null)
				peak.addProperty("formula", formula.toString());
			peak.addProperty("structure", "SMILES");	// TODO
			JsonObject peakMetadata = new JsonObject();
			peakMetadata.addProperty("absoluteIntensity", intensity);
			peak.add("peakMetadata", peakMetadata);
			JsonArray peakPairs = new JsonArray();
			peak.add("peakPairs", peakPairs);
			JsonObject peakMatches = new JsonObject(); // TODO
			peak.add("peakMatches", peakMatches);
			peaks.add(peak);
		}
		jSpectrum.add("peaks", peaks);
		return jSpectrum;
	}

	private String getSpectrumName(Spectrum<Peak> spectrum, String fallback) {
		int MsLevel = spectrum.getMsLevel();
		CollisionEnergy energy;
		if (MsLevel != -1 && (energy = spectrum.getCollisionEnergy()) != null)
			return "MS" + String.valueOf(MsLevel) + " " + energy.toString();
		return fallback;
	}

	// PeakPairs ^= Losses in the fragmentation Tree
	protected void annotatePeakPairs(JsonObject jSpectrum, FTree tree, Fragment[] fragments) {
		Fragment u, v;
		MolecularFormula lossFormula;
		JsonArray peaks = jSpectrum.get("peaks").getAsJsonArray();
		for (int ui = 0; ui < fragments.length; ++ui){			
			if ((u = fragments[ui]) == null)
				continue;
			JsonArray uPairs = peaks.get(ui).getAsJsonObject().get("peakPairs").getAsJsonArray();
			for (int vi = ui + 1; vi < fragments.length; ++vi){
				if ((v = fragments[vi]) == null)
					continue;
				JsonArray vPairs = peaks.get(vi).getAsJsonObject().get("peakPairs").getAsJsonArray();
				Loss loss;
				try{ 
					loss = tree.getLoss(u, v);
				} catch (IndexOutOfBoundsException e){
					loss = null;
				}
				if (loss == null){
					try{
						// if fragments are always sorted by mass, this should be the correct order
						loss = tree.getLoss(v, u);
					} catch (IndexOutOfBoundsException e){
						loss = null;
					}
				}
				if (loss != null && (lossFormula = loss.getFormula()) != null){
					JsonObject uPair = new JsonObject();
					JsonObject vPair = new JsonObject();
					uPair.addProperty("index", vi);
					vPair.addProperty("index", ui);
					JsonObject metadata = new JsonObject();
					metadata.addProperty("formula", lossFormula.toString());
					uPair.add("metadata", metadata);
					vPair.add("metadata", metadata);
					uPairs.add(uPair);
					vPairs.add(vPair);
				}
			}
		}
	}

	// Peak matches *between* spectra
	protected void annotatePeakMatches(JsonArray spectra, List<PeakMatch> matches) {
		JsonArray pattern1Peaks = spectra.get(0).getAsJsonObject()
			.get("peaks").getAsJsonArray();			
		JsonArray pattern2Peaks = spectra.get(1).getAsJsonObject()
			.get("peaks").getAsJsonArray();
		for (PeakMatch m : matches) {
			JsonObject p1Matches = pattern1Peaks.get(m.index1).getAsJsonObject()
				.get("peakMatches").getAsJsonObject();
			p1Matches.addProperty("index", m.index2);
			p1Matches.add("metadata", new JsonObject());
			JsonObject p2Matches = pattern2Peaks.get(m.index2).getAsJsonObject()
				.get("peakMatches").getAsJsonObject();
			p2Matches.addProperty("index", m.index1);
			p2Matches.add("metadata", new JsonObject());
		}
	}

	protected List<PeakMatch> matchPeaks(FTree ftree, SimpleSpectrum spectrum){
		SiriusIsotopePattern siriusIsotopePattern = new SiriusIsotopePattern(ftree, spectrum);
		List<PeakMatch> peakMatches = new LinkedList<>();
		final SimpleSpectrum pattern = ftree.getAnnotationOrNull(IsotopePattern.class).getPattern();
		for (int i = 0; i < pattern.size(); i++) {
			Peak p = pattern.getPeakAt(i);
			int j = siriusIsotopePattern.findIndexOfPeak(p.getMass(), 0.1);
			if (j >= 0) peakMatches.add(new PeakMatch(j, i));
		}
		return peakMatches;			
	}
	
	// Copied from SingleSpectrumAnnotated
	private Fragment[] annotate(Spectrum<? extends Peak> spectrum, FTree tree) {
		Fragment[] annotatedFormulas = new Fragment[spectrum.size()];
		BitSet isIsotopicPeak = new BitSet(spectrum.size());
		
        final FragmentAnnotation<AnnotatedPeak> annotatedPeak = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class);
        if (annotatedPeak==null) return null;
        final FragmentAnnotation<Ms2IsotopePattern> isoAno = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final Deviation dev = new Deviation(1,0.01);
        double scale = 0d;
        for (Fragment f : tree) {
            AnnotatedPeak peak = annotatedPeak.get(f);
            if (peak == null) {
                continue;
            }
            Ms2IsotopePattern isoPat = isoAno==null ? null : isoAno.get(f);
            if (isoPat!=null) {
                for (Peak p : isoPat.getPeaks()) {
                    if (p.getMass() - peak.getMass() > 0.25) {
                        int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass()-1e-6);
                        for (int j=i; j < spectrum.size(); ++j) {
                            if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                                annotatedFormulas[j] = f;
                                isIsotopicPeak.set(j);
                            } else break;
                        }
                    }
                }
            }
            for (Peak p : peak.getOriginalPeaks()) {
                int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass()-1e-6);
                for (int j=i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
            // due to the recalibration we might be far away from the "original" mass
            final double recalibratedMz = peak.getRecalibratedMass();
            {
                int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, recalibratedMz-1e-4);
                for (int j=i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(recalibratedMz, spectrum.getMzAt(j))) {
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
        }

		// return new Pair<Fragment[], BitSet>(annotatedFormulas, isIsotopicPeak);
		return annotatedFormulas;
    }

}
