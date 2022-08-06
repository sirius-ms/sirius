/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is the wrapper for the Instance class to interact with the gui
 * elements. It uses a special property change support that executes the events
 * in the EDT. So you can change all fields from any thread, the gui will still
 * be updated in the EDT. Some operations may NOT be Thread save, so you may have
 * to care about Synchronization.
 */
public class InstanceBean extends Instance implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    //Project-space listener
    private List<ContainerListener.Defined> listeners;

    //todo best hit property change is needed.
    // e.g. if the scoring changes from sirius to zodiac

    //todo make compute state nice
    //todo we may nee background loading tasks for retriving informaion from project space

    //todo som unregister listener stategy

    public InstanceBean(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager<InstanceBean> spaceManager) {
        super(compoundContainer, spaceManager);
    }

    //todo these listeners should be project wide to dramatically reduce number of listeners
    private List<ContainerListener.Defined> configureListeners() {
        final List<ContainerListener.Defined> listeners = new ArrayList<>(3);

        listeners.add(projectSpace().defineCompoundListener().onUpdate().onlyFor(Ms2Experiment.class).onlyFor(getID()).thenDo((event ->
                pcs.firePropertyChange("instance.ms2Experiment", null, event.getAffectedComponent(Ms2Experiment.class)))));

        listeners.add(projectSpace().defineFormulaResultListener().onCreate().thenDo((event -> {
            if (!event.getAffectedID().getParentId().equals(getID()))
                return;
            pcs.firePropertyChange("instance.createFormulaResult", null, event.getAffectedID());
        })));

        listeners.add(projectSpace().defineFormulaResultListener().onDelete().thenDo((event -> {
            if (!event.getAffectedID().getParentId().equals(getID()))
                return;
            pcs.firePropertyChange("instance.deleteFormulaResult", event.getAffectedID(), null);
        })));

        return listeners;

    }

    protected void addToCache() {
        synchronized (spaceManager){
            ((GuiProjectSpaceManager) spaceManager).ringBuffer.add(this);
        }
    }


    @Override
    public synchronized void deleteFormulaResults(@Nullable Collection<FormulaResultId> ridToRemove) {
        List<ContainerListener.Defined> changed = List.of();
        try {
            changed = unregisterProjectSpaceListeners();
            //load contain methods to ensure that it is available
            final CompoundContainer ccache = loadCompoundContainer();

            List<FormulaResultId> old = getResults().stream().map(FormulaResultBean::getID).collect(Collectors.toList());
            List<FormulaResultId> nu = new ArrayList<>(old);
            ArrayList<FormulaResultId> remove = new ArrayList<>(old);
            if (ridToRemove != null){
                remove.retainAll(new HashSet<>(ridToRemove));
                nu.removeAll(ridToRemove);
            }

//            remove.forEach(ccache::removeResult);
            clearFormulaResultsCache();

            pcs.firePropertyChange("instance.clearFormulaResults", old.isEmpty() ? nu : old, nu);

            remove.forEach(v -> {
                try {
                    projectSpace().deleteFormulaResult(ccache, v);
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when deleting result '" + v + "' from '" + getID() + "'.");
                }
            });
        } finally {
            changed.forEach(ContainerListener.Defined::register);
        }
    }


    public List<ContainerListener.Defined> registerProjectSpaceListeners() {
        if (listeners == null)
            listeners = configureListeners();
        return listeners.stream().filter(ContainerListener.Defined::notRegistered).
                map(ContainerListener.Defined::register).collect(Collectors.toList());
    }

    public List<ContainerListener.Defined> unregisterProjectSpaceListeners() {
        if (listeners == null)
            return List.of();
        return listeners.stream().filter(ContainerListener.Defined::isRegistered).
                map(ContainerListener.Defined::unregister).collect(Collectors.toList());
    }

    public String getName() {
        return getID().getCompoundName();
    }

    public String getGUIName() {
        return getName() + " (" + getID().getCompoundIndex() + ")";
    }

    public List<SimpleSpectrum> getMs1Spectra() {
        return getMutableExperiment().getMs1Spectra();
    }

    public List<MutableMs2Spectrum> getMs2Spectra() {
        return getMutableExperiment().getMs2Spectra();
    }

    public SimpleSpectrum getMergedMs1Spectrum() {
        return getMutableExperiment().getMergedMs1Spectrum();
    }

    public PrecursorIonType getIonization() {
        return getID().getIonType().orElseGet(() -> getMutableExperiment().getPrecursorIonType());
    }

    public List<FormulaResultBean> getResults() {
        addToCache();
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> form = loadFormulaResults(List.of(ZodiacScore.class, SiriusScore.class), FormulaScoring.class);
        return IntStream.range(0, form.size()).mapToObj(i -> new FormulaResultBean(form.get(i).getCandidate().getId(), this, i + 1)).collect(Collectors.toList());
    }

    public double getIonMass() {
        return getID().getIonMass().orElse(Double.NaN);
    }


    public void setComputing(boolean computing) {
        if (computing)
            flag(CompoundContainerId.Flag.COMPUTING);
        else
            unFlag(CompoundContainerId.Flag.COMPUTING);
    }

    public boolean isComputing() {
        return hasFlag(CompoundContainerId.Flag.COMPUTING);
    }

    private MutableMs2Experiment getMutableExperiment() {
        addToCache();
        return (MutableMs2Experiment) getExperiment();
    }

    public Setter set() {
        return new Setter();
    }

    public class Setter {
        private List<Consumer<MutableMs2Experiment>> mods = new ArrayList<>();

        private Setter() {
        }

        // this is all MSExperiment update stuff. We listen to experiment changes on the project-space.
        // so calling updateExperiemnt will result in a EDT property change event if it was successful
        public Setter setName(final String name) {
            mods.add((exp) -> {
                if (projectSpace().renameCompound(getID(), name, (idx) -> spaceManager.namingScheme.apply(idx, name)))
                    exp.setName(name);
            });
            return this;
        }

        public Setter setIonization(final PrecursorIonType ionization) {
            mods.add((exp) -> exp.setPrecursorIonType(ionization));
            return this;
        }

        public Setter setIonMass(final double ionMass) {
            mods.add((exp) -> exp.setIonMass(ionMass));
            return this;
        }

        public Setter setMolecularFormula(final MolecularFormula formula) {
            mods.add((exp) -> exp.setMolecularFormula(formula));
            return this;
        }

        public void apply() {
            final MutableMs2Experiment exp = getMutableExperiment();
            for (Consumer<MutableMs2Experiment> mod : mods)
                mod.accept(exp);
            updateExperiment();
        }
    }
}