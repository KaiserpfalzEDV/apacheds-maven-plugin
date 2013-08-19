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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal stop
 * @phase post-integration-test
 */
public class StopMojo extends AbstractApacheDsMojo {
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) return;

        try {
            service.stop(getLog());
        } catch (Exception e) {
            getLog().error(e.getClass().getSimpleName() + " caught: " + e.getMessage(), e);

            throw new MojoExecutionException("Can't stop Apache DS!", e);
        }
    }
}
