/*-
 * #%L
 * org.matonto.web
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
(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name explore
         *
         * @description
         * The `explore` module only provides the `exploreService` service which provides access
         * to the MatOnto explorable-datasets REST endpoints.
         */
        .module('explore', [])
        /**
         * @ngdoc service
         * @name explore.service:exploreService
         * @requires $http
         * @requires $q
         * @requires $httpParamSerializer
         * @requires util.service:utilService
         *
         * @description
         * `exploreService` is a service that provides access to the MatOnto explorable-datasets REST
         * endpoints.
         */
        .service('exploreService', exploreService);
    
    exploreService.$inject = ['$http', '$q', '$httpParamSerializer', 'utilService'];
    
    function exploreService($http, $q, $httpParamSerializer, utilService) {
        var self = this;
        var prefix = '/matontorest/explorable-datasets/';
        var util = utilService;
        
        /**
         * @ngdoc method
         * @name getClassDetails
         * @methodOf explore.service:exploreService
         *
         * @description
         * Calls the GET /matontorest/explorable-datasets/{recordId}/class-details endpoint and returns the
         * array of class details.
         *
         * @returns {Promise} A promise that resolves to an array of the class details for the identified dataset record.
         */
        self.getClassDetails = function(recordId) {
            return $http.get(prefix + encodeURIComponent(recordId) + '/class-details')
                .then(response => $q.when(response.data), response => $q.reject(response.statusText));
        }
        
        /**
         * @ngdoc method
         * @name getClassInstanceDetails
         * @methodOf explore.service:exploreService
         *
         * @description
         * Calls the GET /matontorest/explorable-datasets/{recordId}/classes/{classId}/instance-details endpoint and returns the
         * array of instance details.
         *
         * @returns {Promise} A promise that resolves to an array of the instance details for the identified class of the
         * identified dataset record.
         */
        self.getClassInstanceDetails = function(recordId, classId) {
            // var params = $httpParamSerializer({classId});
            // return $http.get(prefix + encodeURIComponent(recordId) + '/instances?' + params)
            //     .then($q.when, response => $q.reject(response.statusText));
            return $q.when({
                data: [{
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Hydrogen Peroxide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Silicon Carbide',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }, {
                        label: 'Sodium Chloride',
                        overview: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas mollis purus quis dui varius, sed malesuada est auctor. Vestibulum vitae maximus metus. Curabitur magna nibh, fermentum vitae tincidunt in, luctus ut augue.'
                    }],
                headers: () => {
                    return {
                        'x-total-count': 5000,
                        link: '<http://matonto.org/next>; rel="next"'
                    }
                }
            });
        }
        
        /**
         * @ngdoc method
         * @name createPagedResultsObject
         * @methodOf explore.service:exploreService
         *
         * @description
         * Creates an object which contains all of the paginated details from the provided response in the expected format.
         *
         * @param {Object} response The response of an $http call which should contain paginated details in the header.
         * @returns {Object} An object which contains all of the paginated details in the expected format.
         */
        self.createPagedResultsObject = function(response) {
            var object = {};
            _.set(object, 'data', response.data);
            var headers = response.headers();
            _.set(object, 'total', _.get(headers, 'x-total-count', 0));
            var links = util.parseLinks(_.get(headers, 'link', {}));
            _.set(object, 'links.next', _.get(links, 'next', ''));
            _.set(object, 'links.prev', _.get(links, 'prev', ''));
            return object;
        }
    }
})();