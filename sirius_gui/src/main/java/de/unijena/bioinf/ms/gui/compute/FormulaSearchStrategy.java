package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ElementSelectionDialog;
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

    protected Strategy strategy;

    protected final Dialog owner;
    protected final List<InstanceBean> ecs;
    protected final boolean isMs2;
    protected final boolean isBatchDialog;

    protected  JCheckboxListPanel<CustomDataSources.Source> searchDBList;

    /**
     * Map of strategy-specific UI components for showing/hiding when changing the strategy
     */
    private final Map<Strategy, List<Component>> strategyComponents;

    public FormulaSearchStrategy(Dialog owner, List<InstanceBean> ecs, boolean isMs2, boolean isBatchDialog, ParameterBinding parameterBindings) {
        super(parameterBindings);
        this.owner = owner;
        this.ecs = ecs;
        this.isMs2 = isMs2;
        this.isBatchDialog = isBatchDialog;

        strategyComponents = new HashMap<>();
        strategyComponents.put(Strategy.DEFAULT, new ArrayList<>());
        strategyComponents.put(Strategy.DE_NOVO, new ArrayList<>());
        strategyComponents.put(Strategy.DATABASE, new ArrayList<>());

        createPanel();
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return searchDBList;
    }

    private void createPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        final JPanel formulaSearchStrategySelection = new JPanel();
        formulaSearchStrategySelection.setLayout(new BoxLayout(formulaSearchStrategySelection, BoxLayout.PAGE_AXIS));
        formulaSearchStrategySelection.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
        JComboBox<Strategy> strategyBox =  GuiUtils.makeParameterComboBoxFromDescriptiveValues(Strategy.values());
        formulaSearchStrategySelection.add(new TextHeaderBoxPanel("Molecular formula generation", strategyBox));

        add(formulaSearchStrategySelection);
        add(Box.createRigidArea(new Dimension(0, GuiUtils.MEDIUM_GAP)));

        JPanel strategyCardContainer = new JPanel();
        strategyCardContainer.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
        strategyCardContainer.setLayout(new BoxLayout(strategyCardContainer, BoxLayout.LINE_AXIS));

        strategy = (Strategy) strategyBox.getSelectedItem();

        JPanel defaultStrategyParameters = createDefaultStrategyParameters();
        JPanel databaseStrategyParameters = createDatabaseStrategyParameters();

        strategyComponents.get(Strategy.DEFAULT).add(defaultStrategyParameters);
        strategyComponents.get(Strategy.DATABASE).add(databaseStrategyParameters);

        strategyCardContainer.add(defaultStrategyParameters);
        strategyCardContainer.add(databaseStrategyParameters);

        strategyCardContainer.add(createElementFilterPanel());

        add(strategyCardContainer);

        showStrategy(strategy);

        strategyBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            strategy = (Strategy) e.getItem();
            showStrategy(strategy);
        });
    }

    private void showStrategy(Strategy strategy) {
        strategyComponents.forEach((s, lst) -> lst.forEach(c -> c.setVisible(s.equals(strategy))));
    }

    private JPanel createDefaultStrategyParameters() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.PAGE_AXIS));
        JPanel parameterPanel = applyDefaultLayout(new JPanel());

        final TwoColumnPanel busOptions = new TwoColumnPanel();
        JComboBox<SiriusOptions.BottomUpSearchOptions> bottomUpSearchSelector = new JComboBox<>();
        List<SiriusOptions.BottomUpSearchOptions> settings = new ArrayList<>(EnumSet.allOf(SiriusOptions.BottomUpSearchOptions.class));
        settings.remove(SiriusOptions.BottomUpSearchOptions.DISABLED);  //this is not a contradiction by default, but we have the separate Strategy.DE_NOVO for that
        settings.forEach(bottomUpSearchSelector::addItem);
        bottomUpSearchSelector.setSelectedItem(SiriusOptions.BottomUpSearchOptions.CUSTOM);
        busOptions.addNamed("Bottom up search", bottomUpSearchSelector);

        JCheckBox bottomUpSearchEnabled = new JCheckBox();
        bottomUpSearchEnabled.setSelected(true);

        JSpinner denovoUpTo = makeIntParameterSpinner("FormulaSearchSettings.disableDeNovoAboveMass", 0, Integer.MAX_VALUE, 5);  // binding is overwritten

        JLabel bottomUpCheckboxLabel = new JLabel("Perform bottom up search");
        JLabel denovoUpToLabel = new JLabel("Perform de novo below m/z");

        busOptions.add(bottomUpCheckboxLabel, bottomUpSearchEnabled);
        busOptions.add(denovoUpToLabel, denovoUpTo);

        List<Component> customComponents = List.of(bottomUpCheckboxLabel, bottomUpSearchEnabled, denovoUpToLabel, denovoUpTo);

        bottomUpSearchSelector.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            boolean customSelected = e.getItem() == SiriusOptions.BottomUpSearchOptions.CUSTOM;
            customComponents.forEach(c -> c.setVisible(customSelected));
        });

        parameterBindings.put("FormulaSearchSettings.enableBottomUpFromMass", () -> {
            if (strategy == Strategy.DEFAULT) {
                boolean onlyBottomUp = bottomUpSearchSelector.getSelectedItem() == SiriusOptions.BottomUpSearchOptions.BOTTOM_UP_ONLY;
                boolean custom = bottomUpSearchSelector.getSelectedItem() == SiriusOptions.BottomUpSearchOptions.CUSTOM;
                if (onlyBottomUp || (custom && bottomUpSearchEnabled.isSelected())) {
                    return "0";
                }
            }
            return String.valueOf(Double.POSITIVE_INFINITY);
        });

        parameterBindings.put("FormulaSearchSettings.disableDeNovoAboveMass", () -> switch (strategy) {
            case DEFAULT -> bottomUpSearchSelector.getSelectedItem() == SiriusOptions.BottomUpSearchOptions.CUSTOM ?
                    denovoUpTo.getValue().toString()
                    : "0";
            case DE_NOVO -> String.valueOf(Double.POSITIVE_INFINITY);
            case DATABASE -> "0";
        });


        parameterPanel.add(new TextHeaderBoxPanel("Bottom Up Search", busOptions));

        card.add(parameterPanel);
        return card;
    }

    private JPanel createDatabaseStrategyParameters() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.PAGE_AXIS));

        initDatabasePanel();
        searchDBList.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));

        card.add(searchDBList);
        return card;
    }

    private void initDatabasePanel() {
        searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Use DB formulas only");
        GuiUtils.assignParameterToolTip(searchDBList.checkBoxList, "FormulaSearchDB");

        PropertyManager.DEFAULTS.createInstanceWithDefaults(StructureSearchDB.class).searchDBs
                .forEach(s -> searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(s.name())));

        parameterBindings.put("FormulaSearchDB", () -> strategy == Strategy.DATABASE ? String.join(",", getFormulaSearchDBStrings()) : ",");
        PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSearchDB.class).searchDBs
                .forEach(s -> searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(s.name())));
    }

    private JPanel createElementFilterPanel() {
        Set<Element> autoDetectableElements = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor().getSetOfPredictableElements();
        final FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);

        final TwoColumnPanel filterFields = new TwoColumnPanel();

        JLabel constraintsLabel = new JLabel("Allowed elements");
        JTextField enforcedTextBox = makeParameterTextField("FormulaSettings.enforced", formulaSettings.getEnforcedAlphabet().toString(), 20);

        JLabel autodetectLabel = new JLabel("Autodetect");
        final JTextField detectableTextBox = isBatchDialog ? makeParameterTextField("FormulaSettings.detectable", 20) : null;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        addDefaultStrategyElementFilterSettings(filterFields);

        List<Component> filterComponents = new ArrayList<>(List.of(constraintsLabel, enforcedTextBox, buttonPanel));
        if (isBatchDialog) {
            filterComponents.addAll(List.of(autodetectLabel, detectableTextBox));
        }
        addDatabaseStrategyElementFilterSettings(filterFields, filterComponents);

        int constraintsGridY = filterFields.both.gridy;
        filterFields.add(constraintsLabel, enforcedTextBox);
        if (isBatchDialog) {
            filterFields.add(autodetectLabel, detectableTextBox);
        }

        JButton buttonEdit = new JButton("…");  // Ellipsis symbol instead of ... because 1-char buttons don't get side insets
        buttonEdit.setToolTipText("Customize allowed elements and their quantities");
        buttonPanel.add(buttonEdit);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = constraintsGridY;
        c.gridheight = isBatchDialog ? 2 : 1;
        filterFields.add(buttonPanel, c);

        buttonEdit.addActionListener(e -> {
            FormulaConstraints currentConstraints = FormulaConstraints.fromString(enforcedTextBox.getText());
            Set<Element> currentAuto = null;
            if (isBatchDialog) {
                try {
                    currentAuto = ChemicalAlphabet.fromString(detectableTextBox.getText()).toSet();
                } catch (UnknownElementException ex) {
                    currentAuto = autoDetectableElements;
                }
            }
            ElementSelectionDialog dialog = new ElementSelectionDialog(owner, "Filter Elements", isBatchDialog ? autoDetectableElements : null, currentAuto, currentConstraints);
            if (dialog.isSuccess()) {
                enforcedTextBox.setText(dialog.getConstraints().toString());
                if (isBatchDialog) {
                    detectableTextBox.setText(join(dialog.getAutoDetect()));
                }
            }
        });

        if (!isBatchDialog) {
            JButton buttonAutodetect = new JButton("Auto");
            buttonAutodetect.setToolTipText("Auto detectable element are: " + join(autoDetectableElements));
            buttonAutodetect.addActionListener(e -> detectElements(autoDetectableElements, enforcedTextBox));
            buttonPanel.add(buttonAutodetect);
        }

        JPanel elementFilterPanel = applyDefaultLayout(new JPanel());
        elementFilterPanel.add(new TextHeaderBoxPanel("Element Filter", filterFields));

        return elementFilterPanel;
    }

    private void addDefaultStrategyElementFilterSettings(TwoColumnPanel filterFields) {
        JComboBox<ElementAlphabetStrategy> elementAlphabetStrategySelector = new JComboBox<>(); //todo NewWorflow: implement this feature in sirius-libs
        List<ElementAlphabetStrategy> settingsElements = List.copyOf(EnumSet.allOf(ElementAlphabetStrategy.class));
        settingsElements.forEach(elementAlphabetStrategySelector::addItem);
        elementAlphabetStrategySelector.setSelectedItem(ElementAlphabetStrategy.DE_NOVO_ONLY);
        parameterBindings.put("FormulaSearchSettings.applyFormulaContraintsToBottomUp", () -> Boolean.toString(elementAlphabetStrategySelector.getSelectedItem() == ElementAlphabetStrategy.BOTH));

        JLabel label = new JLabel("Apply element filter to");
        filterFields.add(label, elementAlphabetStrategySelector);

        strategyComponents.get(Strategy.DEFAULT).add(label);
        strategyComponents.get(Strategy.DEFAULT).add(elementAlphabetStrategySelector);
    }

    private void addDatabaseStrategyElementFilterSettings(TwoColumnPanel filterFields, List<Component> filterComponents) {
        JCheckBox useElementFilter = new JCheckBox() { //todo NewWorkflow: implement this feature. This makes the organics filter obsolete. Maybe dont use the checkbox but always select the organics. Make new Element panel popup
            @Override
            public void setVisible(boolean flag) {
                super.setVisible(flag);
                if (flag) {
                    filterComponents.forEach(c -> c.setVisible(this.isSelected()));
                } else {
                    filterComponents.forEach(c -> c.setVisible(true));
                }
            }
        };

        parameterBindings.put("FormulaSearchSettings.applyFormulaContraintsToCandidateLists", () -> Boolean.toString(useElementFilter.isSelected()));

        JLabel label = new JLabel("Enable element filter");
        filterFields.add(label, useElementFilter);

        strategyComponents.get(Strategy.DATABASE).add(label);
        strategyComponents.get(Strategy.DATABASE).add(useElementFilter);

        useElementFilter.addActionListener(e -> filterComponents.forEach(c -> c.setVisible(useElementFilter.isSelected())));
    }

    private String join(Collection<?> objects) {
        return objects.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    protected void detectElements(Set<Element> autoDetectable, JTextField formulaConstraintsTextBox) {
        String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
        FormulaConstraints currentConstraints = FormulaConstraints.fromString(formulaConstraintsTextBox.getText());
        InstanceBean ec = ecs.get(0);
        if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
            Jobs.runInBackgroundAndLoad(owner, "Detecting Elements...", () -> {
                final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
                ProcessedInput pi = pp.preprocess(new MutableMs2Experiment(ec.getExperiment(), false));

                pi.getAnnotation(FormulaConstraints.class).
                        ifPresentOrElse(c -> {
                                    for (Element element : c.getChemicalAlphabet()) {
                                        if (autoDetectable.contains(element)) {
                                            currentConstraints.setBound(element, c.getLowerbound(element), c.getUpperbound(element));
                                        }
                                    }
                                    formulaConstraintsTextBox.setText(currentConstraints.toString());
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
