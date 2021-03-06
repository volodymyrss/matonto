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
describe('Datasets Ontology Picker directive', function() {
    var $compile, scope, $q, datasetStateSvc, catalogManagerSvc, utilSvc, prefixes;

    beforeEach(function() {
        module('templates');
        module('datasetsOntologyPicker');
        mockDatasetState();
        mockCatalogManager();
        mockUtil();
        mockPrefixes();

        inject(function(_$compile_, _$rootScope_, _datasetStateService_, _catalogManagerService_, _$q_, _prefixes_, _utilService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            datasetStateSvc = _datasetStateService_;
            catalogManagerSvc = _catalogManagerService_;
            utilSvc = _utilService_;
            prefixes = _prefixes_;
            $q = _$q_;
        });

        this.ontology1Record = {
                '@id': 'ontology1Record',
                '@type': ['http://www.w3.org/2002/07/owl#Thing',
                        'http://mobi.com/ontologies/catalog#Record',
                        'http://mobi.com/ontologies/catalog#VersionedRecord',
                        'http://mobi.com/ontologies/ontology-editor#OntologyRecord',
                        'http://mobi.com/ontologies/catalog#VersionedRDFRecord'],
                'catalog:branch': [{ '@id': 'ontology1Branch' }],
                'catalog:catalog': [{ '@id': 'http://mobi.com/catalog-local' }],
                'catalog:masterBranch': [{ '@id': 'ontology1Branch' }],
                'ontEdit:ontologyIRI': [{ '@id': 'ontology1' }],
                'dcterms:description': [{ '@value': '' }],
                'dcterms:issued': [{
                        '@type': 'http://www.w3.org/2001/XMLSchema#dateTime',
                        '@value': '2017-07-12T10:28:15-04:00' }],
                'dcterms:modified': [{
                        '@type': 'http://www.w3.org/2001/XMLSchema#dateTime',
                        '@value': '2017-07-12T10:28:15-04:00' }],
                'dcterms:publisher': [{ '@id': 'http://mobi.com/users/user1' }],
                'dcterms:title': [{ '@value': 'Ontology 1' }]
        };
        this.ontology2Record = {
                '@id': 'ontology2Record',
                '@type': ['http://www.w3.org/2002/07/owl#Thing',
                        'http://mobi.com/ontologies/catalog#Record',
                        'http://mobi.com/ontologies/catalog#VersionedRecord',
                        'http://mobi.com/ontologies/ontology-editor#OntologyRecord',
                        'http://mobi.com/ontologies/catalog#VersionedRDFRecord'],
                'catalog:branch': [{ '@id': 'ontology2Branch' }],
                'catalog:catalog': [{ '@id': 'http://mobi.com/catalog-local' }],
                'catalog:masterBranch': [{ '@id': 'ontology2Branch' }],
                'ontEdit:ontologyIRI': [{ '@id': 'ontology2' }],
                'dcterms:description': [{ '@value': '' }],
                'dcterms:issued': [{
                        '@type': 'http://www.w3.org/2001/XMLSchema#dateTime',
                        '@value': '2017-07-12T10:28:15-04:00' }],
                'dcterms:modified': [{
                        '@type': 'http://www.w3.org/2001/XMLSchema#dateTime',
                        '@value': '2017-07-12T10:28:15-04:00' }],
                'dcterms:publisher': [{ '@id': 'http://mobi.com/users/user1' }],
                'dcterms:title': [{ '@value': 'Ontology 2' }]
        };

        catalogManagerSvc.localCatalog = {'@id': 'http://mobi.com/catalog-local'};
        this.headers = { 'x-total-count': 2, link: '' };
        this.response = {
            data: [this.ontology1Record, this.ontology2Record],
            headers: jasmine.createSpy('headers').and.returnValue(this.headers)
        };
        catalogManagerSvc.getRecords.and.returnValue($q.when(this.response));
        scope.selectedOntologies = [];
        scope.error = '';
        this.element = $compile(angular.element('<datasets-ontology-picker selected-ontologies="selectedOntologies" error="error"></datasets-ontology-picker>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('datasetsOntologyPicker');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        $q = null;
        datasetStateSvc = null;
        datasetManagerSvc = null;
        catalogManagerSvc = null;
        utilSvc = null;
        prefixes = null;
        this.element.remove();
    });

    describe('controller bound variable', function() {
        it('selectedOntologies should be two way bound.', function() {
            this.controller.selectedOntologies = [{'@id': 'id'}];
            scope.$apply();
            expect(scope.selectedOntologies).toEqual([{'@id': 'id'}]);
        });
        it('error should be two way bound.', function() {
            this.controller.error = 'Error';
            scope.$apply();
            expect(scope.error).toEqual('Error');
        });
    });
    describe('controller methods', function() {
        describe('should get a list of ontologies', function() {
            it('unless an error occurs', function() {
                catalogManagerSvc.getRecords.and.returnValue($q.reject('Error Message'));
                this.controller.getOntologies();
                scope.$apply();
                expect(this.controller.ontologySearchConfig.pageIndex).toBe(0);
                expect(catalogManagerSvc.getRecords).toHaveBeenCalledWith(catalogManagerSvc.localCatalog['@id'], this.controller.ontologySearchConfig);
                expect(this.controller.ontologies).toEqual([]);
                expect(this.controller.totalSize).toEqual(0);
                expect(this.controller.links).toEqual({next: '', prev: ''});
                expect(utilSvc.parseLinks.calls.count()).toEqual(1);
                expect(this.controller.error).toBe('Error Message');
            });
            it('successfully', function() {
                utilSvc.parseLinks.and.returnValue({prev: 'prev', next: 'next'});
                this.controller.getOntologies();
                scope.$apply();
                expect(this.controller.ontologySearchConfig.pageIndex).toBe(0);
                expect(catalogManagerSvc.getRecords).toHaveBeenCalledWith(catalogManagerSvc.localCatalog['@id'], this.controller.ontologySearchConfig);
                expect(this.controller.ontologies).toEqual(this.response.data);
                expect(this.response.headers).toHaveBeenCalled();
                expect(this.controller.totalSize).toBe(this.headers['x-total-count']);
                expect(utilSvc.parseLinks.calls.count()).toEqual(2);
                expect(this.controller.links.prev).toBe('prev');
                expect(this.controller.links.next).toBe('next');
                expect(this.controller.error).toBe('');
            });
        });
        it('should test whether an ontology is selected', function() {
            expect(this.controller.isSelected('id')).toBe(false);
            this.controller.selectedOntologies = [{'@id': 'id'}];
            expect(this.controller.isSelected('id')).toBe(true);
            expect(this.controller.isSelected('test')).toBe(false);
        });
        it('should select an ontology', function() {
            var ontology = {'@id': 'id'};
            spyOn(this.controller, 'isSelected').and.returnValue(true);
            this.controller.selectOntology(ontology);
            expect(this.controller.selectedOntologies).not.toContain(ontology);

            this.controller.isSelected.and.returnValue(false);
            this.controller.selectOntology(ontology);
            expect(this.controller.selectedOntologies).toContain(ontology);
        });
        it('should unselect an ontology', function() {
            this.controller.selectedOntologies = [{'@id': 'id'}];
            this.controller.unselectOntology('test');
            expect(this.controller.selectedOntologies.length).toBe(1);
            this.controller.unselectOntology('id');
            expect(this.controller.selectedOntologies.length).toBe(0);
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.hasClass('datasets-ontology-picker')).toBe(true);
        });
        it('with a .input-group', function() {
            expect(this.element.querySelectorAll('.input-group.ontologies-search-bar').length).toBe(1);
        });
        it('with a .list-group', function() {
            expect(this.element.querySelectorAll('.list-group.ontology-records-list').length).toBe(1);
        });
        it('with a paging-details', function() {
            expect(this.element.find('paging-details').length).toBe(1);
        });
        it('with a pagination', function() {
            expect(this.element.find('pagination').length).toBe(1);
        });
        it('depending on how many ontologies there are', function() {
            this.controller.ontologies = [{}];
            scope.$digest();
            expect(this.element.querySelectorAll('.ontology-records-list button').length).toBe(this.controller.ontologies.length);
        });
        it('depending on whether an ontology has been selected', function() {
            this.controller.ontologies = [{'@id': 'ontology'}];
            spyOn(this.controller, 'isSelected').and.returnValue(true);
            scope.$digest();
            var button = angular.element(this.element.querySelectorAll('.ontology-records-list button')[0]);
            expect(button.hasClass('active')).toBe(true);
        });
        it('depending on how many ontologies have been selected', function() {
            expect(this.element.querySelectorAll('.selected-ontologies span').length).toBe(2);
            this.controller.selectedOntologies = [{'@id': '1'}, {'@id': '2'}];
            scope.$digest();
            expect(this.element.querySelectorAll('.selected-ontologies span').length).toBe(this.controller.selectedOntologies.length + 1);
        });
    });
    it('should call getOntologies when the search button is clicked', function() {
        scope.$digest();
        spyOn(this.controller, 'getOntologies');
        var searchButton = angular.element(this.element.querySelectorAll('.ontologies-search-bar button')[0]);
        searchButton.triggerHandler('click');
        expect(this.controller.getOntologies).toHaveBeenCalled();
    });
    it('should select an ontology when clicked', function() {
        this.controller.ontologies = [{}];
        scope.$digest();
        spyOn(this.controller, 'selectOntology');
        var button = angular.element(this.element.querySelectorAll('.ontology-records-list button')[0]);
        button.triggerHandler('click');
        expect(this.controller.selectOntology).toHaveBeenCalledWith({});
    });
    it('should unselect an ontology when clicked', function() {
        this.controller.selectedOntologies = [{'@id': 'id'}];
        scope.$digest();
        spyOn(this.controller, 'unselectOntology');
        var link = angular.element(this.element.querySelectorAll('.selected-ontologies span a')[0]);
        link.triggerHandler('click');
        expect(this.controller.unselectOntology).toHaveBeenCalledWith('id');
    });
});