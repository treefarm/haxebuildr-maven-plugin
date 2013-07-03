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

import com.yelbota.plugins.haxe.components.HxcppCompiler;
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
import org.apache.commons.io.FileUtils;

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
@Mojo(name = "compileHxcpp", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CompileHxcppMojo extends AbstractCompileMojo {

    @Component
    protected HxcppCompiler hxcppCompiler;

    //@Parameter(required = true)
    //protected String buildProjectFile;

    @Parameter(required = true)
    protected String hxcppProjectFile;

    @Parameter(required = false)
    protected List<String> compilerFlags;

    @Parameter(required = false)
    protected List<String> cacheFiles;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        File projectFile;

        if (cacheFiles != null) {
            File cacheFile;
            for (String cacheFileName : cacheFiles) {
                getLog().info("Deleting cache '"+cacheFileName+"'.");
                cacheFile = new File(cacheFileName);
                FileUtils.deleteQuietly(cacheFile);
            }
        }

        if (hxcppProjectFile != null) {
            projectFile = new File(hxcppProjectFile);
            if (projectFile.exists()) {
                getLog().info("Building Hxcpp project '"+projectFile.getName()+"'.");
                try
                {
                    hxcppCompiler.initialize(debug);
                    hxcppCompiler.compile(project, projectFile, projectFile.getParentFile(), compilerFlags);
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Hxcpp build failed ", e);
                }
            } else {
                throw new MojoFailureException("Hxcpp project file '"+projectFile.getName()+"' does not exist!");
            }
        }
    }
}
