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
describe('Util service', function() {
    var utilSvc,
        prefixes,
        toastr,
        splitIRIFilter,
        beautifyFilter;

    beforeEach(function() {
        module('util');
        mockPrefixes();
        injectSplitIRIFilter();
        injectBeautifyFilter();
        mockToastr();

        inject(function(utilService, _prefixes_, _toastr_, _splitIRIFilter_, _beautifyFilter_) {
            utilSvc = utilService;
            prefixes = _prefixes_;
            toastr = _toastr_;
            splitIRIFilter = _splitIRIFilter_;
            beautifyFilter = _beautifyFilter_;
        });
    });

    describe('should get the beautified version of an IRI', function() {
        beforeEach(function() {
            this.iri = 'iri';
        });
        it('if it has a local name', function() {
            splitIRIFilter.and.returnValue({begin: 'begin', then: 'then', end: 'end'});
            var result = utilSvc.getBeautifulIRI(this.iri);
            expect(splitIRIFilter).toHaveBeenCalledWith(this.iri);
            expect(beautifyFilter).toHaveBeenCalledWith('end');
            expect(result).toBe('end');
        });
        it('if it does not have a local name', function() {
            var result = utilSvc.getBeautifulIRI(this.iri);
            expect(splitIRIFilter).toHaveBeenCalledWith(this.iri);
            expect(beautifyFilter).not.toHaveBeenCalled();
            expect(result).toBe(this.iri);
        });
    });
    describe('should get a property value from an entity', function() {
        it('if it contains the property', function() {
            var prop = 'property';
            var entity = {'property': [{'@value': 'value'}]};
            expect(utilSvc.getPropertyValue(entity, prop)).toBe('value');
        });
        it('if it does not contain the property', function() {
            expect(utilSvc.getPropertyValue({}, 'prop')).toBe('');
        });
    });
    describe('should get a dcterms property value from an entity', function() {
        it('if it contains the property', function() {
            var prop = 'prop',
                entity = {};
            entity[prefixes.dcterms + prop] = [{'@value': 'value'}];
            expect(utilSvc.getDctermsValue(entity, prop)).toBe('value');
        });
        it('if it does not contain the property', function() {
            expect(utilSvc.getDctermsValue({}, 'prop')).toBe('');
        });
    });
    describe('should parse a link header string', function() {
        it('unless it is empty', function() {
            expect(utilSvc.parseLinks('')).toEqual({});
        })
        it('correctly', function() {
            var link = 'http://example.com';
            var links = '<' + link + '>; rel="test"';
            expect(utilSvc.parseLinks(links)).toEqual({test: link});
        });
    });
    it('should create an error toast', function() {
        utilSvc.createErrorToast('Text');
        expect(toastr.error).toHaveBeenCalledWith('Text', 'Error', {timeOut: 0});
    });
});