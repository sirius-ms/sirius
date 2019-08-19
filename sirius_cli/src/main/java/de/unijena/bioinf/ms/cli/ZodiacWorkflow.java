package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.GibbsSampling.ZodiacUtils;
import de.unijena.bioinf.GibbsSampling.model.CompoundResult;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;
import de.unijena.bioinf.GibbsSampling.model.LibraryHit;
import de.unijena.bioinf.GibbsSampling.model.ZodiacResultsWithClusters;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import net.sf.jniinchi.*;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ZodiacWorkflow implements Workflow<ExperimentResult> {

    private static final  org.slf4j.Logger LOG = LoggerFactory.getLogger(ZodiacWorkflow.class);
    private ZodiacOptions options;

    private ZodiacInstanceProcessor zodiacIP;

    @Override
    public boolean setup() {
        return zodiacIP.setup();
    }

    @Override
    public boolean validate() {
        return zodiacIP.validate();
    }


    public ZodiacWorkflow(ZodiacOptions options) {
        this.options = options;
        this.zodiacIP = new ZodiacInstanceProcessor(options);
    }

    @Override
    public void compute(Iterator<ExperimentResult> experimentResultIterator) {
        long starttime = System.currentTimeMillis();
        List<ExperimentResult> allExperimentResults = new ArrayList<>();
        while (experimentResultIterator.hasNext()) {
            allExperimentResults.add(experimentResultIterator.next());
        }
        //todo reads original experiments twice!
        try {
            Path outputPath = Paths.get(options.getOutput());
            if (options.getIsolationWindowWidth()!=null){
                double width = options.getIsolationWindowWidth();
                double shift = options.getIsolationWindowShift();
                allExperimentResults = zodiacIP.updateQuality(allExperimentResults, width, shift, outputPath);
            } else {
                allExperimentResults = zodiacIP.updateQuality(allExperimentResults, -1d, -1d, outputPath);
            }

        } catch (IOException e) {
            LOG.error("IOException while estimating data quality.", e);
            return;
        }

        LOG.info("reading data and updating quality took "+(System.currentTimeMillis()-starttime)+" ms");

        if (options.isOnlyComputeStats()){
            return;
        }


        if (options.getCrossvalidationNumberOfFolds()!=null){
            //eval on library anchors
            int numberOfFolds = options.getCrossvalidationNumberOfFolds();
            computeWithLibraryHitsInCrossvalidation(allExperimentResults, numberOfFolds);
        } else {
            //default
            starttime = System.currentTimeMillis();
            ZodiacJJob zodiacJJob = zodiacIP.makeZodiacJob(allExperimentResults);
            LOG.info("creating ZODIAC job took "+(System.currentTimeMillis()-starttime)+" ms");
            try {
                starttime = System.currentTimeMillis();
                ZodiacResultsWithClusters zodiacResults = SiriusJobs.getGlobalJobManager().submitJob(zodiacJJob).awaitResult();
                LOG.info("running ZODIAC job took "+(System.currentTimeMillis()-starttime)+" ms");
                if (zodiacResults==null) return; //no results. likely, empty input
                zodiacIP.writeResults(allExperimentResults, zodiacResults);
            } catch (ExecutionException e) {
                LOG.error("An error occurred while running ZODIAC.", e);
            }catch (IOException e) {
                LOG.error("Error writing ZODIAC output.", e);

            }
        }


    }


    /**
     * this only output the library hits an no other compounds
     * @param allExperimentResults
     */
    public void computeWithLibraryHitsInCrossvalidation(List<ExperimentResult> allExperimentResults, int numberOfBatches) {
        List<LibraryHit> anchors = null;
        Path libraryHitsFile = (options.getLibraryHitsFile() == null ? null : Paths.get(options.getLibraryHitsFile()));
        try {
            LOG.info("Parse library hits");
            anchors = (libraryHitsFile == null) ? null : ZodiacUtils.parseLibraryHits(libraryHitsFile, allExperimentResults, LOG); //GNPS and in-house format
        } catch (IOException e) {
            LOG.error("Cannot load library hits from file.", e);
        }

        if (anchors==null){
            LOG.error("Expected library hits. But could not parse any.");
            return;
        }

        int allHitsCount = anchors.size();
        anchors = extractHitsWithPositiveScore(anchors, options.getLowestCosine());
        LOG.info(String.format("extracted %d of %d library hits which can result in positive scores", anchors.size(), allHitsCount));
//        anchors = extractHitsWithPositiveScoreOld(anchors, options.getLowestCosine());
//        LOG.info(String.format("old:extracted %d of %d library hits which can result in positive scores", anchors.size(), allHitsCount));

        if (options.isExcludeBadQualityAnchors()){
            allHitsCount = anchors.size();
            anchors = extractHitsWithGoodQuality(anchors);
            LOG.info(String.format("extracted %d of %d library hits which have good quality", anchors.size(), allHitsCount));
        }


        List<LibraryHit>[] libraryBatches;
        try {
            LOG.info("create library hits batches");
            libraryBatches = disjointLibraryBatches(anchors, numberOfBatches, options.isMFDisjointCV(), options.isCompoundDisjointCV(), options.isRandomCV());
        } catch (CDKException e) {
            LOG.warn("Cannot create library hits batches");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            LOG.warn("Cannot create library hits batches");
            e.printStackTrace();
            return;
        }



        try {
            zodiacIP.writeCrossvalidationAnchors(libraryBatches);
        } catch (IOException e) {
            LOG.warn("Cannot write library hits batches to file.");
            e.printStackTrace();
            return;
        }

        //this is super inefficient since similarity graph is remcomputed angain and again. But so what. it is only for eval.
        List<String> zodiacEvalIds = new ArrayList<>();
        List<CompoundResult<FragmentsCandidate>> zodiacEvalResults = new ArrayList<>();

        for (int i = 0; i < libraryBatches.length+1; i++) {
            LOG.info(String.format("cross-validation round %d / %d", i+1, libraryBatches.length));

            List<LibraryHit> trainBatch;
            Set<String> queryIdSet;
            if (i<libraryBatches.length) {
                List<LibraryHit> evalBatch = libraryBatches[i];
                trainBatch = getTrainingBatches(libraryBatches, i);

                queryIdSet = getQueryIdSet(evalBatch);
            } else {
                if (!options.isComputeResultsForCompoundsWithoutLibraryHits()) break;
                trainBatch = anchors;
                Set<String> trainingIDs = getQueryIdSet(trainBatch);
                queryIdSet = new HashSet<>();
                for (ExperimentResult experimentResult : allExperimentResults) {
                    String name = experimentResult.getExperiment().getName();
                    if (!trainingIDs.contains(name)){
                        queryIdSet.add(name);
                    }
                }
                System.out.println("final round: training_size "+trainingIDs.size()+" | query_size "+queryIdSet.size());

            }

            ZodiacJJob zodiacJJob = zodiacIP.makeZodiacJob(allExperimentResults, trainBatch);

            try {
                ZodiacResultsWithClusters zodiacResults = SiriusJobs.getGlobalJobManager().submitJob(zodiacJJob).awaitResult();
                if (zodiacResults==null) {
                    LOG.error("No ZODIAC results. Was input empty?");
                    return;
                }

                String[] currentZodiacIds = zodiacResults.getIds();
                CompoundResult<FragmentsCandidate>[] currentZodiacResults = zodiacResults.getResults();
                for (int j = 0; j < currentZodiacIds.length; j++) {
                    String id = currentZodiacIds[j];
                    if (queryIdSet.contains(id)){
                        zodiacEvalIds.add(id);
                        zodiacEvalResults.add(currentZodiacResults[j]);
                    }
                }

            } catch (ExecutionException e) {
                LOG.error("An error occurred while running ZODIAC.", e);
                return;
            }
        }

        try {
            zodiacIP.writeResultsWithoutClusters(allExperimentResults, zodiacEvalIds.toArray(new String[0]), zodiacEvalResults.toArray(new CompoundResult[0]));
        } catch (IOException e) {
            LOG.error("Error writing ZODIAC output.", e);
            return;

        }

    }

    private List<LibraryHit> extractHitsWithPositiveScore(List<LibraryHit> anchors, double lowestCosine) {
        double cosineMalusForBiotransformations = 0.1;
//        double cosineMalusForBiotransformations = 0.0;
        List<LibraryHit> libraryHitsWhichCanProduceAPositiveScore = new ArrayList<>();
        for (LibraryHit libraryHit : anchors) {
            double libMz = libraryHit.getPrecursorMz();
            if (Math.abs(libraryHit.getQueryExperiment().getIonMass()-libMz)<=0.1){
                //this is no biotransformation
                if (libraryHit.getCosine()>lowestCosine) {
                    libraryHitsWhichCanProduceAPositiveScore.add(libraryHit);
                }
            } else {
                //this is with biotransformation; this is scored using a lowered cosine
                if (libraryHit.getCosine()>lowestCosine+cosineMalusForBiotransformations) {
                    libraryHitsWhichCanProduceAPositiveScore.add(libraryHit);
                }
            }
        }
        return libraryHitsWhichCanProduceAPositiveScore;
    }

    private List<LibraryHit> extractHitsWithPositiveScoreOld(List<LibraryHit> anchors, double lowestCosine) {
        double cosineMalusForBiotransformations = 0.1;
        List<LibraryHit> libraryHitsWhichCanProduceAPositiveScore = new ArrayList<>();
        for (LibraryHit libraryHit : anchors) {
            double libMz = libraryHit.getPrecursorMz();
            if (Math.abs(libraryHit.getQueryExperiment().getIonMass()-libMz)<=0.1){
                //this is no biotransformation
                if (libraryHit.getCosine()>lowestCosine) {
                    libraryHitsWhichCanProduceAPositiveScore.add(libraryHit);
                }
            } else {
                //this is with biotransformation; this is scored using a lowered cosine
                if (libraryHit.getCosine()>lowestCosine-cosineMalusForBiotransformations) {
                    libraryHitsWhichCanProduceAPositiveScore.add(libraryHit);
                }
            }
        }
        return libraryHitsWhichCanProduceAPositiveScore;
    }

    private List<LibraryHit> extractHitsWithGoodQuality(List<LibraryHit> anchors) {
        List<LibraryHit> libraryHitsWithGoodQualityExperiments = new ArrayList<>();
        for (LibraryHit libraryHit : anchors) {
            if (CompoundQuality.isNotBadQuality(libraryHit.getQueryExperiment())){
                libraryHitsWithGoodQualityExperiments.add(libraryHit);
            }
        }
        return libraryHitsWithGoodQualityExperiments;
    }

    private Set<String> getQueryIdSet(List<LibraryHit> libraryHits) {
        Set<String> idSet = new HashSet<>();
        for (LibraryHit libraryHit : libraryHits) {
            idSet.add(libraryHit.getQueryExperiment().getName());
        }
        return idSet;
    }
    private <T> List<T> getTrainingBatches(List<T>[] allBatches, int evalBatchNumber) {
        List<T> trainBatches = new ArrayList<>();
        for (int i = 0; i < allBatches.length; i++) {
            if (i==evalBatchNumber) continue;
            trainBatches.addAll(allBatches[i]);
        }
        return trainBatches;
    }


    /**
     *
     * @param libraryHits
     * @param numberOfBatches
     * @param isCompoundStructureDisjoint if false use library structure disjoint batches. if true only compound structures disjoint ( a library hit may match multiple compound structures with different biotranfsormations.)
     * @return
     * @throws CDKException
     * @throws IOException
     */
    public List<LibraryHit>[] disjointLibraryBatches(List<LibraryHit> libraryHits, int numberOfBatches, boolean isMolecularFormulaDisjoint, boolean isCompoundStructureDisjoint, boolean randomCV) throws CDKException, IOException {
        Map<String, List<LibraryHit>> keyToLibraryHits = new HashMap<>();
        for (LibraryHit libraryHit : libraryHits) {
            InChI inChI;
            if (!isMolecularFormulaDisjoint) {
                //structure information necessary
                String structure = libraryHit.getStructure();
                if (structure==null || structure.trim().length()==0){
                    LOG.warn("Unknown structure for "+libraryHit.getQueryExperiment().getName()+". Skip.");
                    continue;
                }


                try {
                    inChI = getInchiAndInchiKey(structure);
                } catch (CDKException e) {
                    LOG.warn("Cannot parse library hit for compound "+libraryHit.getQueryExperiment().getName()+". ");
                    throw e;
                } catch (IOException e) {
                    LOG.warn("Cannot parse library hit for compound "+libraryHit.getQueryExperiment().getName()+". ");
                    throw e;
                }
            } else {
                inChI = null;
            }


            String key;
            if (isMolecularFormulaDisjoint) {
                key = libraryHit.getMolecularFormula().formatByHill();
            } else if (isCompoundStructureDisjoint) {
                //this should result in compound structure disjoint batches
                key = inChI.key2D()+"|"+libraryHit.getMolecularFormula().formatByHill();
            } else {
                //library hit disjoint
                key = inChI.key2D();
            }
            List<LibraryHit> libraryHitsSameStructure = keyToLibraryHits.get(key);
            if (libraryHitsSameStructure==null) {
                libraryHitsSameStructure = new ArrayList<>();
                keyToLibraryHits.put(key, libraryHitsSameStructure);
            }

            libraryHitsSameStructure.add(libraryHit);
        }


        List<LibraryHit>[] batches = new ArrayList[numberOfBatches];
        for (int i = 0; i < batches.length; i++) {
            batches[i] = new ArrayList();
        }

        List<KeyWithNumberOfLibraryHits> keyWithNumberOfLibraryHitsList = new ArrayList<>();
        for (String key : keyToLibraryHits.keySet()) {
            keyWithNumberOfLibraryHitsList.add(new KeyWithNumberOfLibraryHits(key, keyToLibraryHits.get(key).size()));
        }

        if (randomCV){
            Collections.shuffle(keyWithNumberOfLibraryHitsList);
        } else {
            Collections.sort(keyWithNumberOfLibraryHitsList);
        }


//        //todo shuffle? this might result in different outcomes
//        List<String> allInchiKeys2D = new ArrayList<>(keyToLibraryHits.keySet());
//        Collections.shuffle(allInchiKeys2D);

        //distribute Library hits equally over batches by the number of different structures!
        int counter = 0;
        for (KeyWithNumberOfLibraryHits keyWithNumberOfLibraryHits : keyWithNumberOfLibraryHitsList) {
            String key = keyWithNumberOfLibraryHits.key;
            batches[counter%numberOfBatches].addAll(keyToLibraryHits.get(key));
            ++counter;
        }

        return batches;
    }

    private class KeyWithNumberOfLibraryHits implements Comparable<KeyWithNumberOfLibraryHits> {
        private String key;
        private int numberOfLibraryHits;

        public KeyWithNumberOfLibraryHits(String key, int numberOfLibraryHits) {
            //as default key is a 2D inchikey
            this.key = key;
            this.numberOfLibraryHits = numberOfLibraryHits;
        }


        @Override
        public int compareTo(@NotNull ZodiacWorkflow.KeyWithNumberOfLibraryHits o) {
            //first highest number of hits (to evenly distribute library hits on batches)
            if (numberOfLibraryHits!=o.numberOfLibraryHits) return -Integer.compare(numberOfLibraryHits, o.numberOfLibraryHits);
            //second by inchikey hash
            return key.compareTo(o.key);
        }
    }






    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //todo replace with code from InChI Utils when branches have been merged
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static IAtomContainer getAtomContainer(String someStructureFormat) throws CDKException, IOException {
        if (isInchi(someStructureFormat)) {
            return InChIGeneratorFactory.getInstance().getInChIToStructure(someStructureFormat, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
        } else if (someStructureFormat.contains("\n")) {
            //it is a structure format from some file
            ReaderFactory readerFactory = new ReaderFactory();
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
            ISimpleChemObjectReader reader = readerFactory.createReader(in);
//            MDLV2000Reader reader = new MDLV2000Reader();
//            reader.setReader(in);
            if (reader==null) {
                in.close();
                //try with another new-line
                someStructureFormat += "\n";
                in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
                reader = readerFactory.createReader(in);
            }
            if (reader==null) {
                in.close();
                throw new IOException("No reader found for given format");
            }else if (reader.accepts(ChemFile.class)) {
                ChemFile cfile = new ChemFile();
                cfile = reader.read(cfile);
                List<IAtomContainer> atomContainerList = ChemFileManipulator.getAllAtomContainers(cfile);

                if (atomContainerList.size()>1){
                    throw new IOException("Multiple structures in input");
                } else if (atomContainerList.size()==0){
                    throw new IOException("Could not parse any structure");
                }
                return atomContainerList.get(0);

            } else {
                throw new IOException("Unknown format");
            }
        } else {
            //assume SMILES
            //todo do we need to do any processing?!?
            SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            IAtomContainer iAtomContainer = smilesParser.parseSmiles(someStructureFormat);
            return iAtomContainer;
        }
    }

    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static InChI getInchiAndInchiKey(String someStructureFormat) throws CDKException, IOException {
        if (isInchi(someStructureFormat)) {
            if (!isStandardInchi(someStructureFormat)) {
                someStructureFormat = getStdInchi(someStructureFormat);
            }
            String key = inchi2inchiKey(someStructureFormat);
            return new InChI(key, someStructureFormat);
        } else {
            return getInchi(getAtomContainer(someStructureFormat));
        }
    }

    public static boolean isInchi(String s ) {
        return s.trim().startsWith("InChI=");
    }


    private static boolean isStandardInchi(String inChI) {
        return inChI.startsWith("InChI=1S/");
    }


    public static String inchi2inchiKey(String inchi) {

        //todo isotopes in inchiKey14 bug removed with latest version 2.2?
        try {
            if (inchi==null) throw new NullPointerException("Given InChI is null");
            if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
            JniInchiOutputKey key = JniInchiWrapper.getInchiKey(inchi);
            if(key.getReturnStatus() == INCHI_KEY.OK) {
                return key.getKey();
            } else {
                throw new RuntimeException("Error while creating InChIKey: " + key.getReturnStatus());
            }
        } catch (JniInchiException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getStdInchi(String inchi) {
        try {
            if (inchi==null) throw new NullPointerException("Given InChI is null");
            if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");

            JniInchiInputInchi inputInchi = new JniInchiInputInchi(inchi);
            JniInchiStructure structure = JniInchiWrapper.getStructureFromInchi(inputInchi);
            JniInchiInput input = new JniInchiInput(structure);
            JniInchiOutput output = JniInchiWrapper.getStdInchi(input);
            if(output.getReturnStatus() == INCHI_RET.WARNING) {
                LOG.warn("Warning issued while computing standard InChI: "+output.getMessage());
                return output.getInchi();
            } else if(output.getReturnStatus() == INCHI_RET.OKAY) {
                return output.getInchi();
            } else {
                throw new RuntimeException("Error while computing standard InChI: " + output.getReturnStatus()
                        +"\nError message: "+output.getMessage());
            }
        } catch (JniInchiException e) {
            throw new RuntimeException(e);
        }
    }

    public static InChI getInchi(IAtomContainer atomContainer) throws CDKException {
        //todo does getInChIGenerator need any specific options!?!?!?
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer);

        String inchi = inChIGenerator.getInchi();
        if (inchi==null) return null;
        String key = inChIGenerator.getInchiKey();
        return new InChI(key, inchi);
    }


}
