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
describe('Annotation Tab directive', function() {
    var $compile,
        scope,
        element,
        annotations = [{ localName: 'prop1' }, { localName: 'prop2' }];

    injectBeautifyFilter();

    beforeEach(function() {
        module('templates');
        module('annotationTab');

        module(function($provide) {
            $provide.value('showAnnotationsFilter', jasmine.createSpy('showAnnotationsFilter').and.callFake(function(obj, arr) {
                return obj ? annotations : [];
            }));
        });

        inject(function(_$compile_, _$rootScope_) {
            $compile = _$compile_;
            scope = _$rootScope_;
        });
    });

    describe('replaces the element with the correct html', function() {
        beforeEach(function() {
            scope.vm = {
                getItemIri: jasmine.createSpy('getItemIri').and.callFake(function(key) { return key.localName; }),
                getAnnotationLocalNameLowercase: jasmine.createSpy('getAnnotationLocalNameLowercase').and.callFake(function(key) { return key.localName; }),
                selected: {
                    'prop1': [{'@id': 'value1'}],
                    'prop2': [{'@value': 'value2'}]
                },
                ontology: {
                    matonto: {
                        annotations: [
                            'prop1'
                        ]
                    }
                }
            }
            element = $compile(angular.element('<annotation-tab></annotation-tab>'))(scope);
            scope.$digest();
        });
        it('for a DIV', function() {
            expect(element.prop('tagName')).toBe('DIV');
        });
        it('based on annotation button', function() {
            var icon = element.querySelectorAll('.fa-plus');
            expect(icon.length).toBe(1);
        });
        it('based on listed annotations', function() {
            var formList = element.querySelectorAll('.annotation');
            expect(formList.length).toBe(2);

            scope.vm = {};
            scope.$digest();
            formList = element.querySelectorAll('.annotation');
            expect(formList.length).toBe(0);
        });
        it('based on values', function() {
            var values = element.querySelectorAll('.value-container');
            expect(values.length).toBe(2);
        });
        it('based on buttons', function() {
            var editButtons = element.querySelectorAll('[title=Edit]');
            expect(editButtons.length).toBe(1);
            var deleteButtons = element.querySelectorAll('[title=Delete]');
            expect(deleteButtons.length).toBe(2);
        });
    });
    describe('user interactions', function() {
        beforeEach(function() {
            scope.vm = {
                editClicked: jasmine.createSpy('editClicked'),
                openRemoveAnnotationOverlay: jasmine.createSpy('openRemoveAnnotationOverlay'),
                openAddAnnotationOverlay: jasmine.createSpy('openAddAnnotationOverlay'),
                getItemIri: jasmine.createSpy('getItemIri').and.callFake(function(key) { return key.localName; }),
                getAnnotationLocalNameLowercase: jasmine.createSpy('getAnnotationLocalNameLowercase').and.callFake(function(key) { return key.localName; }),
                selected: {
                    'prop1': [{'@value': 'value1'}]
                },
                ontology: {
                    matonto: {
                        annotations: ['prop1']
                    }
                }
            }
            element = $compile(angular.element('<annotation-tab></annotation-tab>'))(scope);
            scope.$digest();
        });
        it('should call vm.openAddAnnotationOverlay when add button is clicked', function() {
            var buttonContainer = element.querySelectorAll('.btn-container');
            var addButtons = buttonContainer.querySelectorAll('.btn-link');
            expect(addButtons.length).toBe(1);
            angular.element(addButtons[0]).triggerHandler('click');
            expect(scope.vm.openAddAnnotationOverlay).toHaveBeenCalled();
        });
        it('should call vm.editClicked when edit button is clicked', function() {
            var editButtons = element.querySelectorAll('[title=Edit]');
            expect(editButtons.length).toBe(1);
            angular.element(editButtons[0]).triggerHandler('click');
            expect(scope.vm.editClicked).toHaveBeenCalled();
        });
        it('should call vm.openRemoveAnnotationOverlay when remove button is clicked', function() {
            var deleteButtons = element.querySelectorAll('[title=Delete]');
            expect(deleteButtons.length).toBe(1);
            angular.element(deleteButtons[0]).triggerHandler('click');
            expect(scope.vm.openRemoveAnnotationOverlay).toHaveBeenCalled();
        });
    });
});