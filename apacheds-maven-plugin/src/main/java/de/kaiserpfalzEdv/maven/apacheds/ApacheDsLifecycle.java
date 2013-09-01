/*
 * Copyright 2013 Kaiserpfalz EDV-Service, Nicol und Roland Lichti.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kaiserpfalzEdv.maven.apacheds;

import de.kaiserpfalzEdv.maven.apacheds.config.LdifLoader;
import de.kaiserpfalzEdv.maven.apacheds.config.Partition;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author klenkes
 * @since 2013Q
 */
public class ApacheDsLifecycle {
    private DirectoryService directory;
    private LdapServer ldapServer;
    private ApacheDS server;

    private Log logger;


    private int port;

    private File workingDirectory;
    private File schema;
    private final List<File> schemaDirectories = new ArrayList<File>();
    private File preload;

    private final ArrayList<Partition> partitions = new ArrayList<Partition>();

    public void init(final Log logger) throws Exception {
        logger.info("Initializing Apache Directory Server ...");

        this.logger = logger;

        DirectoryServiceFactory directoryFactory = new DefaultDirectoryServiceFactory();
        directory = directoryFactory.getDirectoryService();

        if (workingDirectory != null) {
            directory.setInstanceLayout(new InstanceLayout(workingDirectory));
        }

        logger.debug(directory.getInstanceLayout().toString());

        directoryFactory.init("LDAP Integration Tester");

        loadLdapSchema(logger);

        ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(port));
        ldapServer.setDirectoryService(directory);

        server = new ApacheDS(ldapServer);

        loadAdditionalPartitions();

        if (preload != null) {
            logger.info("Setting LDIF preload to: " + preload);
            server.setLdifDirectory(preload);
        }
    }


    private void loadLdapSchema(final Log logger) throws Exception {
        if (schema != null) {
            schemaDirectories.add(schema);
        }

        if (!schemaDirectories.isEmpty()) {
            logger.info("Loading schema: " + schemaDirectories);

            LdifLoader loader = new LdifLoader(directory.getSession(), directory.getSchemaPartition());
            loader.loadLdifs(logger, schemaDirectories);
        }
    }


    private void loadAdditionalPartitions() throws Exception {
        if (!partitions.isEmpty()) {
            for (Partition p : partitions) {
                logger.info("Adding partition: " + p);

                directory.addPartition(p.createPartition(logger, directory));
            }
        }
    }


    public void start(final Log logger) throws Exception {
        logger.info("Starting Apache Directory Server ...");

        if (!directory.isStarted()) {
            directory.startup();
        }

        if (preload != null) {
            server.loadLdifs();
        }

        if (!ldapServer.isStarted()) {
            ldapServer.start();
        }
    }


    public void stop(final Log logger) throws Exception {
        if (isStarted()) {
            logger.info("Stopping Apache Directory Server ...");

            server.shutdown();
        } else {
            logger.warn("No Apache Directory Server to be stopped!");
        }
    }


    public boolean isStarted() {
        return server != null && server.isStarted();
    }


    public void setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setAdditionalSchema(final File schema) {
        this.schema = schema;
    }

    public void setPreloadLdif(final File ldif) {
        this.preload = ldif;
    }

    public void addPartitions(final Collection<Partition> partitions) {
        if (partitions != null) {
            this.partitions.addAll(partitions);
        }
    }

    public void setSchemaDirectories(final List<File> schemaDirectories) {
        this.schemaDirectories.clear();

        if (schemaDirectories != null) {
            this.schemaDirectories.addAll(schemaDirectories);
        }
    }

    public void setPort(final int port) {
        this.port = port;
    }
}
