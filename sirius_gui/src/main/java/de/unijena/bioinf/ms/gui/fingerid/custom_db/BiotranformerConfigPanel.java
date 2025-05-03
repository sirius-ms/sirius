package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.biotransformer.Cyp450Mode;
import de.unijena.bioinf.ms.biotransformer.MetabolicTransformation;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.BioTransformerOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.TextHeaderPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.utils.GuiUtils.MEDIUM_GAP;
import static de.unijena.bioinf.ms.gui.utils.GuiUtils.SMALL_GAP;
import static java.awt.event.ItemEvent.SELECTED;

public class BiotranformerConfigPanel extends SubToolConfigPanel<BioTransformerOptions> {
    public static final java.util.List<MetaboTransWrapper> OVERALL_TRANSFORMATIONS = makeOverallTransformations();
    private final JComboBox<MetaboTransWrapper> transformationModes;

    private static java.util.List<MetaboTransWrapper> makeOverallTransformations() {
        List<MetaboTransWrapper> trans = Arrays.stream(MetabolicTransformation.values())
                .map(MetaboTransWrapper::new)
                .collect(Collectors.toList());
        trans.add(MetabolicTransformation.ALL_HUMAN.ordinal() + 1, MetaboTransWrapper.SUPER_BIO);
        return trans;
    }

    public static final java.util.List<MetaboTransWrapper> SEQUENCE_TRANSFORMATIONS = Stream.concat(
            MetabolicTransformation.valueSequenceOnly().stream().map(MetaboTransWrapper::new),
            Stream.of(MetaboTransWrapper.NONE)
    ).toList();

    public BiotranformerConfigPanel() {
        super(BioTransformerOptions.class);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());

        TwoColumnPanel settings = new TwoColumnPanel();

        transformationModes = makeTransformationBox(OVERALL_TRANSFORMATIONS, OVERALL_TRANSFORMATIONS.getLast());
        settings.addNamed("Metabolic Transformation", transformationModes);

        JComboBox<Cyp450ModeWrapper> cypModeBox = new JComboBox<>();
        Stream.concat(Arrays.stream(Cyp450Mode.values()).map(Cyp450ModeWrapper::new), Stream.of(Cyp450ModeWrapper.NONE))
                .forEach(cypModeBox::addItem);
        cypModeBox.setSelectedItem(Cyp450ModeWrapper.DEFAULT);
        settings.addNamed("CYP450 Mode", cypModeBox);

        JSpinner iterations = makeIterationsSpinner((MetaboTransWrapper) transformationModes.getSelectedItem());
        JLabel iterationsLabel = new JLabel("Number of Reaction Iterations");
        settings.add(iterationsLabel, iterations);


        JPanel multiTransPanel = makeMultiTransPanel();
        settings.add(multiTransPanel);
        multiTransPanel.setEnabled(false);
        multiTransPanel.setVisible(false);


        transformationModes.addItemListener(evt -> {
            if (evt.getStateChange() == SELECTED) {
                //configure cypbox
                MetaboTransWrapper trans = (MetaboTransWrapper) evt.getItem();
                SpinnerNumberModel iterModel = ((SpinnerNumberModel) iterations.getModel());

                updateIterationSpinner(iterationsLabel, iterations, trans);
                if (trans.isTransformationSequence()) {
                    if (!cypModeBox.isEnabled()) {
                        cypModeBox.removeItem(Cyp450ModeWrapper.NONE);
                        cypModeBox.setSelectedItem(Cyp450ModeWrapper.DEFAULT);
                        cypModeBox.setEnabled(true);
                    }


                    multiTransPanel.setEnabled(true);
                    multiTransPanel.setVisible(true);
                } else if (trans.isSuperBio()) {
                    if (!cypModeBox.isEnabled()) {
                        cypModeBox.removeItem(Cyp450ModeWrapper.NONE);
                        cypModeBox.setSelectedItem(Cyp450ModeWrapper.DEFAULT);
                        cypModeBox.setEnabled(true);
                    }

                    multiTransPanel.setEnabled(false);
                    multiTransPanel.setVisible(false);
                } else {
                    MetabolicTransformation metaboTrans = trans.getMetaboTrans();
                    cypModeBox.setEnabled(metaboTrans.isCypMode());
                    cypModeBox.removeItem(Cyp450ModeWrapper.NONE);

                    if (cypModeBox.isEnabled()) {
                        cypModeBox.setSelectedItem(Cyp450ModeWrapper.DEFAULT);
                    } else {
                        cypModeBox.addItem(Cyp450ModeWrapper.NONE);
                        cypModeBox.setSelectedItem(Cyp450ModeWrapper.NONE);
                    }

                    multiTransPanel.setEnabled(false);
                    multiTransPanel.setVisible(false);
                }
            }
        });

        add(settings, BorderLayout.CENTER);

        //init states by firring event
        transformationModes.setSelectedItem(OVERALL_TRANSFORMATIONS.getFirst());

    }

    private static JPanel makeMultiTransPanel() {
        JPanel multiTransPanel = new JPanel(new MigLayout("fill", "[grow]10[]40[]10[]", ""));
        List<JComboBox<MetaboTransWrapper>> bioTransBoxes = new ArrayList<>();
        List<JSpinner> iterSpinners = new ArrayList<>();

        // Create components for 4 steps
        for (int i = 0; i < 4; i++) {
            JLabel stepLabel = new JLabel("Step " + (i + 1 ) + ":");
            multiTransPanel.add(stepLabel);

            // Create transformation dropdown - defaulting to NONE
            JComboBox<MetaboTransWrapper> bioTransBox = makeTransformationBox(SEQUENCE_TRANSFORMATIONS, MetaboTransWrapper.NONE);
            multiTransPanel.add(bioTransBox);
            bioTransBoxes.add(bioTransBox);

            // Create iterations label
            multiTransPanel.add(new JLabel("Iterations:"));

            // Create iterations dropdown
            JSpinner iterSpin = makeIterationsSpinner(null);
            iterSpin.setEnabled(false); // Disabled by default since NONE is selected
            multiTransPanel.add(iterSpin, "wrap");
            iterSpinners.add(iterSpin);

            // Add listener to update iterations when transformation changes
            final int currentStep = i;
            bioTransBox.addItemListener(evt -> {
                if (evt.getStateChange() == SELECTED) {
                    MetaboTransWrapper trans = (MetaboTransWrapper) evt.getItem();
                    updateIterationSpinner(null, iterSpin, trans);

                    if (trans.equals(MetaboTransWrapper.NONE)) {
                        bioTransBoxes.stream().skip(Math.max(1, currentStep + 1)).forEach(it -> {
                            it.setSelectedItem(MetaboTransWrapper.NONE);
                            it.setEnabled(false);
                        });
                    } else {
                        if (currentStep < bioTransBoxes.size() - 1)
                            bioTransBoxes.get(currentStep + 1).setEnabled(true);
                    }
                }
            });

            updateIterationSpinner(null, iterSpin, (MetaboTransWrapper) bioTransBox.getSelectedItem());
        }

        bioTransBoxes.stream().skip(1).forEach(it -> it.setEnabled(false));
        iterSpinners.stream().skip(1).forEach(it -> it.setEnabled(false));

        return new TextHeaderPanel<>("Specify biotransformation sequence", multiTransPanel, MEDIUM_GAP, SMALL_GAP);
    }

    public static JComboBox<MetaboTransWrapper> makeTransformationBox(@NotNull Collection<MetaboTransWrapper> possibleValues, @Nullable MetaboTransWrapper selected) {
        JComboBox<MetaboTransWrapper> box = new JComboBox<>();
        possibleValues.forEach(box::addItem);

        if (selected != null)
            box.setSelectedItem(selected);
        return box;
    }

    public static void updateIterationSpinner(@NotNull JSpinner spinnerToUpdate, @Nullable MetaboTransWrapper boundaries) {
        updateIterationSpinner(null, spinnerToUpdate, boundaries);
    }

    public static void updateIterationSpinner(@Nullable JLabel spinnerLabel, @NotNull JSpinner spinnerToUpdate, @Nullable MetaboTransWrapper boundaries) {
        SpinnerNumberModel iterModel = (SpinnerNumberModel) spinnerToUpdate.getModel();

        // reset to default
        iterModel.setMaximum(3);
        iterModel.setMinimum(1);
        iterModel.setValue(1);
        spinnerToUpdate.setEnabled(true);
        spinnerToUpdate.setVisible(true);
        if (spinnerLabel != null) {
            spinnerLabel.setEnabled(true);
            spinnerLabel.setVisible(true);
        }

        if (boundaries != null) {
            if (boundaries.isTransformationSequence()) {
                spinnerToUpdate.setEnabled(false);
                spinnerToUpdate.setVisible(false);
                if (spinnerLabel != null) {
                    spinnerLabel.setEnabled(false);
                    spinnerLabel.setVisible(false);
                }
            } else if (boundaries.isSuperBio()) {
                iterModel.setMaximum(4);
                iterModel.setMinimum(4);
                iterModel.setValue(4);
                spinnerToUpdate.setEnabled(false);
            } else if (boundaries.isTransformation()) {
                MetabolicTransformation metaboTrans = boundaries.getMetaboTrans();
                //configure iterations
                iterModel.setMaximum(metaboTrans.getMaxIterations());
                iterModel.setMinimum(metaboTrans.getMinIterations());
                iterModel.setValue(metaboTrans.getMinIterations());
                spinnerToUpdate.setEnabled(metaboTrans.getMaxIterations() > metaboTrans.getMinIterations());
            } else if (boundaries.isNone()) {
                iterModel.setMaximum(0);
                iterModel.setMinimum(0);
                iterModel.setValue(0);
                spinnerToUpdate.setEnabled(false);
            }
        }
    }

    public static JSpinner makeIterationsSpinner(@Nullable MetaboTransWrapper boundaries) {
        JSpinner iterations = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        iterations.setMinimumSize(new Dimension(70, 26));
        iterations.setPreferredSize(new Dimension(95, 26));
        iterations.setMaximumSize(new Dimension(120, 26));

        updateIterationSpinner(iterations, boundaries);
        return iterations;
    }

    @Getter
    public static class Cyp450ModeWrapper {
        public static final Cyp450ModeWrapper NONE = new Cyp450ModeWrapper("None");
        public static final Cyp450ModeWrapper DEFAULT = new Cyp450ModeWrapper(Cyp450Mode.COMBINED);

        @Nullable
        public final Cyp450Mode cyp450Mode;
        @NotNull
        public final String displayName;

        public Cyp450ModeWrapper(@NotNull String displayName) {
            this.cyp450Mode = null;
            this.displayName = displayName;
        }

        public Cyp450ModeWrapper(@NotNull Cyp450Mode cyp450Mode) {
            this.cyp450Mode = cyp450Mode;
            this.displayName = this.cyp450Mode.getDisplayName();
        }

        public boolean isNone() {
            return NONE.equals(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cyp450ModeWrapper that)) return false;
            return Objects.equals(cyp450Mode, that.cyp450Mode);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(cyp450Mode);
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    @Getter
    public static class MetaboTransWrapper {
        public static final MetaboTransWrapper NONE = new MetaboTransWrapper("None");
        public static final MetaboTransWrapper SUPER_BIO = new MetaboTransWrapper("Human and Human Gut Microbial (SuperBio)");

        @Nullable
        public final MetabolicTransformation metaboTrans;
        @NotNull
        public final String displayName;

        public MetaboTransWrapper(@NotNull String displayName) {
            this.metaboTrans = null;
            this.displayName = displayName;
        }

        public MetaboTransWrapper(@NotNull MetabolicTransformation transformation) {
            this.metaboTrans = transformation;
            this.displayName = this.metaboTrans.getDisplayName();
        }

        public boolean isTransformation() {
            return metaboTrans != null;
        }

        public boolean isTransformationSequence() {
            return isTransformation() && metaboTrans == MetabolicTransformation.HUMAN_CUSTOM_MULTI;
        }

        public boolean isSuperBio() {
            return SUPER_BIO.equals(this);
        }

        public boolean isNone() {
            return NONE.equals(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MetaboTransWrapper that)) return false;
            return Objects.equals(displayName, that.displayName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(displayName);
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }
}
