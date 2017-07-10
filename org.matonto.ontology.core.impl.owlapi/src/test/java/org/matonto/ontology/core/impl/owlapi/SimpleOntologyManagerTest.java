package org.matonto.ontology.core.impl.owlapi;

/*-
 * #%L
 * org.matonto.ontology.core.impl.owlapi
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matonto.cache.api.CacheManager;
import org.matonto.catalog.api.CatalogManager;
import org.matonto.catalog.api.builder.RecordConfig;
import org.matonto.catalog.api.ontologies.mcat.Branch;
import org.matonto.catalog.api.ontologies.mcat.BranchFactory;
import org.matonto.catalog.api.ontologies.mcat.Catalog;
import org.matonto.catalog.api.ontologies.mcat.CatalogFactory;
import org.matonto.catalog.api.ontologies.mcat.Commit;
import org.matonto.catalog.api.ontologies.mcat.CommitFactory;
import org.matonto.ontology.core.api.Ontology;
import org.matonto.ontology.core.api.builder.OntologyRecordConfig;
import org.matonto.ontology.core.api.ontologies.ontologyeditor.OntologyRecord;
import org.matonto.ontology.core.api.ontologies.ontologyeditor.OntologyRecordFactory;
import org.matonto.ontology.utils.api.SesameTransformer;
import org.matonto.ontology.utils.cache.OntologyCache;
import org.matonto.persistence.utils.Bindings;
import org.matonto.query.TupleQueryResult;
import org.matonto.query.api.Binding;
import org.matonto.rdf.api.IRI;
import org.matonto.rdf.api.Model;
import org.matonto.rdf.api.ModelFactory;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rdf.core.impl.sesame.LinkedHashModelFactory;
import org.matonto.rdf.core.impl.sesame.SimpleValueFactory;
import org.matonto.rdf.core.utils.Values;
import org.matonto.rdf.orm.conversion.ValueConverterRegistry;
import org.matonto.rdf.orm.conversion.impl.DefaultValueConverterRegistry;
import org.matonto.rdf.orm.conversion.impl.DoubleValueConverter;
import org.matonto.rdf.orm.conversion.impl.FloatValueConverter;
import org.matonto.rdf.orm.conversion.impl.IRIValueConverter;
import org.matonto.rdf.orm.conversion.impl.IntegerValueConverter;
import org.matonto.rdf.orm.conversion.impl.LiteralValueConverter;
import org.matonto.rdf.orm.conversion.impl.ResourceValueConverter;
import org.matonto.rdf.orm.conversion.impl.ShortValueConverter;
import org.matonto.rdf.orm.conversion.impl.StringValueConverter;
import org.matonto.rdf.orm.conversion.impl.ValueValueConverter;
import org.matonto.repository.api.Repository;
import org.matonto.repository.api.RepositoryConnection;
import org.matonto.repository.api.RepositoryManager;
import org.matonto.repository.impl.core.SimpleRepositoryManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.cache.Cache;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SimpleOntologyValues.class)
public class SimpleOntologyManagerTest {

    @Mock
    private CatalogManager catalogManager;

    @Mock
    private SesameTransformer sesameTransformer;

    @Mock
    private Ontology ontology;

    @Mock
    private Ontology vocabulary;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache<String, Ontology> mockCache;

    @Mock
    private RepositoryManager mockRepoManager;

    private SimpleOntologyManager manager;
    private ValueFactory valueFactory = SimpleValueFactory.getInstance();
    private ModelFactory modelFactory = LinkedHashModelFactory.getInstance();
    private OntologyRecordFactory ontologyRecordFactory = new OntologyRecordFactory();
    private CommitFactory commitFactory = new CommitFactory();
    private BranchFactory branchFactory = new BranchFactory();
    private CatalogFactory catalogFactory = new CatalogFactory();
    private ValueConverterRegistry vcr = new DefaultValueConverterRegistry();
    private IRI missingIRI;
    private IRI recordIRI;
    private IRI branchIRI;
    private IRI commitIRI;
    private IRI catalogIRI;
    private IRI ontologyIRI;
    private IRI versionIRI;
    private OntologyRecord record;
    private org.semanticweb.owlapi.model.IRI owlOntologyIRI;
    private org.semanticweb.owlapi.model.IRI owlVersionIRI;
    private RepositoryManager repoManager = new SimpleRepositoryManager();
    private Repository repo;

    @Before
    public void setUp() throws Exception {
        missingIRI = valueFactory.createIRI("http://matonto.org/missing");
        recordIRI = valueFactory.createIRI("http://matonto.org/record");
        branchIRI = valueFactory.createIRI("http://matonto.org/branch");
        commitIRI = valueFactory.createIRI("http://matonto.org/commit");
        catalogIRI = valueFactory.createIRI("http://matonto.org/catalog");
        ontologyIRI = valueFactory.createIRI("http://matonto.org/ontology");
        versionIRI = valueFactory.createIRI("http://matonto.org/ontology/1.0");
        owlOntologyIRI = org.semanticweb.owlapi.model.IRI.create("http://matonto.org/ontology");
        owlVersionIRI = org.semanticweb.owlapi.model.IRI.create("http://matonto.org/ontology/1.0");

        ontologyRecordFactory.setModelFactory(modelFactory);
        ontologyRecordFactory.setValueFactory(valueFactory);
        ontologyRecordFactory.setValueConverterRegistry(vcr);

        commitFactory.setModelFactory(modelFactory);
        commitFactory.setValueFactory(valueFactory);
        commitFactory.setValueConverterRegistry(vcr);

        branchFactory.setModelFactory(modelFactory);
        branchFactory.setValueFactory(valueFactory);
        branchFactory.setValueConverterRegistry(vcr);

        catalogFactory.setModelFactory(modelFactory);
        catalogFactory.setValueFactory(valueFactory);
        catalogFactory.setValueConverterRegistry(vcr);

        vcr.registerValueConverter(ontologyRecordFactory);
        vcr.registerValueConverter(commitFactory);
        vcr.registerValueConverter(branchFactory);
        vcr.registerValueConverter(catalogFactory);
        vcr.registerValueConverter(new ResourceValueConverter());
        vcr.registerValueConverter(new IRIValueConverter());
        vcr.registerValueConverter(new DoubleValueConverter());
        vcr.registerValueConverter(new IntegerValueConverter());
        vcr.registerValueConverter(new FloatValueConverter());
        vcr.registerValueConverter(new ShortValueConverter());
        vcr.registerValueConverter(new StringValueConverter());
        vcr.registerValueConverter(new ValueValueConverter());
        vcr.registerValueConverter(new LiteralValueConverter());

        record = ontologyRecordFactory.createNew(recordIRI);
        MockitoAnnotations.initMocks(this);

        Catalog catalog = catalogFactory.createNew(catalogIRI);
        when(catalogManager.getLocalCatalogIRI()).thenReturn(catalogIRI);
        when(catalogManager.getLocalCatalog()).thenReturn(catalog);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(ontologyRecordFactory))).thenReturn(record);
        doThrow(new IllegalArgumentException()).when(catalogManager).getMasterBranch(catalogIRI, missingIRI);
        doThrow(new IllegalArgumentException()).when(catalogManager).getBranch(catalogIRI, recordIRI, missingIRI, branchFactory);
        doThrow(new IllegalArgumentException()).when(catalogManager).getCommit(catalogIRI, recordIRI, branchIRI, missingIRI);

        when(sesameTransformer.sesameModel(any(Model.class))).thenReturn(new org.openrdf.model.impl.LinkedHashModel());

        InputStream testOntology = getClass().getResourceAsStream("/test-ontology.ttl");
        when(ontology.asModel(modelFactory)).thenReturn(Values.matontoModel(Rio.parse(testOntology, "",
                RDFFormat.TURTLE)));

        InputStream testVocabulary = getClass().getResourceAsStream("/test-vocabulary.ttl");
        when(vocabulary.asModel(modelFactory)).thenReturn(Values.matontoModel(Rio.parse(testVocabulary, "",
                RDFFormat.TURTLE)));

        PowerMockito.mockStatic(SimpleOntologyValues.class);
        when(SimpleOntologyValues.owlapiIRI(ontologyIRI)).thenReturn(owlOntologyIRI);
        when(SimpleOntologyValues.owlapiIRI(versionIRI)).thenReturn(owlVersionIRI);
        when(SimpleOntologyValues.matontoIRI(owlOntologyIRI)).thenReturn(ontologyIRI);
        when(SimpleOntologyValues.matontoIRI(owlVersionIRI)).thenReturn(versionIRI);
        when(SimpleOntologyValues.matontoOntology(any(OWLOntology.class))).thenReturn(ontology);

        when(mockCache.containsKey(anyString())).thenReturn(false);

        when(cacheManager.getCache(anyString(), eq(String.class), eq(Ontology.class))).thenReturn(Optional.of(mockCache));

        repo = repoManager.createMemoryRepository();
        repo.initialize();
        try (RepositoryConnection conn = repo.getConnection()) {
            InputStream testData = getClass().getResourceAsStream("/testCatalogData.trig");
            conn.add(Values.matontoModel(Rio.parse(testData, "", RDFFormat.TRIG)));
        }
        when(mockRepoManager.createMemoryRepository()).thenReturn(repoManager.createMemoryRepository());
        when(mockRepoManager.getRepository("system")).thenReturn(Optional.of(repo));

        manager = spy(new SimpleOntologyManager());
        manager.setValueFactory(valueFactory);
        manager.setModelFactory(modelFactory);
        manager.setSesameTransformer(sesameTransformer);
        manager.setCatalogManager(catalogManager);
        manager.setOntologyRecordFactory(ontologyRecordFactory);
        manager.setBranchFactory(branchFactory);
        manager.setRepositoryManager(mockRepoManager);
        manager.setCacheManager(cacheManager);
    }

    @After
    public void tearDown() throws Exception {
        repo.shutDown();
    }

    @Test
    public void testCreateOntologyRecordWithOntologyIRI() throws Exception {
        IRI ontologyIRI = valueFactory.createIRI("http://test.com/ontology");
        OntologyRecordConfig config = new OntologyRecordConfig.OntologyRecordBuilder("title", Collections.emptySet())
                .ontologyIRI(ontologyIRI).build();

        OntologyRecord result = manager.createOntologyRecord(config);
        assertTrue(result.getOntologyIRI().isPresent());
        assertEquals(ontologyIRI, result.getOntologyIRI().get());
    }

    @Test
    public void testCreateOntologyRecordWithoutOntologyIRI() throws Exception {
        OntologyRecordConfig config = new OntologyRecordConfig.OntologyRecordBuilder("title", Collections.emptySet())
                .build();

        OntologyRecord record = manager.createOntologyRecord(config);
        assertFalse(record.getOntologyIRI().isPresent());
    }

    @Test
    public void testCreateOntology() throws Exception {
        Ontology result = manager.createOntology(modelFactory.createModel());
        assertEquals(ontology, result);
    }

    // Testing retrieveOntologyByIRI

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyByIRIWithMissingRepo() {
        // Setup:
        doReturn(Optional.empty()).when(mockRepoManager).getRepository(anyString());

        manager.retrieveOntologyByIRI(ontologyIRI);
    }

    @Test
    public void testRetrieveOntologyByIRIThatDoesNotExist() {
        Optional<Ontology> result = manager.retrieveOntologyByIRI(missingIRI);
        assertFalse(result.isPresent());
    }

    @Test
    public void testRetrieveOntologyByIRIWithCacheMiss() {
        // Setup
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());

        Optional<Ontology> result = manager.retrieveOntologyByIRI(ontologyIRI);
        assertTrue(result.isPresent());
        assertEquals(ontology, result.get());
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).put(eq(key), eq(result.get()));
    }

    @Test
    public void testRetrieveOntologyByIRIWithCacheHit() {
        // Setup
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());
        when(mockCache.containsKey(key)).thenReturn(true);
        when(mockCache.get(key)).thenReturn(ontology);

        Optional<Ontology> result = manager.retrieveOntologyByIRI(ontologyIRI);
        assertTrue(result.isPresent());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).get(eq(key));
        verify(mockCache, times(0)).put(eq(key), eq(result.get()));
    }

    // Testing retrieveOntology(Resource recordId)

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyWithMissingIdentifier() {
        manager.retrieveOntology(missingIRI);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyWithMasterBranchNotSet() {
        // Setup:
        doThrow(new IllegalStateException()).when(catalogManager).getMasterBranch(catalogIRI, recordIRI);

        manager.retrieveOntology(recordIRI);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyWithHeadCommitNotSet() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);

        manager.retrieveOntology(recordIRI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyWhenCompiledResourceCannotBeFound() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);
        doThrow(new IllegalArgumentException()).when(catalogManager).getCompiledResource(commitIRI);

        manager.retrieveOntology(recordIRI);
    }

    @Test
    public void testRetrieveOntologyWithCacheMiss() {
        // Setup
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI);
        assertTrue(optionalOntology.isPresent());
        assertEquals(ontology, optionalOntology.get());
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).put(eq(key), eq(optionalOntology.get()));
    }

    @Test
    public void testRetrieveOntologyWithCacheHit() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        when(catalogManager.getMasterBranch(catalogIRI, recordIRI)).thenReturn(branch);
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());
        when(mockCache.containsKey(key)).thenReturn(true);
        when(mockCache.get(key)).thenReturn(ontology);

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI);
        assertTrue(optionalOntology.isPresent());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).get(eq(key));
        verify(mockCache, times(0)).put(eq(key), eq(optionalOntology.get()));
    }

    // Testing retrieveOntology(Resource recordId, Resource branchId)

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyUsingABranchWithMissingIdentifier() throws Exception {
        manager.retrieveOntology(recordIRI, missingIRI);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyUsingABranchThatCannotBeRetrieved() {
        // Setup:
        doThrow(new IllegalStateException()).when(catalogManager).getBranch(catalogIRI, recordIRI, missingIRI, branchFactory);

        manager.retrieveOntology(recordIRI, missingIRI);
    }

    @Test
    public void testRetrieveOntologyUsingAMissingBranch() {
        // Setup:
        when(catalogManager.getBranch(catalogIRI, recordIRI, branchIRI, branchFactory)).thenReturn(Optional.empty());

        Optional<Ontology> result = manager.retrieveOntology(recordIRI, branchIRI);
        assertFalse(result.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyUsingABranchWithHeadCommitNotSet() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        when(catalogManager.getBranch(catalogIRI, recordIRI, branchIRI, branchFactory)).thenReturn(Optional.of(branch));

        manager.retrieveOntology(recordIRI, branchIRI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyUsingABranchWhenCompiledResourceCannotBeFound() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        when(catalogManager.getBranch(catalogIRI, recordIRI, branchIRI, branchFactory)).thenReturn(Optional.of(branch));
        doThrow(new IllegalArgumentException()).when(catalogManager).getCompiledResource(commitIRI);

        manager.retrieveOntology(recordIRI, branchIRI);
    }

    @Test
    public void testRetrieveOntologyUsingABranchCacheMiss() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        when(catalogManager.getBranch(catalogIRI, recordIRI, branchIRI, branchFactory)).thenReturn(Optional.of(branch));
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI, branchIRI);
        assertTrue(optionalOntology.isPresent());
        assertEquals(ontology, optionalOntology.get());
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).put(eq(key), eq(optionalOntology.get()));
    }

    @Test
    public void testRetrieveOntologyUsingABranchCacheHit() {
        // Setup:
        Branch branch = branchFactory.createNew(branchIRI);
        branch.setHead(commitFactory.createNew(commitIRI));
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        when(catalogManager.getBranch(catalogIRI, recordIRI, branchIRI, branchFactory)).thenReturn(Optional.of(branch));
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());
        when(mockCache.containsKey(key)).thenReturn(true);
        when(mockCache.get(key)).thenReturn(ontology);

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI, branchIRI);
        assertTrue(optionalOntology.isPresent());
        verify(mockCache).containsKey(eq(key));
        verify(mockCache).get(eq(key));
        verify(mockCache, times(0)).put(eq(key), eq(optionalOntology.get()));
    }

    // Testing retrieveOntology(Resource recordId, Resource branchId, Resource commitId)

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyUsingACommitWithMissingIdentifier() throws Exception {
        manager.retrieveOntology(recordIRI, branchIRI, missingIRI);
    }

    @Test(expected = IllegalStateException.class)
    public void testRetrieveOntologyUsingACommitThatCannotBeRetrieved() {
        // Setup:
        doThrow(new IllegalStateException()).when(catalogManager).getCommit(catalogIRI,recordIRI, branchIRI, missingIRI);

        manager.retrieveOntology(recordIRI, branchIRI, missingIRI);
    }

    @Test
    public void testRetrieveOntologyUsingAMissingCommit() {
        // Setup:
        when(catalogManager.getCommit(catalogIRI,recordIRI, branchIRI, commitIRI)).thenReturn(Optional.empty());

        Optional<Ontology> result = manager.retrieveOntology(recordIRI, branchIRI, commitIRI);
        assertFalse(result.isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRetrieveOntologyUsingACommitWhenCompiledResourceCannotBeFound() {
        // Setup:
        Commit commit = commitFactory.createNew(commitIRI);
        when(catalogManager.getCommit(catalogIRI, recordIRI, branchIRI, commitIRI)).thenReturn(Optional.of(commit));
        doThrow(new IllegalArgumentException()).when(catalogManager).getCompiledResource(commitIRI);

        manager.retrieveOntology(recordIRI, branchIRI, commitIRI);
    }

    @Test
    public void testRetrieveOntologyUsingACommitCacheMiss() {
        // Setup:
        Commit commit = commitFactory.createNew(commitIRI);
        when(catalogManager.getCommit(catalogIRI, recordIRI, branchIRI, commitIRI)).thenReturn(Optional.of(commit));
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI, branchIRI, commitIRI);
        assertTrue(optionalOntology.isPresent());
        assertEquals(ontology, optionalOntology.get());
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        verify(mockCache, times(2)).containsKey(eq(key));
        verify(mockCache).put(eq(key), eq(optionalOntology.get()));
    }

    @Test
    public void testRetrieveOntologyUsingACommitCacheHit() {
        // Setup:
        Commit commit = commitFactory.createNew(commitIRI);
        String key = OntologyCache.generateKey(recordIRI.stringValue(), branchIRI.stringValue(), commitIRI.stringValue());
        when(catalogManager.getCommit(catalogIRI, recordIRI, branchIRI, commitIRI)).thenReturn(Optional.of(commit));
        when(catalogManager.getCompiledResource(commitIRI)).thenReturn(modelFactory.createModel());
        when(mockCache.containsKey(key)).thenReturn(true);
        when(mockCache.get(key)).thenReturn(ontology);

        Optional<Ontology> optionalOntology = manager.retrieveOntology(recordIRI, branchIRI, commitIRI);
        assertTrue(optionalOntology.isPresent());
        verify(mockCache).get(eq(key));
        verify(mockCache, times(0)).put(eq(key), eq(optionalOntology.get()));
    }

    // Testing deleteOntology(Resource recordId)

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteOntologyRecordWithMissingIdentifier() {
        doThrow(new IllegalArgumentException()).when(catalogManager).removeRecord(catalogIRI, missingIRI);

        manager.deleteOntology(missingIRI);
    }

    @Test
    public void testDeleteOntology() throws Exception {
        // Setup:
        doNothing().when(mockCache).forEach(any());

        manager.deleteOntology(recordIRI);
        verify(catalogManager).removeRecord(catalogIRI, recordIRI);
        verify(mockCache).forEach(any());
    }

    /* Testing deleteOntologyBranch(Resource recordId, Resource branchId) */

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteOntologyBranchWithMissingIdentifier() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeBranch(catalogIRI, recordIRI, missingIRI);

        manager.deleteOntologyBranch(recordIRI, missingIRI);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteOntologyBranchWithInvalidCommit() {
        // Setup:
        doThrow(new IllegalStateException()).when(catalogManager).removeBranch(catalogIRI, recordIRI, missingIRI);

        manager.deleteOntologyBranch(recordIRI, missingIRI);
    }

    @Test
    public void testDeleteOntologyBranch() throws Exception {
        // Setup:
        doNothing().when(mockCache).forEach(any());

        manager.deleteOntologyBranch(recordIRI, branchIRI);
        verify(catalogManager).removeBranch(catalogIRI, recordIRI, branchIRI);
        verify(mockCache).forEach(any());
    }

    /* Testing getSubClassesOf(Ontology ontology) */

    @Test
    public void testGetSubClassesOf() throws Exception {
        Set<String> parents = Stream.of("http://matonto.org/ontology#Class2a", "http://matonto.org/ontology#Class2b",
                "http://matonto.org/ontology#Class1b", "http://matonto.org/ontology#Class1c",
                "http://matonto.org/ontology#Class1a").collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("http://matonto.org/ontology#Class1b", "http://matonto.org/ontology#Class1c");
        children.put("http://matonto.org/ontology#Class1a", "http://matonto.org/ontology#Class1b");
        children.put("http://matonto.org/ontology#Class2a", "http://matonto.org/ontology#Class2b");

        TupleQueryResult result = manager.getSubClassesOf(ontology);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetSubDatatypePropertiesOf() throws Exception {
        Set<String> parents = Stream.of("http://matonto.org/ontology#dataProperty1b",
                "http://matonto.org/ontology#dataProperty1a").collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("http://matonto.org/ontology#dataProperty1a", "http://matonto.org/ontology#dataProperty1b");

        TupleQueryResult result = manager.getSubDatatypePropertiesOf(ontology);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetSubAnnotationPropertiesOf() throws Exception {
        Set<String> parents = Stream.of("http://matonto.org/ontology#annotationProperty1b",
                "http://matonto.org/ontology#annotationProperty1a", "http://purl.org/dc/terms/title")
                .collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("http://matonto.org/ontology#annotationProperty1a",
                "http://matonto.org/ontology#annotationProperty1b");

        TupleQueryResult result = manager.getSubAnnotationPropertiesOf(ontology);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetSubObjectPropertiesOf() throws Exception {
        Set<String> parents = Stream.of("http://matonto.org/ontology#objectProperty1b",
                "http://matonto.org/ontology#objectProperty1a").collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("http://matonto.org/ontology#objectProperty1a", "http://matonto.org/ontology#objectProperty1b");

        TupleQueryResult result = manager.getSubObjectPropertiesOf(ontology);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetClassesWithIndividuals() throws Exception {
        Set<String> parents = Stream.of("http://matonto.org/ontology#Class2a", "http://matonto.org/ontology#Class2b",
                "http://matonto.org/ontology#Class1b", "http://matonto.org/ontology#Class1c",
                "http://matonto.org/ontology#Class1a").collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("http://matonto.org/ontology#Class1a", "http://matonto.org/ontology#Individual1a");
        children.put("http://matonto.org/ontology#Class1b", "http://matonto.org/ontology#Individual1b");
        children.put("http://matonto.org/ontology#Class1c", "http://matonto.org/ontology#Individual1c");
        children.put("http://matonto.org/ontology#Class2a", "http://matonto.org/ontology#Individual2a");
        children.put("http://matonto.org/ontology#Class2b", "http://matonto.org/ontology#Individual2b");
        TupleQueryResult result = manager.getClassesWithIndividuals(ontology);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("individual");
            if (child.isPresent()) {
                String lclChild = children.get(parent);
                String individual = child.get().getValue().stringValue();
                  assertEquals(lclChild, individual);
                  children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetEntityUsages() throws Exception {
        Set<String> subjects = Stream.of("http://matonto.org/ontology#Class1b",
                "http://matonto.org/ontology#Individual1a").collect(Collectors.toSet());
        Set<String> predicates = Stream.of("http://www.w3.org/2000/01/rdf-schema#subClassOf",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type").collect(Collectors.toSet());

        TupleQueryResult result = manager.getEntityUsages(ontology, valueFactory
                .createIRI("http://matonto.org/ontology#Class1a"));

        assertTrue(result.hasNext());
        result.forEach(b -> {
            Optional<Binding> optionalSubject = b.getBinding("s");
            if (optionalSubject.isPresent()) {
                String subject = optionalSubject.get().getValue().stringValue();
                assertTrue(subjects.contains(subject));
                subjects.remove(subject);
            }
            Optional<Binding> optionalPredicate = b.getBinding("p");
            if (optionalPredicate.isPresent()) {
                String predicate = optionalPredicate.get().getValue().stringValue();
                assertTrue(predicates.contains(predicate));
                predicates.remove(predicate);
            }
        });
        assertEquals(0, subjects.size());
        assertEquals(0, predicates.size());
    }

    @Test
    public void testConstructEntityUsages() throws Exception {
        Resource class1b = valueFactory.createIRI("http://matonto.org/ontology#Class1b");
        IRI subClassOf = valueFactory.createIRI("http://www.w3.org/2000/01/rdf-schema#subClassOf");
        Resource class1a = valueFactory.createIRI("http://matonto.org/ontology#Class1a");
        Resource individual1a = valueFactory.createIRI("http://matonto.org/ontology#Individual1a");
        IRI type = valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Model expected = modelFactory.createModel(Stream.of(valueFactory.createStatement(class1b, subClassOf,
                class1a), valueFactory.createStatement(individual1a, type, class1a)).collect(Collectors.toSet()));

        Model result = manager.constructEntityUsages(ontology, class1a);

        assertTrue(result.equals(expected));
    }

    @Test
    public void testGetConceptRelationships() throws Exception {
        Set<String> parents = Stream.of("https://matonto.org/vocabulary#Concept1",
                "https://matonto.org/vocabulary#Concept2","https://matonto.org/vocabulary#Concept3",
                "https://matonto.org/vocabulary#Concept4")
                .collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("https://matonto.org/vocabulary#Concept1", "https://matonto.org/vocabulary#Concept2");

        TupleQueryResult result = manager.getConceptRelationships(vocabulary);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetConceptSchemeRelationships() throws Exception {
        Set<String> parents = Stream.of("https://matonto.org/vocabulary#ConceptScheme1",
                "https://matonto.org/vocabulary#ConceptScheme2","https://matonto.org/vocabulary#ConceptScheme3")
                .collect(Collectors.toSet());
        Map<String, String> children = new HashMap<>();
        children.put("https://matonto.org/vocabulary#ConceptScheme1", "https://matonto.org/vocabulary#Concept1");
        children.put("https://matonto.org/vocabulary#ConceptScheme2", "https://matonto.org/vocabulary#Concept2");
        children.put("https://matonto.org/vocabulary#ConceptScheme3", "https://matonto.org/vocabulary#Concept3");

        TupleQueryResult result = manager.getConceptSchemeRelationships(vocabulary);

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "parent").stringValue();
            assertTrue(parents.contains(parent));
            parents.remove(parent);
            Optional<Binding> child = b.getBinding("child");
            if (child.isPresent()) {
                assertEquals(children.get(parent), child.get().getValue().stringValue());
                children.remove(parent);
            }
        });
        assertEquals(0, parents.size());
        assertEquals(0, children.size());
    }

    @Test
    public void testGetSearchResults() throws Exception {
        Set<String> entities = Stream.of("http://matonto.org/ontology#Class2a", "http://matonto.org/ontology#Class2b",
                "http://matonto.org/ontology#Class1b", "http://matonto.org/ontology#Class1c",
                "http://matonto.org/ontology#Class1a").collect(Collectors.toSet());

        TupleQueryResult result = manager.getSearchResults(ontology, "class");

        assertTrue(result.hasNext());
        result.forEach(b -> {
            String parent = Bindings.requiredResource(b, "entity").stringValue();
            assertTrue(entities.contains(parent));
            entities.remove(parent);
            assertEquals("http://www.w3.org/2002/07/owl#Class", Bindings.requiredResource(b, "type").stringValue());
        });
        assertEquals(0, entities.size());
    }
}
