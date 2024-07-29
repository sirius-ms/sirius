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

package org.libsvm;

import libsvm.*;

import java.io.*;
import java.util.Random;
import java.util.StringTokenizer;

//
// Kernel Cache
//
// l is the number of total data items
// size is the cache size limit in bytes
//

class Cache {
    private final int l;
    private long size;
    private final class HeadT
    {
        HeadT prev, next;	// a cicular list
        float[] data;
        int len;		// data[0,len) is cached in this entry
    }
    private final HeadT[] head;
    private HeadT lru_head;

    Cache(int l_, long size_)
    {
        l = l_;
        size = size_;
        head = new HeadT[l];
        for(int i=0;i<l;i++) head[i] = new HeadT();
        size /= 4;
        size -= l * (16/4);	// sizeof(HeadT) == 16
        size = Math.max(size, 2* (long) l);  // cache must be large enough for two columns
        lru_head = new HeadT();
        lru_head.next = lru_head.prev = lru_head;
    }

    private void lru_delete(HeadT h)
    {
        // delete from current location
        h.prev.next = h.next;
        h.next.prev = h.prev;
    }

    private void lru_insert(HeadT h)
    {
        // insert to last position
        h.next = lru_head;
        h.prev = lru_head.prev;
        h.prev.next = h;
        h.next.prev = h;
    }

    // request data [0,len)
    // return some position p where [p,len) need to be filled
    // (p >= len if nothing needs to be filled)
    // java: simulate pointer using single-element array
    int get_data(int index, float[][] data, int len)
    {
        HeadT h = head[index];
        if(h.len > 0) lru_delete(h);
        int more = len - h.len;

        if(more > 0)
        {
            // free old space
            while(size < more)
            {
                HeadT old = lru_head.next;
                lru_delete(old);
                size += old.len;
                old.data = null;
                old.len = 0;
            }

            // allocate new space
            float[] new_data = new float[len];
            if(h.data != null) System.arraycopy(h.data,0,new_data,0,h.len);
            h.data = new_data;
            size -= more;
            {int tmp=h.len; h.len=len; len=tmp;}
        }

        lru_insert(h);
        data[0] = h.data;
        return len;
    }

    void swap_index(int i, int j)
    {
        if(i==j) return;

        if(head[i].len > 0) lru_delete(head[i]);
        if(head[j].len > 0) lru_delete(head[j]);
        do {float[] underscore=head[i].data; head[i].data=head[j].data; head[j].data=underscore;} while(false);
        do {int underscore=head[i].len; head[i].len=head[j].len; head[j].len=underscore;} while(false);
        if(head[i].len > 0) lru_insert(head[i]);
        if(head[j].len > 0) lru_insert(head[j]);

        if(i>j) do {int underscore=i; i=j; j=underscore;} while(false);
        for(HeadT h = lru_head.next; h!=lru_head; h=h.next)
        {
            if(h.len > i)
            {
                if(h.len > j)
                    do {float underscore=h.data[i]; h.data[i]=h.data[j]; h.data[j]=underscore;} while(false);
                else
                {
                    // give up
                    lru_delete(h);
                    size += h.len;
                    h.data = null;
                    h.len = 0;
                }
            }
        }
    }
}

//
// Kernel evaluation
//
// the static method k_function is for doing single kernel evaluation
// the constructor of Kernel prepares to calculate the l*l kernel matrix
// the member function get_Q is for getting one column from the Q MatrixUtils
//
abstract class QMatrix {
    abstract float[] get_Q(int column, int len);
    abstract double[] get_QD();
    abstract void swap_index(int i, int j);
}

abstract class Kernel extends QMatrix {
    private svm_node[][] x;
    private final double[] x_square;

    // svm_parameter
    private final int kernel_type;
    private final int degree;
    private final double gamma;
    private final double coef0;

    abstract float[] get_Q(int column, int len);
    abstract double[] get_QD();

    void swap_index(int i, int j)
    {
        do {svm_node[] underscore=x[i]; x[i]=x[j]; x[j]=underscore;} while(false);
        if(x_square != null) do {double underscore=x_square[i]; x_square[i]=x_square[j]; x_square[j]=underscore;} while(false);
    }

    private static double powi(double base, int times)
    {
        double tmp = base, ret = 1.0;

        for(int t=times; t>0; t/=2)
        {
            if(t%2==1) ret*=tmp;
            tmp = tmp * tmp;
        }
        return ret;
    }

    double kernel_function(int i, int j)
    {
        switch(kernel_type)
        {
            case svm_parameter.LINEAR:
                return dot(x[i],x[j]);
            case svm_parameter.POLY:
                return powi(gamma*dot(x[i],x[j])+coef0,degree);
            case svm_parameter.RBF:
                return Math.exp(-gamma*(x_square[i]+x_square[j]-2*dot(x[i],x[j])));
            case svm_parameter.SIGMOID:
                return Math.tanh(gamma*dot(x[i],x[j])+coef0);
            case svm_parameter.PRECOMPUTED:
                return x[i][(int)(x[j][0].value)].value;
            default:
                return 0;	// java
        }
    }

    Kernel(int l, svm_node[][] x_, svm_parameter param)
    {
        this.kernel_type = param.kernel_type;
        this.degree = param.degree;
        this.gamma = param.gamma;
        this.coef0 = param.coef0;

        x = x_.clone();

        if(kernel_type == svm_parameter.RBF)
        {
            x_square = new double[l];
            for(int i=0;i<l;i++)
                x_square[i] = dot(x[i],x[i]);
        }
        else x_square = null;
    }

    static double dot(svm_node[] x, svm_node[] y)
    {
        double sum = 0;
        int xlen = x.length;
        int ylen = y.length;
        int i = 0;
        int j = 0;
        while(i < xlen && j < ylen)
        {
            if(x[i].index == y[j].index)
                sum += x[i++].value * y[j++].value;
            else
            {
                if(x[i].index > y[j].index)
                    ++j;
                else
                    ++i;
            }
        }
        return sum;
    }

    static double k_function(svm_node[] x, svm_node[] y,
                             svm_parameter param)
    {
        switch(param.kernel_type)
        {
            case svm_parameter.LINEAR:
                return dot(x,y);
            case svm_parameter.POLY:
                return powi(param.gamma*dot(x,y)+param.coef0,param.degree);
            case svm_parameter.RBF:
            {
                double sum = 0;
                int xlen = x.length;
                int ylen = y.length;
                int i = 0;
                int j = 0;
                while(i < xlen && j < ylen)
                {
                    if(x[i].index == y[j].index)
                    {
                        double d = x[i++].value - y[j++].value;
                        sum += d*d;
                    }
                    else if(x[i].index > y[j].index)
                    {
                        sum += y[j].value * y[j].value;
                        ++j;
                    }
                    else
                    {
                        sum += x[i].value * x[i].value;
                        ++i;
                    }
                }

                while(i < xlen)
                {
                    sum += x[i].value * x[i].value;
                    ++i;
                }

                while(j < ylen)
                {
                    sum += y[j].value * y[j].value;
                    ++j;
                }

                return Math.exp(-param.gamma*sum);
            }
            case svm_parameter.SIGMOID:
                return Math.tanh(param.gamma*dot(x,y)+param.coef0);
            case svm_parameter.PRECOMPUTED:
                return	x[(int)(y[0].value)].value;
            default:
                return 0;	// java
        }
    }
}

// An SMO algorithm in Fan et al., JMLR 6(2005), p. 1889--1918
// Solves:
//
//	min 0.5(\alpha^T Q \alpha) + p^T \alpha
//
//		y^T \alpha = \delta
//		y_i = +1 or -1
//		0 <= alpha_i <= Cp for y_i = 1
//		0 <= alpha_i <= Cn for y_i = -1
//
// Given:
//
//	Q, p, y, Cp, Cn, and an initial feasible point \alpha
//	l is the size of vectors and matrices
//	eps is the stopping tolerance
//
// solution will be put in \alpha, objective value will be put in obj
//
class Solver {
    int active_size;
    byte[] y;
    double[] G;		// gradient of objective function
    static final byte LOWER_BOUND = 0;
    static final byte UPPER_BOUND = 1;
    static final byte FREE = 2;
    byte[] alpha_status;	// LOWER_BOUND, UPPER_BOUND, FREE
    double[] alpha;
    QMatrix Q;
    double[] QD;
    double eps;
    double Cp,Cn;
    double[] p;
    int[] active_set;
    double[] G_bar;		// gradient, if we treat free variables as 0
    int l;
    boolean unshrink;	// XXX

    double[] sample_weights;

    static final double INF = java.lang.Double.POSITIVE_INFINITY;

    double get_C(int i)
    {
        final double C = (y[i] > 0)? Cp : Cn;
        if (sample_weights==null)
            return C;
        else {
            return C * sample_weights[i];
        }
    }
    void update_alpha_status(int i)
    {
        if(alpha[i] >= get_C(i))
            alpha_status[i] = UPPER_BOUND;
        else if(alpha[i] <= 0)
            alpha_status[i] = LOWER_BOUND;
        else alpha_status[i] = FREE;
    }
    boolean is_upper_bound(int i) { return alpha_status[i] == UPPER_BOUND; }
    boolean is_lower_bound(int i) { return alpha_status[i] == LOWER_BOUND; }
    boolean is_free(int i) {  return alpha_status[i] == FREE; }

    // java: information about solution except alpha,
    // because we cannot return multiple values otherwise...
    static class SolutionInfo {
        double obj;
        double rho;
        double upper_bound_p;
        double upper_bound_n;
        double r;	// for Solver_NU
    }

    void swap_index(int i, int j)
    {
        Q.swap_index(i,j);
        do {byte tmp=y[i]; y[i]=y[j]; y[j]=tmp;} while(false);
        do {double tmp=G[i]; G[i]=G[j]; G[j]=tmp;} while(false);
        do {byte tmp=alpha_status[i]; alpha_status[i]=alpha_status[j]; alpha_status[j]=tmp;} while(false);
        do {double tmp=alpha[i]; alpha[i]=alpha[j]; alpha[j]=tmp;} while(false);
        do {double tmp=p[i]; p[i]=p[j]; p[j]=tmp;} while(false);
        do {int tmp=active_set[i]; active_set[i]=active_set[j]; active_set[j]=tmp;} while(false);
        do {double tmp=G_bar[i]; G_bar[i]=G_bar[j]; G_bar[j]=tmp;} while(false);
    }

    void reconstruct_gradient()
    {
        // reconstruct inactive elements of G from G_bar and free variables

        if(active_size == l) return;

        int i,j;
        int nr_free = 0;

        for(j=active_size;j<l;j++)
            G[j] = G_bar[j] + p[j];

        for(j=0;j<active_size;j++)
            if(is_free(j))
                nr_free++;

        if(2*nr_free < active_size)
            SVM.info("\nWARNING: using -h 0 may be faster\n");

        if (nr_free*l > 2*active_size*(l-active_size))
        {
            for(i=active_size;i<l;i++)
            {
                float[] Q_i = Q.get_Q(i,active_size);
                for(j=0;j<active_size;j++)
                    if(is_free(j))
                        G[i] += alpha[j] * Q_i[j];
            }
        }
        else
        {
            for(i=0;i<active_size;i++)
                if(is_free(i))
                {
                    float[] Q_i = Q.get_Q(i,l);
                    double alpha_i = alpha[i];
                    for(j=active_size;j<l;j++)
                        G[j] += alpha_i * Q_i[j];
                }
        }
    }

    void Solve(int l, QMatrix Q, double[] p_, byte[] y_,
               double[] alpha_, double Cp, double Cn, double eps, SolutionInfo si, int shrinking, double[] sample_weights)
    {
        this.l = l;
        this.Q = Q;
        QD = Q.get_QD();
        p = p_.clone();
        y = y_.clone();
        alpha = alpha_.clone();
        this.Cp = Cp;
        this.Cn = Cn;
        this.eps = eps;
        this.unshrink = false;
        this.sample_weights = sample_weights;

        // initialize alpha_status
        {
            alpha_status = new byte[l];
            for(int i=0;i<l;i++)
                update_alpha_status(i);
        }

        // initialize active set (for shrinking)
        {
            active_set = new int[l];
            for(int i=0;i<l;i++)
                active_set[i] = i;
            active_size = l;
        }

        // initialize gradient
        {
            G = new double[l];
            G_bar = new double[l];
            int i;
            for(i=0;i<l;i++)
            {
                G[i] = p[i];
                G_bar[i] = 0;
            }
            for(i=0;i<l;i++)
                if(!is_lower_bound(i))
                {
                    float[] Q_i = Q.get_Q(i,l);
                    double alpha_i = alpha[i];
                    int j;
                    for(j=0;j<l;j++)
                        G[j] += alpha_i*Q_i[j];
                    if(is_upper_bound(i))
                        for(j=0;j<l;j++)
                            G_bar[j] += get_C(i) * Q_i[j];
                }
        }

        // optimization step

        int iter = 0;
        int max_iter = Math.max(10000000, l>Integer.MAX_VALUE/100 ? Integer.MAX_VALUE : 100*l);
        int counter = Math.min(l,1000)+1;
        int[] working_set = new int[2];

        while(iter < max_iter)
        {
            // show progress and do shrinking

            if(--counter == 0)
            {
                counter = Math.min(l,1000);
                if(shrinking!=0) do_shrinking();
                SVM.info(".");
            }

            if(select_working_set(working_set)!=0)
            {
                // reconstruct the whole gradient
                reconstruct_gradient();
                // reset active set size and check
                active_size = l;
                SVM.info("*");
                if(select_working_set(working_set)!=0)
                    break;
                else
                    counter = 1;	// do shrinking next iteration
            }

            int i = working_set[0];
            int j = working_set[1];

            ++iter;

            // update alpha[i] and alpha[j], handle bounds carefully

            float[] Q_i = Q.get_Q(i,active_size);
            float[] Q_j = Q.get_Q(j,active_size);

            double C_i = get_C(i);
            double C_j = get_C(j);

            double old_alpha_i = alpha[i];
            double old_alpha_j = alpha[j];

            if(y[i]!=y[j])
            {
                double quad_coef = QD[i]+QD[j]+2*Q_i[j];
                if (quad_coef <= 0)
                    quad_coef = 1e-12;
                double delta = (-G[i]-G[j])/quad_coef;
                double diff = alpha[i] - alpha[j];
                alpha[i] += delta;
                alpha[j] += delta;

                if(diff > 0)
                {
                    if(alpha[j] < 0)
                    {
                        alpha[j] = 0;
                        alpha[i] = diff;
                    }
                }
                else
                {
                    if(alpha[i] < 0)
                    {
                        alpha[i] = 0;
                        alpha[j] = -diff;
                    }
                }
                if(diff > C_i - C_j)
                {
                    if(alpha[i] > C_i)
                    {
                        alpha[i] = C_i;
                        alpha[j] = C_i - diff;
                    }
                }
                else
                {
                    if(alpha[j] > C_j)
                    {
                        alpha[j] = C_j;
                        alpha[i] = C_j + diff;
                    }
                }
            }
            else
            {
                double quad_coef = QD[i]+QD[j]-2*Q_i[j];
                if (quad_coef <= 0)
                    quad_coef = 1e-12;
                double delta = (G[i]-G[j])/quad_coef;
                double sum = alpha[i] + alpha[j];
                alpha[i] -= delta;
                alpha[j] += delta;

                if(sum > C_i)
                {
                    if(alpha[i] > C_i)
                    {
                        alpha[i] = C_i;
                        alpha[j] = sum - C_i;
                    }
                }
                else
                {
                    if(alpha[j] < 0)
                    {
                        alpha[j] = 0;
                        alpha[i] = sum;
                    }
                }
                if(sum > C_j)
                {
                    if(alpha[j] > C_j)
                    {
                        alpha[j] = C_j;
                        alpha[i] = sum - C_j;
                    }
                }
                else
                {
                    if(alpha[i] < 0)
                    {
                        alpha[i] = 0;
                        alpha[j] = sum;
                    }
                }
            }

            // update G

            double delta_alpha_i = alpha[i] - old_alpha_i;
            double delta_alpha_j = alpha[j] - old_alpha_j;

            for(int k=0;k<active_size;k++)
            {
                G[k] += Q_i[k]*delta_alpha_i + Q_j[k]*delta_alpha_j;
            }

            // update alpha_status and G_bar

            {
                boolean ui = is_upper_bound(i);
                boolean uj = is_upper_bound(j);
                update_alpha_status(i);
                update_alpha_status(j);
                int k;
                if(ui != is_upper_bound(i))
                {
                    Q_i = Q.get_Q(i,l);
                    if(ui)
                        for(k=0;k<l;k++)
                            G_bar[k] -= C_i * Q_i[k];
                    else
                        for(k=0;k<l;k++)
                            G_bar[k] += C_i * Q_i[k];
                }

                if(uj != is_upper_bound(j))
                {
                    Q_j = Q.get_Q(j,l);
                    if(uj)
                        for(k=0;k<l;k++)
                            G_bar[k] -= C_j * Q_j[k];
                    else
                        for(k=0;k<l;k++)
                            G_bar[k] += C_j * Q_j[k];
                }
            }

        }

        if(iter >= max_iter)
        {
            if(active_size < l)
            {
                // reconstruct the whole gradient to calculate objective value
                reconstruct_gradient();
                active_size = l;
                SVM.info("*");
            }
            System.err.print("\nWARNING: reaching max number of iterations\n");
        }

        // calculate rho

        si.rho = calculate_rho();

        // calculate objective value
        {
            double v = 0;
            int i;
            for(i=0;i<l;i++)
                v += alpha[i] * (G[i] + p[i]);

            si.obj = v/2;
        }

        // put back the solution
        {
            for(int i=0;i<l;i++)
                alpha_[active_set[i]] = alpha[i];
        }

        si.upper_bound_p = Cp;
        si.upper_bound_n = Cn;

        SVM.info("\noptimization finished, #iter = "+iter+"\n");
    }

    // return 1 if already optimal, return 0 otherwise
    int select_working_set(int[] working_set)
    {
        // return i,j such that
        // i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
        // j: mimimizes the decrease of obj value
        //    (if quadratic coefficeint <= 0, replace it with tau)
        //    -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)

        double Gmax = -INF;
        double Gmax2 = -INF;
        int Gmax_idx = -1;
        int Gmin_idx = -1;
        double obj_diff_min = INF;

        for(int t=0;t<active_size;t++)
            if(y[t]==+1)
            {
                if(!is_upper_bound(t))
                    if(-G[t] >= Gmax)
                    {
                        Gmax = -G[t];
                        Gmax_idx = t;
                    }
            }
            else
            {
                if(!is_lower_bound(t))
                    if(G[t] >= Gmax)
                    {
                        Gmax = G[t];
                        Gmax_idx = t;
                    }
            }

        int i = Gmax_idx;
        float[] Q_i = null;
        if(i != -1) // null Q_i not accessed: Gmax=-INF if i=-1
            Q_i = Q.get_Q(i,active_size);

        for(int j=0;j<active_size;j++)
        {
            if(y[j]==+1)
            {
                if (!is_lower_bound(j))
                {
                    double grad_diff=Gmax+G[j];
                    if (G[j] >= Gmax2)
                        Gmax2 = G[j];
                    if (grad_diff > 0)
                    {
                        double obj_diff;
                        double quad_coef = QD[i]+QD[j]-2.0*y[i]*Q_i[j];
                        if (quad_coef > 0)
                            obj_diff = -(grad_diff*grad_diff)/quad_coef;
                        else
                            obj_diff = -(grad_diff*grad_diff)/1e-12;

                        if (obj_diff <= obj_diff_min)
                        {
                            Gmin_idx=j;
                            obj_diff_min = obj_diff;
                        }
                    }
                }
            }
            else
            {
                if (!is_upper_bound(j))
                {
                    double grad_diff= Gmax-G[j];
                    if (-G[j] >= Gmax2)
                        Gmax2 = -G[j];
                    if (grad_diff > 0)
                    {
                        double obj_diff;
                        double quad_coef = QD[i]+QD[j]+2.0*y[i]*Q_i[j];
                        if (quad_coef > 0)
                            obj_diff = -(grad_diff*grad_diff)/quad_coef;
                        else
                            obj_diff = -(grad_diff*grad_diff)/1e-12;

                        if (obj_diff <= obj_diff_min)
                        {
                            Gmin_idx=j;
                            obj_diff_min = obj_diff;
                        }
                    }
                }
            }
        }

        if(Gmax+Gmax2 < eps || Gmin_idx == -1)
            return 1;

        working_set[0] = Gmax_idx;
        working_set[1] = Gmin_idx;
        return 0;
    }

    private boolean be_shrunk(int i, double Gmax1, double Gmax2)
    {
        if(is_upper_bound(i))
        {
            if(y[i]==+1)
                return(-G[i] > Gmax1);
            else
                return(-G[i] > Gmax2);
        }
        else if(is_lower_bound(i))
        {
            if(y[i]==+1)
                return(G[i] > Gmax2);
            else
                return(G[i] > Gmax1);
        }
        else
            return(false);
    }

    void do_shrinking()
    {
        int i;
        double Gmax1 = -INF;		// max { -y_i * grad(f)_i | i in I_up(\alpha) }
        double Gmax2 = -INF;		// max { y_i * grad(f)_i | i in I_low(\alpha) }

        // find maximal violating pair first
        for(i=0;i<active_size;i++)
        {
            if(y[i]==+1)
            {
                if(!is_upper_bound(i))
                {
                    if(-G[i] >= Gmax1)
                        Gmax1 = -G[i];
                }
                if(!is_lower_bound(i))
                {
                    if(G[i] >= Gmax2)
                        Gmax2 = G[i];
                }
            }
            else
            {
                if(!is_upper_bound(i))
                {
                    if(-G[i] >= Gmax2)
                        Gmax2 = -G[i];
                }
                if(!is_lower_bound(i))
                {
                    if(G[i] >= Gmax1)
                        Gmax1 = G[i];
                }
            }
        }

        if(unshrink == false && Gmax1 + Gmax2 <= eps*10)
        {
            unshrink = true;
            reconstruct_gradient();
            active_size = l;
        }

        for(i=0;i<active_size;i++)
            if (be_shrunk(i, Gmax1, Gmax2))
            {
                active_size--;
                while (active_size > i)
                {
                    if (!be_shrunk(active_size, Gmax1, Gmax2))
                    {
                        swap_index(i,active_size);
                        break;
                    }
                    active_size--;
                }
            }
    }

    double calculate_rho()
    {
        double r;
        int nr_free = 0;
        double ub = INF, lb = -INF, sum_free = 0;
        for(int i=0;i<active_size;i++)
        {
            double yG = y[i]*G[i];

            if(is_lower_bound(i))
            {
                if(y[i] > 0)
                    ub = Math.min(ub,yG);
                else
                    lb = Math.max(lb,yG);
            }
            else if(is_upper_bound(i))
            {
                if(y[i] < 0)
                    ub = Math.min(ub,yG);
                else
                    lb = Math.max(lb,yG);
            }
            else
            {
                ++nr_free;
                sum_free += yG;
            }
        }

        if(nr_free>0)
            r = sum_free/nr_free;
        else
            r = (ub+lb)/2;

        return r;
    }

}

//
// Q matrices for various formulations
//
class SVC_Q extends Kernel
{
    private final byte[] y;
    private final Cache cache;
    private final double[] QD;

    SVC_Q(svm_problem prob, svm_parameter param, byte[] y_)
    {
        super(prob.l, prob.x, param);
        y = y_.clone();
        cache = new Cache(prob.l,(long)(param.cache_size*(1<<20)));
        QD = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            QD[i] = kernel_function(i,i);
    }

    float[] get_Q(int i, int len)
    {
        float[][] data = new float[1][];
        int start, j;
        if((start = cache.get_data(i,data,len)) < len)
        {
            for(j=start;j<len;j++)
                data[0][j] = (float)(y[i]*y[j]*kernel_function(i,j));
        }
        return data[0];
    }

    double[] get_QD()
    {
        return QD;
    }

    void swap_index(int i, int j)
    {
        cache.swap_index(i,j);
        super.swap_index(i,j);
        { byte tmp=y[i]; y[i]=y[j]; y[j]=tmp;};
        { double tmp=QD[i]; QD[i]=QD[j]; QD[j]=tmp;};
    }
}

class ONE_CLASS_Q extends Kernel
{
    private final Cache cache;
    private final double[] QD;

    ONE_CLASS_Q(svm_problem prob, svm_parameter param)
    {
        super(prob.l, prob.x, param);
        cache = new Cache(prob.l,(long)(param.cache_size*(1<<20)));
        QD = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            QD[i] = kernel_function(i,i);
    }

    float[] get_Q(int i, int len)
    {
        float[][] data = new float[1][];
        int start, j;
        if((start = cache.get_data(i,data,len)) < len)
        {
            for(j=start;j<len;j++)
                data[0][j] = (float)kernel_function(i,j);
        }
        return data[0];
    }

    double[] get_QD()
    {
        return QD;
    }

    void swap_index(int i, int j)
    {
        cache.swap_index(i,j);
        super.swap_index(i,j);
        { double tmp=QD[i]; QD[i]=QD[j]; QD[j]=tmp;};
    }
}

class SVR_Q extends Kernel
{
    private final int l;
    private final Cache cache;
    private final byte[] sign;
    private final int[] index;
    private int next_buffer;
    private float[][] buffer;
    private final double[] QD;

    SVR_Q(svm_problem prob, svm_parameter param)
    {
        super(prob.l, prob.x, param);
        l = prob.l;
        cache = new Cache(l,(long)(param.cache_size*(1<<20)));
        QD = new double[2*l];
        sign = new byte[2*l];
        index = new int[2*l];
        for(int k=0;k<l;k++)
        {
            sign[k] = 1;
            sign[k+l] = -1;
            index[k] = k;
            index[k+l] = k;
            QD[k] = kernel_function(k,k);
            QD[k+l] = QD[k];
        }
        buffer = new float[2][2*l];
        next_buffer = 0;
    }

    void swap_index(int i, int j)
    {
        { byte tmp=sign[i]; sign[i]=sign[j]; sign[j]=tmp;};
        { int tmp=index[i]; index[i]=index[j]; index[j]=tmp;};
        { double tmp=QD[i]; QD[i]=QD[j]; QD[j]=tmp;};
    }

    float[] get_Q(int i, int len)
    {
        float[][] data = new float[1][];
        int j, real_i = index[i];
        if(cache.get_data(real_i,data,l) < l)
        {
            for(j=0;j<l;j++)
                data[0][j] = (float)kernel_function(real_i,j);
        }

        // reorder and copy
        float buf[] = buffer[next_buffer];
        next_buffer = 1 - next_buffer;
        byte si = sign[i];
        for(j=0;j<len;j++)
            buf[j] = (float) si * sign[j] * data[0][index[j]];
        return buf;
    }

    double[] get_QD()
    {
        return QD;
    }
}

public class SVM {
    public static final boolean DONT_USE_SAMPLE_WEIGHTS = true;

    //
    // construct and solve various formulations
    //
    public static final int LIBSVM_VERSION = 321;
    public static final Random rand = new Random();

    private static svm_print_interface svm_print_stdout = new svm_print_interface() {
        public void print(String s)
        {
            System.out.print(s);
            System.out.flush();
        }
    };

    private static svm_print_interface svm_print_string = svm_print_stdout;

    static void info(String s)
    {
        svm_print_string.print(s);
    }

    private static void solve_c_svc(svm_problem prob, svm_parameter param,
                                    double[] alpha, Solver.SolutionInfo si,
                                    double Cp, double Cn, double[] sample_weigts)
    {
        int l = prob.l;
        double[] minus_ones = new double[l];
        byte[] y = new byte[l];

        int i;

        for(i=0;i<l;i++)
        {
            alpha[i] = 0;
            minus_ones[i] = -1;
            if(prob.y[i] > 0) y[i] = +1; else y[i] = -1;
        }

        Solver s = new Solver();
        s.Solve(l, new SVC_Q(prob,param,y), minus_ones, y,
                alpha, Cp, Cn, param.eps, si, param.shrinking, sample_weigts);

        double sum_alpha=0;
        for(i=0;i<l;i++)
            sum_alpha += alpha[i];

        if (Cp==Cn)
            SVM.info("nu = "+sum_alpha/(Cp*prob.l)+"\n");

        for(i=0;i<l;i++)
            alpha[i] *= y[i];
    }
    //
    // DecisionFunction
    //
    static class DecisionFunction
    {
        double[] alpha;
        double rho;
    }

    static DecisionFunction svm_train_one(
            svm_problem prob, svm_parameter param,
            double Cp, double Cn, double[] sample_weights)
    {
        double[] alpha = new double[prob.l];
        Solver.SolutionInfo si = new Solver.SolutionInfo();
        switch(param.svm_type)
        {
            case svm_parameter.C_SVC:
                solve_c_svc(prob,param,alpha,si,Cp,Cn,sample_weights);
                break;
            default: throw new UnsupportedOperationException();
        }

        SVM.info("obj = "+si.obj+", rho = "+si.rho+"\n");

        // output SVs

        int nSV = 0;
        int nBSV = 0;
        for(int i=0;i<prob.l;i++)
        {
            if(Math.abs(alpha[i]) > 0)
            {
                ++nSV;
                if(prob.y[i] > 0)
                {
                    if(Math.abs(alpha[i]) >= si.upper_bound_p)
                        ++nBSV;
                }
                else
                {
                    if(Math.abs(alpha[i]) >= si.upper_bound_n)
                        ++nBSV;
                }
            }
        }

        SVM.info("nSV = "+nSV+", nBSV = "+nBSV+"\n");

        DecisionFunction f = new DecisionFunction();
        f.alpha = alpha;
        f.rho = si.rho;
        return f;
    }

    // Platt's binary SVM Probablistic Output: an improvement from Lin et al.
    public static void sigmoid_train(int l, double[] dec_values, double[] labels,
                                      double[] probAB)
    {
        double A, B;
        double prior1=0, prior0 = 0;
        int i;

        for (i=0;i<l;i++)
            if (labels[i] > 0) prior1+=1;
            else prior0+=1;

        int max_iter=100;	// Maximal number of iterations
        double min_step=1e-10;	// Minimal step taken in line search
        double sigma=1e-12;	// For numerically strict PD of Hessian
        double eps=1e-5;
        double hiTarget=(prior1+1.0)/(prior1+2.0);
        double loTarget=1/(prior0+2.0);
        double[] t= new double[l];
        double fApB,p,q,h11,h22,h21,g1,g2,det,dA,dB,gd,stepsize;
        double newA,newB,newf,d1,d2;
        int iter;

        // Initial Point and Initial Fun Value
        A=0.0; B=Math.log((prior0+1.0)/(prior1+1.0));
        double fval = 0.0;

        for (i=0;i<l;i++)
        {
            if (labels[i]>0) t[i]=hiTarget;
            else t[i]=loTarget;
            fApB = dec_values[i]*A+B;
            if (fApB>=0)
                fval += t[i]*fApB + Math.log(1+Math.exp(-fApB));
            else
                fval += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
        }
        for (iter=0;iter<max_iter;iter++)
        {
            // Update Gradient and Hessian (use H' = H + sigma I)
            h11=sigma; // numerically ensures strict PD
            h22=sigma;
            h21=0.0;g1=0.0;g2=0.0;
            for (i=0;i<l;i++)
            {
                fApB = dec_values[i]*A+B;
                if (fApB >= 0)
                {
                    p=Math.exp(-fApB)/(1.0+Math.exp(-fApB));
                    q=1.0/(1.0+Math.exp(-fApB));
                }
                else
                {
                    p=1.0/(1.0+Math.exp(fApB));
                    q=Math.exp(fApB)/(1.0+Math.exp(fApB));
                }
                d2=p*q;
                h11+=dec_values[i]*dec_values[i]*d2;
                h22+=d2;
                h21+=dec_values[i]*d2;
                d1=t[i]-p;
                g1+=dec_values[i]*d1;
                g2+=d1;
            }

            // Stopping Criteria
            if (Math.abs(g1)<eps && Math.abs(g2)<eps)
                break;

            // Finding Newton direction: -inv(H') * g
            det=h11*h22-h21*h21;
            dA=-(h22*g1 - h21 * g2) / det;
            dB=-(-h21*g1+ h11 * g2) / det;
            gd=g1*dA+g2*dB;


            stepsize = 1;		// Line Search
            while (stepsize >= min_step)
            {
                newA = A + stepsize * dA;
                newB = B + stepsize * dB;

                // New function value
                newf = 0.0;
                for (i=0;i<l;i++)
                {
                    fApB = dec_values[i]*newA+newB;
                    if (fApB >= 0)
                        newf += t[i]*fApB + Math.log(1+Math.exp(-fApB));
                    else
                        newf += (t[i] - 1)*fApB +Math.log(1+Math.exp(fApB));
                }
                // Check sufficient decrease
                if (newf<fval+0.0001*stepsize*gd)
                {
                    A=newA;B=newB;fval=newf;
                    break;
                }
                else
                    stepsize = stepsize / 2.0;
            }

            if (stepsize < min_step)
            {
                SVM.info("Line search fails in two-class probability estimates\n");
                break;
            }
        }

        if (iter>=max_iter)
            SVM.info("Reaching maximal iterations in two-class probability estimates\n");
        probAB[0]=A;probAB[1]=B;
    }

    private static double sigmoid_predict(double decision_value, double A, double B)
    {
        double fApB = decision_value*A+B;
        if (fApB >= 0)
            return Math.exp(-fApB)/(1.0+Math.exp(-fApB));
        else
            return 1.0/(1+Math.exp(fApB)) ;
    }

    // Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
    private static void multiclass_probability(int k, double[][] r, double[] p)
    {
        int t,j;
        int iter = 0, max_iter=Math.max(100,k);
        double[][] Q=new double[k][k];
        double[] Qp=new double[k];
        double pQp, eps=0.005/k;

        for (t=0;t<k;t++)
        {
            p[t]=1.0/k;  // Valid if k = 1
            Q[t][t]=0;
            for (j=0;j<t;j++)
            {
                Q[t][t]+=r[j][t]*r[j][t];
                Q[t][j]=Q[j][t];
            }
            for (j=t+1;j<k;j++)
            {
                Q[t][t]+=r[j][t]*r[j][t];
                Q[t][j]=-r[j][t]*r[t][j];
            }
        }
        for (iter=0;iter<max_iter;iter++)
        {
            // stopping condition, recalculate QP,pQP for numerical accuracy
            pQp=0;
            for (t=0;t<k;t++)
            {
                Qp[t]=0;
                for (j=0;j<k;j++)
                    Qp[t]+=Q[t][j]*p[j];
                pQp+=p[t]*Qp[t];
            }
            double max_error=0;
            for (t=0;t<k;t++)
            {
                double error=Math.abs(Qp[t]-pQp);
                if (error>max_error)
                    max_error=error;
            }
            if (max_error<eps) break;

            for (t=0;t<k;t++)
            {
                double diff=(-Qp[t]+pQp)/Q[t][t];
                p[t]+=diff;
                pQp=(pQp+diff*(diff*Q[t][t]+2*Qp[t]))/(1+diff)/(1+diff);
                for (j=0;j<k;j++)
                {
                    Qp[j]=(Qp[j]+diff*Q[t][j])/(1+diff);
                    p[j]/=(1+diff);
                }
            }
        }
        if (iter>=max_iter)
            SVM.info("Exceeds max_iter in multiclass_prob\n");
    }

    // Cross-validation decision values for probability estimates
    public static void svm_binary_svc_probability(svm_problem prob, svm_parameter param, double Cp, double Cn, double[] probAB, double[] sample_weights)
    {
        int i;
        int nr_fold = 5;
        int[] perm = new int[prob.l];
        double[] dec_values = new double[prob.l];

        // random shuffle
        for(i=0;i<prob.l;i++) perm[i]=i;
        for(i=0;i<prob.l;i++)
        {
            int j = i+rand.nextInt(prob.l-i);
            { int tmp=perm[i]; perm[i]=perm[j]; perm[j]=tmp;};
        }
        for(i=0;i<nr_fold;i++)
        {
            int begin = i*prob.l/nr_fold;
            int end = (i+1)*prob.l/nr_fold;
            int j,k;
            svm_problem subprob = new svm_problem();

            subprob.l = prob.l-(end-begin);
            subprob.x = new svm_node[subprob.l][];
            subprob.y = new double[subprob.l];

            k=0;
            for(j=0;j<begin;j++)
            {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }
            for(j=end;j<prob.l;j++)
            {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }
            int p_count=0,n_count=0;
            for(j=0;j<k;j++)
                if(subprob.y[j]>0)
                    p_count++;
                else
                    n_count++;

            if(p_count==0 && n_count==0)
                for(j=begin;j<end;j++)
                    dec_values[perm[j]] = 0;
            else if(p_count > 0 && n_count == 0)
                for(j=begin;j<end;j++)
                    dec_values[perm[j]] = 1;
            else if(p_count == 0 && n_count > 0)
                for(j=begin;j<end;j++)
                    dec_values[perm[j]] = -1;
            else
            {
                svm_parameter subparam = (svm_parameter)param.clone();
                subparam.probability=0;
                subparam.C=1.0;
                subparam.nr_weight=2;
                subparam.weight_label = new int[2];
                subparam.weight = new double[2];
                subparam.weight_label[0]=+1;
                subparam.weight_label[1]=-1;
                subparam.weight[0]=Cp;
                subparam.weight[1]=Cn;
                svm_model submodel = svm_train(subprob,subparam, sample_weights);
                for(j=begin;j<end;j++)
                {
                    double[] dec_value=new double[1];
                    svm_predict_values(submodel,prob.x[perm[j]],dec_value);
                    dec_values[perm[j]]=dec_value[0];
                    // ensure +1 -1 order; reason not using CV subroutine
                    dec_values[perm[j]] *= submodel.label[0];
                }
            }
        }
        sigmoid_train(prob.l,dec_values,prob.y,probAB);
    }

    // label: label name, start: begin of each class, count: #data of classes, perm: indices to the original data
    // perm, length l, must be allocated before calling this subroutine
    private static void svm_group_classes(svm_problem prob, int[] nr_class_ret, int[][] label_ret, int[][] start_ret, int[][] count_ret, int[] perm)
    {
        int l = prob.l;
        int max_nr_class = 16;
        int nr_class = 0;
        int[] label = new int[max_nr_class];
        int[] count = new int[max_nr_class];
        int[] data_label = new int[l];
        int i;

        for(i=0;i<l;i++)
        {
            int this_label = (int)(prob.y[i]);
            int j;
            for(j=0;j<nr_class;j++)
            {
                if(this_label == label[j])
                {
                    ++count[j];
                    break;
                }
            }
            data_label[i] = j;
            if(j == nr_class)
            {
                if(nr_class == max_nr_class)
                {
                    max_nr_class *= 2;
                    int[] new_data = new int[max_nr_class];
                    System.arraycopy(label,0,new_data,0,label.length);
                    label = new_data;
                    new_data = new int[max_nr_class];
                    System.arraycopy(count,0,new_data,0,count.length);
                    count = new_data;
                }
                label[nr_class] = this_label;
                count[nr_class] = 1;
                ++nr_class;
            }
        }

        //
        // Labels are ordered by their first occurrence in the training set.
        // However, for two-class sets with -1/+1 labels and -1 appears first,
        // we swap labels to ensure that internally the binary SVM has positive data corresponding to the +1 instances.
        //
        if (nr_class == 2 && label[0] == -1 && label[1] == +1)
        {
            { int tmp=label[0]; label[0]=label[1]; label[1]=tmp;};
            { int tmp=count[0]; count[0]=count[1]; count[1]=tmp;};
            for(i=0;i<l;i++)
            {
                if(data_label[i] == 0)
                    data_label[i] = 1;
                else
                    data_label[i] = 0;
            }
        }

        int[] start = new int[nr_class];
        start[0] = 0;
        for(i=1;i<nr_class;i++)
            start[i] = start[i-1]+count[i-1];
        for(i=0;i<l;i++)
        {
            perm[start[data_label[i]]] = i;
            ++start[data_label[i]];
        }
        start[0] = 0;
        for(i=1;i<nr_class;i++)
            start[i] = start[i-1]+count[i-1];

        nr_class_ret[0] = nr_class;
        label_ret[0] = label;
        start_ret[0] = start;
        count_ret[0] = count;
    }

    //
    // Interface functions
    //
    public static svm_model svm_train(svm_problem prob, svm_parameter param, double[] sample_weights)
    {
        if (sample_weights == null || DONT_USE_SAMPLE_WEIGHTS) return svm.svm_train(prob, param);
        svm_model model = new svm_model();
        model.param = param;

        if(param.svm_type == svm_parameter.ONE_CLASS ||
                param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR)
        {
            throw new UnsupportedOperationException();
        }
        else
        {
            // classification
            int l = prob.l;
            int[] tmp_nr_class = new int[1];
            int[][] tmp_label = new int[1][];
            int[][] tmp_start = new int[1][];
            int[][] tmp_count = new int[1][];
            int[] perm = new int[l];

            // group training data of the same class
            svm_group_classes(prob,tmp_nr_class,tmp_label,tmp_start,tmp_count,perm);
            int nr_class = tmp_nr_class[0];
            int[] label = tmp_label[0];
            int[] start = tmp_start[0];
            int[] count = tmp_count[0];

            if(nr_class == 1)
                SVM.info("WARNING: training data in only one class. See README for details.\n");

            svm_node[][] x = new svm_node[l][];
            int i;
            for(i=0;i<l;i++)
                x[i] = prob.x[perm[i]];

            // calculate weighted C

            double[] weighted_C = new double[nr_class];
            for(i=0;i<nr_class;i++)
                weighted_C[i] = param.C;
            for(i=0;i<param.nr_weight;i++)
            {
                int j;
                for(j=0;j<nr_class;j++)
                    if(param.weight_label[i] == label[j])
                        break;
                if(j == nr_class)
                    System.err.print("WARNING: class label "+param.weight_label[i]+" specified in weight is not found\n");
                else
                    weighted_C[j] *= param.weight[i];
            }

            // train k*(k-1)/2 models

            boolean[] nonzero = new boolean[l];
            for(i=0;i<l;i++)
                nonzero[i] = false;
            DecisionFunction[] f = new DecisionFunction[nr_class*(nr_class-1)/2];

            double[] probA=null,probB=null;
            if (param.probability == 1)
            {
                probA=new double[nr_class*(nr_class-1)/2];
                probB=new double[nr_class*(nr_class-1)/2];
            }

            int p = 0;
            for(i=0;i<nr_class;i++)
                for(int j=i+1;j<nr_class;j++)
                {
                    svm_problem sub_prob = new svm_problem();
                    int si = start[i], sj = start[j];
                    int ci = count[i], cj = count[j];
                    sub_prob.l = ci+cj;
                    sub_prob.x = new svm_node[sub_prob.l][];
                    sub_prob.y = new double[sub_prob.l];
                    int k;
                    for(k=0;k<ci;k++)
                    {
                        sub_prob.x[k] = x[si+k];
                        sub_prob.y[k] = +1;
                    }
                    for(k=0;k<cj;k++)
                    {
                        sub_prob.x[ci+k] = x[sj+k];
                        sub_prob.y[ci+k] = -1;
                    }

                    if(param.probability == 1)
                    {
                        double[] probAB=new double[2];
                        svm_binary_svc_probability(sub_prob,param,weighted_C[i],weighted_C[j],probAB, sample_weights);
                        probA[p]=probAB[0];
                        probB[p]=probAB[1];
                    }

                    f[p] = svm_train_one(sub_prob,param,weighted_C[i],weighted_C[j],sample_weights);
                    for(k=0;k<ci;k++)
                        if(!nonzero[si+k] && Math.abs(f[p].alpha[k]) > 0)
                            nonzero[si+k] = true;
                    for(k=0;k<cj;k++)
                        if(!nonzero[sj+k] && Math.abs(f[p].alpha[ci+k]) > 0)
                            nonzero[sj+k] = true;
                    ++p;
                }

            // build output

            model.nr_class = nr_class;

            model.label = new int[nr_class];
            for(i=0;i<nr_class;i++)
                model.label[i] = label[i];

            model.rho = new double[nr_class*(nr_class-1)/2];
            for(i=0;i<nr_class*(nr_class-1)/2;i++)
                model.rho[i] = f[i].rho;

            if(param.probability == 1)
            {
                model.probA = new double[nr_class*(nr_class-1)/2];
                model.probB = new double[nr_class*(nr_class-1)/2];
                for(i=0;i<nr_class*(nr_class-1)/2;i++)
                {
                    model.probA[i] = probA[i];
                    model.probB[i] = probB[i];
                }
            }
            else
            {
                model.probA=null;
                model.probB=null;
            }

            int nnz = 0;
            int[] nz_count = new int[nr_class];
            model.nSV = new int[nr_class];
            for(i=0;i<nr_class;i++)
            {
                int nSV = 0;
                for(int j=0;j<count[i];j++)
                    if(nonzero[start[i]+j])
                    {
                        ++nSV;
                        ++nnz;
                    }
                model.nSV[i] = nSV;
                nz_count[i] = nSV;
            }

            SVM.info("Total nSV = "+nnz+"\n");

            model.l = nnz;
            model.SV = new svm_node[nnz][];
            model.sv_indices = new int[nnz];
            p = 0;
            for(i=0;i<l;i++)
                if(nonzero[i])
                {
                    model.SV[p] = x[i];
                    model.sv_indices[p++] = perm[i] + 1;
                }

            int[] nz_start = new int[nr_class];
            nz_start[0] = 0;
            for(i=1;i<nr_class;i++)
                nz_start[i] = nz_start[i-1]+nz_count[i-1];

            model.sv_coef = new double[nr_class-1][];
            for(i=0;i<nr_class-1;i++)
                model.sv_coef[i] = new double[nnz];

            p = 0;
            for(i=0;i<nr_class;i++)
                for(int j=i+1;j<nr_class;j++)
                {
                    // classifier (i,j): coefficients with
                    // i are in sv_coef[j-1][nz_start[i]...],
                    // j are in sv_coef[i][nz_start[j]...]

                    int si = start[i];
                    int sj = start[j];
                    int ci = count[i];
                    int cj = count[j];

                    int q = nz_start[i];
                    int k;
                    for(k=0;k<ci;k++)
                        if(nonzero[si+k])
                            model.sv_coef[j-1][q++] = f[p].alpha[k];
                    q = nz_start[j];
                    for(k=0;k<cj;k++)
                        if(nonzero[sj+k])
                            model.sv_coef[i][q++] = f[p].alpha[ci+k];
                    ++p;
                }
        }
        return model;
    }

    public static int svm_get_svm_type(svm_model model)
    {
        return model.param.svm_type;
    }

    public static int svm_get_nr_class(svm_model model)
    {
        return model.nr_class;
    }

    public static void svm_get_labels(svm_model model, int[] label)
    {
        if (model.label != null)
            for(int i=0;i<model.nr_class;i++)
                label[i] = model.label[i];
    }

    public static void svm_get_sv_indices(svm_model model, int[] indices)
    {
        if (model.sv_indices != null)
            for(int i=0;i<model.l;i++)
                indices[i] = model.sv_indices[i];
    }

    public static int svm_get_nr_sv(svm_model model)
    {
        return model.l;
    }

    public static double svm_get_svr_probability(svm_model model)
    {
        if ((model.param.svm_type == svm_parameter.EPSILON_SVR || model.param.svm_type == svm_parameter.NU_SVR) &&
                model.probA!=null)
            return model.probA[0];
        else
        {
            System.err.print("Model doesn't contain information for SVR probability inference\n");
            return 0;
        }
    }

    public static double svm_predict_values(svm_model model, svm_node[] x, double[] dec_values)
    {
        int i;
        if(model.param.svm_type == svm_parameter.ONE_CLASS ||
                model.param.svm_type == svm_parameter.EPSILON_SVR ||
                model.param.svm_type == svm_parameter.NU_SVR)
        {
            double[] sv_coef = model.sv_coef[0];
            double sum = 0;
            for(i=0;i<model.l;i++)
                sum += sv_coef[i] * Kernel.k_function(x,model.SV[i],model.param);
            sum -= model.rho[0];
            dec_values[0] = sum;

            if(model.param.svm_type == svm_parameter.ONE_CLASS)
                return (sum>0)?1:-1;
            else
                return sum;
        }
        else
        {
            int nr_class = model.nr_class;
            int l = model.l;

            double[] kvalue = new double[l];
            for(i=0;i<l;i++)
                kvalue[i] = Kernel.k_function(x,model.SV[i],model.param);

            int[] start = new int[nr_class];
            start[0] = 0;
            for(i=1;i<nr_class;i++)
                start[i] = start[i-1]+model.nSV[i-1];

            int[] vote = new int[nr_class];
            for(i=0;i<nr_class;i++)
                vote[i] = 0;

            int p=0;
            for(i=0;i<nr_class;i++)
                for(int j=i+1;j<nr_class;j++)
                {
                    double sum = 0;
                    int si = start[i];
                    int sj = start[j];
                    int ci = model.nSV[i];
                    int cj = model.nSV[j];

                    int k;
                    double[] coef1 = model.sv_coef[j-1];
                    double[] coef2 = model.sv_coef[i];
                    for(k=0;k<ci;k++)
                        sum += coef1[si+k] * kvalue[si+k];
                    for(k=0;k<cj;k++)
                        sum += coef2[sj+k] * kvalue[sj+k];
                    sum -= model.rho[p];
                    dec_values[p] = sum;

                    if(dec_values[p] > 0)
                        ++vote[i];
                    else
                        ++vote[j];
                    p++;
                }

            int vote_max_idx = 0;
            for(i=1;i<nr_class;i++)
                if(vote[i] > vote[vote_max_idx])
                    vote_max_idx = i;

            return model.label[vote_max_idx];
        }
    }

    public static double svm_predict(svm_model model, svm_node[] x)
    {
        int nr_class = model.nr_class;
        double[] dec_values;
        if(model.param.svm_type == svm_parameter.ONE_CLASS ||
                model.param.svm_type == svm_parameter.EPSILON_SVR ||
                model.param.svm_type == svm_parameter.NU_SVR)
            dec_values = new double[1];
        else
            dec_values = new double[nr_class*(nr_class-1)/2];
        double pred_result = svm_predict_values(model, x, dec_values);
        return pred_result;
    }

    public static double svm_predict_probability(svm_model model, svm_node[] x, double[] prob_estimates)
    {
        if ((model.param.svm_type == svm_parameter.C_SVC || model.param.svm_type == svm_parameter.NU_SVC) &&
                model.probA!=null && model.probB!=null)
        {
            int i;
            int nr_class = model.nr_class;
            double[] dec_values = new double[nr_class*(nr_class-1)/2];
            svm_predict_values(model, x, dec_values);

            double min_prob=1e-7;
            double[][] pairwise_prob=new double[nr_class][nr_class];

            int k=0;
            for(i=0;i<nr_class;i++)
                for(int j=i+1;j<nr_class;j++)
                {
                    pairwise_prob[i][j]=Math.min(Math.max(sigmoid_predict(dec_values[k],model.probA[k],model.probB[k]),min_prob),1-min_prob);
                    pairwise_prob[j][i]=1-pairwise_prob[i][j];
                    k++;
                }
            multiclass_probability(nr_class,pairwise_prob,prob_estimates);

            int prob_max_idx = 0;
            for(i=1;i<nr_class;i++)
                if(prob_estimates[i] > prob_estimates[prob_max_idx])
                    prob_max_idx = i;
            return model.label[prob_max_idx];
        }
        else
            return svm_predict(model, x);
    }

    static final String svm_type_table[] =
            {
                    "c_svc","nu_svc","one_class","epsilon_svr","nu_svr",
            };

    static final String kernel_type_table[]=
            {
                    "linear","polynomial","rbf","sigmoid","precomputed"
            };

    public static void svm_save_model(String model_file_name, svm_model model) throws IOException
    {
        DataOutputStream fp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(model_file_name)));

        svm_parameter param = model.param;

        fp.writeBytes("svm_type "+svm_type_table[param.svm_type]+"\n");
        fp.writeBytes("kernel_type "+kernel_type_table[param.kernel_type]+"\n");

        if(param.kernel_type == svm_parameter.POLY)
            fp.writeBytes("degree "+param.degree+"\n");

        if(param.kernel_type == svm_parameter.POLY ||
                param.kernel_type == svm_parameter.RBF ||
                param.kernel_type == svm_parameter.SIGMOID)
            fp.writeBytes("gamma "+param.gamma+"\n");

        if(param.kernel_type == svm_parameter.POLY ||
                param.kernel_type == svm_parameter.SIGMOID)
            fp.writeBytes("coef0 "+param.coef0+"\n");

        int nr_class = model.nr_class;
        int l = model.l;
        fp.writeBytes("nr_class "+nr_class+"\n");
        fp.writeBytes("total_sv "+l+"\n");

        {
            fp.writeBytes("rho");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.rho[i]);
            fp.writeBytes("\n");
        }

        if(model.label != null)
        {
            fp.writeBytes("label");
            for(int i=0;i<nr_class;i++)
                fp.writeBytes(" "+model.label[i]);
            fp.writeBytes("\n");
        }

        if(model.probA != null) // regression has probA only
        {
            fp.writeBytes("probA");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.probA[i]);
            fp.writeBytes("\n");
        }
        if(model.probB != null)
        {
            fp.writeBytes("probB");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.probB[i]);
            fp.writeBytes("\n");
        }

        if(model.nSV != null)
        {
            fp.writeBytes("nr_sv");
            for(int i=0;i<nr_class;i++)
                fp.writeBytes(" "+model.nSV[i]);
            fp.writeBytes("\n");
        }

        fp.writeBytes("SV\n");
        double[][] sv_coef = model.sv_coef;
        svm_node[][] SV = model.SV;

        for(int i=0;i<l;i++)
        {
            for(int j=0;j<nr_class-1;j++)
                fp.writeBytes(sv_coef[j][i]+" ");

            svm_node[] p = SV[i];
            if(param.kernel_type == svm_parameter.PRECOMPUTED)
                fp.writeBytes("0:"+(int)(p[0].value));
            else
                for(int j=0;j<p.length;j++)
                    fp.writeBytes(p[j].index+":"+p[j].value+" ");
            fp.writeBytes("\n");
        }

        fp.close();
    }

    private static double atof(String s)
    {
        return Double.valueOf(s).doubleValue();
    }

    private static int atoi(String s)
    {
        return Integer.parseInt(s);
    }

    private static boolean read_model_header(BufferedReader fp, svm_model model)
    {
        svm_parameter param = new svm_parameter();
        model.param = param;
        try
        {
            while(true)
            {
                String cmd = fp.readLine();
                String arg = cmd.substring(cmd.indexOf(' ')+1);

                if(cmd.startsWith("svm_type"))
                {
                    int i;
                    for(i=0;i<svm_type_table.length;i++)
                    {
                        if(arg.indexOf(svm_type_table[i])!=-1)
                        {
                            param.svm_type=i;
                            break;
                        }
                    }
                    if(i == svm_type_table.length)
                    {
                        System.err.print("unknown SVM type.\n");
                        return false;
                    }
                }
                else if(cmd.startsWith("kernel_type"))
                {
                    int i;
                    for(i=0;i<kernel_type_table.length;i++)
                    {
                        if(arg.indexOf(kernel_type_table[i])!=-1)
                        {
                            param.kernel_type=i;
                            break;
                        }
                    }
                    if(i == kernel_type_table.length)
                    {
                        System.err.print("unknown kernel function.\n");
                        return false;
                    }
                }
                else if(cmd.startsWith("degree"))
                    param.degree = atoi(arg);
                else if(cmd.startsWith("gamma"))
                    param.gamma = atof(arg);
                else if(cmd.startsWith("coef0"))
                    param.coef0 = atof(arg);
                else if(cmd.startsWith("nr_class"))
                    model.nr_class = atoi(arg);
                else if(cmd.startsWith("total_sv"))
                    model.l = atoi(arg);
                else if(cmd.startsWith("rho"))
                {
                    int n = model.nr_class * (model.nr_class-1)/2;
                    model.rho = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.rho[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("label"))
                {
                    int n = model.nr_class;
                    model.label = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.label[i] = atoi(st.nextToken());
                }
                else if(cmd.startsWith("probA"))
                {
                    int n = model.nr_class*(model.nr_class-1)/2;
                    model.probA = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.probA[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("probB"))
                {
                    int n = model.nr_class*(model.nr_class-1)/2;
                    model.probB = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.probB[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("nr_sv"))
                {
                    int n = model.nr_class;
                    model.nSV = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.nSV[i] = atoi(st.nextToken());
                }
                else if(cmd.startsWith("SV"))
                {
                    break;
                }
                else
                {
                    System.err.print("unknown text in model file: ["+cmd+"]\n");
                    return false;
                }
            }
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }

    public static svm_model svm_load_model(String model_file_name) throws IOException
    {
        return svm_load_model(new BufferedReader(new FileReader(model_file_name)));
    }

    public static svm_model svm_load_model(BufferedReader fp) throws IOException
    {
        // read parameters

        svm_model model = new svm_model();
        model.rho = null;
        model.probA = null;
        model.probB = null;
        model.label = null;
        model.nSV = null;

        if (read_model_header(fp, model) == false)
        {
            System.err.print("ERROR: failed to read model\n");
            return null;
        }

        // read sv_coef and SV

        int m = model.nr_class - 1;
        int l = model.l;
        model.sv_coef = new double[m][l];
        model.SV = new svm_node[l][];

        for(int i=0;i<l;i++)
        {
            String line = fp.readLine();
            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            for(int k=0;k<m;k++)
                model.sv_coef[k][i] = atof(st.nextToken());
            int n = st.countTokens()/2;
            model.SV[i] = new svm_node[n];
            for(int j=0;j<n;j++)
            {
                model.SV[i][j] = new svm_node();
                model.SV[i][j].index = atoi(st.nextToken());
                model.SV[i][j].value = atof(st.nextToken());
            }
        }

        fp.close();
        return model;
    }

    public static String svm_check_parameter(svm_problem prob, svm_parameter param)
    {
        // svm_type

        int svm_type = param.svm_type;
        if(svm_type != svm_parameter.C_SVC &&
                svm_type != svm_parameter.NU_SVC &&
                svm_type != svm_parameter.ONE_CLASS &&
                svm_type != svm_parameter.EPSILON_SVR &&
                svm_type != svm_parameter.NU_SVR)
            return "unknown SVM type";

        // kernel_type, degree

        int kernel_type = param.kernel_type;
        if(kernel_type != svm_parameter.LINEAR &&
                kernel_type != svm_parameter.POLY &&
                kernel_type != svm_parameter.RBF &&
                kernel_type != svm_parameter.SIGMOID &&
                kernel_type != svm_parameter.PRECOMPUTED)
            return "unknown kernel type";

        if(param.gamma < 0)
            return "gamma < 0";

        if(param.degree < 0)
            return "degree of polynomial kernel < 0";

        // cache_size,eps,C,nu,p,shrinking

        if(param.cache_size <= 0)
            return "cache_size <= 0";

        if(param.eps <= 0)
            return "eps <= 0";

        if(svm_type == svm_parameter.C_SVC ||
                svm_type == svm_parameter.EPSILON_SVR ||
                svm_type == svm_parameter.NU_SVR)
            if(param.C <= 0)
                return "C <= 0";

        if(svm_type == svm_parameter.NU_SVC ||
                svm_type == svm_parameter.ONE_CLASS ||
                svm_type == svm_parameter.NU_SVR)
            if(param.nu <= 0 || param.nu > 1)
                return "nu <= 0 or nu > 1";

        if(svm_type == svm_parameter.EPSILON_SVR)
            if(param.p < 0)
                return "p < 0";

        if(param.shrinking != 0 &&
                param.shrinking != 1)
            return "shrinking != 0 and shrinking != 1";

        if(param.probability != 0 &&
                param.probability != 1)
            return "probability != 0 and probability != 1";

        if(param.probability == 1 &&
                svm_type == svm_parameter.ONE_CLASS)
            return "one-class SVM probability output not supported yet";

        // check whether nu-svc is feasible

        if(svm_type == svm_parameter.NU_SVC)
        {
            int l = prob.l;
            int max_nr_class = 16;
            int nr_class = 0;
            int[] label = new int[max_nr_class];
            int[] count = new int[max_nr_class];

            int i;
            for(i=0;i<l;i++)
            {
                int this_label = (int)prob.y[i];
                int j;
                for(j=0;j<nr_class;j++)
                    if(this_label == label[j])
                    {
                        ++count[j];
                        break;
                    }

                if(j == nr_class)
                {
                    if(nr_class == max_nr_class)
                    {
                        max_nr_class *= 2;
                        int[] new_data = new int[max_nr_class];
                        System.arraycopy(label,0,new_data,0,label.length);
                        label = new_data;

                        new_data = new int[max_nr_class];
                        System.arraycopy(count,0,new_data,0,count.length);
                        count = new_data;
                    }
                    label[nr_class] = this_label;
                    count[nr_class] = 1;
                    ++nr_class;
                }
            }

            for(i=0;i<nr_class;i++)
            {
                int n1 = count[i];
                for(int j=i+1;j<nr_class;j++)
                {
                    int n2 = count[j];
                    if(param.nu*(n1+n2)/2 > Math.min(n1,n2))
                        return "specified nu is infeasible";
                }
            }
        }

        return null;
    }

    public static int svm_check_probability_model(svm_model model)
    {
        if (((model.param.svm_type == svm_parameter.C_SVC || model.param.svm_type == svm_parameter.NU_SVC) &&
                model.probA!=null && model.probB!=null) ||
                ((model.param.svm_type == svm_parameter.EPSILON_SVR || model.param.svm_type == svm_parameter.NU_SVR) &&
                        model.probA!=null))
            return 1;
        else
            return 0;
    }

    public static void svm_set_print_string_function(svm_print_interface print_func)
    {
        if (print_func == null)
            svm_print_string = svm_print_stdout;
        else
            svm_print_string = print_func;
    }
}
