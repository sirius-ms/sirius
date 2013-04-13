/*
 * Sirius MassSpec Tool
 * based on the Epos Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Sirius.
 *
 * Sirius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sirius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Sirius.  If not, see <http://www.gnu.org/licenses/>;.
*/
package de.unijena.bioinf.babelms.mzxml.model;

import java.io.Serializable;
import java.net.URI;

public class Contact implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = 2808026012363338652L;

    private String first;
    private String last;
    private String phone;
    private String mail;
    private String uri;

    public Contact() {

    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        helper.startList();
        if (first != null && last != null) {
            helper.def("name", first + " " + last);
        } else if (last != null) {
            helper.def("name", last);
        }
        helper.def("phone", phone);
        helper.def("mail", mail);
        helper.def("uri", uri);
        helper.endList();
        return helper;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getUri() {
        return uri;
    }

    public URI uri() {
        if (uri == null) return null;
        return URI.create(uri);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        if (first != null ? !first.equals(contact.first) : contact.first != null) return false;
        if (last != null ? !last.equals(contact.last) : contact.last != null) return false;
        if (mail != null ? !mail.equals(contact.mail) : contact.mail != null) return false;
        if (phone != null ? !phone.equals(contact.phone) : contact.phone != null) return false;
        if (uri != null ? !uri.equals(contact.uri) : contact.uri != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (last != null ? last.hashCode() : 0);
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        result = 31 * result + (mail != null ? mail.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        return result;
    }
}
