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

import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgram;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import com.yelbota.plugins.haxe.components.HaxeCompiler;
import com.yelbota.plugins.haxe.components.MUnitCompiler;
import com.yelbota.plugins.haxe.utils.CompileTarget;
import com.yelbota.plugins.haxe.utils.OutputNamesHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Requirement;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.File;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.apache.commons.io.FileUtils;

import org.xml.sax.*;
import org.w3c.dom.*;

/**
 * Compile tests with `neko` compile target.
 */
@Mojo(name = "testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
public class TestCompileMojo extends AbstractCompileMojo {

    private static final String TEST_RUNNER = "TestRunner";
    private static final String TEST_HXML = "test.hxml";
    private static final String TEST_BIN_PATH = "test_bin";

    /**
     * Test runner class.
     */
    @Parameter
    private String testRunner;

    /**
     * Test entrypoint
     */
    @Parameter
    private String testMain;

    /**
     * Test source class path
     */
    @Parameter
    private String testClasspath;

    @Parameter
    private String testHxml;

    @Parameter
    private String testResources;

    @Parameter
    private String testTemplates;

    @Parameter(required = false)
    protected Set<CompileTarget> testTargets;

    @Parameter
    private String testBinPath;

    /**
     * Compile with verbose output
     */
    @Parameter
    protected boolean verbose;

    @Parameter
    protected boolean testCoverage = false;

    @Component
    private HaxeCompiler haxeCompiler;

    @Component
    private MUnitCompiler munitCompiler;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        if (munitCompiler.getHasRequirements()) {

            if (openflIsActive() && testTargets != null && testClasspath != null) {
                String logInfo = "Compiling tests for MassiveUnit using OpenFL ";
                logInfo += (testCoverage ? "WITH code coverage" : "WITHOUT code coverage") + ".";
                getLog().info(logInfo);

                Set<String> classPaths = new HashSet<String>();
                String cleanClassPathList = "";
                try {
                    List<String> displayHxml = openflCompiler.displayHxml(project, testTargets.iterator().next(), nmml, null, null, null);
                    for (String line : displayHxml) {
                        String classPath = StringUtils.substringAfter(line, "-cp ");
                        if (classPath.length() > 0) {
                            classPaths.add(classPath);
                        }
                    }
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Tests compilation failed", e);
                }

                compilerFlags = new ArrayList<String>();
                compilerFlags.add("-lib munit");
                compilerFlags.add("-lib hamcrest");
                if (testCoverage && classPaths.size() > 0) {
                    compilerFlags.add("-lib mcover");
                    compilerFlags.add("-D MCOVER");

                    String mCoverDirective = "--macro mcover.MCover.coverage\\([\\'\\'],[\\'";
                    //String mCoverDirective = "--macro mcover.MCover.coverage([''],['";
                    Iterator<String> it = classPaths.iterator();
                    String classPath;
                    while(it.hasNext()) {
                        classPath = it.next();
                        if (!StringUtils.contains(classPath, ",")
                                && StringUtils.indexOf(classPath, "/") != 0) {
                            if (cleanClassPathList.length() > 0) {
                                cleanClassPathList += ",";
                            }
                            cleanClassPathList += classPath;
                        }
                    }

                    mCoverDirective += cleanClassPathList + "\\'],[\\'\\']\\)";
                    //mCoverDirective += cleanClassPathList + "'],[''])";
                    compilerFlags.add(mCoverDirective);
                }
                compilerFlags.add("-cp " + testClasspath);

                try
                {
                    if (testRunner == null) {
                        testRunner = TEST_RUNNER;
                    }
                    if (testHxml == null) {
                        testHxml = TEST_HXML;
                    }

                    List<String> displayHxml = openflCompiler.displayHxml(project, testTargets, nmml, compilerFlags, testMain, testRunner);

                    String hxmlDump = "";
                    for (String hxmlLine : displayHxml) {
                        hxmlDump += hxmlLine + "\n";
                    }

                    File hxmlFile = new File(outputDirectory, testHxml);
                    if (hxmlFile.exists()) {
                        FileUtils.deleteQuietly(hxmlFile);
                    }
                    hxmlFile.createNewFile();
                    FileWriter fw = new FileWriter(hxmlFile.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(hxmlDump);
                    bw.close();

                    if (testResources != null) {
                        File resourcesFile = new File(outputDirectory.getParentFile(), testResources);
                        File tmpResourcesFile = new File(outputDirectory, "tmp_resources");
                        tmpResourcesFile.mkdirs();
                        FileUtils.copyDirectory(resourcesFile, new File(tmpResourcesFile, resourcesFile.getName()));
                        testResources = tmpResourcesFile.getAbsolutePath();
                    }

                    if (testBinPath == null) {
                        testBinPath = TEST_BIN_PATH;
                    }
                    File testBinFile = new File(outputDirectory, testBinPath);
                    testBinPath = testBinFile.getAbsolutePath();

                    munitCompiler.config(
                        testClasspath,
                        testBinPath,
                        testBinPath,
                        cleanClassPathList,
                        hxmlFile.getAbsolutePath(),
                        testResources,
                        testTemplates);
                    openflCompiler.initialize(debug, verbose);
                    openflCompiler.compile(project, testTargets, nmml, compilerFlags, testMain, testRunner, true);
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Tests compilation failed", e);
                }
            } else {
                getLog().info("Compiling tests using MassiveUnit.");

                try
                {
                    munitCompiler.setOutputDirectory(outputDirectory);
                    munitCompiler.compile(project, null);
                }
                catch (Exception e)
                {
                    throw new MojoFailureException("Tests compilation failed", e);
                }
            }
        } else {
            getLog().info("Compiling tests using standard Haxe unit testing.");

            if (testRunner == null || project.getTestCompileSourceRoots().size() == 0) {
                getLog().info("No test sources to compile");
                return;
            }

            String output = OutputNamesHelper.getTestOutput(project);
            EnumMap<CompileTarget, String> targets = new EnumMap<CompileTarget, String>(CompileTarget.class);
            targets.put(CompileTarget.neko, output);

            try
            {
                haxeCompiler.compile(project, targets, testRunner, true, true, verbose);
            }
            catch (Exception e)
            {
                throw new MojoFailureException("Tests compilation failed", e);
            }
        }
    }
}