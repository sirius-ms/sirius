package de.unijena.bioinf.babelms.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class GuiProjectSpace {
    public static final GuiProjectSpace PS;

    static {
        //todo load workspace form Property
        PropertyManager.getProperty("path/to/space");
        PS = new GuiProjectSpace(null);
    }

    public final BasicEventList<ExperimentResultBean> COMPOUNT_LIST = new BasicEventList<>();
    private final Set<ExperimentResult> changed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, TIntSet> NAMES = new ConcurrentHashMap<>();
    public final SiriusProjectSpace projectSpace;

    public GuiProjectSpace(SiriusProjectSpace space) {
        projectSpace = space;

    }

    public void importCompound(@NotNull final ExperimentResultBean ec) {
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
                projectSpace.deleteExperiment(ec.getIdentifier());
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
    }
}
