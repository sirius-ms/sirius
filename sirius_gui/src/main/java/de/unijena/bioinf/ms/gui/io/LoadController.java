package de.unijena.bioinf.ms.gui.io;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.BatchImportDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.FileImportDialog;
import de.unijena.bioinf.ms.gui.io.filefilter.MsDataFormatsFilter;
import de.unijena.bioinf.ms.gui.io.spectrum.SpectrumContainer;
import de.unijena.bioinf.ms.gui.io.spectrum.csv.CSVFormatReader;
import de.unijena.bioinf.ms.gui.io.spectrum.csv.CSVToSpectrumConverter;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.ExperimentEditPanel;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.properties.PropertyManager;
import gnu.trove.list.array.TDoubleArrayList;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LoadController implements LoadDialogListener {
    private final JFrame owner;
    private DefaultLoadDialog loadDialog;

    private final MutableMs2Experiment expToModify;
    private final InstanceBean instance;

    private final EventList<SpectrumContainer> spectra;

    public LoadController(JFrame owner, InstanceBean inst) {
        this.owner = owner;

        instance = inst;
        if (instance != null) {
            expToModify = (MutableMs2Experiment) inst.getExperiment();
            spectra = new BasicEventList<>(expToModify.getMs1Spectra().size() + expToModify.getMs2Spectra().size());
            loadDialog = new DefaultLoadDialog(owner, spectra);

            loadDialog.ionizationChanged(expToModify.getPrecursorIonType() != null ? expToModify.getPrecursorIonType() : PrecursorIonType.unknown(1));

            loadDialog.editPanel.setMolecularFomula(expToModify);

            loadDialog.experimentNameChanged(expToModify.getName());

            loadDialog.setParentMass(expToModify.getIonMass());

            addToSpectra(expToModify.getMs1Spectra());
            addToSpectra(expToModify.getMs2Spectra());
        } else {
            expToModify = new MutableMs2Experiment();
            spectra = GlazedListsSwing.swingThreadProxyList(new BasicEventList<>());
            loadDialog = new DefaultLoadDialog(owner, spectra);
            loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
            loadDialog.experimentNameChanged("");
            loadDialog.editPanel.formulaTF.setText("");
        }

        loadDialog.addLoadDialogListener(this);
    }

    public LoadController(JFrame owner) {
        this(owner, null);
    }

    public void showDialog() {
        loadDialog.showDialog();
    }

    private void addToSpectra(Collection<? extends Spectrum> sps) {
        List<SpectrumContainer> containers = sps.stream().map(SpectrumContainer::new).collect(Collectors.toList());
        spectra.getReadWriteLock().writeLock().lock();
        try {
            spectra.addAll(containers);
        } finally {
            spectra.getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public void addSpectra() {
        JFileChooser chooser = new JFileChooser(PropertyManager.getFile(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new MsDataFormatsFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog(loadDialog);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            //setzt Pfad as default
            Jobs.runInBackground(() ->
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().
                            setAndStoreProperty(SiriusProperties.DEFAULT_LOAD_DIALOG_PATH, files[0].getParentFile().getAbsolutePath())
            );

            //untersuche die Dateitypen und schaue ob CSV vorhanden, wenn vorhanden behandelte alle CSVs auf
            //gleiche Weise
            importSpectra(Arrays.asList(files));
        }
    }

    private void importSpectra(List<File> files) {
        FileImportDialog idi = new FileImportDialog(owner, files);
        importSpectra(idi.getCSVFiles(), idi.getMSFiles(), idi.getMGFFiles());
    }


    private void importSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        List<String> errorStorage = new ArrayList<>();

        //csv import
        if (csvFiles.size() > 0) {
            CSVDialogReturnContainer cont = null;
            CSVFormatReader csvReader = new CSVFormatReader();

            HashMap<Integer, List<List<TDoubleArrayList>>> columnNumberToData = new HashMap<>();

            for (File file : csvFiles) {
                try {
                    List<TDoubleArrayList> data = csvReader.readCSV(file);
                    Integer key = data.get(0).size();
                    if (columnNumberToData.containsKey(key)) {
                        columnNumberToData.get(key).add(data);
                    } else {
                        List<List<TDoubleArrayList>> list = new ArrayList<>();
                        list.add(data);
                        columnNumberToData.put(key, list);
                    }
                } catch (Exception e) {
                    errorStorage.add(file.getName() + ": Invalid file format.");
                }
            }

            if (columnNumberToData.size() > 0) {
                for (Integer key : columnNumberToData.keySet()) {
                    List<List<TDoubleArrayList>> list = columnNumberToData.get(key);
                    if (list.size() == 1) {
                        CSVDialog diag = new CSVDialog((JDialog) loadDialog, list.get(0), false);
                        if (diag.getReturnValue() == ReturnValue.Success) {
                            cont = diag.getResults();
                            CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
                            Spectrum<?> sp = conv.convertCSVToSpectrum(list.get(0), cont);
                            addToSpectra(Collections.singleton(sp));
                        } else {
                            return; //breche ab
                        }
                    } else {
                        CSVDialog diag = new CSVDialog((JDialog) loadDialog, list.get(0), true);
                        if (diag.getReturnValue() == ReturnValue.Success) {
                            cont = diag.getResults();
                            cont.setMaxEnergy(-1);
                            cont.setMinEnergy(-1);
                            cont.setMsLevel(2);

                            for (List<TDoubleArrayList> data : list) {
                                CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
                                Spectrum<?> sp = conv.convertCSVToSpectrum(data, cont);
                                addToSpectra(Collections.singleton(sp));
                            }
                        }
                    }
                }
            }
        }

        BatchImportDialog batchImportDialog = new BatchImportDialog(loadDialog);
        batchImportDialog.start(msFiles, mgfFiles);
        errorStorage.addAll(batchImportDialog.getErrors());

        Jobs.runInBackgroundAndLoad(loadDialog, "Importing Compounds", new TinyBackgroundJJob() {
            @Override
            protected Object compute() {
                List<Ms2Experiment> r = batchImportDialog.getResults();
                final int maxProg = r.size() + 1;
                int i = 0;
                updateProgress(0, maxProg, i);

                PrecursorIonType precursorIonType = null;
                String name = null;
                Ms2Experiment formulaExp = null;
                double ionMass = -1;
                List<Spectrum<?>> spectra = new ArrayList<>();

                for (Ms2Experiment experiment : r) {
                    expToModify.addAnnotationsFrom(experiment);

                    if (name == null || name.isEmpty())
                        name = experiment.getName();

                    if (precursorIonType == null || experiment.getPrecursorIonType().isIonizationUnknown())
                        precursorIonType = experiment.getPrecursorIonType();

                    if (formulaExp == null && experiment.getMolecularFormula() != null)
                        formulaExp = experiment;

                    if (ionMass < 0 && experiment.getIonMass() > 0) {
                        ionMass = experiment.getIonMass();
                    }

                    spectra.addAll(experiment.getMs1Spectra());
                    spectra.addAll(experiment.getMs2Spectra());

                    updateProgress(0, maxProg, ++i);
                }

                if (precursorIonType != null) {
                    addIonToPeriodicTableAndFireChange(precursorIonType);
                }

                if (formulaExp != null)
                    loadDialog.editPanel.setMolecularFomula(formulaExp);

                if (name != null && !name.isEmpty())
                    loadDialog.experimentNameChanged(name);

                loadDialog.setParentMass(ionMass);

                addToSpectra(spectra);

                updateProgress(0, maxProg, maxProg);
                return true;
            }
        });


        if (errorStorage.size() > 1) {
            new ErrorListDialog(this.owner, errorStorage);
        } else if (errorStorage.size() == 1) {
            new ExceptionDialog(this.owner, errorStorage.get(0));
        }

    }

    private void addIonToPeriodicTableAndFireChange(PrecursorIonType ionization) {
        if (SiriusProperties.addIonToPeriodicTable(ionization))
            loadDialog.editPanel.ionizationCB.refresh();
        loadDialog.ionizationChanged(ionization);
    }

    @Override
    public void removeSpectra(List<SpectrumContainer> sps) {
        spectra.getReadWriteLock().writeLock().lock();
        try {
            spectra.removeAll(sps);
        } finally {
            spectra.getReadWriteLock().writeLock().unlock();
            if (spectra.isEmpty()) {
                loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
                loadDialog.experimentNameChanged("");
                loadDialog.editPanel.formulaTF.setText("");
            }
        }

    }


    @Override
    public void completeProcess() {
        if (!spectra.isEmpty()) {
            expToModify.getMs1Spectra().clear();
            expToModify.getMs2Spectra().clear();

            //add spectra
            for (SpectrumContainer container : spectra) {
                Spectrum<?> spectrum = container.getSpectrum(); // this return already the modified version if one exists
                if (spectrum.getMsLevel() == 1) {
                    if (container.isModified())
                        expToModify.getMs1Spectra().add(new SimpleSpectrum(spectrum));
                    else
                        expToModify.getMs1Spectra().add((SimpleSpectrum) spectrum);
                } else {
                    expToModify.getMs2Spectra().add((MutableMs2Spectrum) spectrum);
                }
            }
            //make changes persistent
            if (instance == null) {
                expToModify.setPrecursorIonType(loadDialog.getIonization());
                expToModify.setIonMass(loadDialog.getParentMass());

                expToModify.setName(loadDialog.getExperimentName());

                if (loadDialog.editPanel.validateFormula()) {
                    expToModify.setMolecularFormula(loadDialog.editPanel.getMolecularFormula());
                }

                Jobs.runInBackgroundAndLoad(owner, () -> MainFrame.MF.ps().newCompoundWithUniqueId(expToModify));
            } else {
                Jobs.runInBackgroundAndLoad(owner, () -> completeExisting(instance, loadDialog.editPanel));
            }
        }
    }

    public static void completeExisting(InstanceBean instance, ExperimentEditPanel editPanel) {
        final InstanceBean.Setter expSetter = instance.set();
        if (editPanel.validateFormula()) {
            final MolecularFormula nuFormula = editPanel.getMolecularFormula();
            expSetter.setMolecularFormula(nuFormula);
        }

        final double ionMass = editPanel.getSelectedIonMass();
        if (ionMass > 0)
            expSetter.setIonMass(ionMass);
        expSetter.setIonization(editPanel.getSelectedIonization());
        expSetter.setName(editPanel.getExperiementName());
        expSetter.apply(); //applies changes to experiment an writes it to projectspace
    }

    @Override
    public void changeCollisionEnergy(SpectrumContainer container) {
        Spectrum sp = container.getSpectrum();
        double oldMin, oldMax;
        if (sp.getCollisionEnergy() == null) {
            oldMin = 0;
            oldMax = 0;
        } else {
            oldMin = sp.getCollisionEnergy().getMinEnergy();
            oldMax = sp.getCollisionEnergy().getMaxEnergy();
        }

        CollisionEnergyDialog ced = new CollisionEnergyDialog((JDialog) loadDialog, oldMin, oldMax);
        if (ced.getReturnValue() == ReturnValue.Success) {
            double newMin = ced.getMinCollisionEnergy();
            double newMax = ced.getMaxCollisionEnergy();
            if (oldMin != newMin || oldMax != newMax) {
                container.getModifiableSpectrum().setCollisionEnergy(new CollisionEnergy(newMin, newMax));
                loadDialog.newCollisionEnergy(container);
            }
        }
    }

    @Override
    public void changeMSLevel(final SpectrumContainer container, int msLevel) {
        //indentity chekc already done before listener call
        MutableMs2Spectrum mod = container.getModifiableSpectrum();
        mod.setMsLevel(msLevel);
        loadDialog.msLevelChanged(container);
    }

    @Override
    public void addSpectra(List<File> files) {
        importSpectra(files);
    }

    public void addSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        importSpectra(csvFiles, msFiles, mgfFiles);
    }

}
