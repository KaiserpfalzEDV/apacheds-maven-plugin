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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Goal which touches a timestamp file.
 *
 * @goal start
 * @phase pre-integration-test
 */
public class StartMojo extends AbstractApacheDsMojo {

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Apache DS is disabled.");
            return;
        }

        String originalClasspath = System.getProperty("java.class.path");
        String classpath = buildPluginClasspath();
        System.setProperty("java.class.path", classpath);

        try {
            service.setWorkingDirectory(workingDirectory);
            service.setPort(port);
            service.setAdditionalSchema(schema);
            service.setPreloadLdif(ldif);
            service.addPartitions(partitions);

            service.init(getLog());

            service.start(getLog());
        } catch (Exception e) {
            getLog().error(e.getClass().getSimpleName() + " caught: " + e.getMessage(), e);

            throw new MojoExecutionException("Can't start Apache DS!", e);
        } finally {
            System.setProperty("java.class.path", originalClasspath);
        }
    }


    private String buildPluginClasspath() {
        StringBuilder result = new StringBuilder();

        for (Artifact pluginDependency : pluginArtifacts) {
            if (result.length() > 0) {
                result.append(File.pathSeparator);
            }

            result.append(pluginDependency.getFile().toURI());
        }

        return result.toString();
    }
}
