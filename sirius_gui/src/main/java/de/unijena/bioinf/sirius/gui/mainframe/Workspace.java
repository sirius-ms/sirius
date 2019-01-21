package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.ms.projectspace.SiriusProjectSpaceGUI;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Workspace {
    public static final SiriusProjectSpaceGUI PROJECT_SPACE = null;
    /*public static final BasicEventList<ExperimentContainer> COMPOUNT_LIST = new BasicEventList<>();
    private static final HashSet<String> NAMES = new HashSet<>();


    public static void clearWorkspace() {
        NAMES.clear();
        COMPOUNT_LIST.clear();
    }

    public static List<ExperimentContainer> toExperimentContainer(Ms2Experiment... exp) {
        return toExperimentContainer(Arrays.asList(exp));
    }

    public static List<ExperimentContainer> toExperimentContainer(List<Ms2Experiment> exp) {
        ArrayList<ExperimentContainer> ecs = new ArrayList<>(exp.size());
        for (Ms2Experiment ms2Experiment : exp) {
            ecs.add(new ExperimentContainer(ms2Experiment));
        }
        return ecs;
    }

    public static void importCompounds(List<ExperimentContainer> ecs) {
        if (ecs != null) {
            for (ExperimentContainer ec : ecs) {
                if (ec == null) {
                    continue;
                } else {
                    importCompound(ec);
                }
            }
        }
    }

    public static void importCompound(final ExperimentContainer ec) {
        SwingUtilities.invokeLater(() -> {
            //search for a missing molecular formula before cleaning the annotations
            if (ec.getMs2Experiment().getMolecularFormula() == null) {
                String f = extractMolecularFormulaString(ec);
                if (f != null && !f.isEmpty())
                    ec.getMs2Experiment().setMolecularFormula(MolecularFormula.parse(f));
            }

            if (ec.getMs2Experiment().getPrecursorIonType() == null) {
                LoggerFactory.getLogger(Workspace.class).warn("Input experiment with name '" + ec.getName() + "' does not have a charge nor an ion type annotation.");
                ec.getMs2Experiment().setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(1));
            }
            clearExperimentAnotations(ec);
            addIonToPeriodicTable(ec.getIonization());
            resolveCompundNameConflict(ec);
            COMPOUNT_LIST.add(ec);
            if (ec.getResults().size() > 0) ec.setSiriusComputeState(ComputingStatus.COMPUTED);
        });
    }

    public static void resolveCompundNameConflict(ExperimentContainer ec) {
        while (true) {
            if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                if (NAMES.contains(ec.getGUIName())) {
                    ec.setNameCounter(ec.getNameCounter() + 1);
                } else {
                    NAMES.add(ec.getGUIName());
                    break;
                }
            } else {
                ec.setName("Unknown");
                ec.setNameCounter(1);
            }
        }
    }

    public static void removeAll(List<ExperimentContainer> containers) {
        for (ExperimentContainer container : containers) {
            NAMES.remove(container.getGUIName());
        }
        COMPOUNT_LIST.removeAll(containers);
    }

    ////////////// helper methods to import data into workspace//////////////////////
    public static String extractMolecularFormulaString(ExperimentContainer ec) {
        MutableMs2Experiment exp = ec.getMs2Experiment();
        return extractMolecularFormulaString(exp);
    }

    public static String extractMolecularFormulaString(Ms2Experiment exp) {
        MolecularFormula formula = exp.getMolecularFormula();
        if (formula != null) {
            return formula.toString();
        }

        if (exp.hasAnnotation(InChI.class)) {
            InChI inChI = exp.getAnnotation(InChI.class);
            formula = inChI.extractFormula();
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

    //to clean fresh imported annotations
    public static void clearExperimentAnotations(ExperimentContainer ec) {
        clearExperimentAnotations(ec.getMs2Experiment());
    }

    public static void clearExperimentAnotations(Ms2Experiment exp) {
        //todo are there more annotaion that we do not want in the gui after import
        exp.clearAnnotation(Smiles.class);
        exp.clearAnnotation(InChI.class);
    }


    public static boolean addIonToPeriodicTable(PrecursorIonType ionization) {
        if (ionization != null) {
            String name = ionization.toString();
            if (name != null) {
                if (!PeriodicTable.getInstance().hasIon(name)) {
                    final PeriodicTable i = PeriodicTable.getInstance();
                    i.addCommonIonType(name);
                    if (ionization.getCharge() > 0)
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.positive",
                                String.join(",", i.getPositiveAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.toList())));
                    else if (ionization.getCharge() < 0)
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.negative",
                                String.join(",", i.getNegativeAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.toList())));
                    return true;
                }
            }
        }
        return false;
    }*/

}
