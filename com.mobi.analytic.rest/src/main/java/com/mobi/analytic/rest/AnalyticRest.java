package com.mobi.analytic.rest;

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

import com.mobi.analytic.ontologies.analytic.Configuration;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/analytics")
@Api(value = "/analytics")
public interface AnalyticRest {

    /**
     * Returns all the available {@link Configuration} types.
     *
     * @return All the available configuration types.
     */
    @GET
    @Path("configuration-types")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @ApiOperation("Retrieves all the available configuration types.")
    Response getConfigurationTypes();

    /**
     * Creates a new AnalyticRecord and associated Configuration in the repository using the passed form data.
     * Determines the type of the Configuration based on the `type` field. Requires the `title` field to be set.
     * Returns a Response with the IRI of the new AnalyticRecord.
     *
     * @param context     The context of the request.
     * @param typeIRI     The required IRI of the type of the new Configuration. Must be a valid IRI for a
     *                    Configuration or one of its subclasses.
     * @param title       The required title for the new AnalyticRecord.
     * @param description The optional description for the new AnalyticRecord.
     * @param keywords    The optional list of keywords strings for the new AnalyticRecord.
     * @param json        The JSON of key value pairs which will be added to the new Configuration.
     * @return A Response with a JSON object containing the IRI strings of the AnalyticRecord and Configuration.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("user")
    @ApiOperation("Creates a new Analytic Record and Configuration in the Catalog.")
    Response createAnalytic(@Context ContainerRequestContext context,
                            @FormDataParam("type") String typeIRI,
                            @FormDataParam("title") String title,
                            @FormDataParam("description") String description,
                            @FormDataParam("keywords") List<FormDataBodyPart> keywords,
                            @FormDataParam("json") String json);


    /**
     * Gets a specific AnalyticRecord and associated Configuration from the local Catalog.
     *
     * @param analyticRecordId The IRI of a AnalyticRecord.
     * @return A Response indicating the success of the request.
     */
    @GET
    @Path("{analyticRecordId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @ApiOperation("Gets a specific AnalyticRecord and associated Configuration from the local Catalog")
    Response getAnalytic(@PathParam("analyticRecordId") String analyticRecordId);

    /**
     * Updates an Analytic based on the ID using the metadata provided by the parameters.
     *
     * @param analyticRecordId The IRI of a AnalyticRecord.
     * @param typeIRI          The required IRI of the type of the new Configuration. Must be a valid IRI for a
     *                         Configuration or one of its subclasses.
     * @param json             The JSON of key value pairs which will be added to the new Configuration.
     * @return A Response indicating whether or not the Analytic was updated.
     */
    @PUT
    @Path("{analyticRecordId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("user")
    @ApiOperation("Updates the Analytic Record and Configuration using the provided metadata.")
    Response updateAnalytic(@PathParam("analyticRecordId") String analyticRecordId,
                            @FormDataParam("type") String typeIRI,
                            @FormDataParam("json") String json);
}
