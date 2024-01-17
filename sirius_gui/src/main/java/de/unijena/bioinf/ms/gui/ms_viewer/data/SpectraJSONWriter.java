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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
		ObjectNode matchMetadata;

		public PeakMatch(int index1, int index2, ObjectNode matchMetadata){
			this.index1 = index1;
			this.index2 = index2;
			this.matchMetadata = matchMetadata;
		}

		public PeakMatch(int index1, int index2){
			this(index1, index2, JsonNodeFactory.instance.objectNode());
		}
	}

	// MS1 vs. simulated MS1 isotope pattern (mirror)
	public String ms1MirrorJSON(@NotNull SiriusIsotopePattern siriusIsotopePattern, Deviation ms1MassDiffDev) {
		SimpleSpectrum spectrum = new SimpleSpectrum(siriusIsotopePattern.spectrum);
		ObjectNode spectra = ms1MirrorIsotope(spectrum, siriusIsotopePattern.simulatedPattern);
		annotatePeakMatches(spectra.withArray("spectra"), matchPeaks(siriusIsotopePattern.simulatedPattern, spectrum, ms1MassDiffDev));
		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(spectra);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error generating JSON", e);
		}
	}

	public String ms2MirrorJSON(SimpleSpectrum query, SimpleSpectrum match, String queryName, String matchName) {
		ObjectNode spectra = ms2Mirror(query, match, queryName, matchName);
		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(spectra);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error generating JSON", e);
		}
	}

	// MS1 spectrum (single)
	public String ms1JSON(@NotNull SimpleSpectrum spectrum, @Nullable SimpleSpectrum extractedIsotopePattern, Deviation ms1MassDiffDev){
		ObjectNode spectra;
		if (extractedIsotopePattern != null && !extractedIsotopePattern.isEmpty()){
			spectra = ms1MirrorIsotope(spectrum, extractedIsotopePattern);
			annotatePeakMatches(spectra.withArray("spectra"), matchPeaks(extractedIsotopePattern, spectrum, ms1MassDiffDev));
			spectra.withArray("spectra").remove(1); // remove Isotope spectrum, peak matches are left
		} else
			spectra = jsonSpectraMs1(spectrum, "MS1");
		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(spectra);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error generating JSON", e);
		}
	}

	// MS2 spectrum w/o FragmentationTree
	public String ms2JSON(Ms2Experiment experiment, MutableMs2Spectrum spectrum) {
		ObjectNode spectra = jsonSpectraMs2(experiment, spectrum, "MS2");
		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(spectra);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error generating JSON", e);
		}
	}

	// MS2 Spectrum with FragmentationTree (single)
	public String ms2JSON(Ms2Experiment experiment, MutableMs2Spectrum spectrum, FTree tree) {
		Fragment[] fragments = annotate(spectrum, tree);
		ObjectNode jSpectrum = ms2Annotated(experiment, spectrum, fragments);
		annotatePeakPairs(jSpectrum, tree, fragments);
		final ObjectNode j = JsonNodeFactory.instance.objectNode();
		ArrayNode spectra = JsonNodeFactory.instance.arrayNode();
		spectra.add(jSpectrum);
		j.set("spectra", spectra);
		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(j);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// MS2 Spectrum with FragmentationTree (single) MERGED
	public String ms2JSON(Ms2Experiment experiment, FTree tree) {
		final ArrayNode jPeaks = ms2JsonPeaks(experiment, tree);
		final ObjectNode jSpectrum = JsonNodeFactory.instance.objectNode();
		jSpectrum.put("name", "MS2 merged");
		jSpectrum.put("parentmass", experiment.getIonMass());
		final ObjectNode spectrumMetadata = JsonNodeFactory.instance.objectNode(); // TODO
		jSpectrum.set("spectrumMetadata", spectrumMetadata);
		final ObjectNode j = JsonNodeFactory.instance.objectNode();
		jSpectrum.set("peaks", jPeaks);
		ArrayNode spectra = JsonNodeFactory.instance.arrayNode();
		spectra.add(jSpectrum);
		j.set("spectra", spectra);

		final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		try {
			return objectMapper.writeValueAsString(j);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		// Return null or a default value in case of an error
		return null;
	}



	protected ObjectNode ms1MirrorIsotope(SimpleSpectrum pattern1, SimpleSpectrum pattern2) {
		final ObjectMapper objectMapper = new ObjectMapper();
		final ObjectNode j = objectMapper.createObjectNode();

		ObjectNode spectrum1 = spectrum2json(pattern1);
		ObjectNode spectrum2 = spectrum2json(pattern2);
		spectrum1.put("name", "MS1");
		spectrum2.put("name", "Sim. isotope pattern");

		ArrayNode spectra = objectMapper.createArrayNode();
		spectra.add(spectrum1);
		spectra.add(spectrum2);

		j.set("spectra", spectra);

		return j;
	}

	protected ObjectNode ms2Mirror(SimpleSpectrum query, SimpleSpectrum match, String queryName, String matchName) {
		final ObjectMapper objectMapper = new ObjectMapper();
		final ObjectNode j = objectMapper.createObjectNode();

		ObjectNode spectrum1 = spectrum2json(query);
		ObjectNode spectrum2 = spectrum2json(match);
		spectrum1.put("name", queryName);
		spectrum2.put("name", matchName);

		ArrayNode spectra = objectMapper.createArrayNode();
		spectra.add(spectrum1);
		spectra.add(spectrum2);

		j.set("spectra", spectra);

		return j;
	}


	protected ObjectNode jsonSpectraMs1(Spectrum<? extends Peak> spectrum, String name) {
		final ObjectNode j = JsonNodeFactory.instance.objectNode();
		ObjectNode spectrum1 = spectrum2json(spectrum);
		spectrum1.put("name", name);
		ArrayNode spectra = JsonNodeFactory.instance.arrayNode();
		spectra.add(spectrum1);
		j.set("spectra", spectra);
		return j;
	}

	protected ObjectNode jsonSpectraMs2(Ms2Experiment experiment, Spectrum<? extends Peak> spectrum, String name){
		final ObjectNode j = JsonNodeFactory.instance.objectNode();
		ObjectNode spectrum1 = spectrum2json(spectrum);
		spectrum1.put("name", name);
		spectrum1.put("parentmass", experiment.getIonMass());
		ArrayNode spectra = JsonNodeFactory.instance.arrayNode();
		spectra.add(spectrum1);
		j.set("spectra", spectra);
		return j;
	}

	protected ObjectNode spectrum2json(Spectrum<? extends Peak> spectrum) {
		final ObjectNode jSpectrum = JsonNodeFactory.instance.objectNode();

		jSpectrum.put("name", "");

		final ObjectNode spectrumMetadata = JsonNodeFactory.instance.objectNode();
		jSpectrum.set("spectrumMetadata", spectrumMetadata);

		double scale = spectrum.getMaxIntensity();
		final ArrayNode peaks = JsonNodeFactory.instance.arrayNode();

		for (int i = 0; i < spectrum.size(); i++) {
			ObjectNode peak = JsonNodeFactory.instance.objectNode();
			peak.put("mz", spectrum.getMzAt(i));
			double intensity = spectrum.getIntensityAt(i);
			peak.put("intensity", intensity / scale);

			final ObjectNode peakMetadata = JsonNodeFactory.instance.objectNode();
			peakMetadata.put("absoluteIntensity", intensity);
			peak.set("peakMetadata", peakMetadata);

			final ArrayNode peakPairs = JsonNodeFactory.instance.arrayNode();
			peak.set("peakPairs", peakPairs);

			final ObjectNode peakMatches = JsonNodeFactory.instance.objectNode();
			peak.set("peakMatches", peakMatches);

			peaks.add(peak);
		}

		jSpectrum.set("peaks", peaks);

		return jSpectrum;
	}

	@Deprecated
	protected ObjectNode spectrum2json(MSViewerDataModel dmodel) {
		final ObjectMapper objectMapper = new ObjectMapper();
		final ObjectNode jSpectrum = objectMapper.createObjectNode();

		jSpectrum.put("name", "");

		final ObjectNode spectrumMetadata = objectMapper.createObjectNode();
		jSpectrum.set("spectrumMetadata", spectrumMetadata);

		final ArrayNode peaks = objectMapper.createArrayNode();
		for (int i = 0; i < dmodel.getSize(); i++) {
			final ObjectNode peak = objectMapper.createObjectNode();
			peak.put("mz", dmodel.getMass(i));
			peak.put("intensity", dmodel.getRelativeIntensity(i));

			final ObjectNode peakMetadata = objectMapper.createObjectNode();
			peakMetadata.put("absoluteIntensity", dmodel.getAbsoluteIntensity(i));
			peakMetadata.put("formula", dmodel.getMolecularFormula(i));
			peakMetadata.put("ionization", dmodel.getIonization(i));
			peak.set("peakMetadata", peakMetadata);

			final ArrayNode peakPairs = objectMapper.createArrayNode();
			peak.set("peakPairs", peakPairs);

			final ObjectNode peakMatches = objectMapper.createObjectNode();
			peak.set("peakMatches", peakMatches);

			peaks.add(peak);
		}

		jSpectrum.set("peaks", peaks);

		return jSpectrum;
	}


	protected ObjectNode ms2Annotated(Ms2Experiment experiment, Spectrum<? extends Peak> spectrum, Fragment[] fragments) {
		final ObjectMapper mapper = new ObjectMapper();
		final ObjectNode jSpectrum = mapper.createObjectNode();

		jSpectrum.put("name", getSpectrumName(spectrum, "MS2"));
		jSpectrum.put("parentmass", experiment.getIonMass());

		final ObjectNode spectrumMetadata = mapper.createObjectNode();
		jSpectrum.set("spectrumMetadata", spectrumMetadata);

		double scale = spectrum.getMaxIntensity();
		final ArrayNode peaks = mapper.createArrayNode();
		for (int i = 0; i < spectrum.size(); i++) {
			final ObjectNode peak = mapper.createObjectNode();
			peak.put("mz", spectrum.getMzAt(i));
			double intensity = spectrum.getIntensityAt(i);
			peak.put("intensity", intensity / scale);
			Fragment fragment = fragments[i];
			MolecularFormula formula;
			if (fragment != null && (formula = fragment.getFormula()) != null) {
				peak.put("formula", formula.toString());
			}

			final ObjectNode peakMetadata = mapper.createObjectNode();
			peakMetadata.put("absoluteIntensity", intensity);
			peak.set("peakMetadata", peakMetadata);

			final ArrayNode peakPairs = mapper.createArrayNode();
			peak.set("peakPairs", peakPairs);

			final ObjectNode peakMatches = mapper.createObjectNode();
			peak.set("peakMatches", peakMatches);

			peaks.add(peak);
		}

		jSpectrum.set("peaks", peaks);

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
	protected void annotatePeakPairs(ObjectNode jSpectrum, FTree tree, Fragment[] fragments) {
		Fragment u, v;
		MolecularFormula lossFormula;
		ArrayNode peaks = (ArrayNode) jSpectrum.get("peaks");
		for (int ui = 0; ui < fragments.length; ++ui) {
			if ((u = fragments[ui]) == null) {
				continue;
			}
			ArrayNode uPairs = (ArrayNode) peaks.get(ui).get("peakPairs");
			for (int vi = ui + 1; vi < fragments.length; ++vi) {
				if ((v = fragments[vi]) == null) {
					continue;
				}
				ArrayNode vPairs = (ArrayNode) peaks.get(vi).get("peakPairs");
				Loss loss;
				try {
					loss = tree.getLoss(u, v);
				} catch (IndexOutOfBoundsException e) {
					loss = null;
				}
				if (loss == null) {
					try {
						// if fragments are always sorted by mass, this should be the correct order
						loss = tree.getLoss(v, u);
					} catch (IndexOutOfBoundsException e) {
						loss = null;
					}
				}
				final ObjectMapper mapper = new ObjectMapper();
				if (loss != null && (lossFormula = loss.getFormula()) != null) {
					ObjectNode uPair = mapper.createObjectNode();
					ObjectNode vPair = mapper.createObjectNode();
					uPair.put("index", vi);
					vPair.put("index", ui);
					ObjectNode metadata = mapper.createObjectNode();
					metadata.put("formula", lossFormula.toString());
					uPair.set("metadata", metadata);
					vPair.set("metadata", metadata);
					uPairs.add(uPair);
					vPairs.add(vPair);
				}
			}
		}
	}

	// Peak matches *between* spectra
	protected void annotatePeakMatches(ArrayNode spectra, List<PeakMatch> matches) {
		final ObjectMapper mapper = new ObjectMapper();

		ArrayNode pattern1Peaks = (ArrayNode) spectra.get(0).get("peaks");
		ArrayNode pattern2Peaks = (ArrayNode) spectra.get(1).get("peaks");
		for (PeakMatch m : matches) {
			ObjectNode p1Matches = (ObjectNode) pattern1Peaks.get(m.index1).get("peakMatches");
			p1Matches.put("index", m.index2);
			p1Matches.set("metadata", mapper.createObjectNode());
			ObjectNode p2Matches = (ObjectNode) pattern2Peaks.get(m.index2).get("peakMatches");
			p2Matches.put("index", m.index1);
			p2Matches.set("metadata", mapper.createObjectNode());
		}
	}
	protected List<PeakMatch> matchPeaks(Spectrum<?> spectrum,  Spectrum<?> patterm, Deviation massDiffDev){
		List<PeakMatch> peakMatches = new LinkedList<>();
		for (int i = 0; i < spectrum.size(); i++) {
			Peak p = spectrum.getPeakAt(i);
			int j = Spectrums.mostIntensivePeakWithin(patterm, p.getMass(), massDiffDev);
			if (j >= 0) peakMatches.add(new PeakMatch(j, i));
		}
		return peakMatches;
	}

	protected ArrayNode ms2JsonPeaks(Ms2Experiment experiment, FTree ftree) {
		final ObjectMapper mapper = new ObjectMapper();

		final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
		final ProcessedInput processedInput = preprocessor.preprocess(experiment);
		List<ProcessedPeak> peaks = processedInput.getMergedPeaks();
		final ArrayNode jPeaks = mapper.createArrayNode();
		for (ProcessedPeak peak : peaks) {
			ObjectNode jPeak = mapper.createObjectNode();
			jPeak.put("mz", peak.getMass());
			jPeak.put("intensity", peak.getRelativeIntensity());
			ObjectNode jPeakMetadata = mapper.createObjectNode();
			jPeakMetadata.put("absoluteIntensity", peak.getIntensity());
			jPeak.set("peakMetadata", jPeakMetadata);
			ArrayNode peakPairs = mapper.createArrayNode();
			jPeak.set("peakPairs", peakPairs);
			ObjectNode peakMatches = mapper.createObjectNode();
			jPeak.set("peakMatches", peakMatches);
			jPeaks.add(jPeak);
		}
		if (ftree != null){
			double precursorMass = experiment.getMs2Spectra().get(0).getPrecursorMz();
			processedInput.mapTreeToInput(ftree);
			for (Fragment f: ftree){
				short i = f.getPeakId();
				if (i >= 0){
					ObjectNode jPeak = (ObjectNode) jPeaks.get(i);
					MolecularFormula formula = f.getFormula();
					if (formula != null)
						jPeak.put("formula", formula.toString() + " + " + f.getIonization().toString());
					// deviation (from FTJsonWriter tree2json)
					Deviation dev = ftree.getMassError(f);
					if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
						dev = ftree.getMassErrorTo(f, precursorMass);
					Deviation rdev = ftree.getRecalibratedMassError(f);
					if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
						rdev = ftree.getMassErrorTo(f, precursorMass);
					jPeak.put("massDeviationMz", dev.getAbsolute());
					jPeak.put("massDeviationPpm", dev.getPpm());
					jPeak.put("recalibratedMassDeviationMz", rdev.getAbsolute());
					jPeak.put("recalibratedMassDeviationPpm", rdev.getPpm());
					ArrayNode jPairs = (ArrayNode) jPeak.get("peakPairs");
					for (Loss l : f.getIncomingEdges()){
						short other_index = l.getSource().getPeakId();
						if (other_index >= 0){
							ObjectNode pair = mapper.createObjectNode();
							pair.put("index", other_index);
							pair.put("formula", l.getFormula().toString());
							jPairs.add(pair);
						}
					}
					for (Loss l : f.getOutgoingEdges()){
						short other_index = l.getTarget().getPeakId();
						if (other_index >= 0){
							ObjectNode pair = mapper.createObjectNode();
							pair.put("index", other_index);
							pair.put("formula", l.getFormula().toString());
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

		return annotatedFormulas;
    }

}
