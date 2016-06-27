package org.matonto.etl.service.delimited;

import aQute.bnd.annotation.component.*;
import org.matonto.etl.api.delimited.Mapping;
import org.matonto.etl.api.delimited.MappingId;
import org.matonto.etl.api.delimited.MappingManager;
import org.matonto.exception.MatOntoException;
import org.matonto.persistence.utils.Statements;
import org.matonto.rdf.api.*;
import org.matonto.repository.api.Repository;
import org.matonto.repository.api.RepositoryConnection;
import org.matonto.repository.base.RepositoryResult;
import org.matonto.repository.config.RepositoryConsumerConfig;
import org.matonto.repository.exception.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import javax.annotation.Nonnull;

@Component(
        name = SimpleMappingManager.COMPONENT_NAME,
        designateFactory = RepositoryConsumerConfig.class,
        configurationPolicy = ConfigurationPolicy.require)
public class SimpleMappingManager implements MappingManager {
    static final String COMPONENT_NAME = "org.matonto.etl.api.MappingManager";
    private Resource registryContext;
    private Resource registrySubject;
    private IRI registryPredicate;
    private static final Logger logger = LoggerFactory.getLogger(SimpleMappingManager.class);
    private ValueFactory factory;
    private ModelFactory modelFactory;
    private Repository repository;

    public SimpleMappingManager() {}

    @Activate
    public void activate() {
        logger.info("Activating " + COMPONENT_NAME);
        initMappingRegistryResources();
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivating " + COMPONENT_NAME);
    }

    @Modified
    public void modified() {
        logger.info("Modifying the " + COMPONENT_NAME);
        initMappingRegistryResources();
    }

    @Reference
    protected void setValueFactory(final ValueFactory vf) {
        factory = vf;
    }

    @Reference
    protected void setModelFactory(final ModelFactory mf) {
        modelFactory = mf;
    }

    @Reference(name = "repository")
    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public Set<Resource> getMappingRegistry() {
        RepositoryConnection conn = null;
        Set<Resource> registry = new HashSet<>();
        try {
            conn = repository.getConnection();
            conn.getStatements(registrySubject, registryPredicate, null, registryContext)
                    .forEach(statement -> Statements.objectResource(statement).ifPresent(registry::add));
        } catch (RepositoryException e) {
            throw new MatOntoException("Error in repository connection", e);
        } finally {
            closeConnection(conn);
        }

        return registry;
    }

    @Override
    public IRI createMappingIRI() {
        String localName = generateUuid();
        return factory.createIRI(Delimited.MAPPINGS.stringValue() + "/" + localName.trim());
    }

    @Override
    public IRI createMappingIRI(String localName) {
        return factory.createIRI(Delimited.MAPPINGS.stringValue() + "/" + localName.trim());
    }

    @Override
    public String getMappingLocalName(IRI iri) {
        if (iri.getNamespace().equals(Delimited.MAPPINGS.stringValue() + "/")) {
            return iri.getLocalName();
        }
        return iri.getNamespace().replace(Delimited.MAPPINGS.stringValue() + "/", "");
    }

    @Override
    public MappingId createMappingId(Resource id) {
        return new SimpleMappingId.Builder(factory).id(id).build();
    }

    @Override
    public MappingId createMappingId(IRI mappingIRI) {
        return new SimpleMappingId.Builder(factory).mappingIRI(mappingIRI).build();
    }

    @Override
    public MappingId createMappingId(IRI mappingIRI, IRI versionIRI) {
        return new SimpleMappingId.Builder(factory).mappingIRI(mappingIRI).versionIRI(versionIRI).build();
    }

    @Override
    public Mapping createMapping(MappingId id) {
        return new SimpleMapping(id, modelFactory, factory);
    }

    @Override
    public Mapping createMapping(File mapping) throws IOException, MatOntoException {
        return new SimpleMapping(mapping, factory);
    }

    @Override
    public Mapping createMapping(String jsonld) throws IOException, MatOntoException {
        return new SimpleMapping(jsonld, factory);
    }

    @Override
    public Mapping createMapping(InputStream in, RDFFormat format) throws IOException, MatOntoException {
        return new SimpleMapping(in, format, factory);
    }

    @Override
    public boolean storeMapping(@Nonnull Mapping mapping) throws MatOntoException {
        if (mappingExists(mapping.getId().getMappingIdentifier())) {
            throw new MatOntoException("Mapping with mapping ID already exists");
        }

        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            conn.add(mapping.getModel(), mapping.getId().getMappingIdentifier());
            conn.add(registrySubject, registryPredicate, mapping.getId().getMappingIdentifier(), registryContext);
        } catch (RepositoryException e) {
            throw new MatOntoException("Error in repository connection", e);
        } finally {
            closeConnection(conn);
        }

        return true;
    }

    @Override
    public Optional<Mapping> retrieveMapping(@Nonnull Resource mappingId) {
        if (!mappingExists(mappingId)) {
            return Optional.empty();
        }
        RepositoryConnection conn = null;
        Model mappingModel = modelFactory.createModel();
        try {
            conn = repository.getConnection();
            RepositoryResult<Statement> statements = conn.getStatements(null, null, null, mappingId);
            statements.forEach(mappingModel::add);
        } catch (RepositoryException e) {
            throw new MatOntoException("Error in repository connection", e);
        } finally {
            closeConnection(conn);
        }
        return Optional.of(new SimpleMapping(mappingModel, factory));
    }

    @Override
    public boolean deleteMapping(@Nonnull Resource mappingIRI) {
        if (!mappingExists(mappingIRI)) {
            throw new MatOntoException("Mapping with mapping ID does not exist");
        }

        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
            conn.clear(mappingIRI);
            conn.remove(registrySubject, registryPredicate, mappingIRI, registryContext);
        } catch (RepositoryException e) {
            throw new MatOntoException("Error in repository connection", e);
        } finally {
            closeConnection(conn);
        }

        return true;
    }

    @Override
    public boolean mappingExists(@Nonnull Resource mappingIRI) {
        RepositoryConnection conn = null;
        boolean exists = false;
        try {
            conn = repository.getConnection();
            RepositoryResult<Statement> statements = conn.getStatements(registrySubject, registryPredicate,
                    mappingIRI, registryContext);
            if (statements.hasNext()) {
                exists = true;
            }
        } catch (RepositoryException e) {
            throw new MatOntoException("Error in repository connection", e);
        } finally {
            closeConnection(conn);
        }
        return exists;
    }

    /**
     * Generates a UUID.
     * 
     * @return a UUID string
     */
    private String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Closes the passed connection to the repository.
     * 
     * @param conn a connection to the repository for mappings
     */
    private void closeConnection(RepositoryConnection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (RepositoryException e) {
            logger.warn("Could not close Repository." + e.toString());
        }
    }

    /**
     * Initializes resources used to store the mapping registry statements in
     * the repository.
     */
    private void initMappingRegistryResources() {
        registryContext = factory.createIRI("https://matonto.org/registry/mappings");
        registrySubject = factory.createIRI("https://matonto.org/registry/mappings");
        registryPredicate = factory.createIRI("https://matonto.org/registry#hasItem");
    }
}
