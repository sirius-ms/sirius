package de.unijena.bioinf.ConfidenceScore.svm;

import libsvm.*;

import java.util.List;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public class LibSVMImpl implements SVMInterface<LibSVMImpl.svm_nodeImpl, LibSVMImpl.svm_problemImpl, libsvm.svm_model> {

    public LibSVMImpl(){
        svm.svm_set_print_string_function(new svm_print_interface() {
            @Override
            public void print(String s) {

            }
        });
    }

    @Override
    public LibSVMImpl.svm_nodeImpl createSVM_Node(int index, double value) {
        if (Double.isNaN(value)){
            throw new IllegalArgumentException("value for node cannot be NaN");
        }
        return new LibSVMImpl.svm_nodeImpl(index, value);
    }

    @Override
    public LibSVMImpl.svm_problemImpl createSVM_Problem() {
        return new LibSVMImpl.svm_problemImpl();
    }



    @Override
    public LibSVMImpl.svm_model svm_train(LibSVMImpl.svm_problemImpl problem, svm_parameter parameter) {
        libsvm.svm_parameter libsSvm_parameter = new libsvm.svm_parameter();
        libsSvm_parameter.C = parameter.C;
        libsSvm_parameter.cache_size = parameter.cache_size;
//        libsSvm_parameter.coef0 = parameter.coef0;
//        libsSvm_parameter.degree = parameter.degree;
        libsSvm_parameter.eps = parameter.eps;
//        libsSvm_parameter.gamma = parameter.gamma;
        libsSvm_parameter.kernel_type = parameter.kernel_type;
        libsSvm_parameter.nr_weight = parameter.nr_weight;
//        libsSvm_parameter.nu = parameter.nu;
        libsSvm_parameter.p = parameter.p;
        libsSvm_parameter.probability = parameter.probability;
        libsSvm_parameter.shrinking = parameter.shrinking;
        libsSvm_parameter.svm_type = parameter.svm_type;
        libsSvm_parameter.weight = parameter.weight;
        libsSvm_parameter.weight_label = parameter.weight_label;
        libsvm.svm_model libsvm_model = svm.svm_train(problem.svm_problem, libsSvm_parameter);

        LibSVMImpl.svm_model model  = new LibSVMImpl.svm_model(libsvm_model);
        return model;
    }

    private LibSVMImpl.svm_nodeImpl[][] convert(libsvm.svm_node[][] nodes){
        LibSVMImpl.svm_nodeImpl[][] nodesImpl =new LibSVMImpl.svm_nodeImpl[nodes.length][];
        for (int i = 0; i < nodes.length; i++) {
            nodesImpl[i] = new LibSVMImpl.svm_nodeImpl[nodes[i].length];
            for (int j = 0; j < nodes[i].length; j++) {
                nodesImpl[i][j] = new LibSVMImpl.svm_nodeImpl(nodes[i][j]);
            }
        }
        return nodesImpl;
    }

    @Override
    public double svm_predict(SVMInterface.svm_model<libsvm.svm_model> model, List<svm_nodeImpl> nodes) {
        libsvm.svm_node[] libsvm_nodes = new libsvm.svm_node[nodes.size()];
        int i = 0;
        for (LibSVMImpl.svm_nodeImpl node : nodes) {
            libsvm_nodes[i++] = node.getNode();
        }
        return svm.svm_predict(model.getModel(), libsvm_nodes);
    }

    @Override
    public LinearSVMPredictor getPredictor(SVMInterface.svm_model<libsvm.svm_model> model, double probA, double probB) {
        double[][] d = convertDualToPrimal(model);
        double[] w = d[0];
        double b = d[1][0];


        if (model.getModel().probA!=null){
            return new LinearSVMPredictor(w, b, model.getModel().probA[0], model.getModel().probB[0]);
        } else {
            return new LinearSVMPredictor(w, b, probA, probB);
        }

    }

    private double[][] convertDualToPrimal(SVMInterface.svm_model<libsvm.svm_model> svm_model){
//        w = model.SVs' * model.sv_coef;
//        b = -model.rho;
//
//        if model.Label(1) == -1
//        w = -w;
//        b = -b;
//        end
//
//        SVMInterface.svm_node[][] SVs = svm_model.SV;
        libsvm.svm_node[][] SVs = svm_model.getModel().SV;
        assert svm_model.getModel().sv_coef.length==1;
        double[] coef = svm_model.getModel().sv_coef[0];

        assert coef.length==SVs.length;

        int featureSize = 0;
        for (int i = 0; i < SVs.length; i++) {
            libsvm.svm_node[] sv = SVs[i];
            for (int j = 0; j < sv.length; j++) {
                final int idx = sv[j].index;
                if (idx>featureSize) featureSize = idx; //idx starts with 1

            }
        }

        double[] w = new double[featureSize];

        for (int s = 0; s < SVs.length; s++) {
            libsvm.svm_node[] sv = SVs[s];
            for (int i = 0; i < sv.length; i++) {
                final libsvm.svm_node svm_node = sv[i];
                final int index = svm_node.index;
                w[index-1] += coef[s]*svm_node.value;
            }
        }


        assert svm_model.getModel().rho.length==1;
        double b = -svm_model.getModel().rho[0];

        if (svm_model.getModel().label[0]==-1){
            for (int i = 0; i < w.length; i++) {
                w[i] = -w[i];
            }
            b = -b;
        }

        return new double[][]{w, new double[]{b}};
    }



    public class svm_nodeImpl extends SVMInterface.svm_node{
        private final libsvm.svm_node svm_node;

        public svm_nodeImpl(libsvm.svm_node node){
            super(node.index, node.value);
            this.svm_node = node;
        }

        public svm_nodeImpl(int index, double value){
            super(index, value);
            this.svm_node = new libsvm.svm_node();
            this.svm_node.index = index;
            this.svm_node.value = value;
        }

        @Override
        public void setValue(double v) {
            svm_node.value = v;
        }

        @Override
        public int getIndex() {
            return svm_node.index;
        }

        @Override
        public double getValue() {
            return svm_node.value;
        }

        public libsvm.svm_node getNode() {
            return svm_node;
        }
    }

    public class svm_problemImpl extends SVMInterface.svm_problem<svm_nodeImpl>{
        public final libsvm.svm_problem svm_problem;
        private List<List<svm_nodeImpl>> svm_nodes;

        public svm_problemImpl(){
            this.svm_problem = new libsvm.svm_problem();
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
            this.svm_nodes = svm_nodes;
            libsvm.svm_node[][] libsvmNode = new libsvm.svm_node[svm_nodes.size()][];
            for (int i = 0; i < svm_nodes.size(); i++) {
                libsvmNode[i] = new libsvm.svm_node[svm_nodes.get(i).size()];
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

    public class svm_model extends SVMInterface.svm_model<libsvm.svm_model>{
        private libsvm.svm_model libsvm_model;
        public svm_model(libsvm.svm_model libsvm_model){
            this.libsvm_model = libsvm_model;
        }

        public libsvm.svm_model getModel() {
            return libsvm_model;
        }
    }

}
