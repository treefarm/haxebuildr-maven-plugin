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

import com.yelbota.plugins.haxe.components.HaxeCompiler;
import com.yelbota.plugins.haxe.utils.CompileTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.EnumMap;

@Mojo(name = "compileNeko", defaultPhase = LifecyclePhase.COMPILE)
/**
 * Compile in nekovm bytecode.
 */
public class CompileNekoMojo extends AbstractCompileMojo {

    @Component
    private HaxeCompiler compiler;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        File output = new File(outputDirectory, project.getBuild().getFinalName() + ".n");

        if (output.exists())
            output.delete();

        EnumMap<CompileTarget, String> targets = new EnumMap<CompileTarget, String>(CompileTarget.class);
        targets.put(CompileTarget.neko, output.getName());

        try
        {
            compiler.compile(project, targets, main, debug, false, verbose);
        }
        catch (Exception e)
        {
            throw new MojoFailureException("Neko compilation failed", e);
        }

        if (output.exists())
            project.getArtifact().setFile(output);
    }
}