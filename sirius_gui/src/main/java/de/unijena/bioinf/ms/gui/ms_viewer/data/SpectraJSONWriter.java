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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

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
	public String ms1MirrorJSON(@NotNull SiriusIsotopePattern siriusIsotopePattern, Deviation ms1MassDiffDev) {
		SimpleSpectrum spectrum = new SimpleSpectrum(siriusIsotopePattern.spectrum);
		JsonObject spectra = ms1MirrorIsotope(spectrum, siriusIsotopePattern.simulatedPattern);
		annotatePeakMatches(spectra.get("spectra").getAsJsonArray(), matchPeaks(siriusIsotopePattern.simulatedPattern, spectrum, ms1MassDiffDev));
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectra);
	}

	// MS1 spectrum (single)
	public String ms1JSON(@NotNull SimpleSpectrum spectrum, @Nullable SimpleSpectrum extractedIsotopePattern, Deviation ms1MassDiffDev){
        JsonObject spectra;
        if (extractedIsotopePattern != null && !extractedIsotopePattern.isEmpty()){
            spectra = ms1MirrorIsotope(spectrum, extractedIsotopePattern);
            annotatePeakMatches(spectra.get("spectra").getAsJsonArray(), matchPeaks(extractedIsotopePattern, spectrum, ms1MassDiffDev));
            spectra.get("spectra").getAsJsonArray().remove(1); // remove Isotope spectrum, peak matches are left
        } else
            spectra = jsonSpectraMs1(spectrum, "MS1");
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectra);
	}

	// MS2 spectrum w/o FragmentationTree
	public String ms2JSON(Ms2Experiment experiment, MutableMs2Spectrum spectrum) {
		JsonObject spectra = jsonSpectraMs2(experiment, spectrum, "MS2");
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(spectra);
	}

	// MS2 Spectrum with FragmentationTree (single)
	public String ms2JSON(Ms2Experiment experiment, MutableMs2Spectrum spectrum, FTree tree) {
		Fragment[] fragments = annotate(spectrum, tree);
		JsonObject jSpectrum = ms2Annotated(experiment, spectrum, fragments);
		annotatePeakPairs(jSpectrum, tree, fragments);
		final JsonObject j = new JsonObject();
		JsonArray spectra = new JsonArray();
		spectra.add(jSpectrum);
		j.add("spectra", spectra);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(j);
	}

	// MS2 Spectrum with FragmentationTree (single) MERGED
	public String ms2JSON(Ms2Experiment experiment, FTree tree) {
		final JsonArray jPeaks = ms2JsonPeaks(experiment, tree);
		final JsonObject jSpectrum = new JsonObject();
		jSpectrum.addProperty("name", "MS2 merged");
		jSpectrum.addProperty("parentmass", experiment.getIonMass());
		final JsonObject spectrumMetadata = new JsonObject(); // TODO
		jSpectrum.add("spectrumMetadata", spectrumMetadata);
		final JsonObject j = new JsonObject();
		jSpectrum.add("peaks", jPeaks);
		JsonArray spectra = new JsonArray();
		spectra.add(jSpectrum);
		j.add("spectra", spectra);
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(j);
	}

	protected JsonObject ms1MirrorIsotope(SimpleSpectrum pattern1, SimpleSpectrum pattern2) {
		final JsonObject j = new JsonObject();
		JsonObject spectrum1 = spectrum2json(pattern1);
		JsonObject spectrum2 = spectrum2json(pattern2);
		spectrum1.addProperty("name", "MS1");
		spectrum2.addProperty("name", "Sim. isotope pattern");
		JsonArray spectra = new JsonArray();
		spectra.add(spectrum1);
		spectra.add(spectrum2);
		j.add("spectra", spectra);
		return j;
	}

	protected JsonObject jsonSpectraMs1(Spectrum<? extends Peak> spectrum, String name){
		final JsonObject j = new JsonObject();
		JsonObject spectrum1 = spectrum2json(spectrum);
		spectrum1.addProperty("name", name);
		JsonArray spectra = new JsonArray();
		spectra.add(spectrum1);
		j.add("spectra", spectra);
		return j;
	}

	protected JsonObject jsonSpectraMs2(Ms2Experiment experiment, Spectrum<? extends Peak> spectrum, String name){
		final JsonObject j = new JsonObject();
		JsonObject spectrum1 = spectrum2json(spectrum);
		spectrum1.addProperty("name", name);
		spectrum1.addProperty("parentmass", experiment.getIonMass());
		JsonArray spectra = new JsonArray();
		spectra.add(spectrum1);
		j.add("spectra", spectra);
		return j;
	}

	protected JsonObject spectrum2json(Spectrum<? extends Peak> spectrum) {
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


	protected JsonObject ms2Annotated(Ms2Experiment experiment, Spectrum<? extends Peak> spectrum, Fragment[] fragments) {
		final JsonObject jSpectrum = new JsonObject();
		jSpectrum.addProperty("name", getSpectrumName(spectrum, "MS2"));
		jSpectrum.addProperty("parentmass", experiment.getIonMass());
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

	private String getSpectrumName(Spectrum<? extends Peak> spectrum, String fallback) {
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

	/*protected List<PeakMatch> matchPeaks(SiriusIsotopePattern siriusIsotopePattern, Deviation massDiffDev){
		return matchPeaks(siriusIsotopePattern.simulatedPattern, siriusIsotopePattern.spectrum, massDiffDev);
	}*/

	protected List<PeakMatch> matchPeaks(Spectrum<?> spectrum,  Spectrum<?> patterm, Deviation massDiffDev){
		List<PeakMatch> peakMatches = new LinkedList<>();
		for (int i = 0; i < spectrum.size(); i++) {
			Peak p = spectrum.getPeakAt(i);
			int j = Spectrums.mostIntensivePeakWithin(patterm, p.getMass(), massDiffDev);
			if (j >= 0) peakMatches.add(new PeakMatch(j, i));
		}
		return peakMatches;
	}

	protected JsonArray ms2JsonPeaks(Ms2Experiment experiment, FTree ftree) {
		final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
		final ProcessedInput processedInput = preprocessor.preprocess(experiment);
		List<ProcessedPeak> peaks = processedInput.getMergedPeaks();
		final JsonArray jPeaks = new JsonArray();
		for (ProcessedPeak peak : peaks) {
			JsonObject jPeak = new JsonObject();
			jPeak.addProperty("mz", peak.getMass());
			jPeak.addProperty("intensity", peak.getRelativeIntensity());
			JsonObject jPeakMetadata = new JsonObject();
			jPeakMetadata.addProperty("absoluteIntensity", peak.getIntensity());
			jPeak.add("peakMetadata", jPeakMetadata);
			JsonArray peakPairs = new JsonArray();
			jPeak.add("peakPairs", peakPairs);
			JsonObject peakMatches = new JsonObject();
			jPeak.add("peakMatches", peakMatches);
			jPeaks.add(jPeak);
		}
		if (ftree != null){
			double precursorMass = experiment.getMs2Spectra().get(0).getPrecursorMz();
			processedInput.mapTreeToInput(ftree);
			for (Fragment f: ftree){
				short i = f.getPeakId();
				if (i >= 0){
					JsonObject jPeak = jPeaks.get(i).getAsJsonObject();
					MolecularFormula formula = f.getFormula();
					if (formula != null)
						jPeak.addProperty("formula", formula.toString()
							+ " + " + f.getIonization().toString());
					// deviation (from FTJsonWriter tree2json)
					Deviation dev = ftree.getMassError(f);
					if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
						dev = ftree.getMassErrorTo(f, precursorMass);
					Deviation rdev = ftree.getRecalibratedMassError(f);
					if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
						rdev = ftree.getMassErrorTo(f, precursorMass);
					jPeak.addProperty("massDeviationMz", dev.getAbsolute());
					jPeak.addProperty("massDeviationPpm", dev.getPpm());
					jPeak.addProperty("recalibratedMassDeviationMz", rdev.getAbsolute());
					jPeak.addProperty("recalibratedMassDeviationPpm", rdev.getPpm());
					JsonArray jPairs = jPeak.get("peakPairs").getAsJsonArray();
					for (Loss l : f.getIncomingEdges()){
						short other_index = l.getSource().getPeakId();
						if (other_index >= 0){
							JsonObject pair = new JsonObject();
							pair.addProperty("index", other_index);
							pair.addProperty("formula", l.getFormula().toString());
							jPairs.add(pair);
						}
					}
					for (Loss l : f.getOutgoingEdges()){
						short other_index = l.getTarget().getPeakId();
						if (other_index >= 0){
							JsonObject pair = new JsonObject();
							pair.addProperty("index", other_index);
							pair.addProperty("formula", l.getFormula().toString());
							jPairs.add(pair);
						}
					}
				}
			}
		}
		return jPeaks;
	}

	// Copied from SingleSpectrumAnnotated
	private Fragment[] annotate(Spectrum<? extends Peak> spectrum, FTree tree) {
		final FragmentAnnotation<AnnotatedPeak> annotatedPeak;
		if (tree == null || (annotatedPeak = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class)) == null)
			return null;
		Fragment[] annotatedFormulas = new Fragment[spectrum.size()];
		BitSet isIsotopicPeak = new BitSet(spectrum.size());
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
