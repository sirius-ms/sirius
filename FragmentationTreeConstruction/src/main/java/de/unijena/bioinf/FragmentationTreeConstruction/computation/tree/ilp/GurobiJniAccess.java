package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import gurobi.GurobiJni;

/**
 * Created by Spectar on 20.11.2014.
 */
public class GurobiJniAccess {

    ////////////////////////
    ///--- PARAMETERS ---///
    ////////////////////////


    final static int get(long env, GRB.IntParam param) throws GRBException {
        int[] value = new int[1];
        int error = GurobiJni.getintparam(env, param.toString(), value);
        if(error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        else
            return value[0];
    }


    final double get(long env, GRB.DoubleParam param) throws GRBException {
        double[] value = new double[1];
        int error = GurobiJni.getdblparam(env, param.toString(), value);
        if(error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        } else {
            return value[0];
        }
    }


    final String get(long env, GRB.StringParam param) throws GRBException {
        String[] value = new String[1];
        int error = GurobiJni.getstrparam(env, param.toString(), value);
        if(error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(env), error);
        } else {
            return value[0];
        }
    }


    final static void set(long env, GRB.IntParam param, int newvalue) throws GRBException {
        int error = GurobiJni.setintparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    final static void set(long env, GRB.DoubleParam param, double newvalue) throws GRBException {
        int error = GurobiJni.setdblparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    final static void set(long env,GRB.StringParam param, String newvalue) throws GRBException {
        int error = GurobiJni.setstrparam(env, param.toString(), newvalue);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(env), error);
    }


    ////////////////////////
    ///--- ATTRIBUTES ---///
    ////////////////////////


    final static int get(long model, GRB.IntAttr attr) throws GRBException {
        if (model == 0L)
            throw new GRBException("Model not loaded", 20003);

        int[] ind = new int[1];
        int[] val = new int[1];
        int error = GurobiJni.getintattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);

        return val[0];
    }


    final static double get(long model, GRB.DoubleAttr attr) throws GRBException {
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


    final static String get(long model, GRB.StringAttr attr) throws GRBException {
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


    final static void set(long model, GRB.IntAttr attr, int newvalue)
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


    final static void set(long model, GRB.DoubleAttr attr, double newvalue) throws GRBException {
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


    final static void set(long model, GRB.StringAttr attr, String newvalue) throws GRBException {
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

}
