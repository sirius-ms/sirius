package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 28.09.16.
 */

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GLPKSolver;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public final class TreeBuilderFactory {
    public final static String GUROBI_VERSION;
    public final static String GLPK_VERSION;
    public final static String CPLEX_VERSION;

    public final static String ILP_VERSIONS_STRING;


    static {
        GLPK_VERSION = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.build.glpk_version");
        GUROBI_VERSION = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.build.gurobi_version");
        CPLEX_VERSION = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.build.cplex_version");
        ILP_VERSIONS_STRING = "Sirius was compiled with the following ILP solvers: GLPK-v" + GLPK_VERSION + " (included), Gurobi-v" + GUROBI_VERSION + ", CPLEX-v" + CPLEX_VERSION;
    }

    private static TreeBuilderFactory INSTANCE = null;

    public enum DefaultBuilder {GUROBI, /*GUROBI_JNI,*/ CPLEX, GLPK, DP}

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
        String[] builderArray = builders.replaceAll("\\s", "").split(",");
        return parseBuilderPriority(builderArray);
    }

    private static DefaultBuilder[] parseBuilderPriority(String[] builders) {
        DefaultBuilder[] b = new DefaultBuilder[builders.length];
        try {
            for (int i = 0; i < b.length; i++) {
                b[i] = DefaultBuilder.valueOf(builders[i].toUpperCase());
            }

            return b;
        } catch (IllegalArgumentException e) {
            LoggerFactory.getLogger(TreeBuilderFactory.class).warn("Illegal TreeBuilder name. Nothing changed!", e);
            return null;
        }
    }

    public static boolean setBuilderPriorities(String... builders) {
        DefaultBuilder[] b = parseBuilderPriority(builders);
        if (b != null) {
            builderPriorities = b;
            return true;
        }

        return false;
    }

    public static DefaultBuilder[] getBuilderPriorities() {
        if (builderPriorities != null) return builderPriorities.clone();
        DefaultBuilder[] b = parseBuilderPriority(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.treebuilder"));
        if (b != null) return b;
        return DefaultBuilder.values();
    }

    public <T extends TreeBuilder> T getTreeBuilderFromClass(String className) {
        try {
            return getTreeBuilderFromClass(((Class<T>) ClassLoader.getSystemClassLoader().loadClass(className)));
        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).warn("Could find and load " + className + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }

    }

    public <T extends TreeBuilder> T getTreeBuilderFromClass(Class<T> builderClass) {
        try {
            return builderClass.getConstructor().newInstance();
        } catch (Throwable e) {
            LoggerFactory.getLogger(this.getClass()).warn("Could not load " + builderClass.getSimpleName() + "! " + ILP_VERSIONS_STRING, e);
            return null;
        }
    }

    public TreeBuilder getTreeBuilder(String builder) {
        return getTreeBuilder(DefaultBuilder.valueOf(builder.toUpperCase()));
    }

    public TreeBuilder getTreeBuilder(DefaultBuilder builder) {
        switch (builder) {
            case GUROBI:
                return getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver"); //we have to use classloader, to prevent class not found exception. because it could be possible that gurobi.jar doe not exist -> runtime dependency
            case GLPK:
                return getTreeBuilderFromClass(GLPKSolver.class); //we deliver the jar file so we can be sure that th class exists
            case CPLEX:
                return getTreeBuilderFromClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.CPLEXTreeBuilder");
            case DP:
                return getTreeBuilderFromClass(DPTreeBuilder.class);
            default:
                LoggerFactory.getLogger(this.getClass()).warn("TreeBuilder " + builder.toString() + " is Unknown, supported are: " + Arrays.toString(DefaultBuilder.values()), new IllegalArgumentException("Unknown BuilderType!"));
                return null;
        }
    }

    public TreeBuilder getTreeBuilder() {
        for (DefaultBuilder builder : getBuilderPriorities()) {
            TreeBuilder b = getTreeBuilder(builder);
            if (b != null)
                return b;
        }
        return null;
    }
}