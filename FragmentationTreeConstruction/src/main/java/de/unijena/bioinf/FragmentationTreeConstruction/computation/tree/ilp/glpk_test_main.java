package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;
import org.gnu.glpk.GLPK;

/**
 * Created by xentrics on 04.03.15.
 */
public class glpk_test_main {

    public static void main(String[] args) {

        System.out.println( GLPK.glp_version());
    }

    /**
     * try to load the jni interface for glpk from the glpk library
     */
    static {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // try to load Windows library
            try {
                System.loadLibrary("glpk_" + GLPKSolver.GLPK_VERSION);
                System.out.println("glpk version: " + GLPK.glp_version() + "  loaded.");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Could not load glpk_4.55 library from windows! Make sure to have the correct" +
                        " version of glpk installed on your system!");
                throw e;
            }
        } else {
            try {
                System.loadLibrary("glpk_java");
                System.out.println("glpk version: " + GLPK.glp_version() + "  loaded.");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("The dynamic link library for GLPK for java could not be loaded. \n" +
                        "Consider using \njava -Djava.library.path=");
                throw e;
            }
        }
    }
}
