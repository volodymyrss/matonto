/*-
 * #%L
 * com.mobi.web
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
        .module('conceptSchemesTab', [])
        .directive('conceptSchemesTab', conceptSchemesTab);

        conceptSchemesTab.$inject = ['ontologyStateService', 'ontologyManagerService', 'propertyManagerService'];

        function conceptSchemesTab(ontologyStateService, ontologyManagerService, propertyManagerService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'modules/ontology-editor/directives/conceptSchemesTab/conceptSchemesTab.html',
                scope: {},
                controllerAs: 'dvm',
                controller: ['$scope', function($scope) {
                    var dvm = this;
                    var pm = propertyManagerService;
                    var om = ontologyManagerService;
                    var os = ontologyStateService;
                    dvm.relationshipList = [];

                    $scope.$watch(() => os.listItem.selected, function(newValue) {
                        if (om.isConcept(os.listItem.selected, os.listItem.derivedConcepts)) {
                            var schemeRelationships = _.filter(pm.conceptSchemeRelationshipList, iri => _.includes(os.listItem.iriList, iri));
                            dvm.relationshipList = _.concat(os.listItem.derivedSemanticRelations, schemeRelationships);
                        } else if (om.isConceptScheme(os.listItem.selected, os.listItem.derivedConceptSchemes)) {
                            dvm.relationshipList = pm.schemeRelationshipList;
                        }
                    });
                }]
            }
        }
})();
