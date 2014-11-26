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

    static int get(long model, GRB.IntAttr attr) throws GRBException {
        if (model == 0L)
            throw new GRBException("Model not loaded", 20003);

        int[] ind = new int[1];
        int[] val = new int[1];
        int error = GurobiJni.getintattrlist(model, attr.toString(), 0, -1, ind, val);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
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


    static double[] get(long model, GRB.DoubleAttr attr, GRBVar[] vars) throws GRBException {
        int len = vars.length;
        if(len <= 0) {
            return null;
        } else {
            double[] values = new double[len];
            //this.getattr(attr.toString(), vars, values, 1, 0, 2); // dim = 1, vorc = 0, type = 2
            len = GurobiJniAccess.checkattrsize(model, attr.toString(), 1);
            if(len != 0) {
                throw new GRBException("Not right attribute", len);
            }

            ind = this.setvarsind(o1, dim);

            return values;
        }
    }

    static int checkattrsize(long model, String attrname, int size) {
        int[] attrinfo = new int[3];
        int error = GurobiJni.getattrinfo(model, attrname, attrinfo);
        if(error == 0 && size != attrinfo[1]) {
            error = 10003;
        }

        return error;
    }




    static void set(long model,GRB.IntParam param, int newvalue) throws GRBException {
        int error = GurobiJni.setintparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    static void set(long model,GRB.DoubleParam param, double newvalue) throws GRBException {
        int error = GurobiJni.setdblparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }

    static void set(long model,GRB.StringParam param, String newvalue) throws GRBException {
        int error = GurobiJni.setstrparam(model, param.toString(), newvalue);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
        }
    }
}
