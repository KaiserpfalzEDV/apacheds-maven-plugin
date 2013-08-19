package de.kaiserpfalzEdv.maven.apacheds;

import de.kaiserpfalzEdv.maven.apacheds.config.Partition;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;

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
    private File preload;

    private final ArrayList<Partition> partitions = new ArrayList<>();

    public void init(final Log logger) throws Exception {
        logger.info("Initializing Apache Directory Server ...");

        this.logger = logger;

        DirectoryServiceFactory directoryFactory = new DefaultDirectoryServiceFactory();
        directory = directoryFactory.getDirectoryService();

        if (workingDirectory != null) {
            directory.setInstanceLayout(new InstanceLayout(workingDirectory));
        }

        logger.debug(directory.getInstanceLayout().toString());

        if (schema != null) {
            loadAdditionalSchema(schema);
        }

        directoryFactory.init("LDAP Integration Tester");

        ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(port));
        ldapServer.setDirectoryService(directory);

        server = new ApacheDS(ldapServer);

        loadAdditionalPartitions();

        if (preload != null) {
            server.setLdifDirectory(preload);
        }
    }


    private void loadAdditionalPartitions() throws Exception {
        if (!partitions.isEmpty()) {
            for (Partition p : partitions) {
                logger.info("Adding partition: " + p);

                if (directory.isStarted()) {
                    logger.info("Directory is started ...");
                }

                directory.addPartition(p.createPartition(directory));
            }
        }
    }


    private void loadAdditionalSchema(final File schema) throws FileNotFoundException, LdapException {
        if (!schema.isDirectory() && schema.getName().endsWith(".ldif")) {
            loadAdditionalSchemaFile(schema);
        } else {
            loadSchemasInDirectory(schema);
            loadSchemasFromSubdirectories(schema);
        }
    }

    private void loadSchemasInDirectory(final File schema) throws FileNotFoundException, LdapException {
        for (File schemaFile : schema.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".ldif") || name.endsWith(".ldiff");
            }
        })) {
            loadAdditionalSchema(schemaFile);
        }
    }

    private void loadSchemasFromSubdirectories(final File schema) throws FileNotFoundException, LdapException {
        for (File schemaDir : schema.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        })) {
            loadAdditionalSchema(schemaDir);
        }
    }

    private void loadAdditionalSchemaFile(final File schema) {
        try {
            LdifReader ldifReader = new LdifReader();

            for (LdifEntry entry : ldifReader.parseLdif(new BufferedReader(new FileReader(schema)))) {
                logger.debug("LDIF entry: " + entry);

                if (entry.isChangeAdd()) {
                    directory.getSchemaPartition().add(
                            new AddOperationContext(directory.getSession(), entry.getEntry())
                    );
                } else if (entry.isChangeModify()) {
                    directory.getSchemaPartition().modify(
                            new ModifyOperationContext(directory.getSession(), entry.getDn(), entry.getModifications())
                    );
                } else {
                    throw new IllegalStateException("Only additions and modifications are possible. LDIF entry is: "
                            + entry);
                }
            }
        } catch (Exception e) {
            logger.error(e.getClass().getSimpleName() + " caught: " + e.getMessage(), e);
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

    public void setPort(final int port) {
        this.port = port;
    }
}
