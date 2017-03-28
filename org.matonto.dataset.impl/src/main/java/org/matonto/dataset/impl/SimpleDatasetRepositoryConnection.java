package org.matonto.dataset.impl;

/*-
 * #%L
 * org.matonto.dataset.impl
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

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.matonto.dataset.api.DatasetConnection;
import org.matonto.dataset.ontology.dataset.Dataset;
import org.matonto.exception.MatOntoException;
import org.matonto.persistence.utils.Bindings;
import org.matonto.persistence.utils.Statements;
import org.matonto.query.TupleQueryResult;
import org.matonto.query.api.BooleanQuery;
import org.matonto.query.api.GraphQuery;
import org.matonto.query.api.Operation;
import org.matonto.query.api.TupleQuery;
import org.matonto.query.api.Update;
import org.matonto.query.exception.MalformedQueryException;
import org.matonto.rdf.api.IRI;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.Statement;
import org.matonto.rdf.api.Value;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.repository.api.RepositoryConnection;
import org.matonto.repository.base.RepositoryConnectionWrapper;
import org.matonto.repository.base.RepositoryResult;
import org.matonto.repository.exception.RepositoryException;
import org.matonto.sparql.query.Query;
import org.matonto.sparql.query.SparqlBaseListener;
import org.matonto.sparql.query.SparqlParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

public class SimpleDatasetRepositoryConnection extends RepositoryConnectionWrapper implements DatasetConnection {

    private Resource dataset;
    private String repositoryId;
    private ValueFactory valueFactory;

    private static final String GET_GRAPHS_QUERY;
    private static final String GET_NAMED_GRAPHS_QUERY;
    private static final String GET_DEFAULT_NAMED_GRAPHS_QUERY;
    private static final String DATSET_BINDING = "dataset";
    private static final String GRAPH_BINDING = "graph";

    private static final Logger log = Logger.getLogger(SimpleDatasetRepositoryConnection.class);

    static {
        try {
            GET_GRAPHS_QUERY = IOUtils.toString(
                    SimpleDatasetManager.class.getResourceAsStream("/get-graphs.rq"),
                    "UTF-8"
            );
            GET_NAMED_GRAPHS_QUERY = IOUtils.toString(
                    SimpleDatasetManager.class.getResourceAsStream("/get-named-graphs.rq"),
                    "UTF-8"
            );
            GET_DEFAULT_NAMED_GRAPHS_QUERY = IOUtils.toString(
                    SimpleDatasetManager.class.getResourceAsStream("/get-default-named-graphs.rq"),
                    "UTF-8"
            );
        } catch (IOException e) {
            throw new MatOntoException(e);
        }
    }

    public SimpleDatasetRepositoryConnection(RepositoryConnection delegate, Resource dataset, String repositoryId, ValueFactory valueFactory) {
        setDelegate(delegate);
        this.dataset = dataset;
        this.repositoryId = repositoryId;
        this.valueFactory = valueFactory;
    }

    @Override
    public void add(Statement stmt, Resource... contexts) throws RepositoryException {
        addStatement(stmt, Dataset.namedGraph_IRI, contexts);
    }

    @Override
    public void addDefault(Statement stmt, Resource... contexts) throws RepositoryException {
        addStatement(stmt, Dataset.defaultNamedGraph_IRI, contexts);
    }

    @Override
    public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
        addStatements(statements, Dataset.namedGraph_IRI, contexts);
    }

    @Override
    public void addDefault(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
        addStatements(statements, Dataset.defaultNamedGraph_IRI, contexts);
    }

    @Override
    public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
        add(valueFactory.createStatement(subject, predicate, object), contexts);
    }

    @Override
    public void addDefault(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
        addDefault(valueFactory.createStatement(subject, predicate, object), contexts);
    }

    @Override
    public void remove(Statement stmt, Resource... contexts) throws RepositoryException {
        // Start a transaction if not currently in one
        boolean startedTransaction = startTransaction();

        Set<Resource> graphs = new HashSet<>();
        graphs.add(getSystemDefaultNG());
        getGraphs(graphs, Dataset.defaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.namedGraph_IRI);

        if (varargsPresent(contexts)) {
            graphs.retainAll(Arrays.asList(contexts));
            getDelegate().remove(stmt, graphs.toArray(new Resource[graphs.size()]));
        } else {
            removeSingleStatement(stmt, graphs);
        }

        if (startedTransaction) {
            commit();
        }
    }

    @Override
    public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
        // TODO: Trivial Implementation
        // Start a transaction if not currently in one
        boolean startedTransaction = startTransaction();

        Set<Resource> graphs = new HashSet<>();
        graphs.add(getSystemDefaultNG());
        getGraphs(graphs, Dataset.defaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.namedGraph_IRI);

        if (varargsPresent(contexts)) {
            graphs.retainAll(Arrays.asList(contexts));
            getDelegate().remove(statements, graphs.toArray(new Resource[graphs.size()]));
        } else {
            statements.forEach(stmt -> removeSingleStatement(stmt, graphs));
        }

        if (startedTransaction) {
            commit();
        }
    }

    @Override
    public void remove(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
        remove(valueFactory.createStatement(subject, predicate, object), contexts);
    }

    @Override
    public void clear(Resource... contexts) throws RepositoryException {
        if (varargsPresent(contexts)) {
            for (Resource context : contexts) {
                if (context.equals(getSystemDefaultNamedGraph())) {
                    getDelegate().clear(getSystemDefaultNamedGraph());
                } else {
                    deleteDatasetGraph(context);
                }
            }
        } else {
            getDelegate().clear(getSystemDefaultNamedGraph());
            deleteDatasetGraph(null);
        }
    }

    @Override
    public long size(Resource... contexts) throws RepositoryException {
        // TODO: Would this be more efficient with a sparql query? I probably wouldn't need a value factory.
        // TODO: Trivial Implementation
        Set<Resource> graphs = new HashSet<>();
        graphs.add(getSystemDefaultNG());
        getGraphs(graphs, Dataset.defaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.namedGraph_IRI);

        if (varargsPresent(contexts)) {
            graphs.retainAll(Arrays.asList(contexts));
        }

        if (graphs.size() == 0) {
            return 0;
        }
        return getDelegate().size(graphs.toArray(new Resource[graphs.size()]));
    }

    @Override
    public RepositoryResult<Resource> getNamedGraphs() {
        TupleQuery query = getDelegate().prepareTupleQuery(GET_NAMED_GRAPHS_QUERY);
        query.setBinding(DATSET_BINDING, getDataset());
        TupleQueryResult result = query.evaluate();

        return new DatasetGraphResultWrapper(result);
    }

    @Override
    public RepositoryResult<Resource> getDefaultNamedGraphs() {
        TupleQuery query = getDelegate().prepareTupleQuery(GET_DEFAULT_NAMED_GRAPHS_QUERY);
        query.setBinding(DATSET_BINDING, getDataset());
        TupleQueryResult result = query.evaluate();

        return new DatasetGraphResultWrapper(result);
    }

    @Override
    public Resource getSystemDefaultNamedGraph() {
        return getSystemDefaultNG();
    }

    @Override
    public void addNamedGraph(Resource graph) {
        getDelegate().add(getDataset(), valueFactory.createIRI(Dataset.namedGraph_IRI), graph, getDataset());
    }

    @Override
    public void addDefaultNamedGraph(Resource graph) {
        getDelegate().add(getDataset(), valueFactory.createIRI(Dataset.defaultNamedGraph_IRI), graph, getDataset());
    }

    @Override
    public void removeGraph(Resource graph) {
        getDelegate().remove(getDataset(), valueFactory.createIRI(Dataset.namedGraph_IRI), graph, getDataset());
        getDelegate().remove(getDataset(), valueFactory.createIRI(Dataset.defaultNamedGraph_IRI), graph, getDataset());
    }

    @Override
    public RepositoryResult<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
        // TODO: Trivial Implementation
        // Maybe I can wrap a query result like in the getContextIDs impl
        Set<Resource> graphs = new HashSet<>();
        graphs.add(getSystemDefaultNG());
        getGraphs(graphs, Dataset.defaultNamedGraph_IRI);
        getGraphs(graphs, Dataset.namedGraph_IRI);

        if (varargsPresent(contexts)) {
            graphs.retainAll(Arrays.asList(contexts));
        }

        return getDelegate().getStatements(subject, predicate, object, graphs.toArray(new Resource[graphs.size()]));
    }

    @Override
    public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
        TupleQuery query = getDelegate().prepareTupleQuery(GET_GRAPHS_QUERY);
        query.setBinding(DATSET_BINDING, getDataset());
        TupleQueryResult result = query.evaluate();

        return new DatasetGraphResultWrapper(result);
    }

    @Override
    public Operation prepareQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Operation prepareQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public TupleQuery prepareTupleQuery(String query) throws RepositoryException, MalformedQueryException {
        SparqlParser parser = Query.getParser(query);
        SparqlParser.QueryContext queryContext = parser.query();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(parser.getTokenStream());
        ParseTreeWalker walker = new ParseTreeWalker();
        DatasetListener listener = new DatasetListener(rewriter);
        walker.walk(listener, queryContext);

        String processedQuery = rewriter.getText();
        log.debug("Dataset Processed Query: \n" + processedQuery);

        return getDelegate().prepareTupleQuery(processedQuery);
    }

    @Override
    public TupleQuery prepareTupleQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public GraphQuery prepareGraphQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public GraphQuery prepareGraphQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public BooleanQuery prepareBooleanQuery(String query) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public BooleanQuery prepareBooleanQuery(String query, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Update prepareUpdate(String update, String baseURI) throws RepositoryException, MalformedQueryException {
        throw new NotImplementedException("Not yet implemented.");
    }

    @Override
    public Resource getDataset() {
        return dataset;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    private boolean varargsPresent(Object[] varargs) {
        return !(varargs == null || varargs.length == 0 || varargs[0] == null);
    }

    private void getGraphs(Set<Resource> graphs, String predString) {
        getDelegate().getStatements(getDataset(), valueFactory.createIRI(predString), null)
                .forEach(stmt -> Statements.objectResource(stmt).ifPresent(graphs::add));
    }

    private Resource getSystemDefaultNG() {
        RepositoryResult<Statement> statements = getDelegate()
                .getStatements(dataset, valueFactory.createIRI(Dataset.systemDefaultNamedGraph_IRI), null);

        if (statements.hasNext()) {
            return Statements.objectResource(statements.next())
                    .orElseThrow(() -> new MatOntoException("Could not retrieve systemDefaultNamedGraph for dataset"));
        } else {
            throw new MatOntoException("Could not retrieve systemDefaultNamedGraph for dataset");
        }
    }

    /**
     * Adds a statement to the dataset, optionally to one or more named contexts. Ensures that the provided dataset
     * graph statement is created. Any statement added without a context (or supplied context) will be added to the
     * system default named graph for that dataset.
     *
     * @param statement The statement to add.
     * @param predicate The String representing the predicate to use for the graph registration in the dataset.
     * @param contexts The contexts to add the statement to. Note that this parameter is a vararg and as such
     *                 is optional. If no contexts are specified, the statement is added to any context specified
     *                 in each statement, or if the statement contains no context, it is added to the system default
     *                 named graph for that dataset. If one or more contexts are specified, the statement is added to
     *                 these contexts, ignoring any context information in the statement itself.
     */
    private void addStatement(Statement statement, String predicate, Resource... contexts) {
        // Start a transaction if not currently in one
        boolean startedTransaction = startTransaction();

        if (varargsPresent(contexts)) {
            getDelegate().add(statement, contexts);
            addGraphStatements(predicate, contexts);
        } else {
            addSingleStatement(statement, predicate);
        }

        if (startedTransaction) {
            commit();
        }
    }

    /**
     * Adds the statements to the dataset, optionally to one or more named contexts. Ensures that the provided dataset
     * graph statements are created. Any statement added without a context (or supplied context) will be added to the
     * system default named graph for that dataset.
     *
     * @param statements The statements to add.
     * @param predicate The String representing the predicate to use for the graph registration in the dataset.
     * @param contexts The contexts to add the statements to. Note that this parameter is a vararg and as such
     *                 is optional. If no contexts are specified, the statements are added to any context specified
     *                 in each statement, or if the statement contains no context, it is added to the system default
     *                 named graph for that dataset. If one or more contexts are specified, the statements are added to
     *                 these contexts, ignoring any context information in the statements themselves.
     */
    private void addStatements(Iterable<? extends Statement> statements, String predicate, Resource... contexts) {
        // Start a transaction if not currently in one
        boolean startedTransaction = startTransaction();

        if (varargsPresent(contexts)) {
            getDelegate().add(statements, contexts);
            addGraphStatements(predicate, contexts);
        } else {
            statements.forEach(stmt -> addSingleStatement(stmt, predicate));
        }

        if (startedTransaction) {
            commit();
        }
    }

    /**
     * Adds a statement to the dataset. If the statement has a context, then add it to that graph and add that graph to
     * the dataset using the provided predicate; otherwise, add the data to the system default named graph.
     *
     * @param statement The Statement to add to the dataset.
     * @param predicate The String representing the predicate to use to add the graph if it has a context.
     */
    private void addSingleStatement(Statement statement, String predicate) {
        if (statement.getContext().isPresent()) {
            getDelegate().add(statement);
            addGraphStatements(predicate, statement.getContext().get());
        } else {
            getDelegate().add(statement, getSystemDefaultNG());
        }
    }

    /**
     * Removes a statement from the dataset if the statement is in an accessible graph. If the statement has a context,
     * then remove it from that graph; otherwise, remove it from the system default named graph.
     *
     * @param statement The Statement to remove from the dataset.
     * @param accessibleGraphs The set of Resources that represent graphs from which the data can be removed.
     */
    private void removeSingleStatement(Statement statement, Set<Resource> accessibleGraphs) {
        if (statement.getContext().isPresent() && accessibleGraphs.contains(statement.getContext().get())) {
            getDelegate().remove(statement);
        } else {
            getDelegate().remove(statement, getSystemDefaultNG());
        }
    }

    /**
     * Adds a set of graph statements to the dataset.
     *
     * @param predicate The predicate to use for the graph statements.
     * @param contexts The graph identifiers.
     */
    private void addGraphStatements(String predicate, Resource... contexts) {
        for (Resource context : contexts) {
            getDelegate().add(dataset, valueFactory.createIRI(predicate), context, dataset);
        }
    }

    /**
     * Starts a transaction for this DatasetConnection if one is not already active.
     *
     * @return True is a transaction was started. False otherwise.
     */
    private boolean startTransaction() {
        if (!isActive()) {
            begin();
            return true;
        }
        return false;
    }

    /**
     * Deletes a graph from the repository and removes the named graph and default named graph predicates from this
     * dataset.
     *
     * @param graph The graph to delete and remove from the dataset.
     */
    private void deleteDatasetGraph(@Nullable Resource graph) {
        IRI ngPred = valueFactory.createIRI(Dataset.namedGraph_IRI);
        IRI dngPred = valueFactory.createIRI(Dataset.defaultNamedGraph_IRI);

        // TODO: This would be much more efficient with a sparql query

        getDelegate().getStatements(getDataset(), ngPred, graph, getDataset()).forEach(stmt ->
                Statements.objectResource(stmt).ifPresent(context -> {
                    getDelegate().remove(getDataset(), ngPred, context, getDataset());
                    getDelegate().clear(context);
                })
        );
        getDelegate().getStatements(getDataset(), dngPred, graph, getDataset()).forEach(stmt ->
                Statements.objectResource(stmt).ifPresent(context -> {
                    getDelegate().remove(getDataset(), dngPred, context, getDataset());
                    getDelegate().clear(context);
                })
        );
    }

    private static class DatasetGraphResultWrapper extends RepositoryResult<Resource> {

        private TupleQueryResult queryResult;

        DatasetGraphResultWrapper(TupleQueryResult queryResult) {
            this.queryResult = queryResult;
        }

        @Override
        public void close() {
            queryResult.close();
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = queryResult.hasNext();
            if (!hasNext) {
                close();
            }
            return hasNext;
        }

        @Override
        public Resource next() {
            return Bindings.requiredResource(queryResult.next(), GRAPH_BINDING);
        }
    }

    private class DatasetListener extends SparqlBaseListener {
        private TokenStreamRewriter rewriter;
        private String datasetClause;

        DatasetListener(TokenStreamRewriter rewriter) {
            this.rewriter = rewriter;
        }

        @Override
        public void enterDatasetClause(SparqlParser.DatasetClauseContext ctx) {
            rewriter.delete(ctx.getStart(), ctx.getStop());
        }

        @Override
        public void enterWhereClause(SparqlParser.WhereClauseContext ctx) {
            rewriter.insertBefore(ctx.getStart(), getDatasetClause());
        }

        private String getDatasetClause() {
            if (datasetClause == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("FROM <");
                stringBuilder.append(getSystemDefaultNamedGraph().stringValue());
                stringBuilder.append(">");
                getNamedGraphs().forEach(resource -> {
                    stringBuilder.append("FROM NAMED <");
                    stringBuilder.append(resource.stringValue());
                    stringBuilder.append(">");
                });
                getDefaultNamedGraphs().forEach(resource -> {
                    stringBuilder.append("FROM <");
                    stringBuilder.append(resource.stringValue());
                    stringBuilder.append(">");
                });
                this.datasetClause = stringBuilder.toString();
            }
            return datasetClause;
        }
    }
}
