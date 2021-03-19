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

package de.unijena.bioinf.canopus;

import org.tensorflow.Tensor;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class BufferedTrainData {

    protected List<Buffer> inUse;
    protected List<Buffer> done;
    protected List<Buffer> recycled;

    protected class Buffer {
        int size, filled;
        protected FloatBuffer f, p, l;

        public Buffer(int size, int fsize, int psize, int lsize) {
            this.size = size;
            this.filled = 0;
            this.f = FloatBuffer.allocate(size*fsize);
            this.p = FloatBuffer.allocate(size*psize);
            this.l = FloatBuffer.allocate(size*lsize);
        }

        TrainingBatch use() {
            f.rewind();
            p.rewind();
            l.rewind();
            final TrainingBatch b = new TrainingBatch(
                    Tensor.create(new long[]{size,psize},p),
                    Tensor.create(new long[]{size,fsize},f),
                    Tensor.create(new long[]{size,lsize},l)
            );
            recycle();
            return b;
        }

        private void recycle() {
            f.rewind();
            p.rewind();
            l.rewind();
            filled=0;
            recycled.add(this);
        }

        void fill() {
            ++filled;
            if (filled >= size) {
                synchronized (inUse) {
                    // get a fresh buffer from recycled queue
                    if (recycled.isEmpty()) {
                        recycled.add(new Buffer(bsize, fsize, psize, lsize));
                    }
                    final Buffer newOne = recycled.remove(recycled.size()-1);
                    final int ownIndex = inUse.indexOf(this);
                    inUse.set(ownIndex, newOne);
                    done.add(this);
                }
            }
        }

    }

    protected int bsize, fsize, psize, lsize;
    public BufferedTrainData(TrainingData data, int capacity, int bufferSize) {
        this.inUse = new ArrayList<>(capacity);
        this.bsize = bufferSize;
        this.fsize = data.nformulas;
        this.psize = data.nplatts;
        this.lsize = data.nlabels;
        for (int i=0; i < capacity; ++i)
            inUse.add(new Buffer(bsize, fsize, psize, lsize));
        this.recycled = new ArrayList<>();
        this.done = new ArrayList<>();
    }

    public Buffer getDone() {
        if (done.size()>0) return done.remove(done.size()-1);
        else return null;
    }

    public Buffer getBuffer(int i) {
        return inUse.get(i % inUse.size());
    }
}
