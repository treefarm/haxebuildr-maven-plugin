/**
 * Copyright (C) 2013 https://github.com/treefarm/haxebuildr-maven-plugin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.treefarm.plugins.haxe;

import io.treefarm.plugins.haxe.components.HaxeCompiler;
import io.treefarm.plugins.haxe.components.NativeBootstrap;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

@Mojo(name="clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractHaxeMojo {

    @Override
    protected void initialize(MavenProject project, ArtifactRepository localRepository) throws Exception
    {
        bootstrap.clean(project);
    }
}
