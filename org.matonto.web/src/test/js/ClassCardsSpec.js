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
describe('Class Cards directive', function() {
    var $compile, scope, element, discoverStateSvc, exploreSvc, utilSvc, $q;

    beforeEach(function() {
        module('templates');
        module('classCards');
        mockDiscoverState();
        mockUtil();
        mockExplore();

        inject(function(_$compile_, _$rootScope_, _discoverStateService_, _exploreService_, _utilService_, _$q_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            discoverStateSvc = _discoverStateService_;
            exploreSvc = _exploreService_;
            utilSvc = _utilService_;
            $q = _$q_;
        });

        discoverStateSvc.explore.recordId = 'recordId';
        discoverStateSvc.explore.classDetails = [{
            instancesCount: 1,
            classTitle: 'z'
        }, {
            instancesCount: 2,
            classTitle: 'z'
        }, {
            instancesCount: 2,
            classTitle: 'a'
        }, {
            instancesCount: 1,
            classTitle: 'a'
        }];
        element = $compile(angular.element('<class-cards></class-cards>'))(scope);
        scope.$digest();
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.prop('tagName')).toBe('DIV');
            expect(element.hasClass('class-cards')).toBe(true);
            expect(element.hasClass('full-height')).toBe(true);
        });
        it('with a .rows-container.full-height', function() {
            expect(element.querySelectorAll('.rows-container.full-height').length).toBe(1);
        });
        it('with a .row', function() {
            expect(element.querySelectorAll('.row').length).toBe(2);
        });
        it('with a .col-xs-4.card-container', function() {
            expect(element.querySelectorAll('.col-xs-4.card-container').length).toBe(4);
        });
        it('with a md-card', function() {
            expect(element.find('md-card').length).toBe(4);
        });
        it('with a md-card-title', function() {
            expect(element.find('md-card-title').length).toBe(4);
        });
        it('with a md-card-title-text', function() {
            expect(element.find('md-card-title-text').length).toBe(4);
        });
        it('with a .card-header', function() {
            expect(element.querySelectorAll('.card-header').length).toBe(4);
        });
        it('with a .md-headline.text', function() {
            expect(element.querySelectorAll('.md-headline.text').length).toBe(4);
        });
        it('with a .badge', function() {
            expect(element.querySelectorAll('.badge').length).toBe(4);
        });
        it('with a md-card-content', function() {
            expect(element.find('md-card-content').length).toBe(4);
        });
        it('with a .overview', function() {
            expect(element.querySelectorAll('.overview').length).toBe(4);
        });
        it('with a .text-muted', function() {
            expect(element.querySelectorAll('.text-muted').length).toBe(8);
        });
    });
    it('properly defines controller.chunks on load', function() {
        var expected = [[{
            instancesCount: 2,
            classTitle: 'a'
        }, {
            instancesCount: 2,
            classTitle: 'z'
        }, {
            instancesCount: 1,
            classTitle: 'a'
        }], [{
            instancesCount: 1,
            classTitle: 'z'
        }]];
        expect(angular.copy(element.controller('classCards').chunks)).toEqual(expected);
    });
    describe('controller methods', function() {
        beforeEach(function() {
            controller = element.controller('classCards');
        });
        describe('exploreData should set the correct variables when getClassInstances is', function() {
            it('resolved', function() {
                var data = [{prop: 'data'}];
                var nextLink = 'http://example.com/next';
                var prevLink = 'http://example.com/prev';
                var headers = jasmine.createSpy('headers').and.returnValue({
                    'x-total-count': 10,
                    link: 'link'
                });
                utilSvc.parseLinks.and.returnValue({next: nextLink, prev: prevLink});
                discoverStateSvc.explore.breadcrumbs = [''];
                exploreSvc.getClassInstanceDetails.and.returnValue($q.when({data: data, headers: headers}));
                exploreSvc.createPagedResultsObject.and.returnValue({prop: 'paged'});
                controller.exploreData({classTitle: 'new', classIRI: 'classId'});
                scope.$apply();
                expect(exploreSvc.getClassInstanceDetails).toHaveBeenCalledWith('recordId', 'classId');
                expect(exploreSvc.createPagedResultsObject).toHaveBeenCalledWith({data: data, headers: headers});
                expect(discoverStateSvc.explore.instanceDetails).toEqual(jasmine.objectContaining({prop: 'paged'}));
                expect(discoverStateSvc.explore.breadcrumbs).toEqual(['', 'new']);
            });
            it('rejected', function() {
                exploreSvc.getClassInstanceDetails.and.returnValue($q.reject('error'));
                controller.exploreData({classTitle: 'new', classIRI: 'classId'});
                scope.$apply();
                expect(exploreSvc.getClassInstanceDetails).toHaveBeenCalledWith('recordId', 'classId');
                expect(utilSvc.createErrorToast).toHaveBeenCalledWith('error');
            });
        });
    });
});