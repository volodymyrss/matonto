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
        /**
         * @ngdoc overview
         * @name mappingNameOverlay
         *
         * @description
         * The `mappingNameOverlay` module only provides the `mappingNameOverlay` directive which creates
         * an overlay with a {@link mappingNameInput.directive:mappingNameInput mappingNameInput} to edit a mapping name.
         */
        .module('mappingNameOverlay', [])
        /**
         * @ngdoc directive
         * @name mappingNameOverlay.directive:mappingNameOverlay
         * @scope
         * @restrict E
         * @requires  mappingManager.service:mappingManagerService
         * @requires  mapperState.service:mapperStateService
         *
         * @description
         * `mappingNameOverlay` is a directive that creates an overlay containing a
         * {@link mappingNameInput.directive:mappingNameInput mappingNameInput} to edit the currently selected
         * {@link mapperState.service:mapperStateService#mapping mapping's} name. The directive is replaced by
         * the contents of its template.
         */
        .directive('mappingNameOverlay', mappingNameOverlay);

        mappingNameOverlay.$inject = ['$filter', 'mappingManagerService', 'mapperStateService']

        function mappingNameOverlay($filter, mappingManagerService, mapperStateService) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {},
                controller: function() {
                    var dvm = this;
                    dvm.state = mapperStateService;
                    dvm.mm = mappingManagerService;
                    dvm.newName = $filter('splitIRI')(_.get(dvm.state.mapping, 'id', '')).end;

                    dvm.set = function() {
                        var iri = dvm.mm.getMappingId(dvm.newName);
                        if (dvm.state.step === dvm.state.editMappingStep) {
                            dvm.state.changedMapping = true;
                        }
                        dvm.state.mapping.jsonld = dvm.mm.renameMapping(dvm.state.mapping.jsonld, iri);
                        dvm.state.removeAvailableProps(dvm.state.selectedClassMappingId);
                        dvm.state.selectedClassMappingId = iri + '/' + $filter('splitIRI')(dvm.state.selectedClassMappingId).end;
                        dvm.state.setAvailableProps(dvm.state.selectedClassMappingId);
                        dvm.state.mapping.id = iri;
                        dvm.state.editMappingName = false;
                    }
                    dvm.cancel = function() {
                        dvm.state.editMappingName = false;
                    }
                },
                templateUrl: 'modules/mapper/directives/mappingNameOverlay/mappingNameOverlay.html'
            }
        }
})();
