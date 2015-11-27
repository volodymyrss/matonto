package org.matonto.ontology.core.impl.owlapi;

import java.util.Set;

import org.matonto.ontology.core.api.Annotation;
import org.matonto.ontology.core.api.DataPropertyExpression;
import org.matonto.ontology.core.api.DataPropertyRangeAxiom;
import org.matonto.ontology.core.api.DataRange;

import com.google.common.base.Preconditions;


public class SimpleDataPropertyRangeAxiom 
	extends SimpleAxiom 
	implements DataPropertyRangeAxiom {

	
	public DataPropertyExpression property;
	public DataRange range;
	
	
	public SimpleDataPropertyRangeAxiom(DataPropertyExpression property, DataRange range, Set<Annotation> annotations) 
	{
		super(annotations);
		this.property = Preconditions.checkNotNull(property, "property cannot be null");
		this.range = Preconditions.checkNotNull(range, "range cannot be null");
	}

	
	@Override
	public DataPropertyRangeAxiom getAxiomWithoutAnnotations() 
	{
		if(!isAnnotated())
			return this;
		
		return new SimpleDataPropertyRangeAxiom(property, range, NO_ANNOTATIONS);	
	}
	

	@Override
	public DataPropertyRangeAxiom getAnnotatedAxiom(Set<Annotation> annotations) 
	{
		return new SimpleDataPropertyRangeAxiom(property, range, mergeAnnos(annotations));
	}

	
	@Override
	public SimpleAxiomType getAxiomType() 
	{
		return SimpleAxiomType.DATA_PROPERTY_RANGE;
	}

	
	@Override
	public DataPropertyExpression getDataProperty() 
	{
		return property;
	}

	
	@Override
	public DataRange getRange() 
	{
		return range;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
		    return true;
		
		if (!super.equals(obj)) 
			return false;
		
		if (obj instanceof DataPropertyRangeAxiom) {
			DataPropertyRangeAxiom other = (DataPropertyRangeAxiom)obj;			 
			return ((property.equals(other.getDataProperty())) && (range.equals(other.getRange())));
		}
		
		return false;
	}

}
