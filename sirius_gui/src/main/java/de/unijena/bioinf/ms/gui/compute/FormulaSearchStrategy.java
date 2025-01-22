package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.chemdb.annotations.SearchableDBAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.ElementSelectionDialog;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.unijena.bioinf.chemdb.annotations.SearchableDBAnnotation.NO_DB;

public class FormulaSearchStrategy extends ConfigPanel {
    public enum Strategy implements DescriptiveOptions {
        DEFAULT("De novo + bottom up (recommended)", "Perform both a bottom up search and de novo molecular formula generation."),
        BOTTOM_UP("Bottom up", "Generate molecular formula candidates using bottom up search: if a fragement + precursor loss have candidates in the formula database, these are combined to a precursor formula candidate."),
        DE_NOVO("De novo", "Generate molecular formula candidates de novo."),
        DATABASE("Database search", "Retrieve molecular formula candidates from a database."),
        PROVIDED("Provide molecular formulas", "Use the given list of candidate molecular formulas.");

        private final String description;
        private final String displayName;

        Strategy(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ElementAlphabetStrategy implements DescriptiveOptions {
        DE_NOVO_ONLY("De novo", "Use set of elements for de novo generation only."),
        BOTH("De novo + bottom up", "Use set of elements for de novo generation and filter of bottom up search.");

        private final String description;

        private final String displayName;

        ElementAlphabetStrategy(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * currently selected strategy
     */
    protected Strategy strategy;

    protected final Dialog owner;
    protected final SiriusGui gui;
    protected final List<InstanceBean> ecs;
    protected final boolean isMs2;
    protected final boolean hasMs1AndIsSingleMode;
    protected final boolean isBatchDialog;
    protected final FormulaIDConfigPanel formulaIDConfigPanel;

    protected DBSelectionListPanel searchDBList;
    protected JComboBox<ElementAlphabetStrategy> defaultStrategyElementFilterSelector;
    protected JPanel elementFilterPanel;
    protected JCheckBox elementFilterForBottomUp, elementFilterForDatabase;
    protected JSpinner denovoUpTo;
    protected DefaultListModel<String> providedFormulaListModel;
    protected JTextField elementFilterEnforcedTextBox;
    protected JTextField elementFilterDetectableElementsTextBox;
    protected LoadablePanel loadable;

    /**
     * Map of strategy-specific UI components for showing/hiding when changing the strategy
     */
    private final Map<Strategy, List<Component>> strategyComponents;

    /**
     * box to select the active formula generation strategy. User addStrategyChangeListener to easily add listeners
     */
    private final JComboBox<Strategy> strategyBox;

    public FormulaSearchStrategy(SiriusGui gui, Dialog owner, List<InstanceBean> ecs, boolean isMs2, boolean isBatchDialog, ParameterBinding parameterBindings, FormulaIDConfigPanel formulaIDConfigPanel) {
        super(parameterBindings);
        this.owner = owner;
        this.gui = gui;
        this.ecs = ecs;
        this.isMs2 = isMs2;
        this.isBatchDialog = isBatchDialog;
        this.formulaIDConfigPanel = formulaIDConfigPanel;

        //in single mode: does compound has MS1 data?
        this.hasMs1AndIsSingleMode = !isBatchDialog && !ecs.isEmpty() && ecs.getFirst().hasMs1();

        strategyComponents = new HashMap<>();
        strategyComponents.put(Strategy.DEFAULT, new ArrayList<>());
        strategyComponents.put(Strategy.BOTTOM_UP, new ArrayList<>());
        strategyComponents.put(Strategy.DE_NOVO, new ArrayList<>());
        strategyComponents.put(Strategy.DATABASE, new ArrayList<>());
        strategyComponents.put(Strategy.PROVIDED, new ArrayList<>());
        strategyBox = isMs2 ? GuiUtils.makeParameterComboBoxFromDescriptiveValues(Strategy.values()) : GuiUtils.makeParameterComboBoxFromDescriptiveValues(new Strategy[]{Strategy.DE_NOVO, Strategy.DATABASE, Strategy.PROVIDED});

        this.setLayout(new BorderLayout());
        this.loadable = createLoadablePanel();
        this.add(loadable, BorderLayout.CENTER);

        strategyBox.setSelectedItem(Strategy.DE_NOVO);
        strategyBox.setSelectedItem(Strategy.DEFAULT); //fire change to initialize fields
    }

    public JCheckboxListPanel<SearchableDatabase> getSearchDBList() {
        return searchDBList;
    }

    private LoadablePanel createLoadablePanel() {
        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));

        final JPanel formulaSearchStrategySelection = new JPanel();
        formulaSearchStrategySelection.setLayout(new BoxLayout(formulaSearchStrategySelection, BoxLayout.PAGE_AXIS));
        formulaSearchStrategySelection.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
        formulaSearchStrategySelection.add(new TextHeaderBoxPanel("Molecular formula generation", strategyBox));

        content.add(formulaSearchStrategySelection);
        content.add(Box.createRigidArea(new Dimension(0, GuiUtils.MEDIUM_GAP)));

        JPanel strategyCardContainer = new JPanel();
        strategyCardContainer.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
        strategyCardContainer.setLayout(new BoxLayout(strategyCardContainer, BoxLayout.LINE_AXIS));

        strategy = (Strategy) strategyBox.getSelectedItem();

        JPanel defaultStrategyParameters = createDefaultStrategyParameters();
        JPanel databaseStrategyParameters = createDatabaseStrategyParameters();
        JPanel providedStrategyParameters = createProvidedStrategyParameters();

        strategyComponents.get(Strategy.DEFAULT).add(defaultStrategyParameters);
        strategyComponents.get(Strategy.DATABASE).add(databaseStrategyParameters);
        strategyComponents.get(Strategy.PROVIDED).add(providedStrategyParameters);

        strategyCardContainer.add(defaultStrategyParameters);
        strategyCardContainer.add(databaseStrategyParameters);
        strategyCardContainer.add(providedStrategyParameters);

        elementFilterPanel = createElementFilterPanel();
        strategyCardContainer.add(elementFilterPanel);

        content.add(strategyCardContainer);

        hideAllStrategySpecific();
        showStrategySpecific(strategy, true);

        addStrategyChangeListener(s -> {
            showStrategySpecific(strategy, false);
            strategy = s; //update current strategy
            showStrategySpecific(strategy, true);
        });

        return new LoadablePanel(content);
    }

    private void showStrategySpecific(Strategy s, boolean show) {
        strategyComponents.get(s).forEach(c -> c.setVisible(show));
    }

    private void hideAllStrategySpecific() {
        strategyComponents.forEach((s, lst) -> lst.forEach(c -> c.setVisible(false)));
    }

    private JPanel createDefaultStrategyParameters() {
        JPanel parameterPanel = applyDefaultLayout(new JPanel());
        ((RelativeLayout) parameterPanel.getLayout()).setBorderGap(0);
        parameterPanel.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));

        final TwoColumnPanel options = new TwoColumnPanel();

        denovoUpTo = makeIntParameterSpinner("FormulaSearchSettings.performDeNovoBelowMz", 0, Integer.MAX_VALUE, 5);  // binding is overwritten
        options.addNamed("Perform de novo below m/z", denovoUpTo);

        parameterBindings.put("FormulaSearchSettings.performBottomUpAboveMz", () -> switch (strategy) {
            case DEFAULT, BOTTOM_UP -> "0";
            case DE_NOVO, DATABASE, PROVIDED -> String.valueOf(Double.POSITIVE_INFINITY);
        });

        parameterBindings.put("FormulaSearchSettings.performDeNovoBelowMz", () -> switch (strategy) {
            case DEFAULT -> denovoUpTo.getValue().toString();
            case BOTTOM_UP, DATABASE, PROVIDED -> "0";
            case DE_NOVO -> String.valueOf(Double.POSITIVE_INFINITY);
        });

        parameterPanel.add(new TextHeaderBoxPanel("General", options));

        return parameterPanel;
    }

    private JPanel createDatabaseStrategyParameters() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.PAGE_AXIS));

        initDatabasePanel();
        searchDBList.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));

        card.add(searchDBList);
        return card;
    }

    private JPanel createProvidedStrategyParameters() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.LINE_AXIS));

        providedFormulaListModel = new DefaultListModel<>();
        JList<String> formulaList = new JList<>(providedFormulaListModel);
        formulaList.setVisibleRowCount(6);
        JScrollPane listScroller = new JScrollPane(formulaList);

        card.add(listScroller);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton addFormulas = Buttons.getAddButton16("Add molecular formulas");
        JButton removeFormulas = Buttons.getRemoveButton16("Remove selected formulas");

        buttonPanel.add(addFormulas);
        buttonPanel.add(removeFormulas);
        buttonPanel.add(Box.createVerticalGlue());

        card.add(buttonPanel);

        addFormulas.addActionListener(e -> {

            Box addFormulasDialogContents = Box.createVerticalBox();
            addFormulasDialogContents.add(new JLabel("Paste formulas separated by whitespace, commas or semicolons"));
            JTextArea textArea = new JTextArea(5, 20);
            addFormulasDialogContents.add(new JScrollPane(textArea));

            if (JOptionPane.showConfirmDialog(this.owner, addFormulasDialogContents, "Add formulas", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                String input = textArea.getText();
                List<String> unparsed = new ArrayList<>();
                for (String formula : input.split("[\\s,;]+")) {
                    if (!formula.isBlank()) {
                        MolecularFormula mf = MolecularFormula.parseOrNull(formula);
                        if (mf != null && !mf.isEmpty()) {
                            providedFormulaListModel.addElement(mf.toString());
                        } else {
                            unparsed.add(formula);
                        }
                    }
                }
                if (!unparsed.isEmpty()) {
                    JOptionPane.showMessageDialog(this.owner, "Could not parse formulas:\n" + String.join("\n", unparsed), "", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        Action deleteSelectionAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selected = formulaList.getSelectedIndices();
                for (int i = selected.length - 1; i >= 0; i--) {
                    providedFormulaListModel.remove(selected[i]);
                }
            }
        };

        String deleteSelectionName = "deleteSelection";
        formulaList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), deleteSelectionName);
        formulaList.getActionMap().put(deleteSelectionName, deleteSelectionAction);

        removeFormulas.addActionListener(deleteSelectionAction);

        addStrategyChangeListener(s -> elementFilterPanel.setVisible(s != Strategy.PROVIDED));

        parameterBindings.put("CandidateFormulas", () -> strategy == Strategy.PROVIDED ? String.join(",", Arrays.stream(providedFormulaListModel.toArray()).map(x -> (String) x).toList()) : ",");
        //todo we will need a parameter binding to ignore the input file config in single-compute-mode. Hence, these CandidateFormulas are not overriden

        return card;
    }

    private void initDatabasePanel() {
        searchDBList = DBSelectionListPanel.newInstance("Use DB formulas only", gui.getSiriusClient(), Collections::emptyList);
        GuiUtils.assignParameterToolTip(searchDBList.checkBoxList, "FormulaSearchDB");

        searchDBList.selectDefaultDatabases();

        parameterBindings.put("FormulaSearchDB", () -> strategy == Strategy.DATABASE ? String.join(",", getFormulaSearchDBStrings()) : ",");
    }

    private JPanel createElementFilterPanel() {
        final FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        Set<Element> allAutoDetectableElements = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor().getSetOfPredictableElements(); //intersection of detectable elements of the used predictor and the specified detectable alphabet

        final TwoColumnPanel filterFields = new TwoColumnPanel();

        JLabel constraintsLabel = new JLabel("Allowed elements");
        elementFilterEnforcedTextBox = makeParameterTextField("FormulaSettings.enforced", 20);
        elementFilterEnforcedTextBox.setEditable(false); //todo if we want to allow editing this text we need validation.

        JLabel autodetectLabel = new JLabel("Autodetect");
        elementFilterDetectableElementsTextBox = isBatchDialog ? makeParameterTextField("FormulaSettings.detectable", 20) : null;
        if (elementFilterDetectableElementsTextBox != null) {
            elementFilterDetectableElementsTextBox.setEditable(false);
            elementFilterDetectableElementsTextBox.setText(join(allAutoDetectableElements.stream().filter(e -> formulaSettings.getAutoDetectionElements().contains(e)).collect(Collectors.toList()))); //intersection of detectable elements of the used predictor and the specified detectable alphabet
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton buttonEdit = new JButton("…");  // Ellipsis symbol instead of ... because 1-char buttons don't get side insets
        buttonEdit.setToolTipText("Customize allowed elements and their quantities");
        buttonPanel.add(buttonEdit);
        JButton buttonAutodetect = !isBatchDialog ? new ElementDetectionButton(hasMs1AndIsSingleMode) : null;
        if (!isBatchDialog) {
            if (hasMs1AndIsSingleMode) {
                buttonAutodetect.addActionListener(e ->
                        detectElementsAndLoad(ecs.getFirst(), allAutoDetectableElements, elementFilterEnforcedTextBox));
            }
            buttonPanel.add(buttonAutodetect);
        }

        addDefaultStrategyElementFilterSettings(filterFields);

        List<Component> filterComponents = new ArrayList<>(List.of(constraintsLabel, elementFilterEnforcedTextBox, buttonPanel));
        if (elementFilterDetectableElementsTextBox != null) {
            filterComponents.addAll(List.of(autodetectLabel, elementFilterDetectableElementsTextBox));
        }
        int columnWidth = elementFilterEnforcedTextBox.getPreferredSize().width;
        int sidePanelWidth = buttonPanel.getPreferredSize().width;
        elementFilterForBottomUp = addElementFilterEnabledCheckboxForStrategy(filterFields, filterComponents, Strategy.BOTTOM_UP, columnWidth, sidePanelWidth);
        elementFilterForDatabase = addElementFilterEnabledCheckboxForStrategy(filterFields, filterComponents, Strategy.DATABASE, columnWidth, sidePanelWidth);

        parameterBindings.put("FormulaSearchSettings.applyFormulaConstraintsToBottomUp", () -> Boolean.toString(
                strategy == Strategy.BOTTOM_UP && elementFilterForBottomUp.isSelected()
                        || strategy == Strategy.DEFAULT && defaultStrategyElementFilterSelector.getSelectedItem() == ElementAlphabetStrategy.BOTH));
        parameterBindings.put("FormulaSearchSettings.applyFormulaConstraintsToDatabaseCandidates", () -> Boolean.toString(strategy == Strategy.DATABASE && elementFilterForDatabase.isSelected()));

        int constraintsGridY = filterFields.both.gridy;
        filterFields.add(constraintsLabel, elementFilterEnforcedTextBox);
        if (elementFilterDetectableElementsTextBox != null) {
            filterFields.add(autodetectLabel, elementFilterDetectableElementsTextBox);
        }


        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = constraintsGridY;
        c.gridheight = isBatchDialog ? 2 : 1;
        filterFields.add(buttonPanel, c);

        //open element selection panel
        buttonEdit.addActionListener(e -> {
            FormulaConstraints currentConstraints = FormulaConstraints.fromString(elementFilterEnforcedTextBox.getText());
            Set<Element> currentAuto = isBatchDialog ? getAutodetectableElementsInBatchMode(elementFilterDetectableElementsTextBox, allAutoDetectableElements) : null;
            ElementSelectionDialog dialog = new ElementSelectionDialog(owner, "Filter Elements", isBatchDialog ? allAutoDetectableElements : null, currentAuto, currentConstraints);
            if (dialog.isSuccess()) {
                elementFilterEnforcedTextBox.setText(dialog.getConstraints().toString(","));
                if (elementFilterDetectableElementsTextBox != null) {
                    elementFilterDetectableElementsTextBox.setText(join(dialog.getAutoDetect()));
                }
            }
        });

        //reset element filter when switching strategies
        addStrategyChangeListener(strategy -> {
            if (!isBatchDialog) {
                if (hasMs1AndIsSingleMode) {
                    detectElementsAndLoad(ecs.getFirst(), allAutoDetectableElements, elementFilterEnforcedTextBox);
                    buttonAutodetect.setToolTipText("Element detection has already been performed once opened the compute dialog."
                            + "Auto detectable element are: " + join(allAutoDetectableElements)
                            + ".\nIf no elements can be detected the following fallback is used: " + formulaSettings.getFallbackAlphabet().toString(",")
                            + ".\nAdditionally, the following default elements are always used: " + getEnforedElements(formulaSettings, allAutoDetectableElements).toString(","));
                } else {
                    setDefaultElements(Set.of(), elementFilterEnforcedTextBox);
                }
            } else {
                setDefaultElements(allAutoDetectableElements, elementFilterEnforcedTextBox);
            }
        });

        JPanel elementFilterPanel = applyDefaultLayout(new JPanel());
        elementFilterPanel.add(new TextHeaderBoxPanel("Element Filter", filterFields));

        return elementFilterPanel;
    }

    @Nullable
    private Set<Element> getAutodetectableElementsInBatchMode(JTextField detectableTextBox, Set<Element> fallbackAutoDetectableElements) {
        Set<Element> currentAuto = null;
        if (isBatchDialog) {
            try {
                currentAuto = ChemicalAlphabet.fromString(detectableTextBox.getText()).toSet();
            } catch (UnknownElementException ex) {
                currentAuto = fallbackAutoDetectableElements;
            }
        }
        return currentAuto;
    }

    private FormulaConstraints getOrganicElementsWithoutAutodetectables(Set<Element> autodetectables) {
        FormulaConstraints constraints = FormulaSettings.ORGANIC_ELEMENT_FILTER_CHNOPSBBrClIF;
        return removeElementsFromConstraints(constraints, autodetectables);
    }

    private FormulaConstraints removeElementsFromConstraints(FormulaConstraints constraints, Set<Element> remove) {
        Set<Element> alphabetWithoutRemoved = new HashSet<>(constraints.getChemicalAlphabet().toSet());
        alphabetWithoutRemoved.removeAll(remove);
        return constraints.intersection(alphabetWithoutRemoved.toArray(Element[]::new));
    }

    private void addDefaultStrategyElementFilterSettings(TwoColumnPanel filterFields) {
        defaultStrategyElementFilterSelector = new JComboBox<>(); //todo NewWorflow: implement this feature in sirius-libs
        List<ElementAlphabetStrategy> settingsElements = List.copyOf(EnumSet.allOf(ElementAlphabetStrategy.class));
        settingsElements.forEach(defaultStrategyElementFilterSelector::addItem);
        defaultStrategyElementFilterSelector.setSelectedItem(ElementAlphabetStrategy.DE_NOVO_ONLY);

        JLabel label = new JLabel("Apply element filter to");
        filterFields.add(label, defaultStrategyElementFilterSelector);

        strategyComponents.get(Strategy.DEFAULT).add(label);
        strategyComponents.get(Strategy.DEFAULT).add(defaultStrategyElementFilterSelector);
    }

    /**
     * @return the checkbox to turn on element filter
     */
    private JCheckBox addElementFilterEnabledCheckboxForStrategy(TwoColumnPanel filterFields, List<Component> filterComponents, Strategy s, int columnWidth, int sidePanelWidth) {
        JCheckBox useElementFilter = new JCheckBox() {
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

        JLabel label = new JLabel("Enable element filter");

        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        checkBoxPanel.add(useElementFilter);

        checkBoxPanel.setPreferredSize(new Dimension(columnWidth, checkBoxPanel.getPreferredSize().height));  // Prevent resizing on checking/unchecking

        int constraintsGridY = filterFields.both.gridy;
        filterFields.add(label, checkBoxPanel);
        useElementFilter.addActionListener(e -> filterComponents.forEach(c -> c.setVisible(useElementFilter.isSelected())));

        JPanel invisiblePanel = new JPanel();  // Prevent resizing on checking/unchecking
        invisiblePanel.setPreferredSize(new Dimension(sidePanelWidth, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = constraintsGridY;
        filterFields.add(invisiblePanel, c);

        strategyComponents.get(s).add(label);
        strategyComponents.get(s).add(checkBoxPanel);
        strategyComponents.get(s).add(useElementFilter);
        strategyComponents.get(s).add(invisiblePanel);

        return useElementFilter;
    }

    private String join(Collection<?> objects) {
        return objects.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    /**
     * only used in single mode, not in batch mode
     */
    private void detectElementsAndLoad(InstanceBean ec, Set<Element> autoDetectable, JTextField formulaConstraintsTextBox) {
        loadable.runInBackgroundAndLoad(/*"Detecting Elements...", */() -> detectElements(ec, autoDetectable, formulaConstraintsTextBox));
    }

    private void detectElements(InstanceBean ec, Set<Element> autoDetectable, JTextField formulaConstraintsTextBox) {
        String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
        if (ec.hasMs1()) {
            final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
            Ms2Experiment experiment = new MutableMs2Experiment(ec.asMs2Experiment(), false);
            FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
            formulaSettings = formulaSettings.autoDetect(autoDetectable.toArray(Element[]::new)).enforce(getEnforedElements(formulaSettings, autoDetectable));
            experiment.setAnnotation(FormulaSettings.class, formulaSettings);
            Set<PrecursorIonType> adducts = formulaIDConfigPanel.getSelectedAdducts().getAdducts();
            experiment.setAnnotation(AdductSettings.class, AdductSettings.newInstance(adducts, Collections.emptySet(), adducts, false, true));
            ProcessedInput pi = pp.preprocess(experiment);

            pi.getAnnotation(FormulaConstraints.class).
                    ifPresentOrElse(c -> formulaConstraintsTextBox.setText(c.toString(",")),
                            () -> Jobs.runEDTLater(() -> new ExceptionDialog(owner, notWorkingMessage)));
        }
    }

    /**
     * set default elements.
     * use special alphabet for bottom up and database
     * if no MS1 data is available, fallback is used.
     */
    protected void setDefaultElements(Set<Element> autoDetectable, JTextField formulaConstraintsTextBox) {
        FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        FormulaConstraints constraints = getEnforedElements(formulaSettings, autoDetectable)
                .getExtendedConstraints(removeElementsFromConstraints(formulaSettings.getFallbackAlphabet(), autoDetectable));
        formulaConstraintsTextBox.setText(constraints.toString(","));
    }

    protected FormulaConstraints getEnforedElements(FormulaSettings defaultFormulaSettings, Set<Element> autoDetectable) {
        return isBottomUpOrDatabaseStrategy() ? getOrganicElementsWithoutAutodetectables(autoDetectable) : defaultFormulaSettings.getEnforcedAlphabet();
    }

    protected void addStrategyChangeListener(Consumer<Strategy> consumer) {
        strategyBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            consumer.accept((Strategy) e.getItem());
        });
    }

    protected boolean isBottomUpOrDatabaseStrategy() {
        return (strategy == Strategy.BOTTOM_UP || strategy == Strategy.DATABASE);
    }


    public List<SearchableDatabase> getFormulaSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getFormulaSearchDBStrings() {
        return getFormulaSearchDBs().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toList());
    }

    public Strategy getSelectedStrategy() {
        return strategy;
    }

    private static class ElementDetectionButton extends JButton {
        private final boolean isActivatable;

        private ElementDetectionButton(boolean isActivatable) {
            this.isActivatable = isActivatable;
            setText("Re-detect");
            if (!isActivatable) {
                super.setEnabled(false);
                setToolTipText("Element detection requires MS1 spectrum with isotope pattern. " +
                        "\nSuggesting default set of elements.");
            }
        }

        @Override
        public void setEnabled(boolean b) {
            if (isActivatable) super.setEnabled(b);
        }
    }

    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        Strategy s = getStrategyFromPreset(preset);

        if (((DefaultComboBoxModel<Strategy>)strategyBox.getModel()).getIndexOf(s) == -1) {
            throw new UnsupportedOperationException("Strategy from the preset " + s + " is not available for the data.");
        }
        strategyBox.setSelectedItem(s);

        boolean applyElementFilterToBottomUp = Boolean.parseBoolean(preset.get("FormulaSearchSettings.applyFormulaConstraintsToBottomUp"));

        if (s == Strategy.DEFAULT) {
            denovoUpTo.setValue(Double.parseDouble(preset.get("FormulaSearchSettings.performDeNovoBelowMz")));
            defaultStrategyElementFilterSelector.setSelectedItem(applyElementFilterToBottomUp ? ElementAlphabetStrategy.BOTH : ElementAlphabetStrategy.DE_NOVO_ONLY);
        }

        if (s == Strategy.BOTTOM_UP) {
            elementFilterForBottomUp.setSelected(applyElementFilterToBottomUp);
        }

        if (s == Strategy.DATABASE) {
            searchDBList.checkBoxList.uncheckAll();
            searchDBList.select(SearchableDBAnnotation.makeDB(preset.get("FormulaSearchDB")));
            elementFilterForDatabase.setSelected(Boolean.parseBoolean(preset.get("FormulaSearchSettings.applyFormulaConstraintsToDatabaseCandidates")));
        }

        providedFormulaListModel.removeAllElements();
        if (s == Strategy.PROVIDED) {
            for (String c : preset.get("CandidateFormulas").split(",")) {
                providedFormulaListModel.addElement(c);
            }
        }

        FormulaConstraints enforced = FormulaConstraints.fromString(preset.get("FormulaSettings.enforced"));
        elementFilterEnforcedTextBox.setText(enforced.toString(","));

        if (elementFilterDetectableElementsTextBox != null) {
            FormulaConstraints detectable = FormulaConstraints.fromString(preset.get("FormulaSettings.detectable"));
            elementFilterDetectableElementsTextBox.setText(detectable.toString(","));
        }
    }

    private Strategy getStrategyFromPreset(Map<String, String> preset) {
        double bottomUpAbove = Double.parseDouble(preset.get("FormulaSearchSettings.performBottomUpAboveMz"));
        double deNovoBelow = Double.parseDouble(preset.get("FormulaSearchSettings.performDeNovoBelowMz"));

        if (bottomUpAbove == 0 && deNovoBelow > 0 && Double.isFinite(deNovoBelow)) return Strategy.DEFAULT;
        if (bottomUpAbove == 0 && deNovoBelow == 0) return Strategy.BOTTOM_UP;
        if (bottomUpAbove == Double.POSITIVE_INFINITY && deNovoBelow == Double.POSITIVE_INFINITY) return Strategy.DE_NOVO;

        if (bottomUpAbove == Double.POSITIVE_INFINITY && deNovoBelow == 0) {
            String dbs = preset.get("FormulaSearchDB");
            if (!dbs.isBlank() && !dbs.trim().equals(",") && !dbs.trim().equals(NO_DB)) {
                return Strategy.DATABASE;
            }

            String candidates = preset.get("CandidateFormulas");
            if (!candidates.isBlank() && !candidates.trim().equals(",")) {
                return Strategy.PROVIDED;
            }
        }

        throw new UnsupportedOperationException("Formula search strategy could not be determined from the parameters.");
    }
}
