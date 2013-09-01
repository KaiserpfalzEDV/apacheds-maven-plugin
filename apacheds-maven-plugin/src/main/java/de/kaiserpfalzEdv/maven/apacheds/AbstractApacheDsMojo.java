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

import de.kaiserpfalzEdv.maven.apacheds.config.Partition;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractApacheDsMojo extends AbstractMojo {

    /**
     * @component
     */
    protected ApacheDsLifecycle service;


    /**
     * @parameter optional="true" expression="${ldap.port}" default-value="10389"
     */
    protected int port;


    /**
     * @parameter optional="true" expression="${ldap.workingDirectory}" default-value="target/apache-ds"
     */
    protected File workingDirectory;


    /**
     * @parameter optional="true" expression="${ldap.schemaDirectory}"
     */
    protected File schema;

    /**
     * @parameter optional="true" expression="${ldap.schemaDirectories}"
     */
    protected final List<File> schemaDirectories = new ArrayList<>();

    /**
     * An LDIF file or directory to preload the LDAP directory against.
     *
     * @parameter optional="true" expression="${ldap.ldifDirectory}"
     */
    protected File ldif;

    /**
     * @parameter optional="true" expression="${ldap.ldifDirectories}"
     */
    protected final List<File> ldifDirectories = new ArrayList<>();

    /**
     * The partitions to be added to the Apache Server
     *
     * @parameter optional="true" expression="${ldap.partitions}"
     */
    protected List<Partition> partitions;


    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List<Artifact> pluginArtifacts;


    /**
     * @parameter optional="true" expression="${ldap.skip}" default-value="false"
     */
    protected boolean skip;


    @Override
    public abstract void execute() throws MojoExecutionException;
}
