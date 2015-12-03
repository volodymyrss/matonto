package org.matonto.ontology.core.api.axiom;

import org.matonto.ontology.core.api.classexpression.ClassExpression;
import org.matonto.ontology.core.api.propertyexpression.ObjectPropertyExpression;

public interface ObjectPropertyRangeAxiom extends ObjectPropertyAxiom {
	
	public ObjectPropertyExpression getObjectProperty();
	
	public ClassExpression getRange();

}
