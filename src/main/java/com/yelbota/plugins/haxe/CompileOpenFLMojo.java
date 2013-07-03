/**
 * Copyright (C) 2012 https://github.com/yelbota/haxe-maven-plugin
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
package com.yelbota.plugins.haxe;

import com.yelbota.plugins.haxe.components.OpenFLCompiler;
import com.yelbota.plugins.haxe.utils.CompileTarget;
import com.yelbota.plugins.haxe.utils.HarMetadata;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Component;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.handler.ArtifactHandler;

import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;
import java.util.Set;
import java.util.List;

/**
 * Builds a `har` package. This is a zip archive which
 * contains metainfo about supported compilation targets.
 */
@Mojo(name = "compileOpenFL", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CompileOpenFLMojo extends AbstractCompileMojo {

    @Parameter(required = false)
    protected String buildCommandLineHxml;

    @Parameter(required = false)
    protected String buildRunScriptHxml;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        File hxml;

        if (buildCommandLineHxml != null) {
            getLog().info("Building OpenFL command line tools.");
            hxml = new File(buildCommandLineHxml);
            if (hxml.exists()) {
                try
                {
                    compiler.compileHxml(project, hxml, hxml.getParentFile());
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Command line build failed ", e);
                }
            } else {
                throw new MojoFailureException("hxml file '"+buildCommandLineHxml+"' does not exist!");
            }
        } else {
            getLog().info("Not building OpenFL command line tools.");
        }

        if (buildRunScriptHxml != null) {
            getLog().info("Building OpenFL run script.");
            hxml = new File(buildRunScriptHxml);
            if (hxml.exists()) {
                try
                {
                    compiler.compileHxml(project, hxml, hxml.getParentFile());
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Run script build failed ", e);
                }
            } else {
                throw new MojoFailureException("hxml file '"+buildRunScriptHxml+"' does not exist!");
            }
        } else {
            getLog().info("Not building OpenFL run script.");
        }
    }

    @Override
    protected void initialize(MavenProject project, ArtifactRepository localRepository) throws Exception
    {
        super.initialize(project, localRepository);
    }
}
