package com.mobi.ontology.core.api;

/*-
 * #%L
 * com.mobi.ontology.api
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

import com.mobi.ontology.core.api.propertyexpression.AnnotationProperty;
import com.mobi.ontology.core.api.propertyexpression.DataProperty;
import com.mobi.ontology.core.api.propertyexpression.ObjectProperty;
import com.mobi.ontology.core.utils.MobiOntologyException;
import com.mobi.ontology.core.api.axiom.Axiom;
import com.mobi.ontology.core.api.classexpression.CardinalityRestriction;
import com.mobi.ontology.core.api.classexpression.OClass;
import com.mobi.ontology.core.api.datarange.Datatype;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.Resource;

import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;

public interface Ontology {

    Model asModel(ModelFactory factory) throws MobiOntologyException;

    OutputStream asTurtle() throws MobiOntologyException;

    OutputStream asRdfXml() throws MobiOntologyException;

    OutputStream asOwlXml() throws MobiOntologyException;

    /**
     * Returns the Ontology as JSON-LD in an OutputStream.
     *
     * @param skolemize Whether or not blank node ids should be skolemized before rendering
     * @return an OutputStream of JSON-LD
     * @throws MobiOntologyException If an error occurs while parsing
     */
    OutputStream asJsonLD(boolean skolemize) throws MobiOntologyException;

    /**
     * Returns the OntologyID that describes the Ontology IRI, Version IRI,
     * and Ontology identifier. Note: If the serialized ontology contains an
     * Ontology IRI or Version IRI, it must match the Ontology and Version IRIs in
     * this OntologyId object.
     *
     * @return the OntologyID that describes the Ontology IRI, Version IRI,
     * and Ontology identifier
     */
    OntologyId getOntologyId();

    /**
     * Returns the set of IRIs of unloadable imported ontologies.  The set is accumulated during loading the
     * ontology from an ontology document or an ontology IRI.
     *
     * @return set of IRIs
     */
    Set<IRI> getUnloadableImportIRIs();

    /**
     * Gets the set of loaded ontologies that this ontology is related to via the directlyImports relation.
     *
     * @return set of ontologies
     */
    Set<Ontology> getDirectImports();

    /**
     * Gets the set of loaded ontologies that this ontology is related to via the reflexive transitive closure
     * of the directlyImports relation as defined in Section 3.4 of the OWL 2 Structural Specification.
     *
     * <p>Note: The import closure of an ontology O is a set containing O and all the ontologies that O imports.
     * The import closure of O SHOULD NOT contain ontologies O1 and O2 such that O1 and O2
     * are different ontology versions
     * from the same ontology series, or O1 contains an ontology annotation owl:incompatibleWith with
     * the value equal to either
     * the ontology IRI or the version IRI of O2.</p>
     *
     * @return set of ontologies
     */
    Set<Ontology> getImportsClosure();

    /**
     * Gets the set of IRIs for directly imported ontologies of this ontology.
     *
     * @return set of ontology IRIs
     */
    Set<IRI> getImportedOntologyIRIs();

    /**
     * Gets the ontology annotations, excluding annotations for other objects such as classes and entities.
     *
     * @return ontology annotations
     */
    Set<Annotation> getOntologyAnnotations();

    /**
     * Gets all the annotations in the ontology, excluding ontology annotations, annotations for other objects such
     * as classes and entities.
     *
     * @return ontology annotations
     */
    Set<Annotation> getAllAnnotations();

    /**
     * Gets all the annotation properties defined in the ontology.
     *
     * @return ontology annotation properties
     */
    Set<AnnotationProperty> getAllAnnotationProperties();

    /**
     * Determines if the class iri is found in the ontology.
     *
     * @param iri the IRI of the class
     * @return true if the class is in the ontology; otherwise, false
     */
    boolean containsClass(IRI iri);

    Set<OClass> getAllClasses();

    /**
     * Attempts to get all of the object properties (including imports) that can be set on the class with the specified
     * IRI in the ontology.
     *
     * @param iri the IRI of the class
     * @return a Set of all class object properties
     */
    Set<ObjectProperty> getAllClassObjectProperties(IRI iri);

    /**
     * Attempts to get all of the object properties that have no domain set.
     *
     * @return a Set of all object properties without a domain
     */
    Set<ObjectProperty> getAllNoDomainObjectProperties();

    /**
     * Attempts to get all of the data properties (including imports) that can be set on the class with the specified
     * IRI in the ontology.
     *
     * @param iri the IRI of the class
     * @return a Set of all class data properties
     */
    Set<DataProperty> getAllClassDataProperties(IRI iri);

    /**
     * Attempts to get all of the data properties that have no domain set.
     *
     * @return a Set of all data properties without a domain
     */
    Set<DataProperty> getAllNoDomainDataProperties();

    Set<Axiom> getAxioms();

    Set<Datatype> getAllDatatypes();

    Set<ObjectProperty> getAllObjectProperties();

    /**
     * Attempts to get a specific object property in the ontology by its IRI.
     *
     * @param iri the IRI of an object property
     * @return an Optional with the object property if found
     */
    Optional<ObjectProperty> getObjectProperty(IRI iri);

    /**
     * Retrieves a Set of Resources corresponding to the range of the passed object property within the ontology.
     * Set will be empty if the object property cannot be found in the ontology, has no ranges set, or if none of the
     * ranges can be represented as a Resource.
     *
     * @param objectProperty an object property from the ontology
     * @return a Set of Resources representing all the range values of the object property
     */
    Set<Resource> getObjectPropertyRange(ObjectProperty objectProperty);

    Set<DataProperty> getAllDataProperties();

    /**
     * Attempts to get a specific data property in the ontology by its IRI.
     *
     * @param iri the IRI of a data property
     * @return an Optional with the data property if found
     */
    Optional<DataProperty> getDataProperty(IRI iri);

    /**
     * Retrieves a Set of Resources corresponding to the range of the passed data property within the ontology. Set will
     * be empty if the data property cannot be found in the ontology, has no ranges set, or if none of the ranges can be
     * represented as a Resource.
     *
     * @param dataProperty a data property from the ontology
     * @return a {@link Set} of {@link Resource}s representing all the range values of the data property
     */
    Set<Resource> getDataPropertyRange(DataProperty dataProperty);

    /**
     * Retrieves a {@link Set} of all Individuals which includes NamedIndividuals and AnonymousIndividuals.
     *
     * @return a {@link Set} of all {@link Individual}s in the {@link Ontology}
     */
    Set<Individual> getAllIndividuals();

    /**
     * Retrieves a {@link Set} of all NamedIndividuals.
     *
     * @return a {@link Set} of all {@link NamedIndividual}s in the {@link Ontology}
     */
    Set<NamedIndividual> getAllNamedIndividuals();

    /**
     * Searches for all individuals of a particular class or any sub-classes of the provided class.
     *
     * @param classIRI The {@link IRI} of the class of individuals to find.
     * @return The {@link Set} of {@link Individual}s.
     */
    Set<Individual> getIndividualsOfType(IRI classIRI);

    /**
     * Searches for all individuals of a particular class or any sub-classes of the provided class.
     *
     * @param clazz The {@link OClass} of individuals to find.
     * @return The {@link Set} of {@link Individual}s.
     */
    Set<Individual> getIndividualsOfType(OClass clazz);

    /**
     * Searches for all cardinality properties associated with a particular class.
     *
     * @param classIRI The {@link IRI} of the class.
     * @return The {@link Set} of {@link CardinalityRestriction}s.
     */
    Set<CardinalityRestriction> getCardinalityProperties(IRI classIRI);

    /**
     * Compares two SimpleOntology objects by their resource ids (ontologyId) and RDF model of the ontology objects,
     * and returns true if the resource ids are equal and their RDF models are isomorphic.
     *
     * <p>Two models are considered isomorphic if for each of the graphs in one model, an isomorphic graph exists in the
     * other model, and the context identifiers of these graphs are either identical or (in the case of blank nodes)
     * map 1:1 on each other.  RDF graphs are isomorphic graphs if statements from one graphs can be mapped 1:1 on to
     * statements in the other graphs. In this mapping, blank nodes are not considered mapped when having an identical
     * internal id, but are mapped from one graph to the other by looking at the statements in which the blank nodes
     * occur.</p>
     *
     * <p>Note: Depending on the size of the models, this can be an expensive operation.</p>
     *
     * @return true if the resource ids are equal and their RDF models are isomorphic.
     */
    boolean equals(Object obj);

    int hashCode();
}
