package org.matonto.rdf.orm.conversion.impl;

/*-
 * #%L
 * RDF ORM
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
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

import org.junit.Test;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rdf.core.impl.sesame.SimpleValueFactory;
import org.matonto.rdf.orm.conversion.AbstractValueConverter;
import org.matonto.rdf.orm.conversion.ValueConverter;

import junit.framework.TestCase;

public abstract class ValueConverterTestCase<X> {

	protected ValueConverter<X> valueConverter;

	protected Class<X> type;

	protected static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

	public ValueConverterTestCase(ValueConverter<X> converter, Class<X> type) {
		this(converter, type, valueFactory);
	}

	public ValueConverterTestCase(ValueConverter<X> converter, Class<X> type, ValueFactory valueFactory) {
		if (converter instanceof AbstractValueConverter<?>) {
			((AbstractValueConverter<?>) converter).setValueFactory(valueFactory);
		}
		this.valueConverter = converter;
		this.type = type;
	}

	@Test
	public void testGetType() {
		TestCase.assertEquals(type, valueConverter.getType());
	}

}