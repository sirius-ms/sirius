/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.AbstractSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.AbstractTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.ILPSolverException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.IlpFactory;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public final class TreeBuilderFactory {
    public final static String GUROBI_VERSION;
    public final static String GLPK_VERSION;
    public final static String CPLEX_VERSION;
    public final static String CBC_VERSION;

    public final static String ILP_VERSIONS_STRING;


    static {
        GLPK_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.glpk_version");
        GUROBI_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.gurobi_version");
        CPLEX_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.cplex_version");
        CBC_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.cbc_version");
        ILP_VERSIONS_STRING = "Sirius was compiled with the following ILP solvers: GLPK-v" + GLPK_VERSION + " (included), Gurobi-v" + GUROBI_VERSION + ", CPLEX-v" + CPLEX_VERSION + ", COIN-OR-v" + CBC_VERSION;
    }

    private static TreeBuilderFactory INSTANCE = null;

    public enum DefaultBuilder {GUROBI, CPLEX, GLPK, CLP}

    private TreeBuilderFactory() {
    }

    public static TreeBuilderFactory getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TreeBuilderFactory();
        return TreeBuilderFactory.INSTANCE;
    }

    private static DefaultBuilder[] parseBuilderPriority(String builders) {
        if (builders == null) return null;
        String[] builderArray = builders.replaceAll("\\s", "").split(",");
        return parseBuilderPriority(builderArray);
    }

    private static DefaultBuilder[] parseBuilderPriority(String[] builders) {
        List<DefaultBuilder> bs = new ArrayList<>(builders.length);
        for (int i = 0; i < builders.length; i++) {
            if (builders[i].toUpperCase().equals("DP"))
                continue; // ignore!
            try {
                final DefaultBuilder b = DefaultBuilder.valueOf(builders[i].toUpperCase());
                bs.add(b);
            } catch (IllegalArgumentException e) {
                LoggerFactory.getLogger(TreeBuilderFactory.class).warn("Unknown tree builder '" + builders[i] + "'");
            }
        }

        return bs.toArray(new DefaultBuilder[bs.size()]);
    }

    public static DefaultBuilder[] getBuilderPriorities() {
        final DefaultBuilder[] b = parseBuilderPriority(PropertyManager.getProperty("de.unijena.bioinf.sirius.treebuilder.solvers"));
        if (b != null && b.length > 0) return b;
        return DefaultBuilder.values();
    }

    public <T extends AbstractSolver> IlpFactory<T> getTreeBuilderFromClass(String className, boolean warn) {
        try {
            return getTreeBuilderFromClass((Class<T>) getClass().getClassLoader().loadClass(className), warn);
        } catch (Throwable e) {
            if (warn)
                LoggerFactory.getLogger(this.getClass()).warn("Could not find and load " + className + "! " + ILP_VERSIONS_STRING + ": " + e.getMessage());
            LoggerFactory.getLogger(this.getClass()).debug("Could not find and load " + className + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }
    }

    public <T extends AbstractSolver> IlpFactory<T> getTreeBuilderFromClass(Class<T> builderClass, boolean warn) {
        try {
            return (IlpFactory<T>) builderClass.getDeclaredField("Factory").get(null);
        } catch (Throwable e) {
            if (warn)
                LoggerFactory.getLogger(this.getClass()).warn("Could not load " + builderClass.getSimpleName() + "! " + ILP_VERSIONS_STRING + ": " + e.getMessage());
            LoggerFactory.getLogger(this.getClass()).debug("Could not load " + builderClass.getSimpleName() + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }
    }

    public TreeBuilder getTreeBuilder(String builder) {
        return getTreeBuilder(DefaultBuilder.valueOf(builder.toUpperCase()));
    }


    public TreeBuilder getTreeBuilder(DefaultBuilder builder) {
        return getTreeBuilder(builder, true);
    }

    public TreeBuilder getTreeBuilder(DefaultBuilder builder, boolean warn) {
        IlpFactory<?> factory = null;
        switch (builder) {
            case GUROBI:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GrbSolver", warn);
                break;
            case GLPK:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GLPKSolver", warn);
                break;
            case CPLEX:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.CPLEXSolver", warn);
                break;
            case CLP:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.CLPSolver", warn);
                break;
            default:
                LoggerFactory.getLogger(this.getClass()).warn("TreeBuilder " + builder.toString() + " is Unknown, supported are: " + Arrays.toString(DefaultBuilder.values()), new IllegalArgumentException("Unknown BuilderType!"));
                return null;
        }
        if (factory == null) {
            return null;
        } else {
            try {
                factory.checkSolver();
            } catch (ILPSolverException e) {
                if (warn)
                    LoggerFactory.getLogger(getClass()).warn("Could not load Solver '" + builder + "': " + e.getMessage());
                LoggerFactory.getLogger(getClass()).debug("Could not load Solver '" + builder + "'", e);
                return null;
            }
        }
        return new AbstractTreeBuilder<>(factory);
    }

    public TreeBuilder getTreeBuilder() {
        for (DefaultBuilder builder : getBuilderPriorities()) {
            TreeBuilder b = getTreeBuilder(builder);
            if (b != null)
                return b;
        }
        LoggerFactory.getLogger(TreeBuilderFactory.class).error("Your system does not ship with any instantiatable ILP solver. Please install either CLP,  Gurobi or CPLEX to use SIRIUS.");
        return null;
    }

    public EnumSet<DefaultBuilder> getAvailableBuilders() {
        EnumSet<DefaultBuilder> builders = EnumSet.noneOf(DefaultBuilder.class);
        for (DefaultBuilder builder : getBuilderPriorities()) {
            TreeBuilder b = getTreeBuilder(builder, false);
            if (b != null)
                builders.add(builder);
        }
        return builders;
    }
}
