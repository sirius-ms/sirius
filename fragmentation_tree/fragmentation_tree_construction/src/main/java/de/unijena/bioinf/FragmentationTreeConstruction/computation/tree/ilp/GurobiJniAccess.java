/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
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
