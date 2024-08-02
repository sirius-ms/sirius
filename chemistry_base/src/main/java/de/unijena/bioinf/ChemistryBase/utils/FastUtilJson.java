/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import it.unimi.dsi.fastutil.doubles.*;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;

import java.io.IOException;

public final class FastUtilJson {
    public static class LongSetDeserializer extends LongCollectionDeserializer<LongSet> {
        @Override
        protected LongSet newInstance() {
            return new LongOpenHashSet();
        }
    }

    public static class LongListDeserializer extends LongCollectionDeserializer<LongList> {
        @Override
        protected LongList newInstance() {
            return new LongArrayList();
        }
    }


    public static class LongCollectionSerializer<C extends LongCollection> extends JsonSerializer<C> {
        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeArray(value.toLongArray(), 0, value.size());
        }
    }

    public abstract static class LongCollectionDeserializer<C extends LongCollection> extends JsonDeserializer<C> {
        @Override
        public C deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            C set = newInstance(); //todo to I have to start call nextToken first?
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                while (parser.currentToken() != JsonToken.END_ARRAY) {
                    set.add(parser.getLongValue());
                    parser.nextToken();
                }
                return set;
            }
            throw new IOException("Expected LongSet in arrays format but no Array token found!");
        }

        protected abstract C newInstance();
    }


    public static class DoubleSetDeserializer extends DoubleCollectionDeserializer<DoubleSet> {
        @Override
        protected DoubleSet newInstance() {
            return new DoubleOpenHashSet();
        }
    }

    public static class DoubleListDeserializer extends DoubleCollectionDeserializer<DoubleList> {
        @Override
        protected DoubleList newInstance() {
            return new DoubleArrayList();
        }
    }

    public static class DoubleCollectionSerializer<C extends DoubleCollection> extends JsonSerializer<C> {
        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeArray(value.toDoubleArray(), 0, value.size());
        }
    }

    public static class FloatListDeserializer extends FloatCollectionDeserializer<FloatList> {
        @Override
        protected FloatList newInstance() {
            return new FloatArrayList();
        }
    }

    public static class FloatCollectionSerializer<C extends FloatCollection> extends JsonSerializer<C> {
        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeArray(MatrixUtils.float2double(value.toFloatArray()), 0, value.size());
        }
    }

    public abstract static class DoubleCollectionDeserializer<C extends DoubleCollection> extends JsonDeserializer<C> {
        @Override
        public C deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            C set = newInstance(); //todo to I have to start call nextToken first?
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                while (parser.currentToken() != JsonToken.END_ARRAY) {
                    set.add(parser.getDoubleValue());
                    parser.nextToken();
                }
                return set;
            }
            throw new IOException("Expected LongSet in arrays format but no Array token found!");
        }

        protected abstract C newInstance();
    }

    public abstract static class FloatCollectionDeserializer<C extends FloatCollection> extends JsonDeserializer<C> {
        @Override
        public C deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            C set = newInstance(); //todo to I have to start call nextToken first?
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                while (parser.currentToken() != JsonToken.END_ARRAY) {
                    set.add(parser.getFloatValue());
                    parser.nextToken();
                }
                return set;
            }
            throw new IOException("Expected LongSet in arrays format but no Array token found!");
        }

        protected abstract C newInstance();
    }

    public static class IntSetDeserializer extends IntCollectionDeserializer<IntSet> {
        @Override
        protected IntSet newInstance() {
            return new IntOpenHashSet();
        }
    }

    public static class IntListDeserializer extends IntCollectionDeserializer<IntList> {
        @Override
        protected IntList newInstance() {
            return new IntArrayList();
        }
    }

    public static class IntCollectionSerializer<C extends IntCollection> extends JsonSerializer<C> {
        @Override
        public void serialize(C value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeArray(value.toIntArray(), 0, value.size());
        }
    }

    public abstract static class IntCollectionDeserializer<C extends IntCollection> extends JsonDeserializer<C> {
        @Override
        public C deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            C set = newInstance(); //todo to I have to start call nextToken first?
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                parser.nextToken();
                while (parser.currentToken() != JsonToken.END_ARRAY) {
                    set.add(parser.getIntValue());
                    parser.nextToken();
                }
                return set;
            }
            throw new IOException("Expected LongSet in arrays format but no Array token found!");
        }

        protected abstract C newInstance();
    }
}
