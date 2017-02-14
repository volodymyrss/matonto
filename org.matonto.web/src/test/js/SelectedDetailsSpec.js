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
describe('Selected Details directive', function() {
    var $compile,
        scope,
        element,
        ontologyStateSvc;

    beforeEach(function() {
        module('templates');
        module('selectedDetails');
        mockOntologyManager();
        mockOntologyState();

        inject(function(_$compile_, _$rootScope_, _ontologyStateService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyStateSvc = _ontologyStateService_;
        });

        element = $compile(angular.element('<selected-details></selected-details>'))(scope);
        scope.$digest();
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.prop('tagName')).toBe('DIV');
            expect(element.hasClass('selected-details')).toBe(true);
        });
        it('depending on whether something is selected', function() {
            expect(element.find('div').length).toBe(1);
            expect(element.find('static-iri').length).toBe(1);

            ontologyStateSvc.selected = undefined;
            scope.$digest();
            expect(element.find('div').length).toBe(0);
            expect(element.find('static-iri').length).toBe(0);
        });
    });
});