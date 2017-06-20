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
describe('Characteristics Block directive', function() {
    var $compile, scope, element, ontologyStateSvc, $filter, controller, prefixes, ontologyManagerSvc, functionalProperty, asymmetricProperty, ontoUtils;

    beforeEach(function() {
        module('templates');
        module('characteristicsBlock');
        mockPrefixes();
        mockOntologyState();
        mockOntologyUtilsManager();

        inject(function(_$compile_, _$rootScope_, _ontologyStateService_, _prefixes_, _ontologyUtilsManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyStateSvc = _ontologyStateService_;
            prefixes = _prefixes_;
            ontoUtils = _ontologyUtilsManagerService_;
        });

        functionalProperty = prefixes.owl + 'FunctionalProperty';
        asymmetricProperty = prefixes.owl + 'AsymmetricProperty';
        ontologyStateSvc.selected = undefined;
        element = $compile(angular.element('<characteristics-block></characteristics-block>'))(scope);
        scope.$digest();
        controller = element.controller('characteristicsBlock');
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.prop('tagName')).toBe('DIV');
            expect(element.hasClass('characteristics-block')).toBe(true);
        });
        _.forEach(['block', 'block-header', 'block-content'], function(tag) {
            it('with a ' + tag, function() {
                expect(element.find(tag).length).toBe(1);
            });
        });
        it('with checkboxes', function() {
            expect(element.find('checkbox').length).toBe(0);
            ontologyStateSvc.selected = true;
            scope.$apply();
            expect(element.find('checkbox').length).toBe(2);
        });
    });
    describe('controller methods', function() {
        var statement;
        var characteristicObj;
        var id = 'id';
        beforeEach(function() {
            statement = {
                '@id': id,
                '@type': [functionalProperty]
            };
            characteristicObj = {
                checked: true,
                typeIRI: functionalProperty
            };
            ontologyStateSvc.selected = {'@id': id};
        });
        describe('onChange sets all variables correctly when characteristic', function() {
            it('is checked and no match in deletions', function() {
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(true);
                expect(ontologyStateSvc.addToAdditions).toHaveBeenCalledWith(ontologyStateSvc.listItem.recordId, statement);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
            it('is checked and the statement is in deletions', function() {
                ontologyStateSvc.listItem.deletions = [angular.copy(statement)];
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(true);
                expect(ontologyStateSvc.addToAdditions).not.toHaveBeenCalled();
                expect(ontologyStateSvc.listItem.deletions.length).toBe(0);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
            it('is checked and the statement with another property is in deletions', function() {
                var object = angular.copy(statement);
                object.other = 'value';
                ontologyStateSvc.listItem.deletions = [object];
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(true);
                expect(ontologyStateSvc.addToAdditions).not.toHaveBeenCalled();
                expect(_.some(ontologyStateSvc.listItem.deletions, {'@id': id, other: 'value'})).toBe(true);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
            it('is not checked and no match in additions', function() {
                ontologyStateSvc.selected = angular.copy(statement);
                characteristicObj.checked = false;
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(false);
                expect(ontologyStateSvc.addToDeletions).toHaveBeenCalledWith(ontologyStateSvc.listItem.recordId, statement);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
            it('is not checked and the statement is in additions', function() {
                ontologyStateSvc.listItem.additions = [angular.copy(statement)];
                ontologyStateSvc.selected = angular.copy(statement);
                characteristicObj.checked = false;
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(false);
                expect(ontologyStateSvc.addToDeletions).not.toHaveBeenCalled();
                expect(ontologyStateSvc.listItem.additions.length).toBe(0);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
            it('is not checked and the statement is in additions', function() {
                var object = angular.copy(statement);
                object.other = 'value';
                ontologyStateSvc.listItem.additions = [object];
                ontologyStateSvc.selected = angular.copy(statement);
                characteristicObj.checked = false;
                controller.onChange(characteristicObj);
                expect(_.includes(_.get(ontologyStateSvc.selected, '@type', []), functionalProperty)).toBe(false);
                expect(ontologyStateSvc.addToDeletions).not.toHaveBeenCalled();
                expect(_.some(ontologyStateSvc.listItem.additions, {'@id': id, other: 'value'})).toBe(true);
                expect(ontoUtils.saveCurrentChanges).toHaveBeenCalled();
            });
        });
    });
    it('correctly updates the checkboxes when the selected entities changes', function() {
        controller.characteristics.functional.checked = false;
        controller.characteristics.asymmetric.checked = false;
        spyOn(controller, 'onChange');
        ontologyStateSvc.selected = {'@type': [functionalProperty, asymmetricProperty]};
        scope.$digest();
        expect(controller.onChange).not.toHaveBeenCalled();
        expect(controller.characteristics.functional.checked).toBe(true);
        expect(controller.characteristics.asymmetric.checked).toBe(true);
    });
});
