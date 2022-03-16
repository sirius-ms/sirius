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
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public enum SiriusActions {

    COMPUTE(ComputeAction.class),
//    CANCEL_COMPUTE(CancelComputeAction.class),
    COMPUTE_ALL(ComputeAllAction.class),
//    CANCEL_ALL(CancelComputeAllAction.class),
    ORDER_BY_INDEX(OrderCompoundByIndex.class),
    ORDER_BY_RT(OrderCompoundByRT.class),
    ORDER_BY_MASS(OrderCompoundByMass.class),
    ORDER_BY_NAME(OrderCompoundByName.class),
    ORDER_BY_CONFIDENCE(OrderCompoundByConfidence.class),

    TOOGLE_INVERT_FILTER(InvertFilterAction.class),
    RESET_FILTER(ResetFilterAction.class),

//    COMPUTE_CSI(ComputeCSIAction.class),
//    COMPUTE_CSI_LOCAL(ComputeCSILocalAction.class),

    IMPORT_EXP(ImportCompoundAction.class),
    IMPORT_EXP_BATCH(ImportAction.class),
    EDIT_EXP(EditExperimentAction.class),
    DELETE_EXP(DeleteExperimentAction.class),
    REMOVE_FORMULA_EXP(RemoveFormulaAction.class),
    CHANGE_ADDCUCT_EXP(ChangeAdductAction.class),
    SUMMARIZE_EXP(SummarizeSelectedAction.class),


    NEW_WS(ProjectCreateAction.class),
    LOAD_WS(ProjectOpenAction.class),
    SAVE_WS(ProjectSaveAction.class),
    EXPORT_WS(ProjectSaveCopyAction.class),
    SUMMARIZE_WS(SummarizeAllAction.class),
    EXPORT_FBMN(FBMNExportAction.class),

    SHOW_SETTINGS(ShowSettingsDialogAction.class),
    OPEN_ONLINE_DOCUMENTATION(OpenOnlineDocumentationAction.class),
    SHOW_BUGS(ShowBugReportDialogAction.class),
    SHOW_ABOUT(ShowAboutDialogAction.class),
    SHOW_JOBS(ShowJobsDialogAction.class),
    SHOW_DB(ShowDBDialogAction.class),
    SHOW_LOG(OpenLogAction.class),

    SHOW_ACCOUNT(ShowAccountDialog.class),
    SIGN_OUT(SignOutAction.class),
    SIGN_IN(SignInAction.class),
    SIGN_UP(SignUpAction.class),
    RESET_PWD(PasswdResetAction.class),
    DELETE_ACCOUNT(AccountDeleteAction.class),
    ACCEPT_TERMS(AcceptTermsAction.class),

    CHECK_CONNECTION(CheckConnectionAction.class);


    public static final ActionMap ROOT_MANAGER = new ActionMap();
    public final Class<? extends Action> actionClass;

    public synchronized Action getInstance(final boolean createIfNull, final ActionMap map) {
        Action a = map.get(name());
        if (a == null && createIfNull) {
            try {
                a = actionClass.getDeclaredConstructor().newInstance();
                map.put(name(), a);
            } catch ( InstantiationException | IllegalAccessException |NoSuchMethodException | InvocationTargetException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not load following Sirius Action: " + name(), e);
            }
        }
        return a;
    }

    public Action getInstance(final ActionMap map) {
        return getInstance(false, map);
    }

    public Action getInstance(boolean createIfNull) {
        return getInstance(createIfNull, ROOT_MANAGER);
    }

    public Action getInstance() {
        return getInstance(true, ROOT_MANAGER);
    }

    SiriusActions(Class<? extends Action> action) {
        this.actionClass = action;
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