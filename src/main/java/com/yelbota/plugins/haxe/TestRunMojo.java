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
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgramException;
import com.yelbota.plugins.haxe.utils.OutputNamesHelper;
import com.yelbota.plugins.haxe.components.MUnitCompiler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Run tests with `neko`.
 */
@Mojo(name = "testRun", defaultPhase = LifecyclePhase.TEST)
public final class TestRunMojo extends AbstractHaxeMojo {

    @Component(hint = "neko")
    private NativeProgram neko;

    @Component
    private MUnitCompiler munitCompiler;

    @Parameter
    private String testBrowser;

    @Parameter
    private boolean testKillBrowser;

    @Parameter
    private String testDisplay;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        if (munitCompiler.getHasRequirements()) {
            String outputInfo = "Running tests using MassiveUnit.";
            if (testBrowser != null) {
                outputInfo += "\n Using browser '"+testBrowser+"'";
            }
            if (testDisplay != null) {
                outputInfo += "\n on display '"+testDisplay+"'";
            }
            getLog().info(outputInfo);

            try
            {
                munitCompiler.setOutputDirectory(outputDirectory);
                munitCompiler.run(project, testBrowser, testKillBrowser, testDisplay);
            }
            catch (Exception e)
            {
                throw new MojoFailureException("Test failed", e);
            }
        } else {
            getLog().info("Running tests using standard Haxe unit testing.");

            try
            {
                File testFile = new File(outputDirectory, OutputNamesHelper.getTestOutput(project));

                if (testFile.exists())
                {
                    neko.execute(testFile.getAbsolutePath());
                }
                else getLog().info("No tests to run.");
            }
            catch (NativeProgramException e)
            {
                throw new MojoFailureException("Test failed", e);
            }
        }
    }
}
