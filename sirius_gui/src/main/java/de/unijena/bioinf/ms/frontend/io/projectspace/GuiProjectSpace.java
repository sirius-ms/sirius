package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.projectspace.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiProjectSpace {

    public final BasicEventList<InstanceBean> COMPOUNT_LIST = new BasicEventList<>();

    //todo ringbuffer???
    private final Map<CompoundContainerId, InstanceBean> idToWrapperBean = new ConcurrentHashMap<>();

    private ProjectSpaceManager projectSpace;

    public GuiProjectSpace(File projectSpaceLocation) {
        try {
            if (!projectSpaceLocation.exists()) {
                if (!projectSpaceLocation.mkdir())
                    throw new IOException("Could not create new directory for project-space'" + projectSpaceLocation + "'");
            }

            final SiriusProjectSpace psTmp = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(projectSpaceLocation);

            //check for formatter
            StandardMSFilenameFormatter projectSpaceFilenameFormatter;
            try {
                projectSpaceFilenameFormatter = psTmp.getProjectSpaceProperty(FilenameFormatter.PSProperty.class).map(it -> new StandardMSFilenameFormatter(it.formatExpression)).orElse(new StandardMSFilenameFormatter());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).debug("Could not Parse filenameformatter -> Using default", e);
                LoggerFactory.getLogger(getClass()).warn("Could not Parse filenameformatter -> Using default");
                projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();
            }
            //todo when do we write this?
            psTmp.setProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSProperty(projectSpaceFilenameFormatter));

            projectSpace = new ProjectSpaceManager(psTmp, projectSpaceFilenameFormatter, null);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).debug("Could not Parse create ProjectSpace. Try creating Temproray one", e);
            LoggerFactory.getLogger(getClass()).warn("Could not Parse create ProjectSpace. Try creating Temproray one");
            try {
                projectSpace = new ProjectSpaceManager(new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createTemporaryProjectSpace());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        projectSpace.projectSpace().defineCompoundListener().onCreate().thenDo((it) -> {
            //todo projectsapce listener for compound creation an deletion
            //todo we may need so kind of Instance factory heres
//            idToWrapperBean.put(it, ))
        });

    }

    /*public void importCompound(@NotNull final ExperimentResultBean ec) {
        SwingUtilities.invokeLater(() -> {
            cleanExperiment(ec.getMs2Experiment());

            //adding experiment to gui
            addToCompoundList(ec);

            //listen to changes to decide if a compound has to be rewritten
            addListener(ec);

            //write experiment to project-space
            writeToProjectSpace(ec.getExperimentResult());

        });
    }

    //todo make thread safe and check property changes
    private void addListener(final ExperimentResultBean ec) {
        ec.addPropertyChangeListener(evt -> {
            changed.add(ec.getExperimentResult());
        });
    }

    public void importCompound(@NotNull final MutableMs2Experiment ex) {
        importCompound(new ExperimentResultBean(ex));
    }

    private void addToCompoundList(@NotNull final ExperimentResultBean ec) {
        addName(ec);
        COMPOUNT_LIST.add(ec);
    }

    public void writeToProjectSpace(@NotNull final ExperimentResult... exResults) {
        writeToProjectSpace(Arrays.asList(exResults).iterator());
    }

    public void writeToProjectSpace(@NotNull final Iterator<ExperimentResult> exResults) {
        SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ExperimentDirectory>(JJob.JobType.IO) {
            @Override
            protected ExperimentDirectory compute() throws Exception {
                while (exResults.hasNext()) {
                    final ExperimentResult exResult = exResults.next();
                    projectSpace.writeExperiment(exResult);
                    changed.remove(exResult); //todo check locking
                }
                return null;
            }
        });
    }

    public void writeChangedCompounds() {
        writeToProjectSpace(changed.iterator());
    }

    //todo check
    public void writeSummary() {
        writeChangedCompounds();
        projectSpace.writeSummaries(COMPOUNT_LIST.stream().map(ExperimentResultBean::getExperimentResult).collect(Collectors.toList()));
    }


    private void cleanExperiment(@NotNull final MutableMs2Experiment ex) {
        //search for a missing molecular formula before cleaning the annotations
        if (ex.getMolecularFormula() == null) {
            String f = extractMolecularFormulaString(ex);
            if (f != null && !f.isEmpty())
                ex.setMolecularFormula(MolecularFormula.parseOrThrow(f));
        }

        if (ex.getPrecursorIonType() == null) {
            LoggerFactory.getLogger(getClass()).warn("Input experiment with name '" + ex.getName() + "' does not have a charge nor an ion type annotation.");
            ex.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(1));
        }
        clearExperimentAnotations(ex);
        addIonToPeriodicTable(ex.getPrecursorIonType());
    }

    public void remove(ExperimentResultBean... containers) {
        remove(Arrays.asList(containers));
    }

    public void remove(List<ExperimentResultBean> containers) {
        for (ExperimentResultBean ec : containers) {
            NAMES.remove(ec.getGUIName());
            try {
                projectSpace.deleteExperiment(ec.getProjectSpaceID());
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Could not delete Compound: " + ec.getGUIName(), e);
            }
        }
        COMPOUNT_LIST.removeAll(containers);
    }

    public void clear() {
        remove(new ArrayList<>(COMPOUNT_LIST));
    }

    // region static import helper methods
    public void changeName(ExperimentResultBean ec, String old) {
        if (NAMES.containsKey(old)) {
            TIntSet indeces = NAMES.get(old);
            indeces.remove(ec.getNameIndex());

            if (indeces.isEmpty())
                NAMES.remove(old);
        }
        addName(ec);
    }

    public void addName(ExperimentResultBean ec) {
        if (ec.getName() == null || ec.getName().isEmpty()) {
            ec.setName("Unknown");
        } else {
            final TIntSet indeces = NAMES.putIfAbsent(ec.getName(), new TIntHashSet());
            assert indeces != null;

            int counter = 1;
            while (indeces.contains(counter))
                counter++;
            indeces.add(counter);

            ec.setNameIndex(counter);
        }
    }

    //todo check why this is located here
    public static String extractMolecularFormulaString(Ms2Experiment exp) {
        MolecularFormula formula = exp.getMolecularFormula();
        if (formula != null) {
            return formula.toString();
        }

        if (exp.hasAnnotation(InChI.class)) {
            InChI inChI = exp.getAnnotation(InChI.class);
            formula = inChI.extractFormulaOrThrow();
            return formula.toString();

        }

        if (exp.hasAnnotation(Smiles.class)) {
            Smiles smiles = exp.getAnnotation(Smiles.class);
            try {
                final IAtomContainer mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(smiles.smiles);
                String formulaString = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(mol));
                return formulaString;
            } catch (CDKException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void clearExperimentAnotations(Ms2Experiment exp) {
        exp.clearAnnotation(Smiles.class);
        exp.clearAnnotation(InChI.class);
    }


    public static boolean addIonToPeriodicTable(PrecursorIonType ionization) {
        if (ionization != null) {
            String name = ionization.toString();
            if (name != null) {
                if (!PeriodicTable.getInstance().hasIon(name)) {
                    final PeriodicTable i = PeriodicTable.getInstance();
                    try {
                        i.addCommonIonType(name);
                        if (ionization.getCharge() > 0)
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.positive",
                                    i.getPositiveAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")));
                        else if (ionization.getCharge() < 0)
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.negative",
                                    i.getNegativeAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")));
                    } catch (UnknownElementException e) {
                        LoggerFactory.getLogger(GuiProjectSpace.class).error("Could not add ion \"" + name + "\" to default ions.", e);
                    }

                    return true;
                }
            }
        }
        return false;
    }
    //endregion

    public static SiriusProjectSpace createGuiProjectSpace(File location) throws IOException {
        FilenameFormatter formatter = null;
        String formatterString = SiriusProperties.getProperty("de.unijena.bioinf.sirius.projectspace.formatter");
        if (formatterString != null) {
            try {
                formatter = new StandardMSFilenameFormatter(formatterString);
            } catch (ParseException e) {
                LoggerFactory.getLogger(GuiProjectSpace.class).error("Could not parse Formatter String." + formatterString + " Using default Formatter instead!", e);
            }
        }
        final @NotNull SiriusProjectSpace space = SiriusProjectSpaceIO.create(formatter, location,
                (cur, max, mess) -> {//todo progress listener
                },
                new IdentificationResultSerializer(), new FingerIdResultSerializer(ApplicationCore.WEB_API), new CanopusResultSerializer(ApplicationCore.CANOPUS));
        space.registerSummaryWriter(new MztabSummaryWriter());
        return space;
    }*/
}
