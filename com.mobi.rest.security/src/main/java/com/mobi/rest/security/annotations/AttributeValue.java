package com.mobi.rest.security.annotations;

/*-
 * #%L
 * com.mobi.rest.security
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2018 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to represent an attribute value to be set on a RESt endpoint request.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AttributeValue {

    /**
     * The {@link ValueType type} of data provided for the Attribute value.
     */
    ValueType type() default ValueType.PRIMITIVE;

    /**
     * Am IRI string representing the ID of the Attribute.
     */
    String id();

    /**
     * An IRI string representing the XSD datatype of the Attribute value.
     */
    String datatype() default "http://www.w3.org/2001/XMLSchema#string";

    /**
     * A string representing the value of the Attribute.
     */
    String value();
}
