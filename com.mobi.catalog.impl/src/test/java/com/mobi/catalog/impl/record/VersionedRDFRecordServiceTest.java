package com.mobi.catalog.impl.record;

/*-
 * #%L
 * com.mobi.catalog.impl
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mobi.catalog.api.CatalogProvUtils;
import com.mobi.catalog.api.CatalogUtilsService;
import com.mobi.catalog.api.builder.Difference;
import com.mobi.catalog.api.mergerequest.MergeRequestManager;
import com.mobi.catalog.api.ontologies.mcat.Branch;
import com.mobi.catalog.api.ontologies.mcat.Catalog;
import com.mobi.catalog.api.ontologies.mcat.Commit;
import com.mobi.catalog.api.ontologies.mcat.Distribution;
import com.mobi.catalog.api.ontologies.mcat.Record;
import com.mobi.catalog.api.ontologies.mcat.Tag;
import com.mobi.catalog.api.ontologies.mcat.VersionedRDFRecord;
import com.mobi.catalog.api.ontologies.mcat.VersionedRDFRecordFactory;
import com.mobi.catalog.api.record.config.OperationConfig;
import com.mobi.catalog.api.record.config.RecordExportSettings;
import com.mobi.catalog.api.record.config.RecordOperationConfig;
import com.mobi.catalog.api.record.config.VersionedRDFRecordExportSettings;
import com.mobi.jaas.api.ontologies.usermanagement.User;
import com.mobi.ontologies.dcterms._Thing;
import com.mobi.persistence.utils.BatchExporter;
import com.mobi.persistence.utils.impl.SimpleSesameTransformer;
import com.mobi.prov.api.ontologies.mobiprov.DeleteActivity;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.core.utils.Values;
import com.mobi.rdf.orm.OrmFactory;
import com.mobi.rdf.orm.test.OrmEnabledTestCase;
import com.mobi.repository.api.RepositoryConnection;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BufferedGroupingRDFHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionedRDFRecordServiceTest extends OrmEnabledTestCase {

    private final IRI testIRI = VALUE_FACTORY.createIRI("urn:test");
    private final IRI catalogId = VALUE_FACTORY.createIRI("http://mobi.com/test/catalogs#catalog-test");
    private final IRI branchIRI = VALUE_FACTORY.createIRI("http://mobi.com/test/branches#branch");
    private final IRI commitIRI = VALUE_FACTORY.createIRI("http://mobi.com/test/commits#commit");
    private final IRI tagIRI = VALUE_FACTORY.createIRI("http://mobi.com/test/versions#tag");
    private final IRI distributionIRI = VALUE_FACTORY.createIRI("http://mobi.com/test/distributions#distribution");
    private final IRI masterBranchIRI = VALUE_FACTORY.createIRI("http://mobi.com/test/branches#master");

    private SimpleVersionedRDFRecordService recordService;
    private SimpleSesameTransformer transformer;
    private VersionedRDFRecord testRecord;
    private Branch branch;
    private Commit headCommit;
    private Difference difference;
    private User user;
    private DeleteActivity deleteActivity;
    private Tag tag;

    private OrmFactory<VersionedRDFRecord> versionedRDFRecordFactory = getRequiredOrmFactory(VersionedRDFRecord.class);
    private OrmFactory<Catalog> catalogFactory = getRequiredOrmFactory(Catalog.class);
    private OrmFactory<User> userFactory = getRequiredOrmFactory(User.class);
    private OrmFactory<DeleteActivity> deleteActivityFactory = getRequiredOrmFactory(DeleteActivity.class);
    private OrmFactory<Branch> branchFactory = getRequiredOrmFactory(Branch.class);
    private OrmFactory<Commit> commitFactory = getRequiredOrmFactory(Commit.class);
    private OrmFactory<Tag> tagFactory = getRequiredOrmFactory(Tag.class);
    private OrmFactory<Distribution> distributionFactory = getRequiredOrmFactory(Distribution.class);

    @Mock
    private CatalogUtilsService utilsService;

    @Mock
    private VersionedRDFRecordFactory recordFactory;

    @Mock
    private RepositoryConnection connection;

    @Mock
    private CatalogProvUtils provUtils;

    @Mock
    private MergeRequestManager mergeRequestManager;

    @Before
    public void setUp() throws Exception {

        recordService = new SimpleVersionedRDFRecordService();
        transformer = new SimpleSesameTransformer();
        deleteActivity = deleteActivityFactory.createNew(VALUE_FACTORY.createIRI("http://test.org/activity/delete"));

        user = userFactory.createNew(VALUE_FACTORY.createIRI("http://test.org/user"));
        headCommit = commitFactory.createNew(commitIRI);
        branch = branchFactory.createNew(branchIRI);
        branch.setHead(headCommit);
        branch.setProperty(VALUE_FACTORY.createLiteral("Test Record"), VALUE_FACTORY.createIRI(_Thing.title_IRI));

        Model deletions = MODEL_FACTORY.createModel();
        deletions.add(VALUE_FACTORY.createIRI("http://test.com#sub"), VALUE_FACTORY.createIRI(_Thing.description_IRI),
                VALUE_FACTORY.createLiteral("Description"));

        difference = new Difference.Builder()
                .additions(MODEL_FACTORY.createModel())
                .deletions(deletions)
                .build();

        tag = tagFactory.createNew(tagIRI);
        tag.setVersionedDistribution(Collections.singleton(distributionFactory.createNew(distributionIRI)));

        testRecord = versionedRDFRecordFactory.createNew(testIRI);
        testRecord.setProperty(VALUE_FACTORY.createLiteral("Test Record"), VALUE_FACTORY.createIRI(_Thing.title_IRI));
        testRecord.setCatalog(catalogFactory.createNew(catalogId));
        testRecord.setBranch(Collections.singleton(branch));
        testRecord.setVersion(Collections.singleton(tag));
        testRecord.setLatestVersion(tag);
        testRecord.setBranch(Collections.singleton(branch));
        testRecord.setMasterBranch(branchFactory.createNew(masterBranchIRI));


        MockitoAnnotations.initMocks(this);
        when(utilsService.optObject(any(IRI.class), any(OrmFactory.class), eq(connection))).thenReturn(Optional.of(testRecord));
        when(utilsService.getBranch(eq(testRecord), eq(branchIRI), any(OrmFactory.class), eq(connection))).thenReturn(branch);
        when(utilsService.getHeadCommitIRI(eq(branch))).thenReturn(commitIRI);
        doReturn(Stream.of(commitIRI).collect(Collectors.toList()))
                .when(utilsService).getCommitChain(eq(commitIRI), eq(false), any(RepositoryConnection.class));
        when(utilsService.getExpectedObject(eq(commitIRI), any(OrmFactory.class), eq(connection))).thenReturn(headCommit);
        when(utilsService.getRevisionChanges(eq(commitIRI), eq(connection))).thenReturn(difference);
        when(provUtils.startDeleteActivity(any(User.class), any(IRI.class))).thenReturn(deleteActivity);
        doNothing().when(mergeRequestManager).deleteMergeRequestsWithRecordId(eq(testIRI), any(RepositoryConnection.class));

        injectOrmFactoryReferencesIntoService(recordService);
        recordService.setRecordFactory(recordFactory);
        recordService.setUtilsService(utilsService);
        recordService.setVf(VALUE_FACTORY);
        recordService.setProvUtils(provUtils);
        recordService.setMergeRequestManager(mergeRequestManager);
    }

    /* delete() */

    @Test
    public void deleteTest() throws Exception {
        when(recordFactory.getExisting(eq(testIRI), any(Model.class))).thenReturn(Optional.of(testRecord));

        VersionedRDFRecord deletedRecord = recordService.delete(testIRI, user, connection);

        assertEquals(testRecord, deletedRecord);
        verify(utilsService).optObject(eq(testIRI), eq(recordFactory), eq(connection));
        verify(provUtils).startDeleteActivity(eq(user), eq(testIRI));
        verify(recordFactory).getExisting(eq(testIRI), eq(testRecord.getModel()));
        verify(mergeRequestManager).deleteMergeRequestsWithRecordId(eq(testIRI), any(RepositoryConnection.class));
        verify(utilsService).removeVersion(eq(testRecord.getResource()), any(Resource.class), any(RepositoryConnection.class));
        verify(utilsService).removeBranch(eq(testRecord.getResource()), any(Resource.class), any(List.class), any(RepositoryConnection.class));
        verify(provUtils).endDeleteActivity(any(DeleteActivity.class), any(Record.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteRecordDoesNotExistTest() throws Exception {
        when(utilsService.optObject(eq(testIRI), eq(recordFactory), eq(connection))).thenReturn(Optional.empty());

        recordService.delete(testIRI, user, connection);
        verify(utilsService).optObject(eq(testIRI), eq(recordFactory), eq(connection));
    }

    /* export() */

    @Test
    public void exportUsingBatchExporterTest() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BatchExporter exporter =  new BatchExporter(transformer, new BufferedGroupingRDFHandler(Rio.createWriter(RDFFormat.JSONLD, os)));
        RecordOperationConfig config = new OperationConfig();

        config.set(RecordExportSettings.BATCH_EXPORTER, exporter);

        assertFalse(exporter.isActive());
        exporter.startRDF();
        assertTrue(exporter.isActive());
        recordService.export(testIRI, config, connection);
        exporter.endRDF();
        assertFalse(exporter.isActive());

        Model outputModel = Values.mobiModel(Rio.parse((IOUtils.toInputStream(os.toString())), "", RDFFormat.JSONLD));
        assertTrue(outputModel.containsAll(testRecord.getModel()));
        assertTrue(outputModel.containsAll(branch.getModel()));
        assertTrue(outputModel.containsAll(difference.getAdditions()));
        assertTrue(outputModel.containsAll(difference.getDeletions()));

        verify(utilsService).optObject(eq(testIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getBranch(eq(testRecord), eq(branchIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getHeadCommitIRI(eq(branch));
        verify(utilsService).getCommitChain(eq(commitIRI), eq(false), any(RepositoryConnection.class));
        verify(utilsService).getExpectedObject(eq(commitIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getRevisionChanges(eq(commitIRI), eq(connection));
    }

    @Test
    public void exportRecordOnlyTest() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BatchExporter exporter =  new BatchExporter(transformer, new BufferedGroupingRDFHandler(Rio.createWriter(RDFFormat.JSONLD, os)));
        RecordOperationConfig config = new OperationConfig();

        config.set(RecordExportSettings.BATCH_EXPORTER, exporter);
        config.set(VersionedRDFRecordExportSettings.WRITE_VERSIONED_DATA, false);

        assertFalse(exporter.isActive());
        exporter.startRDF();
        assertTrue(exporter.isActive());
        recordService.export(testIRI, config, connection);
        exporter.endRDF();
        assertFalse(exporter.isActive());

        Model outputModel = Values.mobiModel(Rio.parse((IOUtils.toInputStream(os.toString())), "", RDFFormat.JSONLD));
        assertTrue(outputModel.containsAll(testRecord.getModel()));
        assertFalse(outputModel.containsAll(branch.getModel()));
        assertFalse(outputModel.containsAll(difference.getDeletions()));

        verify(utilsService).optObject(eq(testIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService, never()).getBranch(eq(testRecord), eq(branchIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService, never()).getHeadCommitIRI(eq(branch));
        verify(utilsService, never()).getCommitChain(eq(commitIRI), eq(false), any(RepositoryConnection.class));
        verify(utilsService, never()).getExpectedObject(eq(commitIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService, never()).getRevisionChanges(eq(commitIRI), eq(connection));
    }

    @Test
    public void exportSpecificBranch() throws Exception {
        Branch doNotWriteBranch = branchFactory.createNew(VALUE_FACTORY.createIRI("http://mobi.com/test/branches#branch2"));
        doNotWriteBranch.setHead(headCommit);
        doNotWriteBranch.setProperty(VALUE_FACTORY.createLiteral("Test Record"), VALUE_FACTORY.createIRI(_Thing.title_IRI));
        testRecord.addBranch(doNotWriteBranch);

        Set<Resource> branchesToExport = new HashSet<>();
        branchesToExport.add(branchIRI);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BatchExporter exporter =  new BatchExporter(transformer, new BufferedGroupingRDFHandler(Rio.createWriter(RDFFormat.JSONLD, os)));
        RecordOperationConfig config = new OperationConfig();

        config.set(RecordExportSettings.BATCH_EXPORTER, exporter);
        config.set(VersionedRDFRecordExportSettings.BRANCHES_TO_EXPORT, branchesToExport);

        assertFalse(exporter.isActive());
        exporter.startRDF();
        assertTrue(exporter.isActive());
        recordService.export(testIRI, config, connection);
        exporter.endRDF();
        assertFalse(exporter.isActive());

        Model outputModel = Values.mobiModel(Rio.parse((IOUtils.toInputStream(os.toString())), "", RDFFormat.JSONLD));
        testRecord.removeBranch(doNotWriteBranch);
        assertTrue(outputModel.containsAll(testRecord.getModel()));
        assertTrue(outputModel.containsAll(branch.getModel()));
        assertFalse(outputModel.containsAll(doNotWriteBranch.getModel()));
        assertTrue(outputModel.containsAll(difference.getDeletions()));

        verify(utilsService).optObject(eq(testIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getBranch(eq(testRecord), eq(branchIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getHeadCommitIRI(eq(branch));
        verify(utilsService).getCommitChain(eq(commitIRI), eq(false), any(RepositoryConnection.class));
        verify(utilsService).getExpectedObject(eq(commitIRI), any(OrmFactory.class), eq(connection));
        verify(utilsService).getRevisionChanges(eq(commitIRI), eq(connection));
    }

    @Test (expected = IllegalArgumentException.class)
    public void exportNullBatchExporterTest() throws Exception {
        BatchExporter exporter =  null;
        RecordOperationConfig config = new OperationConfig();

        config.set(RecordExportSettings.BATCH_EXPORTER, exporter);
        recordService.export(testIRI, config, connection);
    }

    @Test
    public void getTypeIRITest() throws Exception {
        assertEquals(VersionedRDFRecord.TYPE, recordService.getTypeIRI());
    }
}
