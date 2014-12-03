package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import gurobi.GurobiJni;

/**
 * Created by Spectar on 20.11.2014.
 */
public class GurobiJniAccess {

    static double[] getVariableAssignment(long model, int NumOfVar) {
        GRB.DoubleAttr attr = GRB.DoubleAttr.X;
        double[] values = new double[NumOfVar];

        // new GRBVar is initialized with GRBVar.col_no = -1
        // since I'm not able to create instances of GRBVar or access col_no, I might try it that way
        // TODO: There seems to be no way to access GRBVar.col_no currently. In that case, I'm not able
        // TODO: to use that. Appearently, an array of -1 is not working, since the condition
        // TODO: "GRB.col_no >= 0" must be fulfilled.

        // TODO: => try just getting all columns by a simple index i. Hope that works...
        int[] ind = new int[NumOfVar];
        for (int i=0; i<NumOfVar; i++)
            ind[i] = i;

        GurobiJni.getdblattrlist(model, attr.toString(), NumOfVar, -1, ind, values);

        return values;
    }


    ////////////////////////
    ///--- PARAMETERS ---///
    ////////////////////////


    public static int get(long env, GRB.IntParam param) throws GRBException {
        int[] value = new int[1];
        int error = GurobiJni.getintparam(env, param.toString(), value);
        if(error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        else
            return value[0];
    }


    public double get(long env, GRB.DoubleParam param) throws GRBException {
        double[] value = new double[1];
        int error = GurobiJni.getdblparam(env, param.toString(), value);
        if(error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        } else {
            return value[0];
        }
    }


    public String get(long env, GRB.StringParam param) throws GRBException {
        String[] value = new String[1];
        int error = GurobiJni.getstrparam(env, param.toString(), value);
        if(error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        } else {
            return value[0];
        }
    }


    static void set(long env, GRB.IntParam param, int newvalue) throws GRBException {
        int error = GurobiJni.setintparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    static void set(long env, GRB.DoubleParam param, double newvalue) throws GRBException {
        int error = GurobiJni.setdblparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    static void set(long env,GRB.StringParam param, String newvalue) throws GRBException {
        int error = GurobiJni.setstrparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    ////////////////////////
    ///--- ATTRIBUTES ---///
    ////////////////////////


    static int get(long model, GRB.IntAttr attr) throws GRBException {
        if (model == 0L)
            throw new GRBException("Model not loaded", 20003);

        int[] ind = new int[1];
        int[] val = new int[1];
        int error = GurobiJni.getintattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);

        return val[0];
    }


    static double get(long model, GRB.DoubleAttr attr) throws GRBException {
        if(model == 0L)
            throw new GRBException("Model not loaded", 20003);

        int[] ind = new int[1];
        double[] val = new double[1];
        int error = GurobiJni.getdblattrlist(model, attr.toString(), 0, -1, ind, val);
        if(error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        } else {
            return val[0];
        }
    }


    static String get(long model, GRB.StringAttr attr) throws GRBException {
        if(model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        } else {
            int[] ind = new int[1];
            String[] val = new String[1];
            int error = GurobiJni.getstrattrlist(model, attr.toString(), 0, -1, ind, val);
            if(error != 0) {
                throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
            } else {
                return val[0];
            }
        }
    }


    static void set(long model, GRB.IntAttr attr, int newvalue)
            throws GRBException
    {
        if (model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }
        int[] ind = new int[1];
        int[] val = new int[1];
        val[0] = newvalue;
        int error = GurobiJni.setintattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }


    static void set(long model, GRB.DoubleAttr attr, double newvalue) throws GRBException {
        if(model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        } else {
            int[] ind = new int[1];
            double[] val = new double[]{newvalue};
            int error = GurobiJni.setdblattrlist(model, attr.toString(), 0, -1, ind, val);
            if(error != 0) {
                throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
            }
        }
    }


    static void set(long model, GRB.StringAttr attr, String newvalue) throws GRBException {
        if(model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        } else {
            int[] ind = new int[1];
            String[] val = new String[]{newvalue};
            int error = GurobiJni.setstrattrlist(model, attr.toString(), 0, -1, ind, val);
            if(error != 0) {
                throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
            }
        }
    }

    /*
     Outdated, but working.
     */

}
