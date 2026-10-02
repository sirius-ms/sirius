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
import de.unijena.bioinf.ms.gui.utils.filter.FilterSession;
import de.unijena.bioinf.ms.gui.utils.filter.IntensityFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FilterContext;
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.gui.utils.search.FilterEditorHost;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
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

    // the intensity threshold, one spinner per unit with that unit's range; the unit drop-down shows one of them
    final JSpinner intensityAbsoluteSpinner, intensityPercentSpinner;
    final JPanel intensityThreshold;
    // the unit of the minimum (absolute counts or percent of the run maximum) and the measure it applies to
    final JComboBox<IntensityFilter.Mode> intensityUnit;
    final JComboBox<IntensityFilter.Measure> intensityMeasure;

    final FeatureFilterModel filterModel;
    /**
     * The working value this dialog edits: the widgets and the embedded chips are both views of it (see
     * {@link FilterWidgetBindings}), Apply commits it, Discard drops it.
     */
    private final FilterSession session;
    /** The widget groups as two-way views of the working value, see {@link #wireBindings()}. */
    private final List<FilterWidgetBindings.WidgetBinding> bindings = new ArrayList<>();
    /** Set while the widgets are being seeded from the working value, so their listeners do not echo it back. */
    private boolean seeding = false;
    /** The bindings whose widgets changed in this event turn; their edits are applied once at its end. */
    private final Set<FilterWidgetBindings.WidgetBinding> pendingEdits = new LinkedHashSet<>();
    final CompoundList compoundList;


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
        this.session = new FilterSession(filterModel); // this dialog's working value; disposed with it
        FilterContext ctx = filterModel.getContext(); // bounds and detected adducts the widgets are built for
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

                minMzSpinner = makeSpinner(ctx.minMz(), ctx.minMz(), ctx.maxMz(), 10);
                maxMzSpinner = makeSpinner(ctx.maxMz(), ctx.minMz(), ctx.maxMz(), 10,
                        MZ_BELOW_INFINITE);
                ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxMzSpinner.getModel(), ctx.maxMz()));
                addThresholdHeading(thresholds, 0, "m/z", null);
                addThresholdRow(thresholds, 1, "Min", minMzSpinner, "Max", maxMzSpinner, null);
                ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

                minRtSpinner = makeSpinner(ctx.minRt(), ctx.minRt(), ctx.maxRt(), 10);
                maxRtSpinner = makeSpinner(ctx.maxRt(), ctx.minRt(), ctx.maxRt(), 10,
                        RT_BELOW_INFINITE);
                ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxRtSpinner.getModel(), ctx.maxRt()));
                addThresholdHeading(thresholds, 2, "RT (in sec)", null);
                addThresholdRow(thresholds, 3, "Min", minRtSpinner, "Max", maxRtSpinner, null);
                ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

                // MS1 intensity: a minimum only, absolute or in percent of the run maximum, of either the peak
                // height (apex) or the area under the peak
                // One spinner per unit, so each keeps its own range; seedAll() puts the working value's in.
                intensityAbsoluteSpinner = makeSpinner(0, 0, Double.POSITIVE_INFINITY, 1000);
                intensityPercentSpinner = makeSpinner(0, 0, IntensityFilter.MAX_PERCENT, 1);
                installIntensityUnitFormatter(intensityAbsoluteSpinner, IntensityFilter.Mode.ABSOLUTE);
                installIntensityUnitFormatter(intensityPercentSpinner, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX);
                CardLayout intensityCards = new CardLayout();
                intensityThreshold = new JPanel(intensityCards);
                intensityThreshold.add(intensityAbsoluteSpinner, IntensityFilter.Mode.ABSOLUTE.name());
                intensityThreshold.add(intensityPercentSpinner, IntensityFilter.Mode.RELATIVE_TO_RUN_MAX.name());

                // What the threshold means, as plain drop-downs rather than the segmented switches the lipid and
                // PFAS filters use: a filled capsule means "this filter is on" in this dialog, and neither of
                // these is a filter state - they only say how to read the number next to them. A drop-down also
                // has room to spell the unit out, which is how the percentage becomes findable at all.
                intensityUnit = new JComboBox<>(IntensityFilter.Mode.values());
                intensityUnit.setSelectedItem(IntensityFilter.DEFAULT.mode());
                intensityUnit.setToolTipText(GuiUtils.formatToolTip(INTENSITY_MODE_TOOLTIP));
                // the visible threshold follows the unit - a view of the drop-down, not an edit of the filter
                intensityUnit.addActionListener(e -> intensityCards.show(intensityThreshold,
                        ((IntensityFilter.Mode) Objects.requireNonNull(intensityUnit.getSelectedItem())).name()));

                intensityMeasure = new JComboBox<>(IntensityFilter.Measure.values());
                intensityMeasure.setSelectedItem(IntensityFilter.DEFAULT.measure());
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
                intensityAbsoluteSpinner.setToolTipText(intensityToolTip);
                intensityPercentSpinner.setToolTipText(intensityToolTip);
                addThresholdRow(thresholds, 5, "Min", intensityThreshold, "in", intensityChoices, intensityToolTip);

                // A drop-down asks for more height than a spinner, and that sets the height of its whole row.
                // Held to the fields above it, the three rows keep one rhythm.
                int fieldHeight = intensityAbsoluteSpinner.getPreferredSize().height;
                for (JComboBox<?> choice : List.of(intensityUnit, intensityMeasure))
                    choice.setPreferredSize(new Dimension(choice.getPreferredSize().width, fieldHeight));

                // Every field asks for the width of the two drop-downs together, so a "Max" field ends where
                // "peak height" ends - they set the width because they are the ones with something to show,
                // and makeSpinner's 200px is dropped. Below that width a field falls back to the width of a
                // number, so a narrow tab squeezes an empty number field rather than a drop-down's text.
                // The height stays makeSpinner's, the one the other tabs' spinners have too.
                int fieldWidth = intensityChoices.getPreferredSize().width;
                for (JSpinner field : List.of(minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner,
                        intensityAbsoluteSpinner, intensityPercentSpinner)) {
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

                List<PrecursorIonType> ionizations = new ArrayList<>(ctx.possibleAdducts());
                Collections.sort(ionizations);

                adductOptions.checkBoxList.replaceElements(ionizations);
                adductOptions.checkBoxList.uncheckAll();
                adductOptions.setEnabled(true);

                // preset next to "all"/"none": select only the single-charged, monomeric adducts
                JButton singleAdducts = new JButton("single");
                singleAdducts.setToolTipText(GuiUtils.formatAndStripToolTip(
                        "Select only single-charged, monomeric adducts. SIRIUS can currently run its annotation " +
                                "(molecular formula, structure and compound class identification) only on features with " +
                                "such adducts, so this is the recommended selection."));
                // one edit of the working value; the list follows it like every other widget
                singleAdducts.addActionListener(e -> session.edit(f -> f.withAdducts(session.context().supportedPossibleAdducts())));
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

            blankSpinner = makeSpinner(FeatureFilter.DEFAULT_MIN_BLANK_FOLD_CHANGE, 0.1, Double.POSITIVE_INFINITY, 0.1);

            blankFilter = new JCheckBox("Filter Blanks");
            blankFilter.setToolTipText(BLANK_FILTER_TOOLTIP);
            blankSpinner.setEnabled(false); // follows the check box (listener below) once the widgets are seeded

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
                hasMsMs = new JCheckBox("MS/MS");
                hasMsMs.setToolTipText("Feature must have at least one MS/MS spectrum");

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
                overallQualityPanel = new QualityFilterPanel();
                dataParameters.addNamed("<html><b>Overall quality</b></html>", overallQualityPanel);
                dataParameters.add(Box.createVerticalStrut(5));
                // one slider per category, in category order - wireBindings() relies on that order
                qualityPanels = Arrays.stream(DefaultQualityCategory.values()).map(category -> {
                    QualityFilterPanel qfp = new QualityFilterPanel();
                    dataParameters.addNamed(category.getDisplayName(), qfp);
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

            minConfidenceSpinner = makeSpinner(ctx.minConfidence(), ctx.minConfidence(), ctx.maxConfidence(), .05);
            maxConfidenceSpinner = makeSpinner(ctx.maxConfidence(), ctx.minConfidence(), ctx.maxConfidence(), .05);
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
            // the name shows the same tooltip as the control, so hovering either explains the filter
            classFilters.add(liveToolTipLabel("Lipid detected", lipidFilter::composeToolTip), hugging(lipidFilter));

            pfasFilter = new SegmentedFilterToggle("any", "yes", "no",
                    fieldDescription(FeatureFilterModel.FIELD_PFAS,
                            "The feature carries a PFAS tag: SIRIUS found it in a PFAS homologous series, "
                                    + "or annotated a PFAS molecular formula or structure for it."));
            classFilters.add(liveToolTipLabel("PFAS detected", pfasFilter::composeToolTip), hugging(pfasFilter),
                    MEDIUM_GAP, false); // the two control rows need air between them

            resultParameters.add(classFilters);
        }

        // Clear and Restore live in the embedded query editor (they edit the working value the widgets follow)
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
        wireBindings();                 // widgets <-> working value, both ways
        seedAll();                      // the widgets show the working value (= the applied filter) from the start
        queryEditor.openSession(null);  // working = applied, user query hydrated into chips, autocomplete fields refreshed
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

        pushAll(); // unticking programmatically does not fire the widget listeners
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
                    + "A percentage searches the relative intensity field (e.g. intensity.apexPercent:[10 TO *]) and keeps "
                    + "the features that reach that share of their run's maximum in at least one run.";

    static final String INTENSITY_MEASURE_TOOLTIP =
            "Which intensity the minimum applies to: the peak height at the apex of the chromatographic peak, or the area "
                    + "under the peak. Data imported without an LC-MS trace (peak lists) only has a peak height.";

    /**
     * Lets what is typed into the threshold field set the unit, as an alternative to picking it from the
     * drop-down next to it: a percent sign, or a number too large to be a percentage (see
     * {@link IntensityFilter#typedUnit}). A sign is consumed doing so - the field goes on showing a bare number
     * like every other threshold in this dialog, and the drop-down moving is what says it was understood.
     * <p>
     * Wraps the editor's own formatter rather than replacing it, so the number keeps being read and written
     * exactly as every other spinner here reads and writes one.
     */
    private void installIntensityUnitFormatter(@NotNull JSpinner spinner, @NotNull IntensityFilter.Mode unit) {
        JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
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
                // Read range-free first: which unit this is meant for depends on the number, and the delegate
                // would measure it against the range of the unit currently shown - which is how a percentage's
                // ceiling of 100 used to reject an absolute threshold that only dropped its sign.
                String number = IntensityFilter.stripPercent(text);
                double typed = parseTypedNumber(delegate, number);
                IntensityFilter.Mode typedMode = IntensityFilter.typedUnit(text, typed, unit);
                if (typedMode == unit)
                    return delegate.stringToValue(number);
                // Meant for the other unit: leave this spinner's value alone, the switch puts it into the other
                // one, with that unit's range.
                switchIntensityUnitTo(typedMode, typed);
                return spinner.getValue();
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
     * Follows a unit the user typed rather than picked: an edit of the working value that selects the unit and
     * sets its threshold, which the seed then shows by moving the drop-down - and with it the visible spinner.
     * Deferred because it runs while the field is committing the very value it is about to replace.
     */
    private void switchIntensityUnitTo(@NotNull IntensityFilter.Mode mode, double typed) {
        double threshold = mode == IntensityFilter.Mode.RELATIVE_TO_RUN_MAX ? Math.min(typed, IntensityFilter.MAX_PERCENT) : typed;
        SwingUtilities.invokeLater(() -> session.edit(f -> f.withIntensity(f.intensity().withMode(mode).withMin(mode, threshold))));
    }

    @Override
    public void dispose() {
        session.dispose(); // stop following the applied filter; this dialog's session dies with it
        queryEditor.dispose();
        super.dispose();
    }

    private void saveChanges() {
        if (deleteSelection.isSelected()) {
            deleteSelectedCompoundsAndResetFilter();

        } else {
            pushAll(); // programmatic widget changes do not fire the widget listeners
            queryEditor.commitUserQuery();
            session.commit(); // widgets, inversion, joint and query in one publish, one re-filter
        }
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
     * Builds the embedded {@link QueryEditorPanel}. Its panel chips render the session's working value,
     * of which the widgets are views too ({@link #wireBindings()}), so chips and tabs always agree and the
     * applied filter is untouched until Apply; the host wires the dialog's transaction boundary
     * (Esc = Discard, Enter with nothing left to add = Apply) and turns a model-chip click into a
     * jump to its tab.
     */
    private QueryEditorPanel buildQueryEditor() {
        SearchRenderState renderState = new SearchRenderState(fieldsProvider);
        FilterEditorHost jumpToTab = term -> selectTabForFacet(term.id()); // a model chip was clicked -> jump to its control
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
        // embedded: no in-editor funnel (the dialog has its own tabs) and no quick switches (the tabs are
        // the full editor); Clear and Restore edit the working value, which the widgets follow
        return new QueryEditorPanel(session, fieldsProvider,
                () -> PanelFilterTerms.of(session, gui.getProperties().getConfidenceDisplayMode()), jumpToTab, renderState,
                commit -> {}, () -> {}, host, true, null, List.of());
    }

    // --- widgets <-> working value ---

    /**
     * Makes every widget group a two-way view of the session's working value: a widget change is applied to
     * the working value through its binding's {@code read}, and every change of the working value (from a
     * widget, a chip's remove, Clear, Restore, an import) is written into all widgets through {@code seed}.
     * Seeding never echoes back: the listeners are muted while it runs, and each seed writes only what
     * differs. Check-box lists fire one event per box, so their edits are coalesced to one per event turn.
     */
    private void wireBindings() {
        var mz = FilterWidgetBindings.range("mz", minMzSpinner, maxMzSpinner, FeatureFilter::mz, FeatureFilter::withMz);
        var rt = FilterWidgetBindings.range("rt", minRtSpinner, maxRtSpinner, FeatureFilter::rt, FeatureFilter::withRt);
        var confidence = FilterWidgetBindings.range("confidence", minConfidenceSpinner, maxConfidenceSpinner,
                FeatureFilter::confidence, FeatureFilter::withConfidence);
        var ms1 = FilterWidgetBindings.flag("hasMs1", hasMs1, FeatureFilter::hasMs1, FeatureFilter::withHasMs1);
        var msms = FilterWidgetBindings.flag("hasMsMs", hasMsMs, FeatureFilter::hasMsMs, FeatureFilter::withHasMsMs);
        var adducts = FilterWidgetBindings.adducts(adductOptions.checkBoxList);
        var quality = FilterWidgetBindings.quality("quality", overallQualityPanel, FeatureFilter::featureQuality, FeatureFilter::withFeatureQuality);
        var elements = FilterWidgetBindings.elements(elementsField);
        var dbs = FilterWidgetBindings.databases(searchDBList.checkBoxList, candidateSpinner);
        var blank = FilterWidgetBindings.blankFoldChange(blankFilter, blankSpinner);
        var intensity = FilterWidgetBindings.intensity(intensityMeasure, intensityUnit,
                intensityAbsoluteSpinner, intensityPercentSpinner);
        var lipid = FilterWidgetBindings.tristate("lipid", lipidFilter, FeatureFilter::lipidClassDetected, FeatureFilter::withLipidClassDetected);
        var pfas = FilterWidgetBindings.tristate("pfas", pfasFilter, FeatureFilter::pfasDetected, FeatureFilter::withPfasDetected);
        bindings.addAll(List.of(mz, rt, intensity, confidence, ms1, msms, adducts, quality));
        // the categorized quality panels were built in category order (see the constructor)
        DefaultQualityCategory[] categories = DefaultQualityCategory.values();
        for (int i = 0; i < qualityPanels.size(); i++) {
            DefaultQualityCategory category = categories[i];
            var categorized = FilterWidgetBindings.quality("quality." + category.name(), qualityPanels.get(i),
                    f -> f.categorizedQuality().get(category), (f, selection) -> f.withCategorizedQuality(category, selection));
            bindings.add(categorized);
            qualityPanels.get(i).onChange(() -> edit(categorized));
        }
        bindings.addAll(List.of(elements, dbs, blank, lipid, pfas));

        // widgets -> working value
        for (JSpinner spinner : new JSpinner[]{minMzSpinner, maxMzSpinner})
            spinner.addChangeListener(e -> edit(mz));
        for (JSpinner spinner : new JSpinner[]{minRtSpinner, maxRtSpinner})
            spinner.addChangeListener(e -> edit(rt));
        for (JSpinner spinner : new JSpinner[]{minConfidenceSpinner, maxConfidenceSpinner})
            spinner.addChangeListener(e -> edit(confidence));
        intensityAbsoluteSpinner.addChangeListener(e -> edit(intensity));
        intensityPercentSpinner.addChangeListener(e -> edit(intensity));
        intensityUnit.addActionListener(e -> edit(intensity));
        intensityMeasure.addActionListener(e -> edit(intensity));
        hasMs1.addActionListener(e -> edit(ms1));
        hasMsMs.addActionListener(e -> edit(msms));
        adductOptions.checkBoxList.addCheckBoxListener(e -> editLater(adducts)); // all / none / single tick many boxes at once
        overallQualityPanel.onChange(() -> edit(quality));
        elementsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { edit(elements); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { edit(elements); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { edit(elements); }
        });
        searchDBList.checkBoxList.addCheckBoxListener(e -> editLater(dbs)); // all / none / bio tick many boxes at once
        candidateSpinner.addChangeListener(e -> edit(dbs));
        blankFilter.addActionListener(e -> edit(blank));
        blankSpinner.addChangeListener(e -> edit(blank));
        lipidFilter.onChange(() -> edit(lipid));
        pfasFilter.onChange(() -> edit(pfas));

        // working value -> widgets
        session.subscribe(this::seedAll);
    }

    /** Applies one widget group's state to the working value - unless the widgets are just being seeded from it. */
    private void edit(@NotNull FilterWidgetBindings.WidgetBinding binding) {
        if (seeding)
            return;
        session.edit(binding.read());
    }

    /** {@link #edit} once at the end of this event turn, however many events the widget fires until then. */
    private void editLater(@NotNull FilterWidgetBindings.WidgetBinding binding) {
        if (seeding)
            return;
        boolean first = pendingEdits.isEmpty();
        pendingEdits.add(binding);
        if (first)
            SwingUtilities.invokeLater(() -> {
                List<FilterWidgetBindings.WidgetBinding> due = new ArrayList<>(pendingEdits);
                pendingEdits.clear();
                due.forEach(this::edit);
            });
    }

    /** Writes the working value into every widget group, with the widget listeners muted. */
    private void seedAll() {
        if (seeding)
            return;
        seeding = true;
        try {
            FeatureFilter working = session.working();
            bindings.forEach(binding -> binding.seed().accept(working));
        } finally {
            seeding = false;
        }
    }

    /**
     * Applies every widget group to the working value at once - the safety net before a commit, for widget
     * changes made programmatically (which fire no listener).
     */
    private void pushAll() {
        session.edit(f -> {
            for (FilterWidgetBindings.WidgetBinding binding : bindings)
                f = binding.read().apply(f);
            return f;
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
        pushAll(); // programmatic widget changes fire no listener
        queryEditor.commitUserQuery(); // the working value now carries widgets, inversion, joint and query
        FeatureFilterModel deleteMatcher = new FeatureFilterModel(filterModel.getContext());
        deleteMatcher.set(session.working().withInverted(!session.working().inverted()));
        Optional<String> deleteQuery = deleteMatcher.toLuceneQuery(gui.getProperties().getConfidenceDisplayMode());

        // A blank/absent query means no filter is active -> everything matches -> nothing is "non-matching"
        // to delete. Otherwise confirm this destructive, irreversible action before doing anything.
        if (deleteQuery.isPresent()) {
            if (!new QuestionDialog(gui.getMainFrame(), "Delete non-matching features",
                    "<html>This will <b>permanently delete</b> all features that do <b>not</b> match the current filter.<br>This cannot be undone. Continue?</html>",
                    (String) null).isSuccess())
                return; // aborted: leave the filter untouched; the dialog is disposed by the caller
        }

        // reset the filter (nothing filtered, no query) and close
        queryEditor.clearAll();
        session.commit();
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
