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
        .module('createIndividualOverlay', [])
        .directive('createIndividualOverlay', createIndividualOverlay);

        createIndividualOverlay.$inject = ['$filter', 'ontologyStateService', 'prefixes', 'ontologyUtilsManagerService'];

        function createIndividualOverlay($filter, ontologyStateService, prefixes, ontologyUtilsManagerService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'modules/ontology-editor/directives/createIndividualOverlay/createIndividualOverlay.html',
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    dvm.os = ontologyStateService;
                    dvm.ontoUtils = ontologyUtilsManagerService;
                    dvm.prefix = dvm.os.getDefaultPrefix();
                    dvm.classes = _.keys(dvm.os.listItem.classes.iris);

                    dvm.individual = {
                        '@id': dvm.prefix,
                        '@type': []
                    };

                    dvm.nameChanged = function() {
                        if (!dvm.iriHasChanged) {
                            dvm.individual['@id'] = dvm.prefix + $filter('camelCase')(dvm.name, 'class');
                        }
                    }
                    dvm.onEdit = function(iriBegin, iriThen, iriEnd) {
                        dvm.iriHasChanged = true;
                        dvm.individual['@id'] = iriBegin + iriThen + iriEnd;
                        dvm.os.setCommonIriParts(iriBegin, iriThen);
                    }
                    dvm.getClassOntologyIri = function(iri) {
                        return _.get(dvm.os.listItem.classes.iris, "['" + iri + "']", dvm.os.listItem.ontologyId);
                    }
                    dvm.create = function() {
                        // update relevant lists
                        dvm.ontoUtils.addIndividual(dvm.individual);
                        // add the entity to the ontology
                        dvm.individual['@type'].push(prefixes.owl + 'NamedIndividual');
                        dvm.os.addEntity(dvm.os.listItem, dvm.individual);
                        dvm.os.addToAdditions(dvm.os.listItem.ontologyRecord.recordId, dvm.individual);
                        // select the new individual
                        dvm.os.selectItem(dvm.individual['@id'], false);
                        // add to concept hierarchy if an instance of a derived concept
                        if (dvm.ontoUtils.containsDerivedConcept(dvm.individual['@type'])) {
                            dvm.ontoUtils.addConcept(dvm.individual);
                        } else if (dvm.ontoUtils.containsDerivedConceptScheme(dvm.individual['@type'])) {
                            dvm.ontoUtils.addConceptScheme(dvm.individual);
                        }
                        // hide the overlay
                        dvm.os.showCreateIndividualOverlay = false;
                        dvm.ontoUtils.saveCurrentChanges();
                    }
                }
            }
        }
})();
