(function() {
    'use strict';

    angular
        .module('propForm', ['ontologyManager'])
        .directive('propForm', propForm);

        propForm.$inject = ['$timeout', 'ontologyManagerService'];

        function propForm($timeout, ontologyManagerService) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {
                    classId: '@',
                    set: '&',
                    setNext: '&'
                },
                bindToController: {
                    ontologies: '=',
                    props: '=',
                    selectedProp: '=',
                    isDatatypeProp: '&',
                    isObjectProp: '&'
                },
                controller: function() {
                    var dvm = this;

                    dvm.update = function() {
                        $timeout(function() {
                            if (dvm.selectedProp) {
                                if (dvm.isObjectProperty()) {
                                    dvm.isObjectProp();
                                } else {
                                    dvm.isDatatypeProp();
                                }
                            }
                        });
                    }
                    dvm.isObjectProperty = function() {
                        return ontologyManagerService.isObjectProperty(
                            _.get(_.find(dvm.props, {'@id': dvm.selectedProp}), '@type', [])
                        );
                    }
                    dvm.getClassName = function(classId) {
                        var ontology = ontologyManagerService.findOntologyWithClass(dvm.ontologies, classId);
                        return ontologyManagerService.getEntityName(ontologyManagerService.getClass(ontology, classId));
                    }
                },
                templateUrl: 'modules/mapper/directives/propForm/propForm.html'
            }
        }
})();
