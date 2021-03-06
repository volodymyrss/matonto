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
describe('Class Mapping Select directive', function() {
    var $compile, scope, utilSvc;

    beforeEach(function() {
        module('templates');
        module('classMappingSelect');
        injectTrustedFilter();
        injectHighlightFilter();
        mockMappingManager();
        mockMapperState();
        mockUtil();

        inject(function(_$compile_, _$rootScope_, _utilService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            utilSvc = _utilService_;
        });

        scope.bindModel = '';
        scope.onChange = jasmine.createSpy('onChange');
        this.element = $compile(angular.element('<class-mapping-select ng-model="bindModel" on-change="onChange()"></class-mapping-select>'))(scope);
        scope.$digest();
        this.isolatedScope = this.element.isolateScope();
        this.controller = this.element.controller('classMappingSelect');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        utilSvc = null;
        this.element.remove();
    });

    describe('in isolated scope', function() {
        it('onChange should be called in the parent scope', function() {
            this.isolatedScope.onChange();
            expect(scope.onChange).toHaveBeenCalled();
        });
    });
    describe('controller bound variable', function() {
        it('selectedProp should be two way bound', function() {
            this.controller.bindModel = 'test';
            scope.$digest();
            expect(scope.bindModel).toEqual('test');
        });
    });
    describe('controller methods', function() {
        it('should get the title of a class mapping', function() {
            utilSvc.getDctermsValue.and.returnValue('Title');
            expect(this.controller.getTitle({})).toEqual('Title');
            expect(utilSvc.getDctermsValue).toHaveBeenCalledWith({}, 'title');
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.hasClass('class-mapping-select')).toBe(true);
        });
        it('with a ui-select', function() {
            expect(this.element.find('ui-select').length).toBe(1);
        });
    });
});