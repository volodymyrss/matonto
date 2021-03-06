package com.mobi.catalog.rest.impl;

/*-
 * #%L
 * com.mobi.catalog.rest
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

import static com.mobi.rest.util.RestUtils.checkStringParam;
import static com.mobi.rest.util.RestUtils.createIRI;
import static com.mobi.rest.util.RestUtils.getActiveUser;
import static com.mobi.rest.util.RestUtils.getObjectFromJsonld;
import static com.mobi.rest.util.RestUtils.getRDFFormat;
import static com.mobi.rest.util.RestUtils.groupedModelToString;
import static com.mobi.rest.util.RestUtils.jsonldToModel;
import static com.mobi.rest.util.RestUtils.modelToJsonld;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.mobi.catalog.api.CatalogManager;
import com.mobi.catalog.api.mergerequest.MergeRequestConfig;
import com.mobi.catalog.api.mergerequest.MergeRequestFilterParams;
import com.mobi.catalog.api.mergerequest.MergeRequestManager;
import com.mobi.catalog.api.ontologies.mcat.Catalog;
import com.mobi.catalog.api.ontologies.mergerequests.MergeRequest;
import com.mobi.catalog.api.ontologies.mergerequests.MergeRequestFactory;
import com.mobi.catalog.rest.MergeRequestRest;
import com.mobi.exception.MobiException;
import com.mobi.jaas.api.engines.EngineManager;
import com.mobi.jaas.api.ontologies.usermanagement.User;
import com.mobi.ontologies.dcterms._Thing;
import com.mobi.persistence.utils.api.SesameTransformer;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.repository.api.Repository;
import com.mobi.repository.api.RepositoryConnection;
import com.mobi.repository.api.RepositoryManager;
import com.mobi.rest.util.ErrorUtils;
import com.mobi.rest.util.RestUtils;
import net.sf.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

@Component(immediate = true)
public class MergeRequestRestImpl implements MergeRequestRest {

    private MergeRequestManager manager;
    private CatalogManager catalogManager;
    private RepositoryManager repositoryManager;
    private SesameTransformer transformer;
    private EngineManager engineManager;
    private MergeRequestFactory mergeRequestFactory;
    private ValueFactory vf;

    @Reference
    void setManager(MergeRequestManager manager) {
        this.manager = manager;
    }

    @Reference
    void setCatalogManager(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    @Reference
    void setRepositoryManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    @Reference
    void setTransformer(SesameTransformer transformer) {
        this.transformer = transformer;
    }

    @Reference
    void setEngineManager(EngineManager engineManager) {
        this.engineManager = engineManager;
    }

    @Reference
    void setMergeRequestFactory(MergeRequestFactory mergeRequestFactory) {
        this.mergeRequestFactory = mergeRequestFactory;
    }

    @Reference
    void setVf(ValueFactory vf) {
        this.vf = vf;
    }

    @Override
    public Response getMergeRequests(String sort, boolean asc, boolean accepted) {
        MergeRequestFilterParams.Builder builder = new MergeRequestFilterParams.Builder();
        if (!StringUtils.isEmpty(sort)) {
            builder.setSortBy(createIRI(sort, vf));
        }
        builder.setAscending(asc).setAccepted(accepted);
        try (RepositoryConnection conn = getCatalogRepo().getConnection()) {
            JSONArray result = JSONArray.fromObject(manager.getMergeRequests(builder.build(), conn).stream()
                    .map(request -> modelToJsonld(request.getModel(), transformer))
                    .map(RestUtils::getObjectFromJsonld)
                    .collect(Collectors.toList()));
            return Response.ok(result).build();
        } catch (IllegalStateException | MobiException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response createMergeRequests(ContainerRequestContext context, String title, String description,
                                        String recordId, String sourceBranchId, String targetBranchId,
                                        List<FormDataBodyPart> assignees) {

        checkStringParam(title, "Merge Request title is required");
        checkStringParam(recordId, "Merge Request record is required");
        checkStringParam(sourceBranchId, "Merge Request source branch is required");
        checkStringParam(targetBranchId, "Merge Request target branch is required");
        User activeUser = getActiveUser(context, engineManager);
        MergeRequestConfig.Builder builder = new MergeRequestConfig.Builder(title, createIRI(recordId, vf),
                createIRI(sourceBranchId, vf), createIRI(targetBranchId, vf), activeUser);
        if (!StringUtils.isBlank(description)) {
            builder.description(description);
        }
        if (assignees != null ) {
            assignees.forEach(part -> {
                String username = part.getValue();
                Optional<User> assignee = engineManager.retrieveUser(username);
                if (!assignee.isPresent()) {
                    throw ErrorUtils.sendError("User " + username + " does not exist", Response.Status.BAD_REQUEST);
                }
                builder.addAssignee(assignee.get());
            });
        }
        try (RepositoryConnection conn = getCatalogRepo().getConnection()) {
            MergeRequest request = manager.createMergeRequest(builder.build(), catalogManager.getLocalCatalogIRI(),
                    conn);
            manager.addMergeRequest(request, conn);
            return Response.status(201).entity(request.getResource().stringValue()).build();
        } catch (IllegalArgumentException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException | MobiException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getMergeRequest(String requestId) {
        Resource requestIdResource = createIRI(requestId, vf);
        try (RepositoryConnection conn = getCatalogRepo().getConnection()) {
            MergeRequest request = manager.getMergeRequest(requestIdResource, conn).orElseThrow(() ->
                    ErrorUtils.sendError("Merge Request " + requestId + " could not be found",
                            Response.Status.NOT_FOUND));
            String json = groupedModelToString(request.getModel(), getRDFFormat("jsonld"), transformer);
            return Response.ok(getObjectFromJsonld(json)).build();
        } catch (IllegalStateException | MobiException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response updateMergeRequest(String requestId, String newMergeRequest) {
        Resource requestIdResource = createIRI(requestId, vf);
        try (RepositoryConnection conn = getCatalogRepo().getConnection()) {
            manager.updateMergeRequest(requestIdResource, jsonToMergeRequest(requestIdResource, newMergeRequest), conn);
            return Response.ok().build();
        } catch (IllegalStateException | MobiException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response deleteMergeRequest(String requestId) {
        Resource requestIdResource = createIRI(requestId, vf);
        try (RepositoryConnection conn = getCatalogRepo().getConnection()) {
            manager.deleteMergeRequest(requestIdResource, conn);
            return Response.ok().build();
        } catch (IllegalArgumentException ex) {
            throw ErrorUtils.sendError(ex,"Merge Request " + requestId + " could not be found",
                    Response.Status.NOT_FOUND);
        } catch (IllegalStateException | MobiException ex) {
            throw ErrorUtils.sendError(ex, ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private MergeRequest jsonToMergeRequest(Resource requestId, String jsonMergeRequest) {
        Model mergeReqModel = jsonldToModel(jsonMergeRequest, transformer);
        return mergeRequestFactory.getExisting(requestId, mergeReqModel).orElseThrow(() ->
                ErrorUtils.sendError("MergeRequest IDs must match", Response.Status.BAD_REQUEST));
    }

    private Repository getCatalogRepo() {
        return repositoryManager.getRepository(catalogManager.getRepositoryId()).orElseThrow(() ->
                new IllegalStateException("Cannot retrieve Catalog repository"));
    }
}
