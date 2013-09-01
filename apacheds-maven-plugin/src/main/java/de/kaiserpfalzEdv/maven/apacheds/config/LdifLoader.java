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

package de.kaiserpfalzEdv.maven.apacheds.config;

import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

/**
 * @author klenkes
 * @since 1.0
 */
public class LdifLoader {
    /** The session to use to load LDIFs into. */
    private CoreSession session;

    /** The partition to load the entries into. */
    private Partition partition;

    /** The logger for messages to Maven. */
    private Log logger;

    /** The filename extension accepted as LDIFF files. */
    private String ldifFileExtension = ".ldif";



    /**
     * Creates a LdifLoader for the given partition.
     * @param session The session to load the LDIF into.
     * @param partition The partition to load the LDIF into.
     */
    public LdifLoader(final CoreSession session, final Partition partition) {
        notNull(session, "LdifLoader needs a valid directory session!");
        notNull(partition, "LdifLoader needs a valid partition to load ldifs into.");

        this.session = session;
        this.partition = partition;
    }


    /**
     * Sets a new extension for accepted LDIFF files. Defaults to ".ldif".
     * @param extension The extension accepted as LDIFF files.
     */
    public void setLdifFileExtension(final String extension) {
        this.ldifFileExtension = extension;
    }


    /**
     * Loads the LDIFF files from the given location. Can point to a single file or a session. If it points to a
     * session, all LDIF from the given session and subdirectories of it will be loaded. The order of such a loading
     * is not defined and may vary.
     * @param logger The apache maven logger.
     * @param ldif The LDIF file or directory to be loaded.
     * @throws FileNotFoundException If there is no such file.
     * @throws LdapException If the loading failed.
     */
    public void loadLdif(final Log logger, final File ldif) throws FileNotFoundException, LdapException, MojoExecutionException {
        this.logger = logger;

        loadLdif(ldif);
    }

    /**
     * Loads the LDIFF files from the given location. Can point to a single file or a session. If it points to a
     * session, all LDIF from the given session and subdirectories of it will be loaded. The order of such a loading
     * is not defined and may vary.
     * @param logger The apache maven logger.
     * @param ldifs The LDIF files or directories to be loaded.
     * @throws FileNotFoundException If there is no such file.
     * @throws LdapException If the loading failed.
     */
    public void loadLdifs(final Log logger, final List<File> ldifs) throws FileNotFoundException, LdapException, MojoExecutionException {
        this.logger = logger;

        for (File ldif : ldifs) {
            loadLdif(ldif);
        }
    }

    private void loadLdif(final File ldif) throws FileNotFoundException, LdapException, MojoExecutionException {
        if (!ldif.isDirectory()) {
            if (ldif.getName().endsWith(ldifFileExtension)) {
                loadLdifFile(ldif);
            } else {
                logger.info("LDIF file does not have extension '" + ldifFileExtension + "': " + ldif);
            }
        } else {
            loadLdifsInDirectory(ldif);
            loadLdifsFromSubdirectories(ldif);
        }
    }

    private void loadLdifsInDirectory(final File directory)
            throws FileNotFoundException, LdapException, MojoExecutionException {
        logger.debug("Loading LDIFs from session: " + directory);

        for (File ldif : directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(ldifFileExtension);
            }
        })) {
            loadLdif(ldif);
        }
    }

    private void loadLdifsFromSubdirectories(final File directory)
            throws FileNotFoundException, LdapException, MojoExecutionException {
        for (File ldifDirectory : directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        })) {
            loadLdif(ldifDirectory);
        }
    }

    private void loadLdifFile(final File ldif) throws MojoExecutionException {
        logger.debug("Loading LDIF: " + ldif);

        try {
            LdifReader ldifReader = new LdifReader();

            for (LdifEntry entry : ldifReader.parseLdif(new BufferedReader(new FileReader(ldif)))) {
                addLdifEntry(entry);
            }
        } catch (FileNotFoundException e) {
            logger.error("File '" + ldif + "' not found. LDIF will not be loaded!");

            throw new MojoExecutionException("Can't load configured LDIF '" + ldif + "'. Tests will fail!", e);
        } catch (Exception e) {
            logger.error(e.getClass().getSimpleName() + " caught: " + e.getMessage(), e);
        }
    }


    private void addLdifEntry(final LdifEntry entry) throws Exception {
        logger.debug("    LDIF entry: " + entry);

        try {
            addOrModifyEntry(entry);
        } catch (LdapEntryAlreadyExistsException e) {
            logger.info("    Entry already exists: " + entry.getDn());
        }
    }

    private void addOrModifyEntry(final LdifEntry entry) throws Exception {
        if (entry.isChangeAdd()) {
            addEntry(entry);
        } else if (entry.isChangeModify()) {
            modifyEntry(entry);
        } else {
            throw new IllegalStateException("Only additions and modifications are possible. LDIF entry is: " + entry);
        }
    }

    private void addEntry(final LdifEntry entry) throws Exception {
        partition.add(new AddOperationContext(session, entry.getEntry()));
    }

    private void modifyEntry(final LdifEntry entry) throws Exception {
        partition.modify(new ModifyOperationContext(session, entry.getDn(), entry.getModifications()));
    }
}
