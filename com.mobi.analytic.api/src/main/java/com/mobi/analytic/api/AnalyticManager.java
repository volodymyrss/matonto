package com.mobi.analytic.api;

/*-
 * #%L
 * com.mobi.analytic.api
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

import com.mobi.analytic.api.builder.AnalyticRecordConfig;
import com.mobi.analytic.ontologies.analytic.Configuration;
import com.mobi.analytic.pagination.AnalyticPaginatedSearchParams;
import com.mobi.analytic.ontologies.analytic.AnalyticRecord;
import com.mobi.catalog.api.PaginatedSearchResults;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.orm.OrmFactory;

import java.util.Optional;

/**
 * A service for managing local analytics within the Mobi platform.
 */
public interface AnalyticManager {

    /**
     * Retrieves AnalyticRecords in the local catalog based on the passed search and pagination parameters. Acceptable
     * sort properties are http://purl.org/dc/terms/title, http://purl.org/dc/terms/modified, and
     * http://purl.org/dc/terms/issued.
     *
     * @param searchParams The AnalyticPaginatedSearchParams indicating which records to return.
     * @return The PaginatedSearchResults of AnalyticRecords in the local catalog. AnalyticRecord does not contain
     * referenced Configuration object.
     */
    PaginatedSearchResults<AnalyticRecord> getAnalyticRecords(AnalyticPaginatedSearchParams searchParams);

    /**
     * Retrieves the AnalyticRecord for an analytic described by the specified AnalyticRecord Resource.
     *
     * @param recordId The Resource of the AnalyticRecord.
     * @return The AnalyticRecord from the local catalog which does not contain referenced Configuration object.
     */
    Optional<AnalyticRecord> getAnalyticRecord(Resource recordId);

    /**
     * Creates an AnalyticRecord and associated Configuration according to the specified configuration.
     *
     * @param config The AnalyticRecordConfig describing the details of the AnalyticRecord to create.
     * @return The AnalyticRecord that has been created in the local catalog.
     */
    AnalyticRecord createAnalytic(AnalyticRecordConfig config);

    /**
     * Deletes the AnalyticRecord and associated Configuration.
     *
     * @param recordId The Resource of the AnalyticRecord to be removed along with associated Configuration.
     */
    void deleteAnalytic(Resource recordId);

    /**
     * Retrieves the Configuration associated with the AnalyticRecord described by the specified AnalyticRecord
     * Resource.
     *
     * @param recordId The Resource of the AnalyticRecord associated with the Configuration you want to get.
     * @param factory  The OrmFactory of the Type of Configuration you want to get.
     * @param <T>      An Object which extends Configuration.
     * @return The Configuration from the local catalog.
     */
    <T extends Configuration> Optional<T> getConfigurationByAnalyticRecord(Resource recordId, OrmFactory<T> factory);

    /**
     * Retrieves the Configuration for a configuration described by the specified Configuration Resource.
     *
     * @param configId The Resource of the Configuration.
     * @param factory  The OrmFactory of the Type of Configuration you want to get.
     * @param <T>      An Object which extends Configuration.
     * @return The Configuration from the local catalog.
     */
    <T extends Configuration> Optional<T> getConfiguration(Resource configId, OrmFactory<T> factory);

    /**
     * Creates a Configuration according to the specified config. NOTE: This does not store the configuration in the
     * local catalog.
     *
     * @param json    The JSON with key value pairs of the details of the Configuration to create.
     * @param factory The OrmFactory for creating the Configuration.
     * @param <T>     An Object which extends Configuration.
     * @return The Configuration that has been created.
     */
    <T extends Configuration> T createConfiguration(String json, OrmFactory<T> factory);

    /**
     * Updates the Configuration with the matching IRI from the provided Configuration.
     *
     * @param recordId         The Resource of the AnalyticRecord.
     * @param newConfiguration The Configuration to replace the existing one in the system repository.
     * @param <T>              An Object which extends Configuration.
     */
    <T extends Configuration> void updateConfiguration(Resource recordId, T newConfiguration);
}
