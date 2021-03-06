package com.mobi.ontology.core.impl.owlapi.axiom;

/*-
 * #%L
 * com.mobi.ontology.core.impl.owlapi
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

import com.mobi.ontology.core.api.Annotation;
import com.mobi.ontology.core.api.classexpression.ClassExpression;
import com.mobi.ontology.core.api.types.AxiomType;
import com.mobi.ontology.core.api.Individual;
import com.mobi.ontology.core.api.axiom.ClassAssertionAxiom;

import java.util.Set;
import javax.annotation.Nonnull;


public class SimpleClassAssertionAxiom 
	extends SimpleAxiom 
	implements ClassAssertionAxiom {

	
	private Individual individual;
	private ClassExpression expression;
	
	
	public SimpleClassAssertionAxiom(@Nonnull Individual individual, @Nonnull ClassExpression expression, Set<Annotation> annotations)
	{
		super(annotations);
		this.individual = individual;
		this.expression = expression;
	}

	
	@Override
	public ClassAssertionAxiom getAxiomWithoutAnnotations() 
	{
		if(!isAnnotated())
			return this;
		
		return new SimpleClassAssertionAxiom(individual, expression, NO_ANNOTATIONS);	
	}

	
	@Override
	public ClassAssertionAxiom getAnnotatedAxiom(@Nonnull Set<Annotation> annotations) 
	{
		return new SimpleClassAssertionAxiom(individual, expression, mergeAnnos(annotations));
	}

	
	@Override
	public AxiomType getAxiomType()
	{
		return AxiomType.CLASS_ASSERTION;
	}

	
	@Override
	public Individual getIndividual() 
	{
		return individual;
	}

	
	@Override
	public ClassExpression getClassExpression() 
	{
		return expression;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
		    return true;
		
		if (!super.equals(obj)) 
			return false;
		
		if (obj instanceof ClassAssertionAxiom) {
			ClassAssertionAxiom other = (ClassAssertionAxiom)obj;			 
			return ((individual.equals(other.getIndividual())) && (expression.equals(other.getClassExpression())));
		}
		
		return false;
	}

}
