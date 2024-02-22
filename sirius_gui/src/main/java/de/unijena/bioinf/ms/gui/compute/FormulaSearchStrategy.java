package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class FormulaSearchStrategy extends ConfigPanel {
    public enum Strategy implements DescriptiveOptions {
        DEFAULT("Default strategy"),
        DE_NOVO("Denovo strategy"),
        DATABASE("Database stragegy");

        private final String description;

        Strategy(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public enum ElementAlphabetStrategy implements DescriptiveOptions {
        DE_NOVO_ONLY("Use set of elements for de novo generation only."),
        BOTH("Use set of elements for de novo generation and filter of bottom up search.");

        private final String description;

        ElementAlphabetStrategy(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    protected final Strategy strategy;

    protected final Dialog owner;
    protected final List<InstanceBean> ecs;
    protected final boolean isMs2;
    protected final boolean isBatchDialog;

    protected final ParameterBinding parameterBindings;

    private boolean isEnabled;
    protected  JCheckboxListPanel<CustomDataSources.Source> searchDBList;

    protected ElementsPanel elementPanel;


    public FormulaSearchStrategy(Strategy strategy, Dialog owner, List<InstanceBean> ecs, boolean isMs2, boolean isBatchDialog, ParameterBinding parameterBindings) {
        this.strategy = strategy;
        this.owner = owner;
        this.ecs = ecs;
        this.isMs2 = isMs2;
        this.isBatchDialog = isBatchDialog;
        this.parameterBindings = parameterBindings;

        createPanel();

    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return searchDBList;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        revalidate();
    }

    private void createPanel() {
        this.removeAll();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        searchDBList = createDatabasePanel(); //todo NewWorkflow: always generate, but not use always?


        switch (strategy) {
            case DEFAULT -> {
                createDefaultStrategyPanel(this);
            }
            case DE_NOVO -> {
                createDeNovoStrategyPanel(this);
            }
            case DATABASE -> {
                createDatabaseStrategyPanel(this);
            }
        }
    }


    private void createDefaultStrategyPanel(JPanel main) {
        // bottom up search options
        JPanel center = applyDefaultLayout(new JPanel());
        add(center);
        final TwoColumnPanel busOptions = new TwoColumnPanel();
        JComboBox<SiriusOptions.BottomUpSearchOptions> bottomUpSearchSelector = new JComboBox<>();
        List<SiriusOptions.BottomUpSearchOptions> settings = new ArrayList<>(EnumSet.allOf(SiriusOptions.BottomUpSearchOptions.class));
        if (strategy == Strategy.DE_NOVO) settings.remove(SiriusOptions.BottomUpSearchOptions.BOTTOM_UP_ONLY);
        if (strategy == Strategy.DEFAULT) settings.remove(SiriusOptions.BottomUpSearchOptions.DISABLED);//this is not a contradiction by default, but we have the separate Strategy.DE_NOVO for that
        settings.forEach(bottomUpSearchSelector::addItem);
        busOptions.addNamed("Bottom up search", bottomUpSearchSelector);

        JSpinner bottomUpSearchOnly;
        JCheckBox bottomUpSearchEnabled = new JCheckBox();
        bottomUpSearchOnly = makeIntParameterSpinner("FormulaSearchSettings.disableDeNovoAboveMass", 0, Integer.MAX_VALUE, 5);
        bottomUpSearchEnabled.setEnabled(false);
        bottomUpSearchOnly.setEnabled(false);

        bottomUpSearchEnabled.addActionListener(e -> {
            //enable of disable buttom up search. Not setting specific mz threshold
            setBottomUpSearchMass(bottomUpSearchEnabled);
        });
        bottomUpSearchEnabled.setSelected(true);

        bottomUpSearchSelector.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            final SiriusOptions.BottomUpSearchOptions source = (SiriusOptions.BottomUpSearchOptions) e.getItem();
            switch (source) {
                case BOTTOM_UP_ONLY -> {
                    bottomUpSearchEnabled.setEnabled(false);
                    bottomUpSearchOnly.setEnabled(false);
                    bottomUpSearchEnabled.setSelected(true);
                    bottomUpSearchOnly.setValue(0);
                }
                case DISABLED -> {
                    bottomUpSearchEnabled.setEnabled(false);
                    bottomUpSearchOnly.setEnabled(false);
                    bottomUpSearchEnabled.setSelected(false);
                    bottomUpSearchOnly.setValue(Double.POSITIVE_INFINITY);
                }
                case CUSTOM -> {
                    bottomUpSearchEnabled.setEnabled(true);
                    bottomUpSearchOnly.setEnabled(true);
                    bottomUpSearchEnabled.setSelected(true);
                    bottomUpSearchOnly.setValue(400);
                }
            }
        });
        busOptions.addNamed("Perform bottom up search", bottomUpSearchEnabled);
        busOptions.addNamed("Perform de novo below m/z", bottomUpSearchOnly);

        center.add(new TextHeaderBoxPanel("Bottom Up Search", busOptions));
        main.add(center);


        JComboBox<ElementAlphabetStrategy> elementAlpahAlphabetStrategySelector = new JComboBox<>(); //todo NewWorflow: implement this feature in sirius-libs
        List<ElementAlphabetStrategy> settingsElements = java.util.List.copyOf(EnumSet.allOf(ElementAlphabetStrategy.class));
        settingsElements.forEach(elementAlpahAlphabetStrategySelector::addItem);
        final TwoColumnPanel elementOptions = new TwoColumnPanel();
        elementOptions.addNamed("Apply element filter to", elementAlpahAlphabetStrategySelector);
        main.add(elementOptions);
        elementPanel = createElementPanel(isBatchDialog);
        main.add(elementPanel);

        elementAlpahAlphabetStrategySelector.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            final ElementAlphabetStrategy filterStrategy = (ElementAlphabetStrategy) e.getItem();
            boolean applyToBottomUp = (filterStrategy == ElementAlphabetStrategy.BOTH);

            parameterBindings.put("FormulaSearchSettings.applyFormulaContraintsToBottomUp", () -> Boolean.toString(applyToBottomUp));
        });
        elementAlpahAlphabetStrategySelector.setSelectedItem(ElementAlphabetStrategy.DE_NOVO_ONLY);
    }

    private void setBottomUpSearchMass(JCheckBox bottomUpSearchEnabled) {
        if (bottomUpSearchEnabled.isSelected()) {
            parameterBindings.put("FormulaSearchSettings.enableBottomUpFromMass", () -> "0");
        } else {
            parameterBindings.put("FormulaSearchSettings.enableBottomUpFromMass", () -> String.valueOf(Double.POSITIVE_INFINITY));
        }
    }

    private void createDeNovoStrategyPanel(JPanel main) {
        elementPanel = createElementPanel(isBatchDialog);
        main.add(elementPanel);

        enableDeNovo();
        disableBottomUpSearch();
    }

    private void createDatabaseStrategyPanel(JPanel main) {
        main.add(searchDBList);

        final TwoColumnPanel elementOptions = new TwoColumnPanel();
        JCheckBox useElementFilter = new JCheckBox(); //todo NewWorkflow: implement this feature. This makes the organics filter obsolete. Maybe dont use the checkbox but always select the organics. Make new Element panel popup
        useElementFilter.setSelected(false);
        parameterBindings.put("FormulaSearchSettings.applyFormulaContraintsToCandidateLists", () -> Boolean.toString(useElementFilter.isSelected()));

        elementOptions.addNamed("Use element filter", useElementFilter);
        main.add(elementOptions);

        elementPanel = createElementPanel(isBatchDialog);
        main.add(elementPanel);
        elementPanel.setVisible(useElementFilter.isSelected());

        useElementFilter.addActionListener(e -> {
            elementPanel.setVisible(useElementFilter.isSelected());
            elementPanel.setEnabled(useElementFilter.isSelected()); //todo ElementFilter: this is not the proper way. Buttons still disabled.
            parameterBindings.put("FormulaSearchSettings.applyFormulaContraintsToCandidateLists", () -> Boolean.toString(useElementFilter.isSelected()));
        });

        disableBottomUpSearch();
        disableDeNovo();
    }

    private void disableBottomUpSearch() {
        parameterBindings.put("FormulaSearchSettings.enableBottomUpFromMass", () -> String.valueOf(Double.POSITIVE_INFINITY));
    }

    private void enableDeNovo() {
        parameterBindings.put("FormulaSearchSettings.disableDeNovoAboveMass", () -> String.valueOf(Double.POSITIVE_INFINITY));
    }

    private void disableDeNovo() {
        parameterBindings.put("FormulaSearchSettings.disableDeNovoAboveMass", () -> "0");
    }

    private JCheckboxListPanel<CustomDataSources.Source> createDatabasePanel() {
        if (this.searchDBList != null) return this.searchDBList;
        // configure database to search list
        searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Use DB formulas only");
        GuiUtils.assignParameterToolTip(searchDBList.checkBoxList, "FormulaSearchDB");
        parameterBindings.put("FormulaSearchDB", () -> String.join(",", getFormulaSearchDBStrings()));
        PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSearchDB.class).searchDBs
                .forEach(s -> searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(s.name())));
        return searchDBList;
    }

    protected ElementsPanel createElementPanel(boolean multi) {
        final FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        List<Element> possDetectableElements = new ArrayList<>(ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor().getSetOfPredictableElements());

        final JButton elementAutoDetect;
        if (multi) {
            elementAutoDetect = null;
            elementPanel = new ElementsPanel(owner, 4, possDetectableElements, formulaSettings.getAutoDetectionElements(), formulaSettings.getEnforcedAlphabet());
        } else {
            /////////////Solo Element//////////////////////
            elementPanel = new ElementsPanel(owner, 4, formulaSettings.getEnforcedAlphabet());
            elementAutoDetect = new JButton("Auto detect");
            elementAutoDetect.setToolTipText("Auto detectable element are: "
                    + possDetectableElements.stream().map(Element::toString).collect(Collectors.joining(",")));
            elementAutoDetect.addActionListener(e -> detectElements());
            elementAutoDetect.setEnabled(true);
            elementPanel.lowerPanel.add(elementAutoDetect);
        }

        elementPanel.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));

        // configure Element panel
        parameterBindings.put("FormulaSettings.enforced", () -> {
            return elementPanel.getElementConstraints().toString();
        });
        parameterBindings.put("FormulaSettings.detectable", () -> {
            final List<Element> elementsToAutoDetect = elementPanel.individualAutoDetect ? elementPanel.getElementsToAutoDetect() : Collections.emptyList();
            return (elementsToAutoDetect.isEmpty() ? "," :
                    elementsToAutoDetect.stream().map(Element::toString).collect(Collectors.joining(",")));
        }); //todo check if this makes sense //todo NewWorflow: this is a very old todo. I assume we can remove it

        return elementPanel;
    }

    protected void detectElements() {
        String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
        InstanceBean ec = ecs.get(0);
        if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
            Jobs.runInBackgroundAndLoad(owner, "Detecting Elements...", () -> {
                final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
                ProcessedInput pi = pp.preprocess(new MutableMs2Experiment(ec.getExperiment(), false));

                pi.getAnnotation(FormulaConstraints.class).
                        ifPresentOrElse(c -> {
                                    for (Element element : c.getChemicalAlphabet()) {
                                        if (c.getUpperbound(element) <= 0) {
                                            c.setLowerbound(element, 0);
                                            c.setUpperbound(element, 0);
                                        }
                                    }
                                    elementPanel.setSelectedElements(c);
                                },
                                () -> new ExceptionDialog(owner, notWorkingMessage)
                        );
            }).getResult();
        } else {
            new ExceptionDialog(owner, notWorkingMessage);
        }
    }


    public List<CustomDataSources.Source> getFormulaSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getFormulaSearchDBStrings() {
        return getFormulaSearchDBs().stream().map(CustomDataSources.Source::id).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
