package org.matonto.explorable.dataset.rest.impl;

/*-
 * #%L
 * org.matonto.dataset.rest
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2017 iNovex Information Systems, Inc.
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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import net.sf.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.matonto.catalog.api.CatalogManager;
import org.matonto.dataset.api.DatasetConnection;
import org.matonto.dataset.api.DatasetManager;
import org.matonto.dataset.ontology.dataset.DatasetRecord;
import org.matonto.exception.MatOntoException;
import org.matonto.explorable.dataset.rest.ExplorableDatasetRest;
import org.matonto.ontologies.dcterms._Thing;
import org.matonto.query.TupleQueryResult;
import org.matonto.query.api.BindingSet;
import org.matonto.query.api.TupleQuery;
import org.matonto.rdf.api.BNode;
import org.matonto.rdf.api.IRI;
import org.matonto.rdf.api.Model;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.Statement;
import org.matonto.rdf.api.Value;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rest.util.ErrorUtils;
import org.openrdf.model.vocabulary.RDFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Component(immediate = true)
public class ExplorableDatasetRestImpl implements ExplorableDatasetRest {


    private DatasetManager datasetManager;
    private CatalogManager catalogManager;
    private ValueFactory factory;

    private static final String GET_CLASSES_TYPES;
    private static final String GET_CLASSES_DETAILS;
    private static final String GET_CLASSES_INSTANCES;

    static {
        try {
            GET_CLASSES_TYPES = IOUtils.toString(
                    ExplorableDatasetRestImpl.class.getResourceAsStream("/get-classes-types.rq"),
                    "UTF-8"
            );
        } catch (IOException e) {
            throw new MatOntoException(e);
        }
        try {
            GET_CLASSES_DETAILS = IOUtils.toString(
                    ExplorableDatasetRestImpl.class.getResourceAsStream("/get-classes-details.rq"),
                    "UTF-8"
            );
        } catch (IOException e) {
            throw new MatOntoException(e);
        }
        try {
            GET_CLASSES_INSTANCES = IOUtils.toString(
                    ExplorableDatasetRestImpl.class.getResourceAsStream("/get-classes-instances.rq"),
                    "UTF-8"
            );
        } catch (IOException e) {
            throw new MatOntoException(e);
        }
    }

    @Reference
    public void setDatasetManager(DatasetManager datasetManager) {
        this.datasetManager = datasetManager;
    }

    @Reference
    public void setFactory(ValueFactory factory) {
        this.factory = factory;
    }

    @Reference
    public void setCatalogManager(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    @Override
    public Response getClassDetails(String recordIRI, int offset, int limit, String sort, boolean asc, String filter) {
        Resource datasetRecordRsr = factory.createIRI(recordIRI);
        DatasetRecord record = datasetManager.getDatasetRecord(datasetRecordRsr).orElseThrow(() ->
                ErrorUtils.sendError("The dataset record could not be found.", Response.Status.BAD_REQUEST));
        List<Map<String, Object>> listToJson = new ArrayList<>();
        Map<String, Map<String, Object>> classesFromQuery = new HashMap<>();
        Map<String, String> classesFromQueryList = new HashMap<>();

        if (!recordIRI.isEmpty()) {

            List<Resource> classesList = new ArrayList<>();
            DatasetConnection dsConn = datasetManager.getConnection(datasetRecordRsr);

            try {
                TupleQuery tq = dsConn.prepareTupleQuery(GET_CLASSES_TYPES);
                TupleQueryResult results = tq.evaluate();

                Optional<DatasetRecord> datasetRecordOpt = datasetManager.getDatasetRecord(datasetRecordRsr);
                Model result = datasetRecordOpt.get().getModel();

                results.forEach(bindingSet -> {
                    Map<String, Object> classMap = new HashMap<>();
                    Optional<Value> classType = bindingSet.getValue("type");
                    Optional<Value> classCount = bindingSet.getValue("c");

                    if (classType.isPresent()) {
                        classMap.put("classType", classType.get().stringValue());
                        classMap.put("instancesCount", classCount.get().stringValue());

                        Value classIRI = classType.get();

                        TupleQuery tqEx = dsConn.prepareTupleQuery(GET_CLASSES_DETAILS);
                        tqEx.setBinding("classIRI", classIRI);
                        TupleQueryResult examplesResults = tqEx.evaluate();
                        List<String> exList = new ArrayList<>();

                        examplesResults.forEach(bindingSetEx -> {
                            String lblStr = (bindingSetEx.getValue("label").isPresent() ? bindingSetEx.getValue("label").get().stringValue() : "");
                            String titleStr = (bindingSetEx.getValue("title").isPresent() ? bindingSetEx.getValue("title").get().stringValue() : "");

                            if (!lblStr.isEmpty()) {
                                exList.add(lblStr);
                            } else {
                                if (!titleStr.isEmpty()) {
                                    exList.add(titleStr);
                                } else {
                                    IRI exampleIRI = factory.createIRI(bindingSetEx.getValue("example").get().stringValue());
                                    exList.add(splitCamelCase(exampleIRI.getLocalName()));
                                }
                            }

                        });
                        classMap.put("classExamples", exList);

                        classesList.add(factory.createIRI(classType.get().stringValue()));
                        classesFromQuery.put(classType.get().stringValue(), classMap);
                        classesFromQueryList.put(classType.get().stringValue(), classType.get().stringValue());
                    }
                });

                Set<Value> ontologies = record.getOntology();
                ontologies.forEach(ontBlkNode -> {
                    BNode blankNode = factory.createBNode(ontBlkNode.stringValue());
                    Optional<Statement> commitStmt = result.filter(blankNode,
                            factory.createIRI(DatasetRecord.linksToCommit_IRI), null).stream().findFirst();
                    Optional<Statement> ontologyRecordStmt = result.filter(blankNode,
                            factory.createIRI(DatasetRecord.linksToRecord_IRI), null).stream().findFirst();
                    Optional<Model> compiledResource = catalogManager.getCompiledResource(factory.createIRI(commitStmt
                            .get().getObject().stringValue()));

                    if (compiledResource.isPresent()) {
                        for (Iterator<Map.Entry<String, String>> it = classesFromQueryList.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<String, String> entry = it.next();
                            Map<String, Object> mapToJson = new HashMap<>();
                            Resource classRsrc = factory.createIRI(entry.getValue());
                            Model classModel = compiledResource.get().filter(classRsrc, null, null);
                            if (classModel.size() > 0) {
                                it.remove();

                                mapToJson.put("ontologyRecordTitle", findLabelToDisplay(compiledResource.get(),
                                        factory.createIRI(ontologyRecordStmt.get().getObject().stringValue())));
                                mapToJson.put("classIRI", entry.getValue());
                                mapToJson.put("classTitle", findLabelToDisplay(classModel,
                                        factory.createIRI(classRsrc.stringValue())));
                                mapToJson.put("classDescription", findDescriptionToDisplay(classModel,
                                        factory.createIRI(entry.getValue())));
                                mapToJson.put("instancesCount", Integer.parseInt(classesFromQuery.get(entry.getValue())
                                        .get("instancesCount").toString()));
                                mapToJson.put("classExamples", classesFromQuery.get(entry.getValue())
                                        .get("classExamples"));

                                listToJson.add(mapToJson);
                            }
                        }
                    }
                });
            } catch (MatOntoException e) {
                throw ErrorUtils.sendError(e, e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        JSONArray array = JSONArray.fromObject(listToJson);
        Response.ResponseBuilder response = Response.ok(array).header("X-Total-Count", listToJson.size());
        return response.build();
    }

    @Override
    public Response getInstanceDetails(String recordIRI, String classIRI, int offset, int limit, String sort, boolean asc, int numExamples) {

        List<Map<String, Object>> listToJson = new ArrayList<>();

        if (!recordIRI.isEmpty() && !classIRI.isEmpty()) {

            Resource datasetRecordRsr = factory.createIRI(recordIRI);
            DatasetConnection dsConn = datasetManager.getConnection(datasetRecordRsr);

            try {
                TupleQuery tqEx = dsConn.prepareTupleQuery(GET_CLASSES_INSTANCES);
                Value classIRIVal = factory.createIRI(classIRI);
                tqEx.setBinding("classIRI", classIRIVal);
                TupleQueryResult results = tqEx.evaluate();
                results.forEach(instance -> {

                    Map<String, Object> mapToJson = new HashMap<>();

                    String instanceIRI = (instance.getValue("inst").isPresent() ? instance.getValue("inst").get().stringValue() : "");
                    String title = (instance.getValue("title").isPresent() ? instance.getValue("title").get().stringValue() : "");
                    String label = (instance.getValue("label").isPresent() ? instance.getValue("label").get().stringValue() : "");
                    String comment = (instance.getValue("comment").isPresent() ? instance.getValue("comment").get().stringValue() : "");
                    String description = (instance.getValue("description").isPresent() ? instance.getValue("description").get().stringValue() : "");

                    String desc;
                    // Instance description will be returned using rdfs:comment or dc:title or empty string
                    if (!comment.isEmpty()) {
                        desc = comment;
                    } else {
                        desc = description;
                    }

                    String lbl;
                    // Instance title will be returned using rdfs:label or dc:title or human readable version of uri local name
                    if (!label.isEmpty()) {
                        lbl = label;
                    } else {
                        if (!title.isEmpty()) {
                            lbl = title;
                        } else {
                            IRI instIRI = factory.createIRI(instanceIRI);
                            lbl = splitCamelCase(instIRI.getLocalName());
                        }
                    }

                    mapToJson.put("instanceIRI", instanceIRI);
                    mapToJson.put("title", lbl);
                    mapToJson.put("description", desc);

                    listToJson.add(mapToJson);
                });
            } catch (MatOntoException e) {
                throw ErrorUtils.sendError(e, e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        JSONArray array = JSONArray.fromObject(listToJson);
        Response.ResponseBuilder response = Response.ok(array).header("X-Total-Count", listToJson.size());
        return response.build();
    }

    /**
     * Split camel case string using zero-length matching regex with lookbehind and lookforward to find where to insert
     * spaces.
     *
     * @param string Camel case string to split
     * @return human readable string
     */
    private String splitCamelCase(String string) {
        return string.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " " );
    }

    /**
     * Retrieve entity label/title inside a given model in the following order
     * rdfs:label, dcterms:title, or beautified version of local name.
     *
     * @param model Model with pertinent data
     * @param iri Entity to find label/title of
     * @return entity label
     */
    private String findLabelToDisplay(Model model, IRI iri) {
        return findPropertyToDisplay(model, iri, RDFS.LABEL.stringValue(), _Thing.title_IRI);
    }

    /**
     * Retrieve entity description inside a given model in the following order
     * rdfs:comment, dcterms:description, or default empty string.
     *
     * @param model Model with pertinent data
     * @param iri Entity to find label/title of
     * @return entity label
     */
    private String findDescriptionToDisplay(Model model, IRI iri) {
        return findPropertyToDisplay(model, iri, RDFS.COMMENT.stringValue(), _Thing.description_IRI);
    }

    private String findPropertyToDisplay(Model model, IRI iri, String rdfsProp, String dcProp) {
        if (model.size() > 0) {
            Optional<Statement> rdfs = model.filter(null, factory.createIRI(rdfsProp), null).stream().findFirst();

            if (rdfs.isPresent()) {
                return rdfs.get().getObject().stringValue();
            } else {
                Optional<Statement> dcterm = model.filter(null, factory.createIRI(dcProp), null).stream().findFirst();
                if (dcterm.isPresent()) {
                    return dcterm.get().getObject().stringValue();
                } else {
                    return splitCamelCase(iri.stringValue());
                }
            }
        } else {
            return "";
        }
    }
}