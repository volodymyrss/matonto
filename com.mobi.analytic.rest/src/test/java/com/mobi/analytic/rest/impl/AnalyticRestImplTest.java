package com.mobi.analytic.rest.impl;

/*-
 * #%L
 * com.mobi.analytic.rest
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

import static com.mobi.rdf.orm.test.OrmEnabledTestCase.getModelFactory;
import static com.mobi.rdf.orm.test.OrmEnabledTestCase.getOrmFactoryRegistry;
import static com.mobi.rdf.orm.test.OrmEnabledTestCase.getRequiredOrmFactory;
import static com.mobi.rdf.orm.test.OrmEnabledTestCase.getValueFactory;
import static com.mobi.rest.util.RestUtils.encode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.mobi.analytic.api.AnalyticManager;
import com.mobi.analytic.api.builder.AnalyticRecordConfig;
import com.mobi.analytic.ontologies.analytic.AnalyticRecord;
import com.mobi.analytic.ontologies.analytic.Configuration;
import com.mobi.analytic.ontologies.analytic.TableConfiguration;
import com.mobi.catalog.api.CatalogProvUtils;
import com.mobi.exception.MobiException;
import com.mobi.jaas.api.engines.EngineManager;
import com.mobi.jaas.api.ontologies.usermanagement.User;
import com.mobi.persistence.utils.api.SesameTransformer;
import com.mobi.prov.api.ontologies.mobiprov.CreateActivity;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.api.Statement;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.rdf.core.utils.Values;
import com.mobi.rdf.orm.OrmFactory;
import com.mobi.rdf.orm.Thing;
import com.mobi.repository.exception.RepositoryException;
import com.mobi.rest.util.MobiRestTestNg;
import com.mobi.rest.util.UsernameTestFilter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class AnalyticRestImplTest extends MobiRestTestNg {
    private AnalyticRestImpl rest;
    private OrmFactory<Configuration> configurationFactory;
    private OrmFactory<TableConfiguration> tableConfigurationFactory;
    private AnalyticRecord record;
    private User user;
    private CreateActivity activity;
    private Configuration config;
    private TableConfiguration tableConfig;
    private Resource recordId;

    private static final String RECORD_IRI = "http://mobi.com/test/records#1";
    private static final String USER_IRI = "http://mobi.com/users/tester";
    private static final String ACTIVITY_IRI = "http://mobi.com/test/activities#1";
    private static final String CONFIG_IRI = "http://mobi.com/test/configurations#1";
    private static final String TABLE_CONFIG_IRI = "http://mobi.com/test/configurations#1";

    @Mock
    private AnalyticManager analyticManager;

    @Mock
    private EngineManager engineManager;

    @Mock
    private CatalogProvUtils provUtils;

    @Mock
    private SesameTransformer transformer;

    @Override
    protected Application configureApp() throws Exception {
        ValueFactory vf = getValueFactory();

        OrmFactory<AnalyticRecord> analyticRecordFactory = getRequiredOrmFactory(AnalyticRecord.class);
        OrmFactory<User> userFactory = getRequiredOrmFactory(User.class);
        OrmFactory<CreateActivity> createActivityFactory = getRequiredOrmFactory(CreateActivity.class);
        configurationFactory = getRequiredOrmFactory(Configuration.class);
        tableConfigurationFactory = getRequiredOrmFactory(TableConfiguration.class);

        recordId = vf.createIRI(RECORD_IRI);
        record = analyticRecordFactory.createNew(recordId);
        user = userFactory.createNew(vf.createIRI(USER_IRI));
        activity = createActivityFactory.createNew(vf.createIRI(ACTIVITY_IRI));
        config = configurationFactory.createNew(vf.createIRI(CONFIG_IRI));
        tableConfig = tableConfigurationFactory.createNew(vf.createIRI(TABLE_CONFIG_IRI));

        MockitoAnnotations.initMocks(this);

        rest = new AnalyticRestImpl();
        rest.setAnalyticManager(analyticManager);
        rest.setEngineManager(engineManager);
        rest.setFactoryRegistry(getOrmFactoryRegistry());
        rest.setProvUtils(provUtils);
        rest.setValueFactory(vf);
        rest.setModelFactory(getModelFactory());
        rest.setSesameTransformer(transformer);

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
    public void setupMocks() {
        reset(analyticManager, engineManager, provUtils, transformer);

        when(analyticManager.createConfiguration(anyString(), eq(configurationFactory))).thenReturn(config);
        when(analyticManager.createConfiguration(anyString(), eq(tableConfigurationFactory))).thenReturn(tableConfig);
        when(analyticManager.createAnalytic(any(AnalyticRecordConfig.class))).thenReturn(record);
        when(analyticManager.getAnalyticRecord(recordId)).thenReturn(Optional.of(record));

        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.of(user));
        when(engineManager.getUsername(any(Resource.class))).thenReturn(Optional.of(user.getResource().stringValue()));

        when(provUtils.startCreateActivity(any(User.class))).thenReturn(activity);

        when(transformer.sesameStatement(any(Statement.class))).thenAnswer(i -> Values.sesameStatement(i.getArgumentAt(0, Statement.class)));
    }

    @Test
    public void getConfigurationTypesTest() {
        Response response = target().path("analytics/configuration-types").request().get();
        assertEquals(response.getStatus(), 200);
        try {
            JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(array.size(), 2);
            assertTrue(array.contains(configurationFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(tableConfigurationFactory.getTypeIRI().stringValue()));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void createAnalyticTest() {
        testCreateAnalyticByType(configurationFactory);
    }

    @Test
    public void createTableAnalyticTest() {
        testCreateAnalyticByType(tableConfigurationFactory);
    }

    @Test
    public void createAnalyticWithoutTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");

        verifyCreateAnalyticFailBeforeActivity(fd, 400);
    }

    @Test
    public void createAnalyticWithoutTitleTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("type", Configuration.TYPE);

        verifyCreateAnalyticFailBeforeActivity(fd, 400);
    }

    @Test
    public void createAnalyticWithInvalidTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        fd.field("type", Thing.TYPE);

        verifyCreateAnalyticFailBeforeActivity(fd, 400);
    }

    @Test
    public void createAnalyticForUserThatDoesNotExistTest() {
        //Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        fd.field("type", Configuration.TYPE);

        verifyCreateAnalyticFailBeforeActivity(fd, 401);
    }

    @Test
    public void createAnalyticWithIncorrectPathTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        fd.field("type", Configuration.TYPE);
        doThrow(new IllegalArgumentException()).when(analyticManager).createAnalytic(any(AnalyticRecordConfig.class));

        verifyCreateAnalyticFailAfterActivity(fd, 400);
    }

    @Test
    public void createAnalyticWithErrorTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        fd.field("type", Configuration.TYPE);
        doThrow(new MobiException()).when(analyticManager).createAnalytic(any(AnalyticRecordConfig.class));

        verifyCreateAnalyticFailAfterActivity(fd, 500);
    }

    @Test
    public void getAnalyticTest() {
        Response response = target().path("analytics/" + encode(RECORD_IRI)).request().get();
        assertEquals(response.getStatus(), 200);
        verify(analyticManager).getAnalyticRecord(recordId);
    }

    @Test
    public void getAnalyticThatCouldNotBeFoundTest() {
        // Setup:
        when(analyticManager.getAnalyticRecord(recordId)).thenReturn(Optional.empty());

        Response response = target().path("analytics/" + encode(RECORD_IRI)).request().get();
        assertEquals(response.getStatus(), 404);
        verify(analyticManager).getAnalyticRecord(recordId);
    }

    @Test
    public void getAnalyticThatDoesNotExistTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(analyticManager).getAnalyticRecord(recordId);

        Response response = target().path("analytics/" + encode(RECORD_IRI)).request().get();
        assertEquals(response.getStatus(), 400);
        verify(analyticManager).getAnalyticRecord(recordId);
    }

    @Test
    public void getAnalyticWithIllegalStateTest() {
        // Setup:
        doThrow(new IllegalStateException()).when(analyticManager).getAnalyticRecord(recordId);

        Response response = target().path("analytics/" + encode(RECORD_IRI)).request().get();
        assertEquals(response.getStatus(), 500);
        verify(analyticManager).getAnalyticRecord(recordId);
    }

    @Test
    public void getAnalyticWithFailedConnectionTest() {
        // Setup:
        doThrow(new RepositoryException()).when(analyticManager).getAnalyticRecord(recordId);

        Response response = target().path("analytics/" + encode(RECORD_IRI)).request().get();
        assertEquals(response.getStatus(), 500);
        verify(analyticManager).getAnalyticRecord(recordId);
    }

    @Test
    public void updateAnalyticTest() {
        testUpdateAnalyticByType(configurationFactory, config);
    }

    @Test
    public void updateTableAnalyticTest() {
        testUpdateAnalyticByType(tableConfigurationFactory, tableConfig);
    }

    @Test
    public void updateAnalyticWithInvalidConfigurationTypeTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("typeIRI", "INVALID");
        fd.field("json", "{}");

        verifyUpdateAnalytic(fd, 400);
    }

    @Test
    public void updateAnalyticWithIllegalStateTest() {
        // Setup:
        FormDataMultiPart fd = getValidFormData(configurationFactory);
        doThrow(new IllegalArgumentException()).when(analyticManager).updateConfiguration(recordId, config);

        verifyUpdateAnalytic(fd, 400);
    }

    @Test
    public void updateAnalyticWithFailedConnectionTest() {
        // Setup:
        FormDataMultiPart fd = getValidFormData(configurationFactory);
        doThrow(new RepositoryException()).when(analyticManager).updateConfiguration(recordId, config);

        verifyUpdateAnalytic(fd, 500);
    }

    private <T extends Configuration> FormDataMultiPart getValidFormData(OrmFactory<T> factory) {
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", factory.getTypeIRI().stringValue());
        fd.field("json", "{}");
        return fd;
    }

    private <T extends Configuration> void testCreateAnalyticByType(OrmFactory<T> factory) {
        // Setup:
        FormDataMultiPart fd = getValidFormData(factory);
        fd.field("title", "Title");
        fd.field("description", "Description");
        fd.field("keywords", "keyword");

        Response response = target().path("analytics").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        JSONObject object = response.readEntity(JSONObject.class);
        assertEquals(object.get("analyticRecordId"), RECORD_IRI);
        assertEquals(object.get("configurationId"), CONFIG_IRI);
        verify(analyticManager).createAnalytic(any(AnalyticRecordConfig.class));
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).endCreateActivity(activity, recordId);
    }

    private <T extends Configuration> void testUpdateAnalyticByType(OrmFactory<T> factory, T config) {
        // Setup:
        FormDataMultiPart fd = getValidFormData(factory);

        verifyUpdateAnalytic(fd, 200);
        verify(analyticManager).createConfiguration("{}", factory);
        verify(analyticManager).updateConfiguration(recordId, config);
    }

    private void verifyUpdateAnalytic(FormDataMultiPart fd, int status) {
        Response response = target().path("analytics/" + encode(RECORD_IRI)).request()
                .put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), status);
    }

    private void verifyCreateAnalyticFail(FormDataMultiPart fd, int status) {
        Response response = target().path("analytics").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), status);
    }

    private void verifyCreateAnalyticFailBeforeActivity(FormDataMultiPart fd, int status) {
        verifyCreateAnalyticFail(fd, status);
        verify(provUtils, times(0)).startCreateActivity(user);
    }

    private void verifyCreateAnalyticFailAfterActivity(FormDataMultiPart fd, int status) {
        verifyCreateAnalyticFail(fd, status);
        verify(provUtils).startCreateActivity(user);
        verify(provUtils).removeActivity(activity);
    }
}
