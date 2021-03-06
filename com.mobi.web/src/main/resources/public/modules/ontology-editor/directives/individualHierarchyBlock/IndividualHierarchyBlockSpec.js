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
describe('Individual Hierarchy directive', function() {
    var $compile, scope, ontologyStateSvc, ontologyUtilsManagerSvc;

    beforeEach(function() {
        module('templates');
        module('individualHierarchyBlock');
        mockOntologyState();
        mockOntologyUtilsManager();

        inject(function(_$compile_, _$rootScope_, _ontologyStateService_, _ontologyUtilsManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyStateSvc = _ontologyStateService_;
            ontologyUtilsManagerSvc = _ontologyUtilsManagerService_;
        });

        this.element = $compile(angular.element('<individual-hierarchy-block></individual-hierarchy-block>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('individualHierarchyBlock');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        ontologyStateSvc = null;
        ontologyUtilsManagerSvc = null;
        this.element.remove();
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toBe('DIV');
            expect(this.element.hasClass('individual-hierarchy-block')).toBe(true);
        });
        it('with a block', function() {
            expect(this.element.find('block').length).toBe(1);
        });
        it('with a block-header', function() {
            expect(this.element.find('block-header').length).toBe(1);
        });
        it('with a block-content', function() {
            expect(this.element.find('block-content').length).toBe(1);
        });
        it('with a individual-tree', function() {
            expect(this.element.find('individual-tree').length).toBe(1);
        });
        it('with a block-footer', function() {
            expect(this.element.find('block-footer').length).toBe(1);
        });
        it('with a button to delete an individual', function() {
            var button = this.element.querySelectorAll('block-footer button');
            expect(button.length).toBe(1);
            expect(angular.element(button[0]).text()).toContain('Delete Individual');
        });
        it('based on whether a delete should be confirmed', function() {
            expect(this.element.find('confirmation-overlay').length).toBe(0);

            this.controller.showDeleteConfirmation = true;
            scope.$digest();
            expect(this.element.find('confirmation-overlay').length).toBe(1);
        });
        it('based on whether something is selected', function() {
            var button = angular.element(this.element.querySelectorAll('block-footer button')[0]);
            expect(button.attr('disabled')).toBeFalsy();

            ontologyStateSvc.listItem.selected = undefined;
            scope.$digest();
            expect(button.attr('disabled')).toBeTruthy();
        });
    });
    describe('controller methods', function() {
        it('should delete an individual', function() {
            this.controller.deleteIndividual();
            expect(ontologyUtilsManagerSvc.deleteIndividual).toHaveBeenCalled();
            expect(this.controller.showDeleteConfirmation).toBe(false);
        });
    });
    it('should set the correct state when the create individual link is clicked', function() {
        var link = angular.element(this.element.querySelectorAll('block-header a')[0]);
        link.triggerHandler('click');
        expect(ontologyStateSvc.showCreateIndividualOverlay).toBe(true);
    });
    it('should set the correct state when the delete class button is clicked', function() {
        var button = angular.element(this.element.querySelectorAll('block-footer button')[0]);
        button.triggerHandler('click');
        expect(this.controller.showDeleteConfirmation).toBe(true);
    });
});