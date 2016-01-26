package org.matonto.etl.service.rdf

import org.matonto.rdf.api.Model
import org.matonto.rdf.api.ModelFactory
import org.matonto.rdf.api.Statement
import org.matonto.rdf.core.impl.sesame.LinkedHashModel
import org.matonto.repository.api.RepositoryConnection
import org.matonto.repository.api.RepositoryManager
import org.matonto.repository.api.Repository
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification


class RDFImportSpec extends Specification {

    def bnode = "_:matonto/bnode/"


    def "Throws exception if repository ID does not exist"(){
        setup:
        RepositoryManager manager = Mock()
        RDFImportServiceImpl importService = new RDFImportServiceImpl()
        File testFile = new ClassPathResource("importer/testFile.trig").getFile();
        ModelFactory mf = Mock()
        importService.setModelFactory(mf)

        when:
        importService.setRepositoryManager(manager)
        importService.importFile("test", testFile, true)

        then:
        1 * mf.createModel(_ as Collection<Statement>) >> new LinkedHashModel()
        1 * manager.getRepository("test") >> Optional.empty()
        thrown IllegalArgumentException
    }

    def "Test trig file"(){
        setup:
        Repository repo = Mock()
        RepositoryManager manager = Mock()
        RepositoryConnection repoConn = Mock()
        ModelFactory factory = Mock()
        RDFImportServiceImpl importService = new RDFImportServiceImpl()
        File testFile = new ClassPathResource("importer/testFile.trig").getFile();

        when:
        importService.setRepositoryManager(manager)
        importService.setModelFactory(factory)
        importService.importFile("test", testFile, true)

        then:
        1 * manager.getRepository("test") >> Optional.of(repo)
        1 * repo.getConnection() >> repoConn
        1 * factory.createModel(_) >> Mock(Model.class)
    }

    def "Invalid file type causes error"(){
        setup:
        RDFImportServiceImpl importService = new RDFImportServiceImpl()
        File f = new ClassPathResource("importer/testFile.txt").getFile();

        when:
        importService.importFile("test",f, true);

        then:
        thrown IOException
    }

    def "Nonexistent file throws exception"(){
        setup:
        RDFImportServiceImpl importService = new RDFImportServiceImpl()
        File f = new File("importer/FakeFile.txt");

        when:
        importService.importFile("test",f,true);

        then:
        thrown IOException
    }

}