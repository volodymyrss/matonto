package com.mobi.federation.hazelcast.config;

/*-
 * #%L
 * com.mobi.federation.hazelcast
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

import com.mobi.federation.hazelcast.HazelcastFederationService;

/**
 * Simple enumeration of possible values for the join mechanism to use in the
 * {@link HazelcastFederationServiceConfig} when creating a {@link HazelcastFederationService}
 * instance.
 */
public enum JoinMechanism {

    /**
     * Federate nodes automatically using hazelcast's multicast functionality.
     */
    MULTICAST,

    /**
     * Federate known nodes using direct TCP/IP connections.
     */
    TCPIP
}
