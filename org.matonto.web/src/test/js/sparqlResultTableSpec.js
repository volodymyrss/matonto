describe('SPARQL Result Table directive', function() {
    var $compile,
        $window,
        scope,
        sparqlManagerSvc;

    mockSparqlManager();

    beforeEach(function() {
        module('sparqlResultTable');

        inject(function(sparqlManagerService) {
            sparqlManagerSvc = sparqlManagerService;
        });

        inject(function(_$compile_, _$rootScope_, _$window_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            $window = _$window_;
        });
    });

    injectDirectiveTemplate('modules/sparql/directives/sparqlResultTable/sparqlResultTable.html');

    describe('replaces the element with the correct html', function() {
        it('for a div', function() {
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            expect(element.prop('tagName')).toBe('DIV');
        });
        it('based on table', function() {
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            var table = element.querySelectorAll('.table');
            expect(table.length).toBe(1);
        });
        it('<th>s should match sparqlManagerService.data.head.vars.length', function() {
            scope.sparqlManagerService = sparqlManagerSvc;
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            var theadList = element.querySelectorAll('thead');
            expect(element.html()).not.toContain('None');
            expect(theadList.length).toBe(1);
            var thead = theadList[0];
            expect(thead.querySelectorAll('th').length).toBe(scope.sparqlManagerService.data.head.vars.length);
        });
        it('<tr>s should match sparqlManagerService.data.results.bindings.length', function() {
            scope.sparqlManagerService = sparqlManagerSvc;
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            var tbodyList = element.querySelectorAll('tbody');
            expect(element.html()).not.toContain('None');
            expect(tbodyList.length).toBe(1);
            var tbody = tbodyList[0];
            expect(tbody.querySelectorAll('tr').length).toBe(scope.sparqlManagerService.data.results.bindings.length);
        });
        it('shows error message if populated', function() {
            scope.sparqlManagerService = sparqlManagerSvc;
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            var errorP = element.querySelectorAll('.text-danger');
            expect(errorP.length).toBe(0);

            scope.sparqlManagerService.errorMessage = 'Error message';
            scope.$digest();

            errorP = element.querySelectorAll('.text-danger');
            expect(errorP.length).toBe(1);
        });
        it('shows info message if populated', function() {
            scope.sparqlManagerService = sparqlManagerSvc;
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            var errorP = element.querySelectorAll('.text-info');
            expect(errorP.length).toBe(0);

            scope.sparqlManagerService.infoMessage = 'Info message';
            scope.$digest();

            errorP = element.querySelectorAll('.text-info');
            expect(errorP.length).toBe(1);
        });
    });
    describe('link function code', function() {
        it('resize() is called when window is resized', function() {
            var totalHeight = 200;
            var topHeight = 100;
            var html = '<div class="sparql" style="height: ' + totalHeight + 'px;"></div><div class="sparql-editor" style="height: ' + topHeight + 'px;"></div>';
            angular.element(document.body).append(html);
            var element = $compile(angular.element('<sparql-result-table></sparql-result-table>'))(scope);
            scope.$digest();

            expect(element.attr('style')).toBe(undefined);
            angular.element($window).triggerHandler('resize');
            expect(element.attr('style')).toBe('height: ' + (totalHeight - topHeight) + 'px;');
        });
    });
});