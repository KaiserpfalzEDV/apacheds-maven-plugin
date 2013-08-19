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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;

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
    private String type;


    /**
     * Creates the partition specified by ID and DN for the given schema manager within service.
     * @param service The directory service this partition should be created in.
     * @return The partition.
     */
    public org.apache.directory.server.core.api.partition.Partition createPartition(final DirectoryService service) {

        org.apache.directory.server.core.api.partition.Partition result = createPartitionOfCorrectType(service);

        result.setId(id);

        try {
            result.setSuffixDn(getDnObject());
        } catch (LdapInvalidDnException e) {
            throw new IllegalStateException("Can't set DN to partition!", e);
        }

        return result;
    }

    /**
     * @param service The directory service this partition should be created for.
     * @return The partition of correct type.
     */
    private JdbmPartition createPartitionOfCorrectType(final DirectoryService service) {
        return new JdbmPartition(service.getSchemaManager());
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
