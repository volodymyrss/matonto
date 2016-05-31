describe('File Upload Overlay directive', function() {
    var $compile,
        scope,
        mappingManagerSvc,
        mapperStateSvc,
        csvManagerSvc,
        ontologyManagerSvc;

    mockPrefixes();
    beforeEach(function() {
        module('fileUploadOverlay');
        mockMappingManager();
        mockMapperState();
        mockCsvManager();
        mockOntologyManager();

        inject(function(_mappingManagerService_, _mapperStateService_, _csvManagerService_, _ontologyManagerService_) {
            mappingManagerSvc = _mappingManagerService_;
            mapperStateSvc = _mapperStateService_;
            csvManagerSvc = _csvManagerService_;
            ontologyManagerSvc = _ontologyManagerService_;
        });

        inject(function(_$compile_, _$rootScope_) {
            $compile = _$compile_;
            scope = _$rootScope_;
        });
    });

    injectDirectiveTemplate('modules/mapper/directives/fileUploadOverlay/fileUploadOverlay.html');

    describe('controller methods', function() {
        beforeEach(function() {
            mappingManagerSvc.mapping = {jsonld: []};
            csvManagerSvc.fileObj = {};
            this.element = $compile(angular.element('<file-upload-overlay></file-upload-overlay>'))(scope);
            scope.$digest();
        });
        it('should correctly test whether the file is an Excel file', function() {
            var controller = this.element.controller('fileUploadOverlay');
            var result = controller.isExcel();
            expect(result).toBe(false);

            csvManagerSvc.fileObj = {name: 'test.xls'};
            scope.$digest();
            result = controller.isExcel();
            expect(result).toBe(true);

            csvManagerSvc.fileObj = {name: 'test.xlsx'};
            scope.$digest();
            result = controller.isExcel();
            expect(result).toBe(true);
        });
        it('should get the name of a data mapping', function() {
            var controller = this.element.controller('fileUploadOverlay');
            var result = controller.getDataMappingName('');

            expect(mappingManagerSvc.getPropIdByMappingId).toHaveBeenCalledWith(mappingManagerSvc.mapping.jsonld, '');
            expect(mappingManagerSvc.findClassWithDataMapping).toHaveBeenCalled();
            expect(mappingManagerSvc.getClassIdByMapping).toHaveBeenCalled();
            expect(ontologyManagerSvc.getClassProperty).toHaveBeenCalled();
            expect(ontologyManagerSvc.getClass).toHaveBeenCalled();
            expect(ontologyManagerSvc.getEntityName).toHaveBeenCalled();
            expect(typeof result).toBe('string');
        });
        it('should upload a file', function() {
            var controller = this.element.controller('fileUploadOverlay');
            var invalidProps = mapperStateSvc.invalidProps;
            mapperStateSvc.newMapping = false;
            controller.upload();
            scope.$apply();
            expect(csvManagerSvc.upload).toHaveBeenCalledWith(csvManagerSvc.fileObj);
            expect(csvManagerSvc.fileName).not.toBe('');
            expect(csvManagerSvc.previewFile).toHaveBeenCalledWith(100);
            expect(mapperStateSvc.invalidProps).not.toBe(invalidProps);

            invalidProps = mapperStateSvc.invalidProps;
            mapperStateSvc.newMapping = true;
            controller.upload();
            scope.$apply();
            expect(csvManagerSvc.upload).toHaveBeenCalledWith(csvManagerSvc.fileObj);
            expect(csvManagerSvc.fileName).not.toBe('');
            expect(csvManagerSvc.previewFile).toHaveBeenCalledWith(100);
            expect(mapperStateSvc.invalidProps).toBe(invalidProps);
        });
        it('should set the correct state for canceling', function() {
            var controller = this.element.controller('fileUploadOverlay');
            mapperStateSvc.newMapping = true;
            controller.cancel();
            expect(csvManagerSvc.reset).toHaveBeenCalled();
            expect(mapperStateSvc.initialize).not.toHaveBeenCalled();
            expect(mapperStateSvc.step).toBe(0);
            expect(mapperStateSvc.editMappingName).toBe(true);

            mapperStateSvc.newMapping = false;
            mapperStateSvc.editMappingName = false;
            mapperStateSvc.step = 1;
            controller.cancel();
            expect(csvManagerSvc.reset).toHaveBeenCalled();
            expect(mapperStateSvc.initialize).toHaveBeenCalled();
            expect(mapperStateSvc.step).toBe(1);
            expect(mapperStateSvc.editMappingName).toBe(false);
        });
        it('should set the correct state for continuing', function() {
            var controller = this.element.controller('fileUploadOverlay');
            mapperStateSvc.newMapping = true;
            var name = mapperStateSvc.selectedClassMappingId;
            controller.continue();
            expect(mapperStateSvc.step).toBe(2);
            expect(mappingManagerSvc.getAllClassMappings).not.toHaveBeenCalled();
            expect(mapperStateSvc.selectedClassMappingId).toBe(name);
            expect(mapperStateSvc.updateAvailableProps).not.toHaveBeenCalled();

            var classObj = {'@id': 'class'};
            mappingManagerSvc.getAllClassMappings.and.returnValue([classObj])
            mapperStateSvc.newMapping = false;
            controller.continue();
            expect(mapperStateSvc.step).toBe(4);
            expect(mappingManagerSvc.getAllClassMappings).toHaveBeenCalledWith(mappingManagerSvc.mapping.jsonld);
            expect(mapperStateSvc.selectedClassMappingId).toBe(classObj['@id']);
            expect(mapperStateSvc.updateAvailableProps).toHaveBeenCalled();
        });
        it('should set the upload validity', function() {
            var controller = this.element.controller('fileUploadOverlay');
            controller.setUploadValidity(true);
            expect(controller.fileForm.$valid).toBe(true);

            controller.setUploadValidity(false);
            expect(controller.fileForm.$invalid).toBe(true);
            expect(controller.fileForm.$error.fileUploaded).toBeTruthy();
        });
    });
    describe('replaces the element with the correct html', function() {
        beforeEach(function() {
            mappingManagerSvc.mapping = {jsonld: []};
            csvManagerSvc.fileObj = {};
            this.element = $compile(angular.element('<file-upload-overlay></file-upload-overlay>'))(scope);
            scope.$digest();
        });
        it('for wrapping containers', function() {
            expect(this.element.hasClass('file-upload-overlay')).toBe(true);
            expect(this.element.querySelectorAll('form.content').length).toBe(1);
        });
        it('with a file input', function() {
            expect(this.element.find('file-input').length).toBe(1);
        });
        it('depending on the type of file', function() {
            csvManagerSvc.fileObj = {name: 'test.csv'};
            scope.$digest();
            expect(this.element.querySelectorAll('input[type=radio]').length).toBe(3);

            csvManagerSvc.fileObj = {name: 'test.xls'};
            scope.$digest();
            expect(this.element.querySelectorAll('input[type=radio]').length).toBe(0);
        });
        it('depending on whether the file was uploaded correctly', function() {
            var controller = this.element.controller('fileUploadOverlay');
            controller.fileForm.$setValidity('fileUploaded', false);
            scope.$digest();
            var uploadBtn = angular.element(this.element.querySelectorAll('.upload-btn custom-button')[0]);
            expect(uploadBtn.text()).toBe('Upload');
            expect(this.element.querySelectorAll('.upload-btn .help-block').length).toBe(0);
            expect(this.element.querySelectorAll('.error-msg').length).toBe(1);

            controller.fileForm.$setValidity('fileUploaded', true);
            scope.$digest();
            var uploadBtn = angular.element(this.element.querySelectorAll('.upload-btn custom-button')[0]);
            expect(uploadBtn.text()).toBe('Update');
            expect(this.element.querySelectorAll('.upload-btn .help-block').length).toBe(1);
            expect(this.element.querySelectorAll('.error-msg').length).toBe(0);
        });
        it('depending on whether this is a new mapping', function() {
            mapperStateSvc.newMapping = false;
            scope.$digest();
            var btnContainer = angular.element(this.element.querySelectorAll('.btn-container.clearfix')[0]);
            expect(btnContainer.find('p').length).toBe(1);
            expect(angular.element(btnContainer.find('custom-button')[0]).text()).toBe('Cancel');

            mapperStateSvc.newMapping = true;
            scope.$digest();
            expect(btnContainer.find('p').length).toBe(0);
            expect(angular.element(btnContainer.find('custom-button')[0]).text()).toBe('Back');
        });
        it('depending on whether there are invalid columns', function() {
            mapperStateSvc.invalidProps = [];
            scope.$digest();
            var continueBtn = angular.element(this.element.querySelectorAll('.btn-container.clearfix custom-button')[1]);
            expect(continueBtn.text()).toBe('Continue');
            expect(this.element.querySelectorAll('.invalid-props').length).toBe(0);

            mapperStateSvc.invalidProps = [{'@id': 'prop', index: 0}];
            scope.$digest();
            expect(continueBtn.text()).toBe('Fix');
            var invalidProps = angular.element(this.element.querySelectorAll('.invalid-props')[0]);
            expect(invalidProps).toBeTruthy();
            expect(invalidProps.querySelectorAll('ul li').length).toBe(mapperStateSvc.invalidProps.length);
        });
    });
});