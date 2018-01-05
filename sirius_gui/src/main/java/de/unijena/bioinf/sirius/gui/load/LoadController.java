package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.fingerid.storage.ConfigStorage;
import de.unijena.bioinf.myxo.io.spectrum.CSVFormatReader;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedDataFormatsFilter;
import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.structure.CSVToSpectrumConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadController implements LoadDialogListener {
    LoadDialog loadDialog;

    private ExperimentContainer inputExp;

    Map<Spectrum<?>, MutableMs2Spectrum> spectra = new HashMap<>();
    private Spectrum<?> ms1 = null;

    private String name;
    private double parentMass;
    private PrecursorIonType ionization;
    private URL source;

    private ConfigStorage config;

    private JFrame owner;

    public LoadController(JFrame owner, ExperimentContainer exp, ConfigStorage config) {
        this.owner = owner;

        this.config = config;
        inputExp = exp;
        loadDialog = new DefaultLoadDialog(owner);

        if (inputExp != null) {
            if (exp.getIonization() != null) {
                setIonization(exp.getIonization());
                loadDialog.ionizationChanged(this.ionization);
            }
            if (!Double.isNaN(exp.getDataFocusedMass()) && exp.getDataFocusedMass() > 0) {
                setParentMass(exp.getDataFocusedMass());
                loadDialog.parentMassChanged(this.parentMass);
            }

            experimentNameChanged(exp.getName());

            //todo i think this is always only on ms1
            boolean first = true;
            for (Spectrum<? extends Peak> spectrum : inputExp.getMs1Spectra()) {
                addToSpectra(spectrum);
                if (first) {
                    ms1 = spectrum;
                    first = false;
                }
            }

            for (Spectrum<? extends Peak> spectrum : inputExp.getMs2Spectra()) {
                addToSpectra(spectrum);
            }
        } else {
            spectra.clear();
            setParentMass(-1);
            setIonization(PrecursorIonType.unknown(1));
            experimentNameChanged("");
            source = null;
        }

        loadDialog.addLoadDialogListener(this);
    }

    public LoadController(JFrame owner, ConfigStorage config) {
        this(owner, null, config);
    }

    public void showDialog() {
        loadDialog.showDialog();
    }

    //this is basically som prpety change like thing for the marvin stuff
    private Spectrum<?> addToSpectra(Spectrum<?> sp) {
        if (sp.getMsLevel() == 1) {
            if (ms1 != null) {
                sp = new MutableMs2Spectrum(sp);
                ((MutableMs2Spectrum) sp).setMsLevel(2);
            } else {
                ms1 = sp;
            }
        }

        spectra.putIfAbsent(sp, null);
        loadDialog.spectraAdded(sp);
        return sp;
    }

    private void removeFromSpectra(final Spectrum<?> sp) {
        spectra.remove(sp);
        loadDialog.spectraRemoved(sp);
    }

    private MutableMs2Spectrum getModifiableSpectrum(final Spectrum<?> sp) {
        MutableMs2Spectrum mod = spectra.get(sp);
        if (mod == null) {
            mod = new MutableMs2Spectrum(sp);
            spectra.put(sp, mod);
        }
        return mod;
    }

    @Override
    public void addSpectra() {
        JFileChooser chooser = new JFileChooser(config.getDefaultLoadDialogPath());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new SupportedDataFormatsFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog((JDialog) loadDialog);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File[] files = chooser.getSelectedFiles();

            //setzt Pfad
            config.setDefaultLoadDialogPath(files[0].getParentFile());

            //untersuche die Dateitypen und schaue ob CSV vorhanden, wenn vorhanden behandelte alle CSVs auf
            //gleiche Weise

            importSpectra(files);
        }
    }

    private void importSpectra(File[] files) {

        DataFormatIdentifier dfi = new DataFormatIdentifier();
        List<File> csvFiles = new ArrayList<>();
        List<File> msFiles = new ArrayList<>();
        List<File> mgfFiles = new ArrayList<>();
//		File firstCSV = null;
        for (File file : files) {
            DataFormat df = dfi.identifyFormat(file);
            if (df == DataFormat.CSV) {
                csvFiles.add(file);
            } else if (df == DataFormat.JenaMS) {
                msFiles.add(file);
            } else if (df == DataFormat.MGF) {
                mgfFiles.add(file);
            }
        }

        importSpectra(csvFiles, msFiles, mgfFiles);
    }


    private void importSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {

        List<String> errorStorage = new ArrayList<>();

        CSVDialogReturnContainer cont = null;
        CSVFormatReader csvReader = new CSVFormatReader();

        if (csvFiles.size() > 0) {

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
                            addToSpectra(sp);
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
                                addToSpectra(sp);
                                loadDialog.spectraAdded(sp);
                            }
                        }
                    }
                }
            }
        }


        final MsExperimentParser parser = new MsExperimentParser();

        if (msFiles.size() > 0) {
            for (File file : msFiles) {
                try (CloseableIterator<Ms2Experiment> iter = parser.getParser(file).parseFromFileIterator(file)) {
                    while (iter.hasNext()) {
                        importExperimentContainer(new ExperimentContainer(iter.next()), errorStorage);
                    }
                } catch (Exception e) {
                    errorStorage.add(file.getName() + ": Invalid file format.");
                    continue;
                }
            }
        }

        if (mgfFiles.size() > 0) {
            for (File file : mgfFiles) {
                try (CloseableIterator<Ms2Experiment> iter = parser.getParser(file).parseFromFileIterator(file)) {
                    while (iter.hasNext()) {
                        importExperimentContainer(new ExperimentContainer(iter.next()), errorStorage);
                    }
                } catch (Exception e) {
                    String m = file.getName() + ": Invalid file format.";
                    LoggerFactory.getLogger(this.getClass()).error(m, e);
                    errorStorage.add(m);
                    continue;
                }
            }
        }

        if (errorStorage.size() > 1) {
            ErrorListDialog elDiag = new ErrorListDialog(this.owner, errorStorage);
        } else if (errorStorage.size() == 1) {
            ExceptionDialog eDiag = new ExceptionDialog(this.owner, errorStorage.get(0));
        }

    }

    public void importExperimentContainer(ExperimentContainer ec, List<String> errorStorage) {
        source = ec.getSource();

        if (ionization.isIonizationUnknown() && ec.getIonization() != null && !ec.getIonization().isIonizationUnknown()) {
            setIonization(ec.getIonization());
            loadDialog.ionizationChanged(ec.getIonization());
        }

        if (!Double.isNaN(ec.getDataFocusedMass()) && ec.getDataFocusedMass() > 0 && parentMass < 0) {
            setParentMass(ec.getDataFocusedMass());
            loadDialog.parentMassChanged(ec.getDataFocusedMass());
        }

        if (name == null || name.isEmpty())
            experimentNameChanged(ec.getName());


        double ecFM = ec.getDataFocusedMass();
        if (parentMass <= 0 && ecFM > 0) {
            setParentMass(ecFM);
        }

        if (ec.getMs1Spectra().size() > 0) {
            SimpleSpectrum ms1 = ec.getMs1Spectra().get(0);
            if (ms1 != null) {
                addToSpectra(ms1);
            }
        }

        for (MutableMs2Spectrum sp : ec.getMs2Spectra()) {
            addToSpectra(sp);
        }
    }

    public ExperimentContainer getExperiment() {
        return inputExp;
    }


    @Override
    public void removeSpectrum(Spectrum<?> sp) {
        removeFromSpectra(sp);

        if (spectra.isEmpty()) {
            setParentMass(-1);
            setIonization(PrecursorIonType.unknown(1));
            experimentNameChanged("");
            this.loadDialog.experimentNameChanged("");
        }
    }

    @Override
    public void abortProcess() {
        //nothing to do here
    }

    @Override
    public void completeProcess() {
        if (!spectra.isEmpty()) {
            if (inputExp == null) {
                inputExp = new ExperimentContainer(new MutableMs2Experiment());
            } else {
                inputExp.getMs2Experiment().getMs1Spectra().clear();
                inputExp.getMs2Experiment().getMs2Spectra().clear();
            }

            //add spectra
            for (Map.Entry<Spectrum<?>, MutableMs2Spectrum> e : spectra.entrySet()) {
                if (e.getValue() != null) {
                    if (e.getKey().getMsLevel() == 1)
                        inputExp.getMs2Experiment().getMs1Spectra().add(new SimpleSpectrum(e.getValue()));
                    else
                        inputExp.getMs2Experiment().getMs2Spectra().add(e.getValue());
                } else {
                    if (e.getKey().getMsLevel() == 1)
                        inputExp.getMs2Experiment().getMs1Spectra().add((SimpleSpectrum) e.getKey());
                    else
                        inputExp.getMs2Experiment().getMs2Spectra().add((MutableMs2Spectrum) e.getKey());
                }
            }

            inputExp.setIonization(ionization);
            inputExp.setSelectedFocusedMass(parentMass);
            inputExp.setName(name);
            inputExp.getMs2Experiment().setSource(source);
        }
    }

    @Override
    public void changeCollisionEnergy(Spectrum<?> sp) {
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
                getModifiableSpectrum(sp).setCollisionEnergy(new CollisionEnergy(newMin, newMax));
                loadDialog.newCollisionEnergy(sp);
            }
        }
    }

    @Override
    public void changeMSLevel(final Spectrum<?> sp, int msLevel) {
        if (sp.getMsLevel() == msLevel) {
            return;
        }

        MutableMs2Spectrum mod = getModifiableSpectrum(sp);
        mod.setMsLevel(msLevel);

        if (msLevel == 1) {
            if (ms1 == null) {
                ms1 = sp;
            } else {
                MutableMs2Spectrum oldMS1 = getModifiableSpectrum(ms1);
                oldMS1.setMsLevel(2);
                loadDialog.msLevelChanged(oldMS1);
            }
        }
        loadDialog.msLevelChanged(sp);
    }

    @Override
    public void experimentNameChanged(String name) {
        if (name != null) {
            this.name = name;
            loadDialog.experimentNameChanged(this.name);
        }
    }

    @Override
    public void setParentMass(double mz) {
        this.parentMass = mz;
    }

    @Override
    public void setIonization(PrecursorIonType ionType) {
        this.ionization = ionType;
    }


    @Override
    public void addSpectra(List<File> files) {
        File[] fileArr = new File[files.size()];
        importSpectra(files.toArray(fileArr));

    }

    public void addSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        importSpectra(csvFiles, msFiles, mgfFiles);
    }

}
