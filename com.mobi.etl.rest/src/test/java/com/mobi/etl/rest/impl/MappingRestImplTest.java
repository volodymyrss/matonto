package com.mobi.etl.rest.impl;

/*-
 * #%L
 * com.mobi.etl.rest
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

import static com.mobi.rest.util.RestUtils.encode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.mobi.catalog.api.CatalogManager;
import com.mobi.catalog.api.CatalogProvUtils;
import com.mobi.catalog.api.ontologies.mcat.Branch;
import com.mobi.catalog.api.ontologies.mcat.BranchFactory;
import com.mobi.catalog.api.ontologies.mcat.Record;
import com.mobi.catalog.api.versioning.VersioningManager;
import com.mobi.etl.api.config.delimited.MappingRecordConfig;
import com.mobi.etl.api.delimited.MappingId;
import com.mobi.etl.api.delimited.MappingManager;
import com.mobi.etl.api.delimited.MappingWrapper;
import com.mobi.etl.api.ontologies.delimited.MappingRecord;
import com.mobi.etl.api.ontologies.delimited.MappingRecordFactory;
import com.mobi.jaas.api.engines.EngineManager;
import com.mobi.jaas.api.ontologies.usermanagement.User;
import com.mobi.jaas.api.ontologies.usermanagement.UserFactory;
import com.mobi.persistence.utils.impl.SimpleSesameTransformer;
import com.mobi.prov.api.ontologies.mobiprov.CreateActivity;
import com.mobi.prov.api.ontologies.mobiprov.CreateActivityFactory;
import com.mobi.prov.api.ontologies.mobiprov.DeleteActivity;
import com.mobi.prov.api.ontologies.mobiprov.DeleteActivityFactory;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.rdf.core.impl.sesame.LinkedHashModelFactory;
import com.mobi.rdf.core.impl.sesame.SimpleValueFactory;
import com.mobi.rdf.orm.conversion.ValueConverterRegistry;
import com.mobi.rdf.orm.conversion.impl.DefaultValueConverterRegistry;
import com.mobi.rdf.orm.conversion.impl.DoubleValueConverter;
import com.mobi.rdf.orm.conversion.impl.FloatValueConverter;
import com.mobi.rdf.orm.conversion.impl.IRIValueConverter;
import com.mobi.rdf.orm.conversion.impl.IntegerValueConverter;
import com.mobi.rdf.orm.conversion.impl.LiteralValueConverter;
import com.mobi.rdf.orm.conversion.impl.ResourceValueConverter;
import com.mobi.rdf.orm.conversion.impl.ShortValueConverter;
import com.mobi.rdf.orm.conversion.impl.StringValueConverter;
import com.mobi.rdf.orm.conversion.impl.ValueValueConverter;
import com.mobi.rest.util.MobiRestTestNg;
import com.mobi.rest.util.UsernameTestFilter;
import net.sf.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class MappingRestImplTest extends MobiRestTestNg {
    private MappingRestImpl rest;
    private static final String CATALOG_IRI = "http://test.org/catalog";
    private static final String MAPPING_IRI = "http://test.org/test";
    private static final String MAPPING_RECORD_IRI = "http://test.org/record";
    private static final String BRANCH_IRI = "http://test.org/branch";
    private static final String ERROR_IRI = "http://test.org/error";
    private String mappingJsonld;
    private ValueFactory vf;
    private ModelFactory mf;
    private ValueConverterRegistry vcr;
    private MappingRecordFactory mappingRecordFactory;
    private BranchFactory branchFactory;
    private UserFactory userFactory;
    private CreateActivityFactory createActivityFactory;
    private DeleteActivityFactory deleteActivityFactory;
    private Model fakeModel;
    private User user;
    private CreateActivity createActivity;
    private DeleteActivity deleteActivity;
    private MappingRecord record;
    private Branch branch;

    @Mock
    private MappingManager manager;

    @Mock
    private MappingWrapper mappingWrapper;

    @Mock
    private MappingId mappingId;

    @Mock
    private CatalogManager catalogManager;

    @Mock
    private EngineManager engineManager;

    @Mock
    private VersioningManager versioningManager;

    @Mock
    private CatalogProvUtils provUtils;

    @Override
    protected Application configureApp() throws Exception {
        vf = SimpleValueFactory.getInstance();
        mf = LinkedHashModelFactory.getInstance();
        vcr = new DefaultValueConverterRegistry();

        mappingRecordFactory = new MappingRecordFactory();
        mappingRecordFactory.setValueFactory(vf);
        mappingRecordFactory.setModelFactory(mf);
        mappingRecordFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(mappingRecordFactory);

        branchFactory = new BranchFactory();
        branchFactory.setValueFactory(vf);
        branchFactory.setModelFactory(mf);
        branchFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(branchFactory);

        userFactory = new UserFactory();
        userFactory.setValueFactory(vf);
        userFactory.setModelFactory(mf);
        userFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(userFactory);

        createActivityFactory = new CreateActivityFactory();
        createActivityFactory.setValueFactory(vf);
        createActivityFactory.setModelFactory(mf);
        createActivityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(createActivityFactory);

        deleteActivityFactory = new DeleteActivityFactory();
        deleteActivityFactory.setValueFactory(vf);
        deleteActivityFactory.setModelFactory(mf);
        deleteActivityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(deleteActivityFactory);

        vcr.registerValueConverter(new ResourceValueConverter());
        vcr.registerValueConverter(new IRIValueConverter());
        vcr.registerValueConverter(new DoubleValueConverter());
        vcr.registerValueConverter(new IntegerValueConverter());
        vcr.registerValueConverter(new FloatValueConverter());
        vcr.registerValueConverter(new ShortValueConverter());
        vcr.registerValueConverter(new StringValueConverter());
        vcr.registerValueConverter(new ValueValueConverter());
        vcr.registerValueConverter(new LiteralValueConverter());

        fakeModel = mf.createModel();
        fakeModel.add(vf.createIRI(MAPPING_IRI), vf.createIRI("http://test.org/isTest"), vf.createLiteral(true));
        branch = branchFactory.createNew(vf.createIRI(BRANCH_IRI));
        record = mappingRecordFactory.createNew(vf.createIRI(MAPPING_RECORD_IRI));
        record.setMasterBranch(branch);
        user = userFactory.createNew(vf.createIRI("http://test.org/" + UsernameTestFilter.USERNAME));
        createActivity = createActivityFactory.createNew(vf.createIRI("http://test.org/activity/create"));
        deleteActivity = deleteActivityFactory.createNew(vf.createIRI("http://test.org/activity/delete"));

        MockitoAnnotations.initMocks(this);

        rest = new MappingRestImpl();
        rest.setManager(manager);
        rest.setVf(vf);
        rest.setTransformer(new SimpleSesameTransformer());
        rest.setEngineManager(engineManager);
        rest.setCatalogManager(catalogManager);
        rest.setVersioningManager(versioningManager);
        rest.setProvUtils(provUtils);

        mappingJsonld = IOUtils.toString(getClass().getResourceAsStream("/mapping.jsonld"));

        return new ResourceConfig()
                .register(rest)
                .register(UsernameTestFilter.class)
                .register(MultiPartFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @BeforeMethod
    public void setupMocks() throws Exception {
        reset(mappingId, mappingWrapper, manager, provUtils, catalogManager, versioningManager);

        when(catalogManager.getLocalCatalogIRI()).thenReturn(vf.createIRI(CATALOG_IRI));
        when(catalogManager.getRecord(eq(vf.createIRI(CATALOG_IRI)), eq(record.getResource()), any(MappingRecordFactory.class)))
                .thenReturn(Optional.of(record));

        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.of(user));

        when(mappingId.getMappingIdentifier()).thenReturn(vf.createIRI(MAPPING_IRI));
        when(mappingWrapper.getModel()).thenReturn(fakeModel);
        when(mappingWrapper.getId()).thenReturn(mappingId);
        when(manager.createMappingRecord(any(MappingRecordConfig.class))).thenReturn(record);
        when(manager.createMapping(any(InputStream.class), any(RDFFormat.class))).thenReturn(mappingWrapper);
        when(manager.createMapping(anyString())).thenReturn(mappingWrapper);
        when(manager.retrieveMapping(vf.createIRI(ERROR_IRI))).thenReturn(Optional.empty());
        when(manager.retrieveMapping(vf.createIRI(MAPPING_IRI))).thenReturn(Optional.of(mappingWrapper));
        when(manager.deleteMapping(record.getResource())).thenReturn(record);
        when(manager.createMappingId(any(IRI.class))).thenAnswer(i -> new MappingId() {
            @Override
            public Optional<IRI> getMappingIRI() {
                return null;
            }

            @Override
            public Optional<IRI> getVersionIRI() {
                return null;
            }

            @Override
            public Resource getMappingIdentifier() {
                return vf.createIRI(i.getArguments()[0].toString());
            }
        });

        when(provUtils.startCreateActivity(any(User.class))).thenReturn(createActivity);
        when(provUtils.startDeleteActivity(any(User.class), any(IRI.class))).thenReturn(deleteActivity);
    }

    @Test
    public void uploadEitherFileOrStringTest() {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "Title");
        InputStream content = getClass().getResourceAsStream("/mapping.jsonld");
        fd.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("file").fileName("mapping.jsonld").build(),
                content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        fd.field("jsonld", mappingJsonld);
        Response response = target().path("mappings").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);

        response = target().path("mappings").request().post(Entity.entity(null, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    @Test
    public void uploadFileTest() throws Exception {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "Title");
        InputStream content = getClass().getResourceAsStream("/mapping.jsonld");
        fd.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("file").fileName("mapping.jsonld").build(),
                content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        Response response = target().path("mappings").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertTrue(response.readEntity(String.class).equals(MAPPING_RECORD_IRI));
        verify(manager).createMapping(any(InputStream.class), eq(RDFFormat.JSONLD));
        verify(manager).createMappingRecord(any(MappingRecordConfig.class));
        verify(catalogManager).addRecord(eq(vf.createIRI(CATALOG_IRI)), any(Record.class));
        verify(versioningManager).commit(eq(vf.createIRI(CATALOG_IRI)), eq(vf.createIRI(MAPPING_RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), eq(user), anyString(), any(Model.class), eq(null));
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).endCreateActivity(createActivity, vf.createIRI(MAPPING_RECORD_IRI));
    }

    @Test
    public void uploadStringTest() throws Exception {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "Title");
        fd.field("jsonld", mappingJsonld);
        Response response = target().path("mappings").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertTrue(response.readEntity(String.class).equals(MAPPING_RECORD_IRI));
        verify(manager).createMapping(mappingJsonld);
        verify(manager).createMappingRecord(any(MappingRecordConfig.class));
        verify(catalogManager).addRecord(eq(vf.createIRI(CATALOG_IRI)), any(Record.class));
        verify(versioningManager).commit(eq(vf.createIRI(CATALOG_IRI)), eq(vf.createIRI(MAPPING_RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), eq(user), anyString(), any(Model.class), eq(null));
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).endCreateActivity(createActivity, vf.createIRI(MAPPING_RECORD_IRI));
    }

    @Test
    public void getMappingTest() {
        Response response = target().path("mappings/" + encode(MAPPING_IRI)).request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        assertEquals(response.getStatus(), 200);
        verify(manager).retrieveMapping(vf.createIRI(MAPPING_IRI));
        try {
            JSONArray.fromObject(response.readEntity(String.class));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getMappingThatDoesNotExistTest() {
        Response response = target().path("mappings/" + encode(ERROR_IRI)).request()
                .accept(MediaType.APPLICATION_JSON_TYPE).get();
        verify(manager).retrieveMapping(vf.createIRI(ERROR_IRI));
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void downloadMappingTest() {
        Response response = target().path("mappings/" + encode(MAPPING_IRI)).request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();
        assertEquals(response.getStatus(), 200);
        verify(manager).retrieveMapping(vf.createIRI(MAPPING_IRI));
    }

    @Test
    public void downloadMappingThatDoesNotExistTest() {
        Response response = target().path("mappings/" + encode(ERROR_IRI)).request()
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();
        verify(manager).retrieveMapping(vf.createIRI(ERROR_IRI));
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void deleteMappingTest() {
        Response response = target().path("mappings/" + encode(MAPPING_RECORD_IRI)).request().delete();
        assertEquals(response.getStatus(), 200);

        IRI mrIri = vf.createIRI(MAPPING_RECORD_IRI);
        verify(manager).deleteMapping(mrIri);
        verify(provUtils).startDeleteActivity(user, mrIri);
        verify(provUtils).endDeleteActivity(deleteActivity, record);
    }
}
