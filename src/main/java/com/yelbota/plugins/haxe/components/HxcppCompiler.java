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
package com.yelbota.plugins.haxe.components;

import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.HxcppNativeProgram;
import com.yelbota.plugins.haxe.utils.CompileTarget;
import com.yelbota.plugins.haxe.utils.HarMetadata;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(role = HxcppCompiler.class)
public final class HxcppCompiler {
    @Requirement(hint = "hxcpp")
    private NativeProgram hxcpp;

    @Requirement
    private Logger logger;

    private boolean debug = false;

    private File outputDirectory;


    public void initialize()
    {
        initialize(false);
    }

    public void initialize(boolean debug)
    {
        this.debug = debug;
    }

    public void compile(MavenProject project, File projectFile, File workingDirectory, List<String> additionalArguments) throws Exception
    {
        //haxelib run hxcpp Build.xml -DHXCPP_CLANG -DMACOSX_DEPLOYMENT_TARGET=10.7 -Ddebug
        List<String> args = new ArrayList<String>();

        args.add(projectFile.getAbsolutePath());

        if (additionalArguments != null) {
            args.addAll(additionalArguments);
        }

        if (this.debug) {
            args.add("-Ddebug");
        }

        execute(args, workingDirectory);
    }

    private void execute(List<String> arguments, File workingDirectory) throws Exception
    {
        List<String> list = new ArrayList<String>();
        list.addAll(arguments);
        int returnValue = hxcpp.execute(list, workingDirectory, logger);

        if (returnValue > 0) {
            throw new Exception("Hxcpp compiler encountered an error and cannot proceed.");
        }
    }
}
