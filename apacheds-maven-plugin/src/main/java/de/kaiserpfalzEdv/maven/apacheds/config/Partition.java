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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author klenkes &lt;rlichti@kaiserpfalz-edv.de&gt;
 * @since 1.0.0
 */
public class Partition {
    /** The ID of the partition. */
    private String id;

    /** The DN of the partition. */
    private String dn;

    /** The type of the partition. */
    private String type = "jdbm";

    /** The data to load into the given partition. */
    private File ldif;

    /** An ordered list of ldif files/directories to load. */
    private final List<File> ldifDirectories = new ArrayList<File>();

    /** A flag if a partition already existing should be deleted or used as is. */
    private boolean replaceExisting = false;


    private Log logger;


    /**
     * Creates the partition specified by ID and DN for the given schema manager within service.
     * @param logger The Maven logger.
     * @param service The directory service this partition should be created in.
     * @return The partition.
     */
    public org.apache.directory.server.core.api.partition.Partition createPartition(final Log logger, final DirectoryService service) throws Exception {
        this.logger = logger;

        AbstractBTreePartition result = createPartitionOfCorrectType(service);

        result.setId(id);

        try {
            result.setSuffixDn(getDnObject());

            File partitionPath = new File(service.getInstanceLayout().getPartitionsDirectory(), id);
            result.setPartitionPath(partitionPath.toURI());

            if (partitionPath.exists() && replaceExisting) {
                FileUtils.deleteDirectory(partitionPath);
            }

            Entry contextEntry = new DefaultEntry(service.getSchemaManager(), getDnObject());
            contextEntry.put("objectClass", "top", getObjectClassFromDn());

            result.setContextEntry(contextEntry);
        } catch (LdapInvalidDnException e) {
            throw new IllegalStateException("Can't set DN to partition!", e);
        }

        loadLdifs(service.getSession(), result);

        return result;
    }

    /**
     * @param service The directory service this partition should be created for.
     * @return The partition of correct type.
     */
    private AbstractBTreePartition createPartitionOfCorrectType(final DirectoryService service) {
        if ("jdbm".equalsIgnoreCase(type)) {
            return new JdbmPartition(service.getSchemaManager());
        } else if ("avl".equalsIgnoreCase(type)) {
            return new AvlPartition(service.getSchemaManager());
        }

        throw new IllegalStateException("There is no type '" + type + "' defined. Please use 'jdbm' or 'avl'.");
    }


    private void loadLdifs(final CoreSession session, final AbstractBTreePartition partition)
            throws FileNotFoundException, LdapException, MojoExecutionException {

        if (ldif != null) {
            ldifDirectories.add(ldif);
        }


        if (!ldifDirectories.isEmpty()) {
            logger.info("- Loading data: " + ldifDirectories);

            new LdifLoader(session, partition).loadLdifs(logger, ldifDirectories);
        }
    }



    /**
     * @return The ID of the partition.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The unique ID of the partition within the DS.
     */
    public void setId(final String id) {
        this.id = id;
    }


    public File getLdif() {
        return ldif;
    }

    public void setLdif(final File ldif) {
        this.ldif = ldif;
    }


    public List<File> getLdifDirectories() {
        return ldifDirectories;
    }

    public void setLdifDirectories(final List<File> ldifDirectories) {
        this.ldifDirectories.clear();

        if (ldifDirectories != null) {
            this.ldifDirectories.addAll(ldifDirectories);
        }
    }


    public boolean getReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(final boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }


    /**
     * @return The DN of the partition base.
     */
    public String getDn() {
        return dn;
    }

    /**
     * @return The DN as modelled DN for ApacheDS.
     * @throws IllegalStateException If there is no or an invalid DN set.
     */
    public Dn getDnObject() {
        if (dn == null) {
            throw new IllegalStateException("No DN set!");
        }

        try {
            return new Dn(dn.split(","));
        } catch (LdapInvalidDnException e) {
            throw new IllegalStateException("The DN consists not of valid RDN elements!", e);
        }
    }


    /**
     * @return The name of the object class according the baseDN.
     * @throws IllegalStateException If the DN is not set or is invalid.
     */
    private String getObjectClassFromDn() {
        if (dn == null) {
            throw new IllegalStateException("No DN set!");
        }

        Rdn rdn = getDnObject().getRdn();

        if ("dc".equalsIgnoreCase(rdn.getType())) {
            return "domain";
        } else if ("o".equalsIgnoreCase(rdn.getType())) {
            return "organization";
        } else if ("ou".equalsIgnoreCase(rdn.getType())) {
            return "organizationalUnit";
        } else if ("c".equalsIgnoreCase(rdn.getType())) {
            return "country";
        }

        throw new IllegalStateException("Illegal context entry type. Only 'dc', 'o', 'ou', or 'c' are allowed (rdn is: " + rdn.getType() + "!");
    }

    /**
     * @param dn The unique DN of the partition base.
     */
    public void setDn(final String dn) {
        this.dn = dn;
    }


    /**
     * @return The type of the partition.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The type of the partition.
     */
    public void setType(final String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("dn", dn, true)
                .append("type", type)
                .toString();
    }
}
