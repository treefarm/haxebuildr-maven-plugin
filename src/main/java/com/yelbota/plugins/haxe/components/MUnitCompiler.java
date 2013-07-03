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
import com.yelbota.plugins.haxe.utils.CompileTarget;
import com.yelbota.plugins.haxe.utils.HarMetadata;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(role = MUnitCompiler.class)
public final class MUnitCompiler {

    @Requirement(hint = "munit")
    private NativeProgram munit;

    @Requirement
    private Logger logger;

    private File outputDirectory;

    private boolean debug = false;
    private boolean verbose = false;
    private boolean generateDoc = false;

    public void initialize(boolean debug, boolean verbose)
    {
        this.debug = debug;
        this.verbose = verbose;
    }

    public void config(String src, String bin, String report, String classPaths, String hxml, String resources, String templates) throws Exception
    {
        List<String> list;

        list = new ArrayList<String>();
        list.add("-delete");
        runWithArguments("config", list);

        list = new ArrayList<String>();
        if (src != null) {
            list.add("-src");
            list.add(src);
        }
        if (bin != null) {
            list.add("-bin");
            list.add(bin);
        }
        if (report != null) {
            list.add("-report");
            list.add(report);
        }
        if (classPaths != null && classPaths.length() > 0) {
            list.add("-classPaths");
            list.add(classPaths);
        }
        if (hxml != null) {
            list.add("-hxml");
            list.add(hxml);
        }
        if (resources != null) {
            list.add("-resources");
            list.add(resources);
        }
        if (templates != null) {
            list.add("-templates");
            list.add(templates);
        }
        runWithArguments("config", list);
    } 

    public void compile(MavenProject project, Set<CompileTarget> targets) throws Exception
    {
        compile(project, targets, null);
    }

    public void compile(MavenProject project, Set<CompileTarget> targets, List<String> additionalArguments) throws Exception
    {
        List<String> list = new ArrayList<String>();
        list.add("test.hxml");
        list.add("test_src");
        list.add("test_bin");
        list.add("test_bin");
        list.add("-coverage");
        list.add("-result-exit-code");
        list.add("-norun");
        if (additionalArguments != null) {
            list.addAll(additionalArguments);
        }
        runWithArguments("test", list);
    }

    public void run(MavenProject project, String testBrowser, boolean testKillBrowser, String testDisplay) throws Exception
    {
        if (testDisplay != null) {
            munit.setDisplay(testDisplay);
        }

        List<String> list = new ArrayList<String>();
        //list.add(this.outputDirectory.getAbsolutePath());
        //list.add(testReportPath);
        list.add("-result-exit-code");
        if (testKillBrowser) {
            list.add("-kill-browser");
        }
        if (testBrowser != null) {
            list.add("-browser");
            list.add(testBrowser);
        } 
        runWithArguments("run", list);
    }

    private void runWithArguments(String command, List<String> arguments) throws Exception
    {
        List<String> list = new ArrayList<String>();
        if (command != null) {
            list.add(command);
        }
        list.addAll(arguments);
        int returnValue = munit.execute(list, null, logger, true);

        if (returnValue > 0) {
            throw new Exception("MassiveUnit test encountered an error and cannot proceed.");
        }
    }

    public boolean getHasRequirements()
    {
        return munit != null && munit.getInitialized();
    }

    public void setOutputDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }
}
