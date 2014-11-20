package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import gurobi.GurobiJni;

/**
 * Created by Spectar on 20.11.2014.
 */
public class GurobiJniAccess {



    public static void set(long model, GRB.DoubleAttr attr, double newvalue)
            throws GRBException
    {
        if (model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }
        int[] ind = new int[1];
        double[] val = new double[1];
        val[0] = newvalue;
        int error = GurobiJni.setdblattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    public static void set(long model, GRB.IntAttr attr, int newvalue)
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

    static void set(long model, GRB.StringAttr attr, String newvalue)
            throws GRBException
    {
        if (model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }
        int[] ind = new int[1];
        String[] val = new String[1];
        val[0] = newvalue;
        int error = GurobiJni.setstrattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    static int get(long model, GRB.IntAttr attr)
            throws GRBException
    {
        if (model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }
        int[] ind = new int[1];
        int[] val = new int[1];
        int error = GurobiJni.getintattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
        return val[0];
    }


    static void set(long model,GRB.IntParam param, int newvalue)
            throws GRBException
    {
        int error = GurobiJni.setintparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    static void set(long model,GRB.DoubleParam param, double newvalue)
            throws GRBException
    {
        int error = GurobiJni.setdblparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    static void set(long model,GRB.StringParam param, String newvalue)
            throws GRBException
    {
        int error = GurobiJni.setstrparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }
}
