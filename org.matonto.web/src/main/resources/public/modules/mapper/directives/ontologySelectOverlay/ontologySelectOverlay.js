/*-
 * #%L
 * org.matonto.web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
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
        .module('ontologySelectOverlay', ['ontologyManager'])
        .directive('ontologySelectOverlay', ontologySelectOverlay);

        ontologySelectOverlay.$inject = ['$filter', '$q', 'ontologyManagerService'];

        function ontologySelectOverlay($filter, $q, ontologyManagerService) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {
                    onClickBack: '&',
                    onClickContinue: '&'
                },
                bindToController: {
                    ontology: '='
                },
                controller: function() {
                    var dvm = this;
                    var ontologyObjs = angular.copy(ontologyManagerService.getList());
                    if (dvm.ontology) {
                        ontologyObjs = _.union(ontologyObjs, [dvm.ontology]);
                    }
                    dvm.ontologyIds = _.uniq(_.concat(ontologyManagerService.getOntologyIds(), _.map(ontologyObjs, '@id')));
                    dvm.selectedOntology = undefined;
                    
                    dvm.isOpen = function(ontologyId) {
                        return _.findIndex(ontologyObjs, {'@id': ontologyId}) >= 0;
                    }
                    dvm.getOntology = function(ontologyId) {
                        var deferred = $q.defer();
                        var ontology = _.find(ontologyObjs, {'@id': ontologyId});
                        if (!ontology) {
                            ontologyManagerService.getThenRestructure(ontologyId).then(function(response) {
                                ontologyObjs.push(response);
                                deferred.resolve(response);
                            });
                        } else {
                            deferred.resolve(ontology);
                        }
                        deferred.promise.then(function(response) {
                            dvm.selectedOntology = response;
                        });
                    }
                    dvm.getName = function(ontologyId) {
                        var ontology = _.find(ontologyObjs, {'@id': ontologyId});
                        if (ontology) {
                            return ontologyManagerService.getEntityName(ontology);                            
                        } else {
                            return ontologyManagerService.getBeautifulIRI(ontologyId);
                        }
                    }
                },
                templateUrl: 'modules/mapper/directives/ontologySelectOverlay/ontologySelectOverlay.html'
            }
        }
})();
