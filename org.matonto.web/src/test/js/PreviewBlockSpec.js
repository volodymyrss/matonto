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
describe('Preview Block directive', function() {
    var $compile,
        scope,
        $q,
        element,
        controller,
        ontologyStateSvc,
        ontologyManagerSvc,
        ontologyUtilsManagerSvc;

    beforeEach(function() {
        module('templates');
        module('previewBlock');
        mockOntologyState();
        mockOntologyManager();

        inject(function(_$compile_, _$rootScope_, _$q_, _ontologyStateService_, _ontologyManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            $q = _$q_;
            ontologyStateSvc = _ontologyStateService_;
            ontologyManagerSvc = _ontologyManagerService_;
        });

        element = $compile(angular.element('<preview-block></preview-block>'))(scope);
        scope.$digest();
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.prop('tagName')).toBe('DIV');
            expect(element.hasClass('preview-block')).toBe(true);
        });
        it('with a block', function() {
            expect(element.find('block').length).toBe(1);
        });
        it('with a block-header', function() {
            expect(element.find('block-header').length).toBe(1);
        });
        it('with a block-content', function() {
            expect(element.find('block-content').length).toBe(1);
        });
        it('with a .preview-content', function() {
            expect(element.querySelectorAll('.preview-content').length).toBe(1);
        });
        it('with a serialization-select', function() {
            expect(element.find('serialization-select').length).toBe(1);
        });
        it('depending on whether a preview is generated', function() {
            expect(element.find('ui-codemirror').length).toBe(0);

            controller = element.controller('previewBlock');
            controller.activePage = {preview: 'test'};
            scope.$digest();
            expect(element.find('ui-codemirror').length).toBe(1);
        });
        it('depending on whether a serialization whas selected', function() {
            var button = element.find('button');
            expect(button.attr('disabled')).toBeTruthy();

            controller = element.controller('previewBlock');
            controller.activePage = {serialization: 'test'};
            scope.$digest();
            expect(button.attr('disabled')).toBeFalsy();
        });
    });
    describe('controller methods', function() {
        beforeEach(function() {
            controller = element.controller('previewBlock');
        });
        it('should get a preview', function() {
            var tests = [
                {
                    serialization: 'turtle',
                    mode: 'text/turtle'
                },
                {
                    serialization: 'jsonld',
                    mode: 'application/ld+json'
                },
                {
                    serialization: 'rdf/xml',
                    mode: 'application/xml'
                }
            ];
            _.forEach(tests, function(test) {
                controller.activePage = {serialization: test.serialization};
                controller.getPreview();
                scope.$apply();
                expect(controller.activePage.mode).toBe(test.mode);
                expect(ontologyManagerSvc.getPreview).toHaveBeenCalledWith(ontologyStateSvc.listItem.ontologyId, ontologyStateSvc.listItem.recordId, test.serialization);
                expect(controller.activePage.preview).toEqual({});
            });
        });
    });
    it('should call getPreview when the button is clicked', function() {
        controller = element.controller('previewBlock');
        spyOn(controller, 'getPreview');
        var button = element.find('button');
        button.triggerHandler('click');
        expect(controller.getPreview).toHaveBeenCalled();
    });
});