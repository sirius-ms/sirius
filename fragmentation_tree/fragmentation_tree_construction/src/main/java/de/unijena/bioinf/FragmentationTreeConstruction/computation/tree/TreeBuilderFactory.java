package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 28.09.16.
 */

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public final class TreeBuilderFactory {
    public final static String GUROBI_VERSION;
    public final static String GLPK_VERSION;
    public final static String CPLEX_VERSION;
    public final static String CLP_VERSION;

    public final static String ILP_VERSIONS_STRING;


    static {
        GLPK_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.glpk_version");
        GUROBI_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.gurobi_version");
        CPLEX_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.cplex_version");
        CLP_VERSION = PropertyManager.getProperty("de.unijena.bioinf.sirius.build.cpl_version");
        ILP_VERSIONS_STRING = "Sirius was compiled with the following ILP solvers: GLPK-v" + GLPK_VERSION + " (included), Gurobi-v" + GUROBI_VERSION + ", CPLEX-v" + CPLEX_VERSION + ", COIN-OR-v" + CLP_VERSION;
    }

    private static TreeBuilderFactory INSTANCE = null;

    public enum DefaultBuilder {GUROBI, CPLEX, GLPK, CLP}

    private static DefaultBuilder[] builderPriorities = null;

    private TreeBuilderFactory() {
    }

    public static TreeBuilderFactory getInstance() {
        if (INSTANCE == null)
            INSTANCE = new TreeBuilderFactory();
        return TreeBuilderFactory.INSTANCE;
    }

    public static void setBuilderPriorities(DefaultBuilder... builders) {
        builderPriorities = builders;
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

    public static boolean setBuilderPriorities(String... builders) {
        DefaultBuilder[] b = parseBuilderPriority(builders);
        if (b!=null && b.length>0) {
            builderPriorities = b;
            return true;
        }

        return false;
    }

    public static DefaultBuilder[] getBuilderPriorities() {
        if (builderPriorities != null) return builderPriorities.clone();
        DefaultBuilder[] b = parseBuilderPriority(PropertyManager.getProperty("de.unijena.bioinf.sirius.treebuilder.solvers"));
        if (b!=null && b.length>0) return b;
        return DefaultBuilder.values();
    }

    public <T extends AbstractSolver> IlpFactory<T> getTreeBuilderFromClass(String className) {
        try {
            return getTreeBuilderFromClass((Class<T>) getClass().getClassLoader().loadClass(className));
        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).warn("Could find and load " + className + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }
    }

    public <T extends AbstractSolver> IlpFactory<T> getTreeBuilderFromClass(Class<T> builderClass) {
        try {
            return (IlpFactory<T>) builderClass.getDeclaredField("Factory").get(null);
        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).warn("Could not load " + builderClass.getSimpleName() + "! " + ILP_VERSIONS_STRING);
            LoggerFactory.getLogger(this.getClass()).debug("Could not load " + builderClass.getSimpleName() + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }
    }

    public TreeBuilder getTreeBuilder(String builder) {
        return getTreeBuilder(DefaultBuilder.valueOf(builder.toUpperCase()));
    }


    public TreeBuilder getTreeBuilder(DefaultBuilder builder) {
        IlpFactory<?> factory = null;
        switch (builder) {
            case GUROBI:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GrbSolver"); //we have to use classloader, to prevent class not found exception. because it could be possible that gurobi.jar doe not exist -> runtime dependency
                break;
            case GLPK:
                factory = getTreeBuilderFromClass(GLPKSolver.class); //we deliver the jar file so we can be sure that th class exists
                break;
            case CPLEX:
                factory = getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.CPLEXSolver");
                break;
            case CLP:
                factory = getTreeBuilderFromClass(CPLEXSolver.class); //we deliver the jar file so we can be sure that th class exists
                break;
            default:
                LoggerFactory.getLogger(this.getClass()).warn("TreeBuilder " + builder.toString() + " is Unknown, supported are: " + Arrays.toString(DefaultBuilder.values()), new IllegalArgumentException("Unknown BuilderType!"));
                return null;
        }
        if (factory == null) {
            return null;
        }
        return new AbstractTreeBuilder<>(factory);
    }

    public TreeBuilder getTreeBuilder() {
        for (DefaultBuilder builder : getBuilderPriorities()) {
            TreeBuilder b = getTreeBuilder(builder);
            if (b != null)
                return b;
        }
        LoggerFactory.getLogger(TreeBuilderFactory.class).error("Your system does not ship with any ILP solver. Please install either GLPK for java, Gurobi or CPLEX to use SIRIUS.");
        return null;
    }
}