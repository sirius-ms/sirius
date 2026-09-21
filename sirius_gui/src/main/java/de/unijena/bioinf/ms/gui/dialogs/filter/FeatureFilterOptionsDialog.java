package de.unijena.bioinf.ms.gui.dialogs.filter;
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import com.formdev.flatlaf.FlatClientProperties;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.DBSelectionListPanel;
import de.unijena.bioinf.ms.gui.dialogs.ElementSelectionDialog;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.IntensityFilter;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.gui.utils.search.FilterEditorHost;
import de.unijena.bioinf.ms.gui.utils.search.FilterTerm;
import de.unijena.bioinf.ms.gui.utils.search.PanelFilterTerms;
import de.unijena.bioinf.ms.gui.utils.search.QueryEditorPanel;
import de.unijena.bioinf.ms.gui.utils.search.SearchRenderState;
import de.unijena.bioinf.ms.gui.utils.search.SearchableFieldsProvider;
import io.sirius.ms.sdk.model.SearchableField;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;

import javax.swing.*;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.NumberFormat;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

import static de.unijena.bioinf.ms.gui.utils.GuiUtils.MEDIUM_GAP;

/**
 * Dialog allows to adjust filter criteria of the {@link FeatureFilterModel} which is used to filter compound list.
 */
@Slf4j
public class FeatureFilterOptionsDialog extends JDialog implements ActionListener {

    /**
     * The embedded query editor: renders the full filter configuration (all tabs' widget state) plus
     * the user's own query as chips, live-updating as the widgets change, and lets the user type/edit
     * the query with autocomplete. Replaces the former plain "Fulltext search" field. See
     * {@link QueryEditorPanel}.
     */
    QueryEditorPanel queryEditor;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner;

    // The narrowest a threshold field gets, in columns of its own font: about eleven digits.
    private static final int THRESHOLD_FIELD_COLUMNS = 8;

    // Where the max m/z and max RT spinners land when stepped down from "Infinite" - above anything these
    // measure in practice, so arriving here excludes nothing that was not already excluded by leaving it.
    private static final double MZ_BELOW_INFINITE = 5000d;
    private static final double RT_BELOW_INFINITE = 10000d;
    public final JCheckboxListPanel<PrecursorIonType> adductOptions;
    JButton discard, apply;
    final JCheckBox deleteSelection, hasMs1, hasMsMs;

    final JCheckBox blankFilter;
    final JSpinner blankSpinner;

    final JSpinner intensitySpinner;
    // the unit of the minimum (absolute counts or percent of the run maximum) and the measure it applies to
    final JComboBox<IntensityFilter.Mode> intensityUnit;
    final JComboBox<IntensityFilter.Measure> intensityMeasure;
    // The intensity spinner shows the threshold of one mode at a time; the other mode's value is kept here so
    // switching the mode back and forth loses neither.
    private double intensityMinAbsolute, intensityMinPercent;
    private IntensityFilter.Mode shownIntensityMode;

    final FeatureFilterModel filterModel;
    final CompoundList compoundList;

    /** While true, live chip refreshes are skipped so a bulk widget update rebuilds the chips only once. */
    private boolean suppressChipRefresh = false;

    /** Set while a coalesced chip refresh is already queued for the end of this event turn. */
    private boolean chipRefreshPending = false;


    final SegmentedFilterToggle lipidFilter, pfasFilter;
    final PlaceholderTextField elementsField;

    final DBSelectionListPanel searchDBList;

    private final QualityFilterPanel overallQualityPanel;
    private final List<QualityFilterPanel> qualityPanels;

    private JTabbedPane centerTab;

    /**
     * The index' searchable fields (names, types, descriptions), fetched in the background when the
     * embedded editor opens its session. Also the source of the filter descriptions shown as tooltips -
     * the GUI must not depend on server-side definitions, it only knows what the API tells it.
     */
    private final SearchableFieldsProvider fieldsProvider;

    final SiriusGui gui;

    public FeatureFilterOptionsDialog(SiriusGui gui, FeatureFilterModel filterModel, CompoundList compoundList) {
        this(gui, filterModel, compoundList, null);
    }

    /**
     * @param selectFacetId if non-null, the tab owning that filter facet (see {@link #tabTitleForFacet})
     *                      is preselected - used when a search-bar chip is clicked to jump to its control.
     */
    public FeatureFilterOptionsDialog(SiriusGui gui, FeatureFilterModel filterModel, CompoundList compoundList,
                                      @Nullable String selectFacetId) {
        super(gui.getMainFrame(), "Filter configuration", true);
        this.gui = gui;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        this.fieldsProvider = new SearchableFieldsProvider(gui.getSiriusClient(), gui.getProjectManager().getProjectId());
        setPreferredSize(GuiUtils.getPreferredSizeLimitedByScreenSize(new Dimension(700, 800)));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true); // the embedded query editor's chip area benefits from extra room

        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, MEDIUM_GAP, 0, MEDIUM_GAP));
        add(optionsPanel, BorderLayout.CENTER);

        centerTab = new JTabbedPane();
        optionsPanel.add(centerTab, BorderLayout.NORTH);

        // search query + live view of the full filter configuration as chips (replaces the plain
        // fulltext field): the tab facets render as read-only chips (click jumps to their tab), the
        // user's own query is typed/edited here with autocomplete, and both are applied together on Apply.
        {
            Box searchPanel = Box.createVerticalBox();
            JXTitledSeparator sep = new JXTitledSeparator("Search query");
            sep.setToolTipText("Visualizes the final filter query that will be executed when applying the filter configuration.");
            searchPanel.add(sep);
            searchPanel.add(Box.createVerticalStrut(3));
            queryEditor = buildQueryEditor();
            searchPanel.add(queryEditor);
            searchPanel.add(Box.createVerticalStrut(10));
            optionsPanel.add(searchPanel, BorderLayout.CENTER);
        }


        //input data filters
        {
            final Box inputParameters = Box.createVerticalBox();
            // A TwoColumnPanel tab (Fold Change, Data Quality) puts its first heading 16px below the top;
            // this one is a plain box and has to say so itself, or the heading jumps when you switch tabs.
            inputParameters.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            centerTab.addTab("Input", inputParameters);

            inputParameters.add(Box.createVerticalStrut(11));
            // The thresholds sit centred between their own heading and the adducts: the section's rows defend
            // their height and a glue above and below them takes equal shares of whatever the tab has to spare.
            // Left to themselves the separator and the row block would absorb it, which puts all of it above.
            JXTitledSeparator thresholdsSeparator = new JXTitledSeparator("Thresholds");
            thresholdsSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, thresholdsSeparator.getPreferredSize().height));
            inputParameters.add(thresholdsSeparator);
            inputParameters.add(Box.createVerticalGlue());

            {
                // One grid for the three thresholds, each under a heading of its own, so its fields are named
                // "Min" and "Max" only. The two field columns are shared, which is what lines the rows up.
                JPanel thresholds = new JPanel(new GridBagLayout());
                thresholds.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

                minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
                maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10,
                        MZ_BELOW_INFINITE);
                ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxMzSpinner.getModel(), filterModel.getMaxMz()));
                addThresholdHeading(thresholds, 0, "m/z", null);
                addThresholdRow(thresholds, 1, "Min", minMzSpinner, "Max", maxMzSpinner, null);
                ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

                minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
                maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10,
                        RT_BELOW_INFINITE);
                ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxRtSpinner.getModel(), filterModel.getMaxRt()));
                addThresholdHeading(thresholds, 2, "RT (in sec)", null);
                addThresholdRow(thresholds, 3, "Min", minRtSpinner, "Max", maxRtSpinner, null);
                ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

                // MS1 intensity: a minimum only, absolute or in percent of the run maximum, of either the peak
                // height (apex) or the area under the peak
                IntensityFilter filter = filterModel.getIntensityFilter();
                intensityMinAbsolute = filter.getMinAbsolute();
                intensityMinPercent = filter.getMinPercent();
                intensitySpinner = makeSpinner(0, 0, Double.POSITIVE_INFINITY, 1000);

                // What the threshold means, as plain drop-downs rather than the segmented switches the lipid and
                // PFAS filters use: a filled capsule means "this filter is on" in this dialog, and neither of
                // these is a filter state - they only say how to read the number next to them. A drop-down also
                // has room to spell the unit out, which is how the percentage becomes findable at all.
                intensityUnit = new JComboBox<>(IntensityFilter.Mode.values());
                intensityUnit.setSelectedItem(filter.getMode());
                intensityUnit.setToolTipText(GuiUtils.formatToolTip(INTENSITY_MODE_TOOLTIP));
                showIntensityThresholdFor(filter.getMode());
                intensityUnit.addActionListener(e -> showIntensityThresholdFor(selectedIntensityMode()));

                intensityMeasure = new JComboBox<>(IntensityFilter.Measure.values());
                intensityMeasure.setSelectedItem(filter.getMeasure());
                intensityMeasure.setToolTipText(GuiUtils.formatToolTip(INTENSITY_MEASURE_TOOLTIP));

                // The look and feel keeps 6px of air on either side of a drop-down's text, which reads as a gap
                // between what is selected and the arrow. These two set the section's width, so it is trimmed.
                for (JComboBox<?> choice : List.of(intensityUnit, intensityMeasure))
                    choice.putClientProperty(FlatClientProperties.STYLE, "padding: 1,4,1,2");

                // "in" names the two drop-downs the way "Max" names the field beside it, and "as" separates the
                // unit the number is written in from what it measures.
                Box intensityChoices = Box.createHorizontalBox();
                intensityChoices.add(intensityUnit);
                intensityChoices.add(Box.createHorizontalStrut(GuiUtils.SMALL_GAP));
                intensityChoices.add(new JLabel("as"));
                intensityChoices.add(Box.createHorizontalStrut(GuiUtils.SMALL_GAP));
                intensityChoices.add(intensityMeasure);
                String intensityToolTip = GuiUtils.formatToolTip(INTENSITY_FILTER_TOOLTIP);
                addThresholdHeading(thresholds, 4, "Intensity", intensityToolTip);
                addThresholdRow(thresholds, 5, "Min", intensitySpinner, "in", intensityChoices, intensityToolTip);

                // A drop-down asks for more height than a spinner, and that sets the height of its whole row.
                // Held to the fields above it, the three rows keep one rhythm.
                int fieldHeight = intensitySpinner.getPreferredSize().height;
                for (JComboBox<?> choice : List.of(intensityUnit, intensityMeasure))
                    choice.setPreferredSize(new Dimension(choice.getPreferredSize().width, fieldHeight));

                // Every field asks for the width of the two drop-downs together, so a "Max" field ends where
                // "peak height" ends - they set the width because they are the ones with something to show,
                // and makeSpinner's 200px is dropped. Below that width a field falls back to the width of a
                // number, so a narrow tab squeezes an empty number field rather than a drop-down's text.
                // The height stays makeSpinner's, the one the other tabs' spinners have too.
                int fieldWidth = intensityChoices.getPreferredSize().width;
                for (JSpinner field : List.of(minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, intensitySpinner)) {
                    ((JSpinner.DefaultEditor) field.getEditor()).getTextField().setColumns(THRESHOLD_FIELD_COLUMNS);
                    field.setPreferredSize(null); // drop the forced width, so the editor is asked again
                    field.setMinimumSize(null);
                    field.setMinimumSize(new Dimension(field.getPreferredSize().width, fieldHeight));
                    field.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
                }

                thresholds.setMaximumSize(new Dimension(Integer.MAX_VALUE, thresholds.getPreferredSize().height));
                inputParameters.add(thresholds);
                inputParameters.add(Box.createVerticalGlue()); // the other half of the section's spare height
            }

            // Adduct filter
            {
                inputParameters.add(Box.createVerticalStrut(5));
                adductOptions = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Adducts", GuiUtils.formatToolTip("Select adducts to filter by. Selecting all or none means any adduct can pass."));
                adductOptions.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + H20 + Na]+"), false));
                adductOptions.checkBoxList.setVisibleRowCount(5);

                List<PrecursorIonType> ionizations = new ArrayList<>(filterModel.getPossibleAdducts());
                Collections.sort(ionizations);

                adductOptions.checkBoxList.replaceElements(ionizations);
                adductOptions.checkBoxList.uncheckAll();
                adductOptions.setEnabled(true);

                adductOptions.checkBoxList.checkAll(filterModel.getSelectedAdducts());

                // preset next to "all"/"none": select only the single-charged, monomeric adducts
                JButton singleAdducts = new JButton("single");
                singleAdducts.setToolTipText(GuiUtils.formatAndStripToolTip(
                        "Select only single-charged, monomeric adducts. SIRIUS can currently run its annotation " +
                                "(molecular formula, structure and compound class identification) only on features with " +
                                "such adducts, so this is the recommended selection."));
                singleAdducts.addActionListener(e -> {
                    // batch the (potentially many) check toggles into a single chip rebuild to avoid lag
                    suppressChipRefresh = true;
                    try {
                        adductOptions.checkBoxList.uncheckAll();
                        adductOptions.checkBoxList.checkAll(filterModel.getPossibleAdducts().stream()
                                .filter(FeatureFilterModel::isSupportedAdduct).toList());
                    } finally {
                        suppressChipRefresh = false;
                    }
                    if (queryEditor != null)
                        queryEditor.rebuild();
                });
                adductOptions.buttons.add(singleAdducts);

                // Held to its own height, like the threshold rows above it. A JPanel's maximum height is
                // unbounded, so left alone this panel takes almost everything the tab has to spare - measured
                // on a 700px tab: 586px for a panel that wants 137 - and inside it the button strip takes the
                // surplus, which is the empty band under "all/none/single". With both sections holding their
                // height, the spare height goes to the glues that are there to hold it.
                adductOptions.setMaximumSize(new Dimension(Integer.MAX_VALUE, adductOptions.getPreferredSize().height));
                inputParameters.add(adductOptions);
            }
            inputParameters.add(Box.createVerticalGlue());
        }

        {

            // fold change / blank subtraction filter

            final TwoColumnPanel foldParameters = new TwoColumnPanel();
            centerTab.addTab("Fold Change", foldParameters);

            foldParameters.add(Box.createVerticalStrut(6));
            foldParameters.add(new JXTitledSeparator("Minimum Fold Change Filters"));

            blankSpinner = makeSpinner(filterModel.getSampleBlankFoldChange().getCurrentMinFoldChange(), 0.1, Double.POSITIVE_INFINITY, 0.1);

            blankFilter = new JCheckBox("Filter Blanks");
            blankFilter.setSelected(filterModel.getSampleBlankFoldChange().isEnabled());
            blankFilter.setToolTipText(BLANK_FILTER_TOOLTIP);
            blankSpinner.setEnabled(filterModel.getSampleBlankFoldChange().isEnabled());

            blankFilter.addChangeListener((e) -> blankSpinner.setEnabled(blankFilter.isSelected() && blankFilter.isEnabled()));

            foldParameters.add(blankFilter, blankSpinner);
            foldParameters.addVerticalGlue();
        }

        {
            final TwoColumnPanel dataParameters = new TwoColumnPanel();
            centerTab.addTab("Data Quality", dataParameters);

            //MS data availability filter
            {
                dataParameters.add(Box.createVerticalStrut(6));
                dataParameters.add(new JXTitledSeparator("MS Data Quality"));
                hasMs1 = new JCheckBox("MS1");
                hasMs1.setToolTipText("Feature must have at least one MS1 spectrum");
                hasMs1.setSelected(filterModel.isHasMs1());
                hasMsMs = new JCheckBox("MS/MS");
                hasMsMs.setToolTipText("Feature must have at least one MS/MS spectrum");
                hasMsMs.setSelected(filterModel.isHasMsMs());

                Box box = Box.createHorizontalBox();
                box.add(hasMs1);
                box.add(Box.createHorizontalStrut(50));
                box.add(hasMsMs);
                box.add(Box.createHorizontalStrut(10));
                dataParameters.add(box);
            }

            //quality filter
            {
                dataParameters.add(Box.createVerticalStrut(5));
                dataParameters.add(new JXTitledSeparator("Feature Quality"));
                overallQualityPanel = new QualityFilterPanel(filterModel.getFeatureQualityFilter());
                dataParameters.addNamed("<html><b>Overall quality</b></html>", overallQualityPanel);
                dataParameters.add(Box.createVerticalStrut(5));
                qualityPanels = filterModel.getCategorizedQualityFilters().stream().map(qf -> {
                    QualityFilterPanel qfp = new QualityFilterPanel(qf);
                    dataParameters.addNamed(qf.getName(), qfp);
                    return qfp;
                }).toList();
            }

            dataParameters.addVerticalGlue();
        }

        final Box resultParameters = Box.createVerticalBox();
        resultParameters.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));
        centerTab.addTab("Results", resultParameters);

        // Confidence score filter
        {
            resultParameters.add(Box.createVerticalStrut(9));
            resultParameters.add(new JXTitledSeparator("Confidence score"));

            TwoColumnPanel min = new TwoColumnPanel();
            TwoColumnPanel max = new TwoColumnPanel();

            Box box = Box.createHorizontalBox();
            box.add(min);
            box.add(Box.createHorizontalGlue());
            box.add(max);

            minConfidenceSpinner = makeSpinner(filterModel.getCurrentMinConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
            maxConfidenceSpinner = makeSpinner(filterModel.getCurrentMaxConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
            min.addNamed("Min confidence", minConfidenceSpinner);
            max.addNamed("Max confidence", maxConfidenceSpinner);
            ensureCompatibleBounds(minConfidenceSpinner, maxConfidenceSpinner);

            resultParameters.add(box);

        }

        // Element filter
        {
            resultParameters.add(Box.createVerticalStrut(9));
            resultParameters.add(new JXTitledSeparator("Elements in top molecular formula"));

            JPanel elementSelector = new JPanel();
            elementSelector.setLayout(new BoxLayout(elementSelector, BoxLayout.X_AXIS));
            JButton selectElements = new JButton("...");
            elementsField = new PlaceholderTextField(20);
            if (filterModel.getElementFilter().isActive())
                elementsField.setText(filterModel.getElementFilter().getConstraints().toString());

            selectElements.addActionListener(e -> {
                FormulaConstraints elements = new ElementFilter(elementsField.getText()).getConstraints();
                ElementSelectionDialog diag = new ElementSelectionDialog(this, "Filter Elements", elements);
                elements = diag.getConstraints();
                if (elements.equals(FormulaConstraints.empty()))
                    elementsField.setText(null);
                else
                    elementsField.setText(elements.toString());
            });
            elementsField.setPlaceholder("Enter or select formula constraints");
            elementSelector.add(elementsField);
            elementSelector.add(selectElements);

            resultParameters.add(elementSelector);
        }

        // db filter
        {
            resultParameters.add(Box.createVerticalStrut(10));
            // The same list the compute panel offers, and for the same reason: "Bio Database" is a name for a
            // set of databases rather than one a feature can be a hit in, so it is not in the list and its
            // button ticks the databases it stands for instead. Nothing else here is preselected - a filter
            // starts off filtering nothing.
            searchDBList = DBSelectionListPanel.newInstance("Hit in structure DB", gui.getSiriusClient(),
                    Collections::emptyList);
            searchDBList.checkBoxList.setVisibleRowCount(5);
            searchDBList.checkBoxList.uncheckAll();

            candidateSpinner = makeSpinner(1, 1, 100, 1);
            // all / none / bio and the candidate count both belong below the list, and a border layout keeps
            // only the last thing put there - so they go down together as one footer.
            Box dbFooter = Box.createVerticalBox();
            dbFooter.add(searchDBList.buttons);
            dbFooter.add(new TwoColumnPanel("Candidates to check", candidateSpinner));
            searchDBList.addFooter(dbFooter);

            resultParameters.add(searchDBList);
            resultParameters.add(Box.createVerticalBox());

            if (filterModel.isDbFilterEnabled()) { //null check
                searchDBList.checkBoxList.checkAll(filterModel.getDbFilter().getDbs());
                candidateSpinner.setValue(filterModel.getDbFilter().getNumOfCandidates());
            }
        }

        // structure properties: two yes / no / do-not-filter criteria. Last block of the tab: below the
        // structure-DB controls, directly above the query editor.
        {
            resultParameters.add(Box.createVerticalStrut(10));
            resultParameters.add(new JXTitledSeparator("Structure Properties"));

            TwoColumnPanel classFilters = new TwoColumnPanel();

            // Both criteria use the same generic segment labels, so the two capsules come out identical
            // in size and line up; what is being asked is in the row name ("is Lipid: yes").
            lipidFilter = new SegmentedFilterToggle("any", "yes", "no",
                    fieldDescription(FeatureFilterModel.FIELD_LIPID,
                            "A lipid class was detected (El Gordo) for the top molecular formula of the feature."));
            lipidFilter.setFilterState(filterModel.getLipidClassDetected());
            // the name shows the same tooltip as the control, so hovering either explains the filter
            classFilters.add(liveToolTipLabel("Lipid detected", lipidFilter::composeToolTip), hugging(lipidFilter));

            pfasFilter = new SegmentedFilterToggle("any", "yes", "no",
                    fieldDescription(FeatureFilterModel.FIELD_PFAS,
                            "The feature carries a PFAS tag: SIRIUS found it in a PFAS homologous series, "
                                    + "or annotated a PFAS molecular formula or structure for it."));
            pfasFilter.setFilterState(filterModel.getPfasDetected());
            classFilters.add(liveToolTipLabel("PFAS detected", pfasFilter::composeToolTip), hugging(pfasFilter),
                    MEDIUM_GAP, false); // the two control rows need air between them

            resultParameters.add(classFilters);
        }

        // Reset lives in the embedded query editor now (its clear button, wired to resetFilter)
        discard = new JButton("Discard");
        discard.addActionListener(this);
        apply = new JButton("Apply");
        apply.addActionListener(this);

        // destructive delete option, left-aligned in the footer opposite Apply/Discard
        deleteSelection = new JCheckBox("<html>Delete <b>non-</b>matching</html>");
        deleteSelection.setSelected(false);
        deleteSelection.setToolTipText(GuiUtils.formatAndStripToolTip("Delete all non-matching features, reducing the project to the matching ones. Deleted features cannot be recovered."));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.add(deleteSelection);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(apply);
        buttons.add(discard);

        add(buttons, BorderLayout.SOUTH);

        setMaximumSize(GuiUtils.getEffectiveScreenSize());
        wireLiveChipRefresh();          // widget edits -> live re-render of the panel chips
        queryEditor.openSession(null);  // seed the user query from the shared doc + refresh autocomplete fields
        refreshBlankFilterAvailability(); // whatever is known now; the fetch below settles it
        Jobs.runInBackground(() -> {
            fieldsProvider.refreshIfStale(); // shared with the editor's own refresh: cached, so at most one call
            SwingUtilities.invokeLater(this::refreshBlankFilterAvailability);
        });
        configureActions();
        selectTabForFacet(selectFacetId); // jump to the clicked chip's control, if any
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    static final String BLANK_FILTER_TOOLTIP =
            "<html>Aligned feature must have at least this fold change<br> of sample feature intensity divided by blank feature intensity</html>";

    static final String NO_BLANK_FOLD_CHANGES_TOOLTIP =
            "<html>No blank removal fold changes have been computed for this project.<br>"
                    + "Assign the run types (sample / blank) of your LC-MS runs to compute them.</html>";

    /**
     * Greys the blank filter out while the project has no blank removal fold changes to filter on.
     * <p>
     * Without them the filter matches no feature at all - it searches one index field, and that field only
     * exists once something has written it - so enabling it would empty the feature list and say nothing about
     * why. The tooltip says what to do instead.
     * <p>
     * Free to ask: the searchable fields this dialog already fetches for its autocompletion and descriptions
     * name the fold change fields that actually occur, read from the index's segment metadata rather than by
     * looking through documents. So the question "is there anything to filter on" is answered by the field
     * list already in hand, and asked again when a fetch brings a newer one.
     */
    private void refreshBlankFilterAvailability() {
        boolean available = !fieldsProvider.knownToLackField(FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME);
        if (available == blankFilter.isEnabled())
            return;

        blankFilter.setEnabled(available);
        blankFilter.setToolTipText(available ? BLANK_FILTER_TOOLTIP : NO_BLANK_FOLD_CHANGES_TOOLTIP);
        // Unticked as well as disabled: a filter that cannot match anything is not one to keep switched on,
        // and this is the state Apply would write. Staged like every other edit here - Discard reverts it.
        if (!available)
            blankFilter.setSelected(false);
        blankSpinner.setEnabled(available && blankFilter.isSelected());
        blankSpinner.setToolTipText(available ? null : NO_BLANK_FOLD_CHANGES_TOOLTIP);

        int foldChangeTab = tabIndexOf("Fold Change");
        if (foldChangeTab >= 0)
            centerTab.setToolTipTextAt(foldChangeTab, available ? null : NO_BLANK_FOLD_CHANGES_TOOLTIP);

        queryEditor.rebuild(); // unticking programmatically does not fire the live-refresh listeners
    }

    /** The index of the tab with the given title, or -1 when there is none. */
    private int tabIndexOf(@NotNull String title) {
        for (int i = 0; i < centerTab.getTabCount(); i++)
            if (title.equals(centerTab.getTitleAt(i)))
                return i;
        return -1;
    }

    /** The tab title that owns a given filter facet id (see PanelQueryNodeFactory facet ids); null if unknown. */
    static @Nullable String tabTitleForFacet(@Nullable String facetId) {
        if (facetId == null)
            return null;
        String base = facetId.contains(".") ? facetId.substring(0, facetId.indexOf('.')) : facetId;
        return switch (base) {
            case "mz", "rt", "adducts", "intensity" -> "Input";
            case "blank" -> "Fold Change";
            case "hasMs1", "hasMsMs", "quality" -> "Data Quality";
            case "confidence", "elements", "lipid", "pfas", "db" -> "Results";
            default -> null;
        };
    }

    /** Preselects the tab owning {@code facetId}; a no-op (keeps the default tab) if it is unknown. */
    private void selectTabForFacet(@Nullable String facetId) {
        String title = tabTitleForFacet(facetId);
        if (title == null)
            return;
        for (int i = 0; i < centerTab.getTabCount(); i++)
            if (title.equals(centerTab.getTitleAt(i))) {
                centerTab.setSelectedIndex(i);
                return;
            }
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        String enterAction = "compute";
        String escAction = "abort";
        inputMap.put(enterKey, enterAction);
        inputMap.put(escKey, escAction);
        getRootPane().getActionMap().put(enterAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
                dispose();
            }
        });
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    static final String INTENSITY_FILTER_TOOLTIP =
            "Aligned feature must reach this MS1 intensity in at least one of the runs it was detected in, in raw instrument "
                    + "counts as in the quantification table. Zero means no intensity filter. Features without a known intensity do not pass.";

    static final String INTENSITY_MODE_TOOLTIP =
            "Whether the minimum is an intensity, or a percentage of the most intense feature of the same run. "
                    + "A percentage goes into the query as written (e.g. intensityApex:[10% TO *]) and keeps the features that "
                    + "reach that share of their run's maximum in at least one run.";

    static final String INTENSITY_MEASURE_TOOLTIP =
            "Which intensity the minimum applies to: the peak height at the apex of the chromatographic peak, or the area "
                    + "under the peak. Data imported without an LC-MS trace (peak lists) only has a peak height.";

    private IntensityFilter.Mode selectedIntensityMode() {
        return (IntensityFilter.Mode) intensityUnit.getSelectedItem();
    }

    /** Shows the threshold of {@code mode} in the intensity spinner, keeping the other mode's value for a switch back. */
    private void showIntensityThresholdFor(IntensityFilter.Mode mode) {
        stashIntensityThreshold();
        shownIntensityMode = mode;
        intensitySpinner.setModel(mode == IntensityFilter.Mode.ABSOLUTE
                ? new SpinnerNumberModel(intensityMinAbsolute, 0d, Double.POSITIVE_INFINITY, 1000d)
                : new SpinnerNumberModel(Math.min(intensityMinPercent, 100d), 0d, 100d, 1d));
        installIntensityUnitFormatter(); // a new model brings a new editor, and with it a new formatter
    }

    /**
     * Lets a percent sign typed into the threshold field set the unit, as an alternative to picking it from the
     * drop-down next to it. The sign is consumed doing so - the field goes on showing a bare number like every
     * other threshold in this dialog, and the drop-down moving is what says the sign was understood.
     * <p>
     * Wraps the editor's own formatter rather than replacing it, so the number keeps being read and written
     * exactly as every other spinner here reads and writes one.
     */
    private void installIntensityUnitFormatter() {
        JFormattedTextField field = ((JSpinner.DefaultEditor) intensitySpinner.getEditor()).getTextField();
        JFormattedTextField.AbstractFormatter delegate = field.getFormatter();
        if (delegate == null)
            return;
        field.setFormatterFactory(new DefaultFormatterFactory(new JFormattedTextField.AbstractFormatter() {
            @Override
            public String valueToString(Object value) throws ParseException {
                return delegate.valueToString(value); // a bare number, whatever the unit - see above
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                IntensityFilter.Mode typedMode = IntensityFilter.isPercentTyped(text)
                        ? IntensityFilter.Mode.RELATIVE_TO_RUN_MAX
                        : IntensityFilter.Mode.ABSOLUTE;
                String number = IntensityFilter.stripPercent(text);
                if (typedMode == shownIntensityMode)
                    return delegate.stringToValue(number);
                // The number is meant for the other unit, and the delegate would measure it against this one's
                // range - which is how a percentage's ceiling of 100 used to reject an absolute threshold that
                // only dropped the sign. So read it plainly and leave the shown value alone; the switch below
                // installs it under the unit it was typed for, with that unit's range.
                switchIntensityUnitTo(typedMode, parseTypedNumber(delegate, number));
                return intensitySpinner.getValue();
            }
        }));
    }

    /**
     * Reads a number the way the threshold field reads one, but without a range to fail against: the value is
     * about to move to another unit, whose range is not the one this editor guards.
     */
    private static double parseTypedNumber(@NotNull JFormattedTextField.AbstractFormatter delegate,
                                           @NotNull String number) throws ParseException {
        NumberFormat format = delegate instanceof NumberFormatter formatter && formatter.getFormat() instanceof NumberFormat numbers
                ? numbers
                : NumberFormat.getInstance();
        ParsePosition position = new ParsePosition(0);
        Number value = format.parse(number, position);
        if (value == null || position.getIndex() != number.length())
            throw new ParseException("Not a threshold: " + number, position.getErrorIndex());
        return value.doubleValue();
    }

    /**
     * Follows a unit the user typed rather than picked: takes the number over into that unit's threshold and
     * moves the drop-down, which then shows it. Deferred because it runs while the field is committing the very
     * value it is about to replace - and cleared first, so that value is not also stashed as the unit it was
     * leaving.
     */
    private void switchIntensityUnitTo(@NotNull IntensityFilter.Mode mode, double typed) {
        SwingUtilities.invokeLater(() -> {
            shownIntensityMode = null;
            if (mode == IntensityFilter.Mode.RELATIVE_TO_RUN_MAX)
                intensityMinPercent = Math.min(typed, 100d);
            else
                intensityMinAbsolute = typed;
            intensityUnit.setSelectedItem(mode);
        });
    }

    /** Remembers the spinner value as the threshold of the mode it currently shows. */
    private void stashIntensityThreshold() {
        if (shownIntensityMode == null)
            return;
        double value = getDoubleValue(intensitySpinner);
        if (shownIntensityMode == IntensityFilter.Mode.ABSOLUTE)
            intensityMinAbsolute = value;
        else
            intensityMinPercent = value;
    }

    /** Puts the intensity widgets back to "no intensity filter" with the default thresholds. */
    private void resetIntensityWidgets() {
        intensityMeasure.setSelectedItem(IntensityFilter.Measure.APEX);
        intensityUnit.setSelectedItem(IntensityFilter.Mode.ABSOLUTE); // its listener re-shows the absolute threshold
        intensityMinAbsolute = IntensityFilter.DEFAULT_MIN_ABSOLUTE;
        intensityMinPercent = IntensityFilter.DEFAULT_MIN_PERCENT;
        intensitySpinner.setValue(IntensityFilter.DEFAULT_MIN_ABSOLUTE);
    }

    private void saveChanges() {
        if (deleteSelection.isSelected()) {
            deleteSelectedCompoundsAndResetFilter();

        } else {
            applyToModel(filterModel);
            queryEditor.commitToDocument(); // bake the user query into the shared doc as part of Apply
            filterModel.fireUpdateCompleted();
        }
    }

    private void applyToModel(@NotNull FeatureFilterModel filterModel) {
        filterModel.setInverted(queryEditor.isInverted());
        filterModel.setQueryJoin(queryEditor.getQueryJoin());

        filterModel.setCurrentMinMz(getMinMz());
        filterModel.setCurrentMaxMz(getMaxMz());
        filterModel.setCurrentMinRt(getMinRt());
        filterModel.setCurrentMaxRt(getMaxRt());
        filterModel.setCurrentMinConfidence(getMinConfidence());
        filterModel.setCurrentMaxConfidence(getMaxConfidence());
        filterModel.setHasMs1(hasMs1.isSelected());
        filterModel.setHasMsMs(hasMsMs.isSelected());
        filterModel.setAdducts(new HashSet<>(adductOptions.checkBoxList.getCheckedItems()));

        overallQualityPanel.updateModel(filterModel.getFeatureQualityFilter());

        Iterator<QualityFilterPanel> qualityPanelIt = qualityPanels.iterator();
        Iterator<QualityFilter> qualityFilterIt = filterModel.getCategorizedQualityFilters().iterator();
        while (qualityPanelIt.hasNext() && qualityFilterIt.hasNext())
            qualityPanelIt.next().updateModel(qualityFilterIt.next());

        filterModel.setLipidClassDetected(lipidFilter.getFilterState());
        filterModel.setPfasDetected(pfasFilter.getFilterState());

        filterModel.setElementFilter(new ElementFilter(
                        elementsField.getText() == null || elementsField.getText().isBlank()
                                ? FormulaConstraints.empty()
                                : FormulaConstraints.fromString(elementsField.getText()),
//                        elementsMatchFormula.isSelected(), elementsMatchPrecursorFormula.isSelected()
                        true, false
                )
        );

        filterModel.setDbFilter(new DbFilter(searchDBList.checkBoxList.getCheckedItems(),
                ((SpinnerNumberModel) candidateSpinner.getModel()).getNumber().intValue()));

        filterModel.getSampleBlankFoldChange().setEnabled(blankFilter.isSelected());
        filterModel.getSampleBlankFoldChange().setCurrentMinFoldChange((Double) blankSpinner.getValue());

        stashIntensityThreshold();
        filterModel.getIntensityFilter().setMeasure((IntensityFilter.Measure) intensityMeasure.getSelectedItem());
        filterModel.getIntensityFilter().setMode(selectedIntensityMode());
        filterModel.getIntensityFilter().setMinAbsolute(intensityMinAbsolute);
        filterModel.getIntensityFilter().setMinPercent(intensityMinPercent);
    }

    /**
     * The description the search index reports for a filter's field, falling back to {@code fallback}
     * while the field list is still being fetched or when the index reports none. Read lazily (the
     * fields arrive in the background after the dialog opened), so the caller must not cache the result.
     */
    private Supplier<String> fieldDescription(@NotNull String fieldName, @Nullable String fallback) {
        return () -> fieldsProvider.getCached().stream()
                .filter(field -> fieldName.equals(field.getName()))
                .map(SearchableField::getDescription)
                .filter(description -> description != null && !description.isBlank())
                .findFirst().orElse(fallback);
    }

    /**
     * The control in a row that can be stretched by the layout while the control itself is not: the glue
     * behind it takes the extra width, so the capsule keeps hugging its segments.
     */
    private static Box hugging(@NotNull JComponent control) {
        Box row = Box.createHorizontalBox();
        row.add(control);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /**
     * The heading of one threshold in the grid: what the row below it measures, over the full width of the
     * grid, the way the tab's own sections are headed.
     */
    private static void addThresholdHeading(@NotNull JPanel grid, int row, @NotNull String name,
                                            @Nullable String toolTip) {
        JXTitledSeparator heading = new JXTitledSeparator(name);
        if (toolTip != null)
            heading.setToolTipText(toolTip);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        // air above every heading but the first, which has the section separator right above it
        c.insets = new Insets(row == 0 ? 0 : GuiUtils.MEDIUM_GAP, 0, GuiUtils.SMALL_GAP, 0);
        grid.add(heading, c);
    }

    /**
     * One row of the threshold grid: a named field on each side. The columns are shared by every row, so
     * the fields of the three thresholds line up under each other.
     *
     * @param maxName the name of the second field - "Max" for a range, or whatever else names what follows
     * @param toolTip shown on the row's first field, or null for none
     */
    private static void addThresholdRow(@NotNull JPanel grid, int row,
                                        @NotNull String minName, @NotNull JComponent minField,
                                        @NotNull String maxName, @NotNull JComponent maxField,
                                        @Nullable String toolTip) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridy = row;
        label.anchor = GridBagConstraints.EAST;
        // indented, so a row starts inside its heading rather than under the first letter of it
        label.insets = new Insets(0, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP);

        GridBagConstraints field = new GridBagConstraints();
        field.gridy = row;
        // The two field columns share what the tab has over or under what they asked for, in equal parts, so
        // widening the dialog widens the fields (as it does the confidence ones) and both halves stay equal.
        field.fill = GridBagConstraints.HORIZONTAL;
        field.weightx = 1;
        field.insets = new Insets(0, 0, GuiUtils.SMALL_GAP, 0);

        if (toolTip != null)
            minField.setToolTipText(toolTip);
        label.gridx = 0;
        grid.add(new JLabel(minName), label);
        field.gridx = 1;
        grid.add(minField, field);
        // the two halves of a row are told apart by the air between them, not just by their names
        label.gridx = 2;
        label.insets = new Insets(0, GuiUtils.LARGE_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP);
        grid.add(new JLabel(maxName), label);
        field.gridx = 3;
        grid.add(maxField, field);
    }

    /** A name label that asks for its tooltip when shown, so it always matches its control's. */
    private static JLabel liveToolTipLabel(@NotNull String text, @NotNull Supplier<String> toolTip) {
        JLabel label = new JLabel(text) {
            @Override
            public String getToolTipText() {
                return toolTip.get();
            }
        };
        // registered explicitly: setToolTipText only registers when the previous text was null, which
        // an overridden getToolTipText() never is (see SegmentedFilterToggle)
        ToolTipManager.sharedInstance().registerComponent(label);
        return label;
    }

    // --- embedded query editor (see queryEditor) ---

    /**
     * Builds the embedded {@link QueryEditorPanel}. Its panel chips are rendered from a live snapshot
     * of the dialog's own widget state ({@link #workingTerms()}), so they mirror the tabs without
     * touching the real model until Apply; the host wires the dialog's transaction boundary
     * (Esc = Discard, Enter with nothing left to add = Apply) and turns a model-chip click into a
     * jump to its tab.
     */
    private QueryEditorPanel buildQueryEditor() {
        SearchRenderState renderState = new SearchRenderState(fieldsProvider);
        FilterEditorHost jumpToTab = new FilterEditorHost() {
            @Override
            public void openEditorFor(@NotNull FilterTerm term) {
                selectTabForFacet(term.id()); // a model chip was clicked -> jump to its control
            }

            @Override
            public void removeFilter(@NotNull FilterTerm term) {
                resetFacet(term.id()); // the chip's x -> reset the backing widget (like every edit: until Apply)
            }
        };
        QueryEditorPanel.Host host = new QueryEditorPanel.Host() {
            @Override
            public void editorContentChanged() {
                // the chip area / autocomplete list changed height - relayout NORTH vs. the tabs
                queryEditor.revalidate();
                queryEditor.repaint();
            }

            @Override
            public void editorCloseRequested() {
                dispose(); // Esc inside the editor == Discard
            }

            @Override
            public void editorCommitRequested() {
                saveChanges(); // the editor has nothing left to add -> Enter == Apply
                dispose();
            }

            @Override
            public void editorHandoff(@NotNull Runnable openFullEditor) {
                openFullEditor.run(); // a model chip was clicked -> just jump to its tab, don't close
            }
        };
        // embedded: no in-editor funnel (the dialog has its own tabs); the clear button does a full
        // reset of every filter and the query (replacing the former separate Reset button)
        return new QueryEditorPanel(filterModel, fieldsProvider, this::workingTerms, jumpToTab, renderState,
                commit -> {}, () -> {}, host, true, null, this::resetFilter, this::resetFilterToDefaults);
    }

    /**
     * The active panel facets of the dialog's CURRENT (uncommitted) widget state, as filter terms.
     * Compiles the widgets into a throwaway model - exactly as the delete path does - so the chips
     * reflect the working copy without mutating the real model. Defensive: a half-typed element
     * formula (etc.) that cannot be parsed yields no chips rather than an error.
     */
    private List<FilterTerm> workingTerms() {
        try {
            FeatureFilterModel working = new FeatureFilterModel();
            applyToModel(working);
            return PanelFilterTerms.of(working, gui.getProperties().getConfidenceDisplayMode());
        } catch (Exception ex) {
            log.debug("Could not build the working filter snapshot for the live chips", ex);
            return List.of();
        }
    }

    /** Re-renders the panel chips whenever any filter widget changes, so they track the tabs live. */
    private void wireLiveChipRefresh() {
        // The chip rebuild is not free (it recompiles a throwaway model). A bulk widget update (e.g. the
        // adduct "single" preset checks many boxes at once) sets suppressChipRefresh and rebuilds ONCE at
        // the end instead of once per box, so the preset does not lag.
        Runnable refresh = () -> { if (!suppressChipRefresh) queryEditor.rebuild(); };
        // The database buttons (all / none / bio) tick their boxes one at a time, so a click arrives as a
        // burst of events - and rebuilding per event would recompile the throwaway model a dozen times over.
        // Collapsing the burst into one rebuild at the end of the turn handles every such button, including
        // any added later, which suppressing around a particular one would not.
        Runnable coalescedRefresh = this::requestChipRefresh;
        for (JSpinner s : new JSpinner[]{minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner,
                minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner, blankSpinner, intensitySpinner})
            s.addChangeListener(e -> refresh.run());
        for (JCheckBox c : new JCheckBox[]{hasMs1, hasMsMs, blankFilter})
            c.addActionListener(e -> refresh.run());
        intensityUnit.addActionListener(e -> refresh.run());
        intensityMeasure.addActionListener(e -> refresh.run());
        lipidFilter.onChange(refresh);
        pfasFilter.onChange(refresh);
        adductOptions.checkBoxList.addCheckBoxListener(e -> refresh.run());
        searchDBList.checkBoxList.addCheckBoxListener(e -> coalescedRefresh.run());
        overallQualityPanel.onChange(refresh);
        qualityPanels.forEach(p -> p.onChange(refresh));
        elementsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
        });
    }

    /**
     * Asks for a chip rebuild once this event turn is done, however many times it is asked in the meantime.
     */
    private void requestChipRefresh() {
        if (suppressChipRefresh || chipRefreshPending)
            return;
        chipRefreshPending = true;
        SwingUtilities.invokeLater(() -> {
            chipRefreshPending = false;
            if (!suppressChipRefresh)
                queryEditor.rebuild();
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == apply) {
            saveChanges();
        }
        this.dispose();
    }

    private void deleteSelectedCompoundsAndResetFilter() {
        // Build the query for the features to DELETE: everything that does NOT match the current filter.
        // Flipping the filter's inversion turns the "keep" (visible) query into its complement.
        FeatureFilterModel deleteMatcher = new FeatureFilterModel();
        deleteMatcher.setExecutableQueryForm(filterModel.getExecutableQueryForm()); // same query, same anchoring
        applyToModel(deleteMatcher);
        // applyToModel copies only the widget facets + inversion; the search-bar (user) query lives in
        // the editor and must be included, or the delete complement would not match the visible list
        String userQuery = queryEditor.userQuery();
        if (!userQuery.isBlank()) {
            try {
                deleteMatcher.getSearchTextDoc().insertString(0, userQuery, null);
            } catch (BadLocationException e) {
                log.error("Could not attach the search-bar query to the delete matcher", e);
            }
        }
        deleteMatcher.setInverted(!deleteMatcher.isInverted());
        Optional<String> deleteQuery = deleteMatcher.toLuceneQuery(gui.getProperties().getConfidenceDisplayMode());

        // A blank/absent query means no filter is active -> everything matches -> nothing is "non-matching"
        // to delete. Otherwise confirm this destructive, irreversible action before doing anything.
        if (deleteQuery.isPresent()) {
            if (!new QuestionDialog(gui.getMainFrame(), "Delete non-matching features",
                    "<html>This will <b>permanently delete</b> all features that do <b>not</b> match the current filter.<br>This cannot be undone. Continue?</html>",
                    (String) null).isSuccess())
                return; // aborted: leave the filter untouched; the dialog is disposed by the caller
        }

        // reset global filter and close
        resetFilter();
        saveChanges();
        dispose();

        // clear selection to prevent unnecessary updates during deletions
        gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection();

        if (deleteQuery.isEmpty())
            return;

        // Delete server-side by query (one call; cascades DB + search index) and refresh the project
        // counters and feature list.
        final String query = deleteQuery.get();
        Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Deleting non-matching features...", true, new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                updateProgress(-1, "Deleting non-matching features...");
                gui.getProjectManager().deleteAlignedFeaturesByQuery(query);
                return true;
            }
        });
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        adductOptions.checkBoxList.uncheckAll();
        overallQualityPanel.reset();
        qualityPanels.forEach(QualityFilterPanel::reset);

        lipidFilter.setFilterState(null);
        pfasFilter.setFilterState(null);
        elementsField.setText(null);
        searchDBList.checkBoxList.uncheckAll();
        deleteSelection.setSelected(false);
        hasMs1.setSelected(false);
        hasMsMs.setSelected(false);

        blankFilter.setSelected(false);

        queryEditor.setInverted(false); // inversion lives in the editor now
        // clear the user query and re-render the chips from the freshly-reset widget state (a
        // programmatic checkbox reset does not fire the live-refresh listeners)
        queryEditor.clearUserQuery();
    }

    /**
     * only reset values in the dialog, not the actual filter model.
     * <p>
     * Restores the DEFAULT filter configuration (SIRIUS's recommended starting filter) into the dialog
     * widgets instead of clearing them: MS/MS present, feature quality restricted to DECENT/GOOD, the
     * single-charged monomeric detected adducts selected, everything else off / at full range. Like every
     * edit here it is staged - it takes effect on Apply and is reverted by Discard/Esc.
     */
    private void resetFilterToDefaults() {
        // batch the widget updates into a single chip rebuild (see wireLiveChipRefresh)
        suppressChipRefresh = true;
        try {
            resetSpinnerValues();
            adductOptions.checkBoxList.uncheckAll();
            adductOptions.checkBoxList.checkAll(filterModel.getPossibleAdducts().stream()
                    .filter(FeatureFilterModel::isSupportedAdduct).toList());
            // default overall quality keeps only DECENT/GOOD; a fresh model carries exactly that default
            overallQualityPanel.setFromModel(new FeatureFilterModel().getFeatureQualityFilter());
            qualityPanels.forEach(QualityFilterPanel::reset);

            lipidFilter.setFilterState(null);
            pfasFilter.setFilterState(null);
            elementsField.setText(null);
            searchDBList.checkBoxList.uncheckAll();
            deleteSelection.setSelected(false);
            hasMs1.setSelected(false);
            hasMsMs.setSelected(true); // default: require MS/MS

            blankFilter.setSelected(false);
            resetIntensityWidgets();

            queryEditor.setInverted(false);
        } finally {
            suppressChipRefresh = false;
        }
        // clear the user query and re-render the chips from the freshly-set widget state
        queryEditor.clearUserQuery();
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
        minConfidenceSpinner.setValue(filterModel.getMinConfidence());
        maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
        candidateSpinner.setValue(1);
        blankSpinner.setValue(filterModel.getSampleBlankFoldChange().getMinFoldChange());
    }

    /**
     * Resets the single filter facet backing an embedded query-editor chip to its inactive state,
     * invoked when the chip's remove (x) is clicked. It only touches the dialog widgets - like any
     * other edit here, it takes effect on Apply and is reverted by Discard/Esc. The facet ids match
     * {@link de.unijena.bioinf.ms.gui.utils.filter.PanelQueryNodeFactory}; the base before any '.' is
     * the widget group (categorized quality is "quality.&lt;id&gt;").
     */
    private void resetFacet(@NotNull String facetId) {
        String base = facetId.contains(".") ? facetId.substring(0, facetId.indexOf('.')) : facetId;
        switch (base) {
            case "mz" -> {
                minMzSpinner.setValue(filterModel.getMinMz());
                maxMzSpinner.setValue(filterModel.getMaxMz());
            }
            case "rt" -> {
                minRtSpinner.setValue(filterModel.getMinRt());
                maxRtSpinner.setValue(filterModel.getMaxRt());
            }
            case "confidence" -> {
                minConfidenceSpinner.setValue(filterModel.getMinConfidence());
                maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
            }
            case "hasMs1" -> hasMs1.setSelected(false);
            case "hasMsMs" -> hasMsMs.setSelected(false);
            case "adducts" -> adductOptions.checkBoxList.uncheckAll();
            case "quality" -> resetQualityFacet(facetId);
            case "elements" -> elementsField.setText(null);
            case "blank" -> {
                blankFilter.setSelected(false);
                blankSpinner.setValue(filterModel.getSampleBlankFoldChange().getMinFoldChange());
            }
            case "intensity" -> resetIntensityWidgets();
            case "lipid" -> lipidFilter.setFilterState(null);
            case "pfas" -> pfasFilter.setFilterState(null);
            case "db" -> {
                searchDBList.checkBoxList.uncheckAll();
                candidateSpinner.setValue(1);
            }
            default -> {
                return; // unknown facet - nothing to reset, leave the chips untouched
            }
        }
        // a programmatic checkbox / combo reset does not fire the live-refresh listeners, so re-render
        queryEditor.rebuild();
    }

    /** Resets the overall ("quality") or a categorized ("quality.&lt;id&gt;") quality filter panel. */
    private void resetQualityFacet(@NotNull String facetId) {
        if (facetId.equals("quality")) {
            overallQualityPanel.reset();
            return;
        }
        String filterId = facetId.substring(facetId.indexOf('.') + 1);
        Iterator<QualityFilterPanel> panelIt = qualityPanels.iterator();
        Iterator<QualityFilter> filterIt = filterModel.getCategorizedQualityFilters().iterator();
        while (panelIt.hasNext() && filterIt.hasNext()) {
            QualityFilterPanel panel = panelIt.next();
            if (filterIt.next().getId().equals(filterId)) {
                panel.reset();
                return;
            }
        }
    }

    public double getMinMz() {
        return getDoubleValue(minMzSpinner);
    }

    public double getMaxMz() {
        return getDoubleValue(maxMzSpinner);
    }

    public double getMinRt() {
        return getDoubleValue(minRtSpinner);
    }

    public double getMaxRt() {
        return getDoubleValue(maxRtSpinner);
    }

    public double getMinConfidence() {
        return getDoubleValue(minConfidenceSpinner);
    }

    public double getMaxConfidence() {
        return getDoubleValue(maxConfidenceSpinner);
    }


    public double getDoubleValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().doubleValue();
    }

    public int getIntValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
    }

    private void ensureCompatibleBounds(JSpinner minSpinner, JSpinner maxSpinner) {
        minSpinner.addChangeListener(e -> {
            if (e.getSource() == minSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    maxSpinner.setValue(min);
                }
            }
        });

        maxSpinner.addChangeListener(e -> {
            if (e.getSource() == maxSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    minSpinner.setValue(max);
                }
            }
        });
    }

    public JSpinner makeSpinner(double value, double minimum, double maximum, double stepSize) {
        return makeSpinner(value, minimum, maximum, stepSize, Double.NaN);
    }

    /**
     * @param belowInfinite where the down arrow lands when the value is unbounded. Stepping down from infinity
     *                      arrives at infinity, so a spinner whose maximum means "no bound" needs somewhere to
     *                      go - high enough that nobody's data is cut off by picking it, low enough to be a
     *                      sensible place to start dialling a bound in from. {@code NaN} for a spinner whose
     *                      maximum is a real number and which therefore never faces the question.
     */
    public JSpinner makeSpinner(double value, double minimum, double maximum, double stepSize, double belowInfinite) {
        SpinnerNumberModel model = Double.isNaN(belowInfinite)
                ? new SpinnerNumberModel(value, minimum, maximum, stepSize)
                : new SpinnerNumberModel(value, minimum, maximum, stepSize) {
            @Override
            public Object getPreviousValue() {
                return Double.isInfinite(getNumber().doubleValue()) ? belowInfinite : super.getPreviousValue();
            }
        };
        JSpinner spinner = new JSpinner(model);
        spinner.setMinimumSize(new Dimension(200, 26));
        spinner.setPreferredSize(new Dimension(200, 26));

        return spinner;
    }

}
