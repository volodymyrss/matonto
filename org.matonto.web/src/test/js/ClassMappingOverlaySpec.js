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
describe('Class Mapping Overlay directive', function() {
    var $compile,
        scope,
        element,
        controller,
        mappingManagerSvc,
        mapperStateSvc;

    beforeEach(function() {
        module('templates');
        module('classMappingOverlay');
        mockMappingManager();
        mockMapperState();

        inject(function(_$compile_, _$rootScope_, _mappingManagerService_, _mapperStateService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            mapperStateSvc = _mapperStateService_;
            mappingManagerSvc = _mappingManagerService_;
        });

        mapperStateSvc.mapping = {jsonld: []};
        element = $compile(angular.element('<class-mapping-overlay></class-mapping-overlay>'))(scope);
        scope.$digest();
    });

    describe('controller methods', function() {
        beforeEach(function() {
            controller = element.controller('classMappingOverlay');
        });
        it('should add a class mapping', function() {
            var classMapping = {'@id': 'classMapping'};
            controller.selectedClass = {ontologyId: '', classObj: {'@id': ''}};
            mapperStateSvc.availableClasses = [controller.selectedClass];
            mapperStateSvc.sourceOntologies = [{id: controller.selectedClass.ontologyId, entities: []}];
            mappingManagerSvc.addClass.and.returnValue(classMapping);
            controller.addClass();
            expect(mappingManagerSvc.addClass).toHaveBeenCalledWith(mapperStateSvc.mapping.jsonld, jasmine.any(Array), controller.selectedClass.classObj['@id']);
            expect(mapperStateSvc.setAvailableProps).toHaveBeenCalledWith(classMapping['@id']);
            expect(mapperStateSvc.availableClasses).not.toContain(controller.selectedClass);
            expect(mapperStateSvc.changedMapping).toBe(true);
            expect(mapperStateSvc.resetEdit).toHaveBeenCalled();
            expect(mapperStateSvc.selectedClassMappingId).toBe(classMapping['@id']);
            expect(mapperStateSvc.displayClassMappingOverlay).toBe(false);
        });
        it('should set the correct state for canceling', function() {
            controller.cancel();
            expect(mapperStateSvc.displayClassMappingOverlay).toBe(false);
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.hasClass('class-mapping-overlay')).toBe(true);
            expect(element.querySelectorAll('form.content').length).toBe(1);
        });
        it('with a class select', function() {
            expect(element.find('class-select').length).toBe(1);
        });
        it('with buttons to cancel and submit', function() {
            var buttons = element.querySelectorAll('.btn-container button');
            expect(buttons.length).toBe(2);
            expect(['Cancel', 'Submit']).toContain(angular.element(buttons[0]).text().trim());
            expect(['Cancel', 'Submit']).toContain(angular.element(buttons[1]).text().trim());
        });
        it('depending on whether a class is selected', function() {
            var button = angular.element(element.querySelectorAll('.btn-container button.btn-primary')[0]);
            expect(button.attr('disabled')).toBeTruthy();
            expect(element.find('class-preview').length).toBe(0);

            controller = element.controller('classMappingOverlay');
            controller.selectedClass = {};
            scope.$digest();
            expect(button.attr('disabled')).toBeFalsy();
            expect(element.find('class-preview').length).toBe(1);
        });
    });
    it('should call addClass when the button is clicked', function() {
        controller = element.controller('classMappingOverlay');
        spyOn(controller, 'addClass');

        var continueButton = angular.element(element.querySelectorAll('.btn-container button.btn-primary')[0]);
        continueButton.triggerHandler('click');
        expect(controller.addClass).toHaveBeenCalled();
    });
    it('should call cancel when the button is clicked', function() {
        controller = element.controller('classMappingOverlay');
        spyOn(controller, 'cancel');

        var continueButton = angular.element(element.querySelectorAll('.btn-container button.btn-default')[0]);
        continueButton.triggerHandler('click');
        expect(controller.cancel).toHaveBeenCalled();
    });
});