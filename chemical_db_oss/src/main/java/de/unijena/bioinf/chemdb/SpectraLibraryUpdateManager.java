package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceFragmentationTree;
import de.unijena.bionf.fastcosine.FastCosine;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SpectraLibraryUpdateManager {

    record Index (String inchikey, PrecursorIonType ionType) {};

    private final WriteableSpectralLibrary libraryWriter;
    private final SpectralLibrary libraryReader;
    private final Set<Index> updatedIndices;
    private final static FastCosine fastCosine = new FastCosine();

    public SpectraLibraryUpdateManager(WriteableSpectralLibrary libraryWriter, SpectralLibrary libraryReader) {
        this.libraryWriter = libraryWriter;
        this.libraryReader = libraryReader;
        this.updatedIndices = new HashSet<>();
    }

    public int addSpectra(List<Ms2ReferenceSpectrum> data) throws IOException {
        data.forEach(spec->{
            if (spec.getSearchPreparedSpectrum()==null) {
                spec.setSearchPreparedSpectrum(fastCosine.prepareQuery(spec.getPrecursorMz(), spec.getSpectrum()));
            }
        });
        int r = this.libraryWriter.upsertSpectra(data);
        synchronized (this) {
            for (Ms2ReferenceSpectrum spec : data) {
                if (spec.getCandidateInChiKey() != null) {
                    updatedIndices.add(new Index(spec.getCandidateInChiKey(), spec.getPrecursorIonType()));
                }
            }
        }
        return r;
    }

    /**
     * has to be called after all spectra are inserted. Returns a job that performs maintaining stuff.
     */
    public JJob<Object> finishWriting() {
        final Index[] toUpdate;
        synchronized (this) {
            toUpdate = updatedIndices.toArray(Index[]::new);
            updatedIndices.clear();
        }
        return new Maintainance(libraryWriter,  libraryReader, toUpdate);
    }

    protected static class Maintainance extends BasicMasterJJob<Object> {
        private final WriteableSpectralLibrary writer;
        private final SpectralLibrary reader;
        private final Index[] toUpdate;
        private final Sirius sirius;
        public Maintainance(WriteableSpectralLibrary writer, SpectralLibrary reader, Index[] toUpdate) {
            super(JobType.CPU);
            this.writer = writer;
            this.reader = reader;
            this.toUpdate = toUpdate;
            this.sirius = new Sirius();
        }

        @Override
        protected Object compute() throws Exception {
            for (Index index : toUpdate) {
                submitSubJob(new BasicMasterJJob<Object>(JobType.CPU) {
                    @Override
                    protected Object compute() throws Exception {
                        // 1.) collect all spectra with same index
                        List<Ms2ReferenceSpectrum> specs = new ArrayList<>();
                        List<Ms2ReferenceSpectrum> updateSp = new ArrayList<>();
                        for (Ms2ReferenceSpectrum spec : reader.lookupSpectra(index.inchikey, true)) {
                            if (spec.getPrecursorIonType().equals(index.ionType)) {
                                // minimum fields required:
                                if ((spec.getSpectrum()!=null || spec.getSearchPreparedSpectrum()!=null) && spec.getFormula()!=null) {
                                    if (spec.getSearchPreparedSpectrum()==null) {
                                        spec.setSearchPreparedSpectrum(fastCosine.prepareQuery(spec.getPrecursorMz(), spec.getSpectrum()));
                                        updateSp.add(spec);
                                    }
                                    specs.add(spec);
                                }
                            }
                        }
                        if (specs.isEmpty()) return null;
                        if (!updateSp.isEmpty()) writer.upsertSpectra(updateSp);
                        // 2.) compute merged spectrum
                        Ms2Experiment experiment = toExperiment(index, specs);
                        // 3.) compute fragmentation tree
                        List<IdentificationResult> identificationResult = submitSubJob(sirius.makeIdentificationJob(experiment)).takeResult();
                        FTree tree = !identificationResult.isEmpty() ? identificationResult.getFirst().getTree() : null;
                        if (tree == null) {
                            LoggerFactory.getLogger("Cannot compute tree for " + index);
                            tree = new FTree(experiment.getMolecularFormula(), index.ionType.getIonization());
                        }

                        String name;
                        String smiles;
                        // try to use name from structure instead of arbitrary spectrum because that is the 2d representative
                        if (reader instanceof AbstractChemicalDatabase chemDb){
                            CompoundCandidate structure = chemDb.lookupStructuresByInChI(index.inchikey);
                            name = structure.getName();
                            smiles = structure.getSmiles();
                        } else {
                            Ms2ReferenceSpectrum metaDataReference = specs.stream()
                                    .filter(ref -> ref.getSmiles() != null)
                                    .findFirst().orElse(null);
                            name = metaDataReference.getName();
                            smiles = metaDataReference.getSmiles();
                        }

                        MergedReferenceSpectrum merged = new MergedReferenceSpectrum();
                        merged.setName(name);
                        merged.setFormula(specs.getFirst().getFormula());
                        merged.setSmiles(smiles);
                        merged.setPrecursorMz(experiment.getIonMass());
                        merged.setExactMass(index.ionType.neutralMassToPrecursorMass(merged.getFormula().getMass()));
                        merged.setCandidateInChiKey(index.inchikey);
                        merged.setPrecursorIonType(index.ionType);
                        merged.setIndividualSpectraUIDs(specs.stream().mapToLong(Ms2ReferenceSpectrum::getUuid).toArray());
                        merged.setSearchPreparedSpectrum(fastCosine.prepareMergedQuery(specs.stream().map(Ms2ReferenceSpectrum::getSearchPreparedSpectrum).toList()));
                        ReferenceFragmentationTree refTree = ReferenceFragmentationTree.from(tree, merged);
                        // submit to database
                        writer.insertMergedSpecAndTree(merged, refTree);
                        return null;
                    }
                });
            }
            return null;
        }
    }

    private static Ms2Experiment toExperiment(Index index, List<Ms2ReferenceSpectrum> spectra) {
        MutableMs2Experiment experiment = new MutableMs2Experiment();
        experiment.setMs2Spectra(spectra.stream().map(x->new MutableMs2Spectrum(x.getSpectrum()==null ? x.getSearchPreparedSpectrum() : x.getSpectrum(), x.getPrecursorMz(), x.getCollisionEnergy(), x.getMsLevel())).toList());
        MolecularFormula formula = spectra.getFirst().getFormula();
        experiment.setIonMass(index.ionType.neutralMassToPrecursorMass(formula.getMass()));
        experiment.setMolecularFormula(formula);
        experiment.setAnnotation(Whiteset.class, Whiteset.ofNeutralizedFormulas(Set.of(experiment.getMolecularFormula()),SpectraLibraryUpdateManager.class));
        experiment.setPrecursorIonType(index.ionType);
        return experiment;
    }


}
