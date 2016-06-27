package org.matonto.etl.rest.impl;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.matonto.etl.api.delimited.Mapping;
import org.matonto.etl.api.delimited.MappingId;
import org.matonto.etl.api.delimited.MappingManager;
import org.matonto.rdf.api.IRI;
import org.matonto.rdf.api.Model;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rdf.core.impl.sesame.LinkedHashModel;
import org.matonto.rdf.core.impl.sesame.SimpleValueFactory;
import org.matonto.rest.util.MatontoRestTestNg;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openrdf.rio.RDFFormat;
import org.testng.annotations.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MappingRestImplTest extends MatontoRestTestNg {
    private MappingRestImpl rest;

    @Mock
    MappingManager manager;

    @Mock
    Mapping mapping;

    @Mock
    MappingId mappingId;

    @Override
    protected Application configureApp() throws Exception {
        ValueFactory factory = SimpleValueFactory.getInstance();
        Model fakeModel = new LinkedHashModel();
        fakeModel.add(factory.createIRI("http://test.org"), factory.createIRI("http://test.org/isTest"), factory.createLiteral(true));
        MockitoAnnotations.initMocks(this);
        rest = new MappingRestImpl();
        rest.setManager(manager);
        rest.setFactory(factory);

        when(mappingId.getMappingIdentifier()).thenReturn(factory.createIRI("http://test.org"));
        when(mapping.getModel()).thenReturn(fakeModel);
        when(mapping.getId()).thenReturn(mappingId);
        when(manager.mappingExists(any(Resource.class))).thenAnswer(i -> i.getArguments()[0].toString().contains("none"));
        when(manager.createMapping(any(InputStream.class), any(RDFFormat.class))).thenReturn(mapping);
        when(manager.createMapping(anyString())).thenReturn(mapping);
        when(manager.storeMapping(any(Mapping.class))).thenReturn(true);
        when(manager.deleteMapping(any(Resource.class))).thenReturn(true);
        when(manager.getMappingRegistry()).thenReturn(new HashSet<Resource>());
        when(manager.createMappingIRI()).thenReturn(factory.createIRI("http://test.org"));
        when(manager.createMappingIRI(anyString())).thenAnswer(i -> factory.createIRI("http://test.org/" + i.getArguments()[0]));
        when(manager.retrieveMapping(any(Resource.class))).thenAnswer(i -> i.getArguments()[0].toString().contains("error") ? Optional.empty() : Optional.of(mapping));
        when(manager.getMappingLocalName(any(IRI.class))).thenReturn("");
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
                return factory.createIRI(i.getArguments()[0].toString());
            }
        });

        return new ResourceConfig()
            .register(rest)
            .register(MultiPartFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @Test
    public void uploadEitherFileOrStringTest() {
        FormDataMultiPart fd = new FormDataMultiPart();
        InputStream content = getClass().getResourceAsStream("/mapping.jsonld");
        fd.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("file").fileName("mapping.jsonld").build(),
                content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        fd.field("jsonld", "[]");
        Response response = target().path("mappings").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        Assert.assertEquals(response.getStatus(), 400);

        response = target().path("mappings").request().post(Entity.entity(null, MediaType.MULTIPART_FORM_DATA));
        Assert.assertEquals(response.getStatus(), 400);
    }

    @Test
    public void uploadFileTest() {
        FormDataMultiPart fd = new FormDataMultiPart();
        InputStream content = getClass().getResourceAsStream("/mapping.jsonld");
        fd.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("file").fileName("mapping.jsonld").build(),
                content, MediaType.APPLICATION_OCTET_STREAM_TYPE));
        Response response = target().path("mappings").request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains(manager.createMappingIRI().stringValue()));
    }

    @Test
    public void getMappingNamesTest() {
        Response response = target().path("mappings").request().get();
        Assert.assertEquals(200, response.getStatus());
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            Assert.assertEquals(result.size(), manager.getMappingRegistry().size());
        } catch (Exception e) {
            Assert.fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getMappingsByIdsTest() {
        List<String> ids = Arrays.asList(manager.createMappingIRI("test1").toString(),
                manager.createMappingIRI("test2").toString());
        WebTarget wt = target().path("mappings");
        for (String id : ids) {
            wt = wt.queryParam("ids", id);
        }
        Response response = wt.request().get();
        Assert.assertEquals(200, response.getStatus());
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            Assert.assertEquals(ids.size(), result.size());
        } catch (Exception e) {
            Assert.fail("Expected no exception, but got: " + e.getMessage());
        }

        ids = Arrays.asList(manager.createMappingIRI("test1").toString(),
                manager.createMappingIRI("error").toString());
        wt = target().path("mappings");
        for (String id : ids) {
            wt = wt.queryParam("ids", id);
        }
        response = wt.request().get();
        Assert.assertEquals(200, response.getStatus());
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            Assert.assertEquals(ids.size() - 1, result.size());
        } catch (Exception e) {
            Assert.fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getMappingTest() {
        Response response = target().path("mappings/" + encode(manager.createMappingIRI("test").toString()))
            .request().get();
        Assert.assertEquals(200, response.getStatus());
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
        } catch (Exception e) {
            Assert.fail("Expected no exception, but got: " + e.getMessage());
        }

        response = target().path("mappings/" + encode(manager.createMappingIRI("error").toString())).request().get();
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void downloadMappingTest() {
        Response response = target().path("mappings/" + encode(manager.createMappingIRI("test").toString()))
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();
        Assert.assertEquals(200, response.getStatus());

        response = target().path("mappings/" + encode(manager.createMappingIRI("error").toString()))
                .request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void deleteMappingTest() {
        Response response = target().path("mappings/" + encode(manager.createMappingIRI("test").toString()))
                .request().delete();
        Assert.assertEquals(200, response.getStatus());
        try {
            boolean result = response.readEntity(Boolean.class);
            Assert.assertTrue(result);
        } catch (Exception e) {
            Assert.fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    private String encode(String str) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encoded;
    }
}
