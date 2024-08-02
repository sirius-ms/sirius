/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.confidence_score.svm;


import de.bwaldvogel.liblinear.*;

import java.util.List;

/**
* Created by Marcus Ludwig on 09.03.16.
*/
public class LibLinearImpl implements SVMInterface<LibLinearImpl.svm_nodeImpl, LibLinearImpl.svm_problemImpl, Model> {


    public LibLinearImpl(){
        Linear.disableDebugOutput();
    }

    @Override
    public LibLinearImpl.svm_nodeImpl createSVM_Node(int index, double value) {
        if (Double.isNaN(value)){
            throw new IllegalArgumentException("value for node cannot be NaN");
        }
        return new LibLinearImpl.svm_nodeImpl(index, value);
    }

    @Override
    public LibLinearImpl.svm_problemImpl createSVM_Problem() {
        return new LibLinearImpl.svm_problemImpl();
        }



    @Override
    public LibLinearImpl.svm_model svm_train(LibLinearImpl.svm_problemImpl problem, svm_parameter parameter) {
        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s
       // SolverType solver = SolverType.L1R_L2LOSS_SVC; // -s
       // SolverType solver = SolverType.L2R_LR;
        // 0
//        double C = 1.0;    // cost of constraints violation
//        double eps = 0.001;
        Parameter libLinear_parameter = new Parameter(solver, parameter.C, parameter.eps);

        libLinear_parameter.setC(parameter.C);
        libLinear_parameter.setEps(parameter.eps);
        libLinear_parameter.setP(parameter.p);
        libLinear_parameter.setWeights(parameter.weight, parameter.weight_label);

        Feature[][] features = problem.svm_problem.x;
        for (int i = 0; i < features.length; i++) {
            Feature[] feature = features[i];
            int index = -1;
            for (int j = 0; j < feature.length; j++) {
                Feature f = feature[j];
                if (f.getIndex()<=index){
                    throw new RuntimeException("index of feature node too small");
                }
                index = f.getIndex();
            }
        }

        Model liblinear_model = Linear.train(problem.svm_problem, libLinear_parameter);

        LibLinearImpl.svm_model model  = new LibLinearImpl.svm_model(liblinear_model);
        model.param = parameter;
        return model;
    }

    @Override
    public LinearSVMPredictor getPredictor(SVMInterface.svm_model<Model> model) {
        double[] w = model.getModel().getFeatureWeights();
        double b = model.getModel().getBias();

        return new LinearSVMPredictor(w, b);
    }

    public LibLinearImpl.svm_model convertModel(Model model){

        LibLinearImpl.svm_model model_new  = new LibLinearImpl.svm_model(model);

        return model_new;

    }


    @Override
    public double svm_predict(SVMInterface.svm_model<Model> model, List<svm_nodeImpl> nodes) {
        Feature[] libsvm_nodes = new Feature[nodes.size()];
        int i = 0;
        for (LibLinearImpl.svm_nodeImpl node : nodes) {
            libsvm_nodes[i++] = node.getNode();
        }
        return Linear.predict(model.getModel(), libsvm_nodes);
    }


    public class svm_nodeImpl extends SVMInterface.svm_node{
        private final Feature svm_node;

        public svm_nodeImpl(Feature node){
            super(node.getIndex(), node.getValue());
            this.svm_node = node;
        }

        public svm_nodeImpl(int index, double value){
            super(index, value);
            this.svm_node = new FeatureNode(index, value);
        }

//        @Override
//        public void setIndex(int i) {
//            svm_node.index = i;
//        }

        @Override
        public void setValue(double v) {
            svm_node.setValue(v);
        }

        @Override
        public int getIndex() {
            return svm_node.getIndex();
        }

        @Override
        public double getValue() {
            return svm_node.getValue();
        }

        public Feature getNode() {
            return svm_node;
        }
    }

    public class svm_problemImpl extends SVMInterface.svm_problem<svm_nodeImpl>{
        public final Problem svm_problem;
        private List<List<svm_nodeImpl>> svm_nodes;

        public svm_problemImpl(){
            this.svm_problem = new Problem();
        }

        @Override
        public void setL(int l) {
            svm_problem.l = l;
        }

        @Override
        public int getL() {
            return svm_problem.l;
        }

        @Override
        public void setX(List<List<svm_nodeImpl>> svm_nodes) {
            //ToDo remove get/set L
            svm_problem.n = svm_nodes.get(0).size();
            this.svm_nodes = svm_nodes;
            Feature[][] libsvmNode = new Feature[svm_nodes.size()][];
            for (int i = 0; i < svm_nodes.size(); i++) {
                libsvmNode[i] = new Feature[svm_nodes.get(i).size()];
                for (int j = 0; j < svm_nodes.get(i).size(); j++) {
                    libsvmNode[i][j] = svm_nodes.get(i).get(j).getNode();
                }
            }
            svm_problem.x = libsvmNode;
        }

        @Override
        public List<List<svm_nodeImpl>> getX() {
            return this.svm_nodes;
        }

        @Override
        public void setY(double[] y) {
            svm_problem.y = y;
        }

        @Override
        public double[] getY() {
            return svm_problem.y;
        }
    }

    public class svm_model extends SVMInterface.svm_model<Model>{
        public svm_parameter param;
        private Model libsvm_model;
        public svm_model(Model libsvm_model){
            this.libsvm_model = libsvm_model;
        }

        public Model getModel() {
            return libsvm_model;

        }

        public svm_parameter getParam() {
            return param;
        }
    }

}
