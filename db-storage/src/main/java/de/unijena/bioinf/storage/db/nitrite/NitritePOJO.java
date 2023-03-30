/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nitrite;

import com.esotericsoftware.reflectasm.FieldAccess;
import de.unijena.bioinf.storage.db.NoSQLPOJO;
import org.apache.commons.lang3.ClassUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.mapper.Mappable;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.Id;
import org.dizitart.no2.objects.InheritIndices;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

@InheritIndices
public abstract class NitritePOJO extends NoSQLPOJO implements Mappable, Serializable {

    @Id
    protected NitriteId id;

    public NitritePOJO() {}

    public NitriteId getId() {
        return id;
    }

    public void setId(NitriteId id) {
        this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Document write(NitriteMapper mapper) {
        Document document = new Document();
        FieldAccess access = FieldAccess.get(getClass());
        try {
            for (Field field : access.getFields()) {
                field.setAccessible(true);
                if (field.getType() == NitriteId.class) {
                    NitriteId value = (NitriteId) field.get(this);
                    document.put(field.getName(), (value != null) ? value.getIdValue() : null);
                } else if (field.getType() == Collection.class || ClassUtils.getAllInterfaces(field.getType()).contains(Collection.class)) {
                    Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (generic instanceof Class && ClassUtils.getAllInterfaces((Class<?>) generic).contains(Mappable.class)) {
                        List<Document> target = new ArrayList<>();
                        Collection<Mappable> source = (Collection<Mappable>) field.get(this);
                        for (Mappable src : source) {
                            target.add(src.write(mapper));
                        }
                        document.put(field.getName(), target);
                    } else {
                        document.put(field.getName(), field.get(this));
                    }
                } else {
                    document.put(field.getName(), field.get(this));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(NitriteMapper mapper, Document document) {
        if (document != null) {
            FieldAccess access = FieldAccess.get(getClass());
            try {
                for (Field field : access.getFields()) {
                    field.setAccessible(true);
                    if (field.getType() == NitriteId.class) {
                        field.set(this, NitriteId.createId((Long) document.get(field.getName())));
                    } else if (field.getType() == Collection.class || ClassUtils.getAllInterfaces(field.getType()).contains(Collection.class)) {
                        Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        if (generic instanceof Class && ClassUtils.getAllInterfaces((Class<?>) generic).contains(Mappable.class)) {
                            Collection<Mappable> target;
                            if (field.getType() == List.class) {
                                target = new ArrayList<>();
                            } else if (field.getType() == BlockingDeque.class) {
                                target = new LinkedBlockingDeque<>();
                            } else if (field.getType() == BlockingQueue.class) {
                                target = new LinkedBlockingQueue<>();
                            } else if (field.getType() == Deque.class || field.getType() == Queue.class) {
                                target = new ArrayDeque<>();
                            } else if (field.getType() == Set.class) {
                                target = new HashSet<>();
                            } else if (field.getType() == SortedSet.class) {
                                target = new TreeSet<>();
                            } else if (field.getType() == TransferQueue.class) {
                                target = new LinkedTransferQueue<>();
                            } else {
                                target = (Collection<Mappable>) field.getType().getDeclaredConstructor().newInstance();
                            }
                            Collection<Document> source = (Collection<Document>) document.get(field.getName());
                            for (Document src : source) {
                                Mappable t = (Mappable) ((Class<?>) generic).getDeclaredConstructor().newInstance();
                                t.read(mapper, src);
                                target.add(t);
                            }
                            field.set(this, target);
                        } else {
                            field.set(this, document.get(field.getName()));
                        }
                    } else {
                        field.set(this, document.get(field.getName()));
                    }
                }
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        Class<? extends NitritePOJO> clazz = getClass();
        StringBuilder tos = new StringBuilder(clazz.getSimpleName() + "{");
        FieldAccess access = FieldAccess.get(getClass());
        try {
            Field[] fields = access.getFields();
            Arrays.stream(fields).limit(fields.length - 1).forEach((Field field) -> {
                field.setAccessible(true);
                try {
                    tos.append(field.getName()).append("=").append(field.get(this)).append(",");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            tos.append(fields[fields.length - 1].getName()).append("=").append(fields[fields.length - 1].get(this));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        tos.append("}");
        return tos.toString();
    }

}
