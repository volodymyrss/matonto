package com.mobi.etl.service.workflows;

/*-
 * #%L
 * com.mobi.etl.workflows
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mobi.etl.api.ontologies.etl.Workflow;
import com.mobi.etl.api.workflows.WorkflowConverter;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.orm.OrmFactory;
import com.mobi.rdf.orm.test.OrmEnabledTestCase;
import com.mobi.repository.api.Repository;
import com.mobi.repository.api.RepositoryConnection;
import com.mobi.repository.impl.sesame.SesameRepositoryWrapper;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WorkflowManagerImpl.class)
public class WorkflowManagerImplTest extends OrmEnabledTestCase {
    private WorkflowManagerImpl service;

    private Repository repo;

    private final Resource WORKFLOW1_ID = VALUE_FACTORY.createIRI("http://test.com/workflow1");
    private final Resource WORKFLOW2_ID = VALUE_FACTORY.createIRI("http://test.com/workflow2");
    private Workflow workflow1;
    private Workflow workflow2;

    @Mock
    private MobiCamelContext context;

    @Mock
    private WorkflowConverter converterService;

    @Mock
    private RouteBuilder routeBuilder;

    @Mock
    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
        repo = new SesameRepositoryWrapper(new SailRepository(new MemoryStore()));
        repo.initialize();

        OrmFactory<Workflow> workflowFactory = getRequiredOrmFactory(Workflow.class);
        workflow1 = workflowFactory.createNew(WORKFLOW1_ID);
        workflow2 = workflowFactory.createNew(WORKFLOW2_ID);

        MockitoAnnotations.initMocks(this);
        PowerMockito.whenNew(MobiCamelContext.class).withAnyArguments().thenReturn(context);

        when(converterService.convert(any(Workflow.class), any(CamelContext.class))).thenReturn(routeBuilder);

        service = new WorkflowManagerImpl();
        injectOrmFactoryReferencesIntoService(service);
        service.setConverterService(converterService);
        service.setVf(VALUE_FACTORY);
        service.setMf(MODEL_FACTORY);
        service.setRepository(repo);

        // Start out with workflow2
        try (RepositoryConnection conn = repo.getConnection()) {
            conn.clear();
            conn.add(workflow2.getModel(), WORKFLOW2_ID);
        }
    }

    @After
    public void tearDown() throws Exception {
        repo.shutDown();
    }

    @Test
    public void startTest() throws Exception {
        service.activate(bundleContext);
        Thread.sleep(100);
        assertTrue(service.workflowMap.containsKey(WORKFLOW2_ID));
        verify(context).addRoutes(routeBuilder);
    }

    @Test
    public void startWithFailedWorkflowTest() throws Exception {
        // Setup
        doThrow(new IllegalArgumentException()).when(converterService).convert(any(Workflow.class), any(CamelContext.class));

        service.activate(bundleContext);
        Thread.sleep(3100);
        assertFalse(service.workflowMap.containsKey(WORKFLOW2_ID));
        assertTrue(service.failedWorkflows.contains(WORKFLOW2_ID));
    }

    @Test
    public void getWorkflowsTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        Set<Workflow> result = service.getWorkflows();
        assertEquals(1, result.size());
        assertEquals(WORKFLOW2_ID, result.iterator().next().getResource());
    }

    @Test(expected = IllegalStateException.class)
    public void getWorkflowsWithMissingTest() {
        // Setup:
        service.workflowMap.put(WORKFLOW1_ID, context);

        service.getWorkflows();
    }

    @Test
    public void getFailedWorkflowsTest() throws Exception {
        Set<Workflow> result = service.getFailedWorkflows();
        assertEquals(0, result.size());
    }

    @Test(expected = IllegalStateException.class)
    public void getFailedWorkflowsWithMissingTest() {
        // Setup:
        service.failedWorkflows.add(WORKFLOW1_ID);

        service.getFailedWorkflows();
    }

    @Test
    public void addWorkflowTest() throws Exception {
        service.addWorkflow(workflow1);
        assertTrue(service.workflowMap.containsKey(WORKFLOW1_ID));
        verify(converterService).convert(workflow1, context);
        verify(context).addRoutes(routeBuilder);
        try (RepositoryConnection conn = repo.getConnection()) {
            workflow1.getModel().forEach(statement -> assertTrue(conn.contains(statement.getSubject(), statement.getPredicate(), statement.getObject(), WORKFLOW1_ID)));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void addWorkflowThatAlreadyExistsTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        service.addWorkflow(workflow2);
    }

    @Test
    public void getWorkflowThatDoesNotExistTest() throws Exception {
        Optional<Workflow> result = service.getWorkflow(WORKFLOW1_ID);
        assertFalse(result.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void getMissingWorkflowTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW1_ID, context);

        service.getWorkflow(WORKFLOW1_ID);
    }

    @Test
    public void getWorkflowTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        Optional<Workflow> result = service.getWorkflow(WORKFLOW2_ID);
        assertTrue(result.isPresent());
        Workflow workflow = result.get();
        assertEquals(WORKFLOW2_ID, workflow.getResource());
        workflow2.getModel().forEach(statement -> assertTrue(workflow.getModel().contains(statement.getSubject(), statement.getPredicate(), statement.getObject())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void startWorkflowThatDoesNotExistTest() throws Exception {
        service.startWorkflow(WORKFLOW1_ID);
    }

    @Test
    public void startDeployedWorkflowTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        service.startWorkflow(WORKFLOW2_ID);
        verify(context).start();
    }

    @Test
    public void startFailedWorkflowTest() throws Exception {
        // Setup:
        service.failedWorkflows.add(WORKFLOW1_ID);
        try (RepositoryConnection conn = repo.getConnection()) {
            conn.add(workflow1.getModel(), WORKFLOW1_ID);
        }

        service.startWorkflow(WORKFLOW1_ID);
        verify(converterService).convert(any(Workflow.class), any(CamelContext.class));
        verify(context).addRoutes(routeBuilder);
        assertFalse(service.failedWorkflows.contains(WORKFLOW1_ID));
        assertTrue(service.workflowMap.containsKey(WORKFLOW1_ID));
    }

    @Test
    public void deactivateTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        service.deactivate();
        verify(context).remove();
    }

    @Test(expected = IllegalArgumentException.class)
    public void stopWorkflowThatDoesNotExistTest() throws Exception {
        service.stopWorkflow(WORKFLOW1_ID);
    }

    @Test
    public void stopWorkflowTest() throws Exception {
        // Setup:
        service.workflowMap.put(WORKFLOW2_ID, context);

        service.stopWorkflow(WORKFLOW2_ID);
        verify(context).stop();
    }
}
