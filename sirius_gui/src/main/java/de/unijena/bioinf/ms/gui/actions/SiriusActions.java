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

package de.unijena.bioinf.ms.gui.actions;

import ca.odell.glazedlists.swing.AdvancedListSelectionModel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * @author Markus Fleischauer
 */
public enum SiriusActions {

    COMPUTE(ComputeAction.class),
    COMPUTE_ALL(ComputeAllAction.class),
    ORDER_BY_QUALITY(OrderCompoundByQuality.class),
    ORDER_BY_ID(OrderCompoundById.class),
    ORDER_BY_RT(OrderCompoundByRT.class),
    ORDER_BY_MASS(OrderCompoundByMass.class),
    ORDER_BY_NAME(OrderCompoundByName.class),
    ORDER_BY_CONFIDENCE(OrderCompoundByConfidence.class),

    TOOGLE_CONFIDENCE_MODE(SwitchConfidenceModeAction.class),

    TOOGLE_INVERT_FILTER(InvertFilterAction.class),
    RESET_FILTER(ResetFilterAction.class),
    COPY_ID(CopyIDAction.class),

    IMPORT_EXP_BATCH(ImportAction.class),
    DELETE_EXP(DeleteExperimentAction.class),
    CHANGE_ADDCUCT_EXP(ChangeAdductAction.class),
    SUMMARIZE_EXP(SummarizeSelectedAction.class),

    NEW_WS(ProjectCreateAction.class),
    LOAD_WS(ProjectOpenAction.class),
    SAVE_WS(ProjectSaveAction.class),
    SUMMARIZE_WS(SummarizeAllAction.class),
    EXPORT_FBMN(FBMNExportAction.class),

    SHOW_SETTINGS(ShowSettingsDialogAction.class),
    OPEN_ONLINE_DOCUMENTATION(OpenOnlineDocumentationAction.class), //frame
    SHOW_ABOUT(ShowAboutDialogAction.class),
    SHOW_JOBS(ShowJobsDialogAction.class),
    SHOW_DB(ShowDBDialogAction.class),
    SHOW_LOG(OpenLogAction.class),

    REGISTER_EXPLORER(ExplorerLicRegisterAction.class),

    SHOW_ACCOUNT(ShowAccountDialog.class),
    SIGN_OUT(SignOutAction.class),
    SIGN_IN(SignInAction.class),
    SIGN_UP(SignUpAction.class), //frame
    MANAGE_ACCOUNT(OpenPortalAction.class), //frame
    RESET_PWD(PasswdResetAction.class), //frame
    SELECT_SUBSCRIPTION(SelectActiveSubscriptionAction.class),
    ACCEPT_TERMS(AcceptTermsAction.class),

    CHECK_CONNECTION(CheckConnectionAction.class);


    public final Class<? extends Action> actionClass;

    SiriusActions(Class<? extends Action> action) {
        this.actionClass = action;
    }

    public static final ActionMap SINGLETON_ACTIONS = new ActionMap();


    private synchronized Action getInstance(@Nullable SiriusGui gui, final boolean createIfNull, final ActionMap map) {
        Action a = map.get(name());
        if (a == null && createIfNull) {
            try {
                for (Constructor<?> constructor : actionClass.getDeclaredConstructors()) {
                    List<Class<?>> paras = List.of(constructor.getParameterTypes());
                    if (gui != null) {
                        if (paras.size() == 1 && paras.contains(SiriusGui.class)) {
                            a = (Action) constructor.newInstance(gui);
                            break;
                        } else if (paras.size() == 1 && (paras.contains(MainFrame.class) || paras.contains(Frame.class))) {
                            a = (Action) constructor.newInstance(gui.getMainFrame());
                            break;
                        } else if (paras.isEmpty()) {
                            a = actionClass.getDeclaredConstructor().newInstance();
                            break;
                        }
                    } else if (paras.isEmpty()) {
                        a = actionClass.getDeclaredConstructor().newInstance();
                        break;
                    }
                }
                if (a == null)
                    throw new NullPointerException("Could not find valid constructor for Action!");
                map.put(name(), a);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not load following Sirius Action: " + name(), e);
            }
        }
        return a;
    }

    public Action getInstance(@NotNull SiriusGui gui, final boolean createIfNull) {
        return getInstance(gui, createIfNull, gui.getMainFrame().getGlobalActions());
    }


    private Action getInstance(@Nullable SiriusGui gui, final ActionMap map) {
        return getInstance(gui, false, map);
    }

    public Action getInstance(@NotNull SiriusGui gui) {
        return getInstance(gui, gui.getMainFrame().getGlobalActions());
    }

    public Action getInstance(final boolean createIfNull) {
        return getInstance(null, createIfNull, SINGLETON_ACTIONS);
    }


    public static boolean notComputingOrEmpty(Collection<InstanceBean> instance) {
        return !isComputingOrEmpty(instance);
    }

    public static boolean isComputingOrEmpty(Collection<InstanceBean> instances) {
        if (instances == null || instances.isEmpty())
            return true;
        return instances.stream().anyMatch(InstanceBean::isComputing);
    }

    public static boolean notComputingOrEmptySelected(AdvancedListSelectionModel<InstanceBean> selection) {
        return !isComputingOrEmptySelected(selection);
    }

    public static boolean isComputingOrEmptySelected(AdvancedListSelectionModel<InstanceBean> selection) {
        if (selection == null || selection.isSelectionEmpty())
            return true;
        return selection.getSelected().stream().anyMatch(InstanceBean::isComputing);
    }

    public static boolean notComputingOrEmptyFirstSelected(AdvancedListSelectionModel<InstanceBean> selection) {
        return !isComputingOrEmptyFirstSelected(selection);
    }

    public static boolean isComputingOrEmptyFirstSelected(AdvancedListSelectionModel<InstanceBean> selection) {
        if (selection == null || selection.isSelectionEmpty())
            return true;
        return selection.getSelected().get(0).isComputing();
    }
}