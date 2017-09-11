package org.matonto.prov.rest.impl;

/*-
 * #%L
 * org.matonto.prov.rest
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.matonto.exception.MatOntoException;
import org.matonto.ontologies.provo.Activity;
import org.matonto.ontologies.provo.ActivityFactory;
import org.matonto.ontologies.provo.EntityFactory;
import org.matonto.persistence.utils.api.SesameTransformer;
import org.matonto.prov.api.ProvenanceService;
import org.matonto.rdf.api.ModelFactory;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.Statement;
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
import org.matonto.repository.impl.sesame.SesameRepositoryWrapper;
import org.matonto.rest.util.MatontoRestTestNg;
import org.matonto.rest.util.UsernameTestFilter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openrdf.model.Model;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class ProvRestImplTest extends MatontoRestTestNg {
    private ProvRestImpl rest;

    private ValueFactory vf;
    private ModelFactory mf;
    private ValueConverterRegistry vcr;
    private ActivityFactory activityFactory;
    private EntityFactory entityFactory;
    private Repository repo;

    private String provData;
    private List<String> activityIRIs;
    private List<String> entityIRIs;
    private final String ACTIVITY_NAMESPACE = "http://test.org/activities#";
    private final String ENTITY_NAMESPACE = "http://test.org/entities#";
    private Map<String, List<String>> entityMap;

    @Mock
    private ProvenanceService provService;

    @Mock
    private SesameTransformer transformer;

    @Override
    protected Application configureApp() throws Exception {
        repo = new SesameRepositoryWrapper(new SailRepository(new MemoryStore()));
        repo.initialize();

        vf = SimpleValueFactory.getInstance();
        mf = LinkedHashModelFactory.getInstance();
        vcr = new DefaultValueConverterRegistry();

        activityFactory = new ActivityFactory();
        activityFactory.setValueFactory(vf);
        activityFactory.setModelFactory(mf);
        activityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(activityFactory);

        entityFactory = new EntityFactory();
        entityFactory.setValueFactory(vf);
        entityFactory.setModelFactory(mf);
        entityFactory.setValueConverterRegistry(vcr);
        vcr.registerValueConverter(entityFactory);

        vcr.registerValueConverter(new ResourceValueConverter());
        vcr.registerValueConverter(new IRIValueConverter());
        vcr.registerValueConverter(new DoubleValueConverter());
        vcr.registerValueConverter(new IntegerValueConverter());
        vcr.registerValueConverter(new FloatValueConverter());
        vcr.registerValueConverter(new ShortValueConverter());
        vcr.registerValueConverter(new StringValueConverter());
        vcr.registerValueConverter(new ValueValueConverter());
        vcr.registerValueConverter(new LiteralValueConverter());

        provData = IOUtils.toString(getClass().getResourceAsStream("/prov-data.ttl"));
        activityIRIs = IntStream.range(0, 10)
                .mapToObj(i -> ACTIVITY_NAMESPACE + "Activity" + i)
                .collect(Collectors.toList());
        Collections.reverse(activityIRIs);

        entityIRIs = IntStream.range(0, 5)
                .mapToObj(i -> ENTITY_NAMESPACE + "Entity" + i)
                .collect(Collectors.toList());

        entityMap = new HashMap<>();
        entityMap.put(activityIRIs.get(0), Collections.singletonList(entityIRIs.get(0)));
        entityMap.put(activityIRIs.get(1), Collections.singletonList(entityIRIs.get(0)));
        entityMap.put(activityIRIs.get(2), Collections.singletonList(entityIRIs.get(1)));
        entityMap.put(activityIRIs.get(3), Stream.of(entityIRIs.get(2), entityIRIs.get(3)).collect(Collectors.toList()));
        entityMap.put(activityIRIs.get(4), Stream.of(entityIRIs.get(1), entityIRIs.get(2)).collect(Collectors.toList()));
        entityMap.put(activityIRIs.get(5), Collections.singletonList(entityIRIs.get(0)));
        entityMap.put(activityIRIs.get(6), Collections.singletonList(entityIRIs.get(3)));
        entityMap.put(activityIRIs.get(7), Collections.singletonList(entityIRIs.get(4)));
        entityMap.put(activityIRIs.get(8), Collections.singletonList(entityIRIs.get(4)));
        entityMap.put(activityIRIs.get(9), Stream.of(entityIRIs.get(2), entityIRIs.get(3)).collect(Collectors.toList()));

        MockitoAnnotations.initMocks(this);

        when(transformer.matontoModel(any(Model.class)))
                .thenAnswer(i -> Values.matontoModel(i.getArgumentAt(0, Model.class)));
        when(transformer.sesameModel(any(org.matonto.rdf.api.Model.class)))
                .thenAnswer(i -> Values.sesameModel(i.getArgumentAt(0, org.matonto.rdf.api.Model.class)));
        when(transformer.sesameStatement(any(Statement.class)))
                .thenAnswer(i -> Values.sesameStatement(i.getArgumentAt(0, Statement.class)));

        rest = new ProvRestImpl();
        rest.setMf(mf);
        rest.setVf(vf);
        rest.setProvService(provService);
        rest.setTransformer(transformer);

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
    public void setUpMocks() throws Exception {
        try (RepositoryConnection conn = repo.getConnection()) {
            conn.add(Values.matontoModel(Rio.parse(new ByteArrayInputStream(provData.getBytes()), "", RDFFormat.TURTLE)));
        }
        reset(provService);

        when(provService.getConnection()).thenReturn(repo.getConnection());
        when(provService.getActivity(any(Resource.class))).thenAnswer(i -> {
            Activity activity = activityFactory.createNew(i.getArgumentAt(0, Resource.class));
            entityMap.get(activity.getResource().stringValue()).forEach(entityIRI -> {
                entityFactory.createNew(vf.createIRI(entityIRI), activity.getModel());
            });
            return Optional.of(activity);
        });
    }

    @Test
    public void getActivitiesWithNoneTest() throws Exception {
        // Setup:
        try (RepositoryConnection conn = repo.getConnection()) {
            conn.clear();
        }

        Response response = target().path("provenance").request().get();
        assertEquals(response.getStatus(), 200);
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "0");
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));

            assertTrue(result.containsKey("activities"));
            JSONArray activities = result.optJSONArray("activities");
            assertNotNull(activities);
            assertEquals(activities.size(), 0);

            assertTrue(result.containsKey("entities"));
            JSONArray entities = result.optJSONArray("entities");
            assertNotNull(entities);
            assertEquals(entities.size(), 0);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getActivitiesOnePageTest() throws Exception {
        Response response = target().path("provenance").request().get();
        assertEquals(response.getStatus(), 200);
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "10");
        verify(provService, times(10)).getActivity(any(Resource.class));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));

            assertTrue(result.containsKey("activities"));
            JSONArray activities = result.optJSONArray("activities");
            assertNotNull(activities);
            assertEquals(activities.size(), 10);
            assertResourceOrder(activities, activityIRIs);

            assertTrue(result.containsKey("entities"));
            JSONArray entities = result.optJSONArray("entities");
            assertNotNull(entities);
            assertEquals(entities.size(), 5);
            List<String> resources = getResources(entities);
            assertTrue(resources.containsAll(entityIRIs));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getActivitiesWithLinksTest() throws Exception {
        Response response = target().path("provenance").queryParam("limit", 2).queryParam("offset", 2).request().get();
        assertEquals(response.getStatus(), 200);
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "10");
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        assertTrue(response.hasLink("prev"));
        assertTrue(response.hasLink("next"));
        verify(provService, times(2)).getActivity(any(Resource.class));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));

            assertTrue(result.containsKey("activities"));
            JSONArray activities = result.optJSONArray("activities");
            assertNotNull(activities);
            assertEquals(activities.size(), 2);
            assertResourceOrder(activities, activityIRIs.subList(2, 4));

            assertTrue(result.containsKey("entities"));
            JSONArray entities = result.optJSONArray("entities");
            assertNotNull(entities);
            assertEquals(entities.size(), 3);
            List<String> resources = getResources(entities);
            assertTrue(resources.containsAll(Stream.of(entityIRIs.get(1), entityIRIs.get(2), entityIRIs.get(3)).collect(Collectors.toList())));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getActivitiesWithNegativeLimitTest() {
        Response response = target().path("provenance").queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getActivitiesWithNegativeOffsetTest() {
        Response response = target().path("provenance").queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getActivitiesWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(provService).getConnection();

        Response response = target().path("provenance").request().get();
        assertEquals(response.getStatus(), 500);
    }

    private void assertResourceOrder(JSONArray array, List<String> expectedOrder) {
        List<String> resources = getResources(array);
        assertEquals(resources, expectedOrder);
    }

    private List<String> getResources(JSONArray array) {
        List<String> resources = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.optJSONObject(i);
            assertNotNull(obj);
            String iri = obj.optString("@id");
            assertNotNull(iri);
            resources.add(iri);
        }
        return resources;
    }
}
