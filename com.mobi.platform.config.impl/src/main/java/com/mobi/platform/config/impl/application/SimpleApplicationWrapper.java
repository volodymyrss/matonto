package com.mobi.platform.config.impl.application;

/*-
 * #%L
 * com.mobi.platform.config.impl
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

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import com.mobi.platform.config.api.application.ApplicationConfig;
import com.mobi.platform.config.api.application.ApplicationWrapper;
import com.mobi.platform.config.api.ontologies.platformconfig.Application;
import com.mobi.platform.config.api.ontologies.platformconfig.ApplicationFactory;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.repository.api.Repository;
import com.mobi.repository.api.RepositoryConnection;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(
        immediate = true,
        name = SimpleApplicationWrapper.NAME,
        configurationPolicy = ConfigurationPolicy.require,
        designateFactory = ApplicationConfig.class
)
public class SimpleApplicationWrapper implements ApplicationWrapper {
    private static final String NAMESPACE = "http://mobi.com/applications#";
    protected static final String NAME = "com.mobi.platform.config.application";
    private static final Logger LOG = LoggerFactory.getLogger(SimpleApplicationWrapper.class);

    protected Repository repository;
    protected ValueFactory factory;
    protected ModelFactory modelFactory;
    protected ApplicationFactory appFactory;

    protected String applicationId;

    @Reference(name = "repository")
    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Reference
    protected void setFactory(ValueFactory factory) {
        this.factory = factory;
    }

    @Reference
    protected void setModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Reference
    protected void setAppFactory(ApplicationFactory appFactory) {
        this.appFactory = appFactory;
    }

    @Activate
    protected void start(Map<String, Object> props) {
        ApplicationConfig config = Configurable.createConfigurable(ApplicationConfig.class, props);
        LOG.trace("Starting \"" + config.id() + "\" application...");

        validateConfig(props);
        this.applicationId = config.id();

        Application application = appFactory.createNew(factory.createIRI(NAMESPACE + applicationId));
        application.setProperty(factory.createLiteral(config.title()), factory.createIRI(DCTERMS.TITLE.stringValue()));
        if (config.description() != null && !config.description().equals("")) {
            application.setProperty(factory.createLiteral(config.description()),
                    factory.createIRI(DCTERMS.DESCRIPTION.stringValue()));
        }

        try (RepositoryConnection conn = repository.getConnection()) {
            if (conn.contains(application.getResource(), null, null)) {
                LOG.warn("Replacing existing application \"" + applicationId + "\".");
                conn.remove(application.getResource(), null, null);
            }
            conn.add(application.getModel());
        }
        LOG.debug("Application \"" + applicationId + "\" started.");
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        stop();
        start(props);
    }

    @Deactivate
    protected void stop() {
        LOG.trace("Stopping \"" + applicationId + "\" application...");
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.remove(factory.createIRI(NAMESPACE + applicationId), null, null);
        }
        LOG.debug("Application \"" + applicationId + "\" stopped.");
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        ApplicationConfig config = Configurable.createConfigurable(ApplicationConfig.class, props);

        if (config.id().equals("")) {
            throw new IllegalArgumentException("Application property \"id\" cannot be empty");
        } else if (!config.id().matches("^[a-zA-Z0-9._\\-]+$")) {
            throw new IllegalArgumentException("Application property \"id\" is invalid");
        }
        if (config.title().equals("")) {
            throw new IllegalArgumentException("Application property \"title\" cannot be empty");
        }
    }

    @Override
    public String getId() {
        return applicationId;
    }

    @Override
    public Application getApplication() {
        try (RepositoryConnection conn = repository.getConnection()) {
            Model appModel = modelFactory.createModel();
            conn.getStatements(factory.createIRI(NAMESPACE + applicationId), null, null).forEach(appModel::add);
            return appFactory.getExisting(factory.createIRI(NAMESPACE + applicationId), appModel).orElseThrow(() ->
                    new IllegalStateException("Unable to retrieve application: " + NAMESPACE + applicationId));
        }
    }
}
