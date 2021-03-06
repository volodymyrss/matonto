/*-
 * #%L
 * com.mobi.web
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
         * @name instanceBlock
         *
         * @description
         * The `instanceBlock` module only provides the `instanceBlock` directive which creates
         * the instance block which shows the users instance cards associated with the selected
         * class.
         */
        .module('instanceBlock', [])
        /**
         * @ngdoc directive
         * @name instanceBlock.directive:instanceBlock
         * @scope
         * @restrict E
         * @requires $http
         * @requires $filter
         * @requires discoverState.service:discoverStateService
         * @requires explore.service:exploreService
         * @requires util.service:utilService
         * @requires uuid
         *
         * @description
         * HTML contents in the instance block which shows the users the instances associated
         * with the class they have selected. They have a bread crumb trail to get back to early
         * pages and pagination controls at the bottom of the page.
         */
        .directive('instanceBlock', instanceBlock);

        instanceBlock.$inject = ['$http', '$filter', 'discoverStateService', 'exploreService', 'utilService', 'uuid'];

        function instanceBlock($http, $filter, discoverStateService, exploreService, utilService, uuid) {
            return {
                restrict: 'E',
                templateUrl: 'modules/discover/sub-modules/explore/directives/instanceBlock/instanceBlock.html',
                replace: true,
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    var es = exploreService;
                    var util = utilService;
                    dvm.ds = discoverStateService;

                    dvm.getPage = function(direction) {
                        var url = (direction === 'next') ? dvm.ds.explore.instanceDetails.links.next : dvm.ds.explore.instanceDetails.links.prev;
                        $http.get(url)
                            .then(response => {
                                dvm.ds.explore.instanceDetails.data = [];
                                _.merge(dvm.ds.explore.instanceDetails, es.createPagedResultsObject(response));
                                if (direction === 'next') {
                                    dvm.ds.explore.instanceDetails.currentPage += 1;
                                } else {
                                    dvm.ds.explore.instanceDetails.currentPage -= 1;
                                }
                            }, response => {
                                util.createErrorToast(response.statusText);
                            });
                    }

                    dvm.create = function() {
                        dvm.ds.explore.creating = true;
                        var split = $filter('splitIRI')(_.head(dvm.ds.explore.instanceDetails.data).instanceIRI);
                        var iri = split.begin + split.then + uuid.v4();
                        dvm.ds.explore.instance.entity = [{
                            '@id': iri,
                            '@type': [dvm.ds.explore.classId]
                        }];
                        dvm.ds.explore.instance.metadata.instanceIRI = iri;
                        dvm.ds.explore.breadcrumbs.push('New Instance');
                    }

                    dvm.getClassName = function() {
                        return _.last(dvm.ds.explore.breadcrumbs);
                    }
                }
            }
        }
})();