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
package com.yelbota.plugins.haxe.components.nativeProgram;

import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import com.yelbota.plugins.haxe.utils.HaxelibHelper;
import com.yelbota.plugins.haxe.utils.CleanStream;
import com.yelbota.plugins.nd.UnpackHelper;
import com.yelbota.plugins.nd.utils.DefaultUnpackMethods;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.apache.commons.io.FileUtils;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents package on native application (or group on applications) as
 * simple executor arguments
 */
public abstract class AbstractNativeProgram implements NativeProgram {

    //-------------------------------------------------------------------------
    //
    //  Injections
    //
    //-------------------------------------------------------------------------

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    protected Logger logger;

    //-------------------------------------------------------------------------
    //
    //  Fields
    //
    //-------------------------------------------------------------------------

    protected Artifact artifact;

    protected File outputDirectory;

    protected File pluginHome;

    protected Set<String> path;

    protected File directory;

    protected String display;

    private boolean initialized = false;

    //-------------------------------------------------------------------------
    //
    //  Public methods
    //
    //-------------------------------------------------------------------------

    public void initialize(Artifact artifact, File outputDirectory, File pluginHome, Set<String> path)
    {
        if (initialized)
            return;

        this.artifact = artifact;
        this.outputDirectory = outputDirectory;
        this.pluginHome = pluginHome;
        this.path = path;

        if (!isHaxelibProgram()) {
            try
            {
                this.directory = getDirectory(artifact);
            }
            catch (Exception e)
            {
                logger.error(String.format("Can't unpack %s: %s", artifact.getArtifactId(), e));
            }
        }
    }

    private boolean isHaxelibProgram()
    {
        return (artifact.getType().equals(HaxeFileExtensions.HAXELIB)
                || (artifact.getClassifier() != null 
                    && artifact.getClassifier().equals(HaxeFileExtensions.HAXELIB))
                );
    }

    public int execute(List<String> arguments) throws NativeProgramException
    {
        return execute(arguments, null, null);
    }

    public int execute(List<String> arguments, File workingDirectory) throws NativeProgramException
    {
        return execute(arguments, workingDirectory, logger);
    }

    public int execute(List<String> arguments, Logger outputLogger) throws NativeProgramException
    {
        return execute(arguments, null, outputLogger);
    }

    public int execute(List<String> arguments, File workingDirectory, Logger outputLogger) throws NativeProgramException
    {
        return execute(arguments, workingDirectory, outputLogger, false);
    }

    public int execute(List<String> arguments, File workingDirectory, Logger outputLogger, boolean tolerateErrors) throws NativeProgramException
    {
        Process process = getProcessForExecute(arguments, workingDirectory);
        return processExecution(process, outputLogger, tolerateErrors);
    }

    public BufferedReader executeIntoBuffer(List<String> arguments) throws NativeProgramException
    {
        try
        {
            Process process = getProcessForExecute(arguments);
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            return br;
        }
        catch (InterruptedException e)
        {
            throw new NativeProgramException("Program was interrupted", e);
        }
    }

    private Process getProcessForExecute(List<String> arguments) throws NativeProgramException
    {
        return getProcessForExecute(arguments, null);
    }

    private Process getProcessForExecute(List<String> arguments, File workingDirectory) throws NativeProgramException
    {
        try
        {
            String[] environment = getEnvironment();
            arguments = updateArguments(arguments);
            if (workingDirectory == null || !workingDirectory.exists()) {
                workingDirectory = outputDirectory;
            }
            logger.info("Executing in '"+workingDirectory.getAbsolutePath()+"': " + StringUtils.join(arguments.iterator(), " "));

            for( int i = 0; i <= environment.length - 1; i++)
            {
                logger.debug(" " + environment[i]);
            }

            Process process = Runtime.getRuntime().exec(
                    arguments.toArray(new String[]{}),
                    environment,
                    workingDirectory
            );

            return process;
        }
        catch (IOException e)
        {
            throw new NativeProgramException("Executable not found", e);
        }
        catch (Exception e)
        {
            throw new NativeProgramException("", e);
        } 
    }

    @Override
    public int execute(String[] arguments) throws NativeProgramException
    {
        List<String> list = new ArrayList<String>();

        for (String arg : arguments)
            list.add(arg);

        return execute(list);
    }

    @Override
    public int execute(String arg1) throws NativeProgramException
    {
        return execute(new String[]{arg1});
    }

    @Override
    public int execute(String arg1, String arg2) throws NativeProgramException
    {
        return execute(new String[]{arg1, arg2});
    }

    @Override
    public int execute(String arg1, String arg2, String arg3) throws NativeProgramException
    {
        return execute(new String[]{arg1, arg2, arg3});
    }

    @Override
    public int execute(String arg1, String arg2, String arg3, String arg4) throws NativeProgramException
    {
        return execute(new String[]{arg1, arg2, arg3, arg4});
    }

    protected String[] getEnvironment()
    {
        String[] env = new String[]{
                "PATH=" + StringUtils.join(path.iterator(), ":"),
                "HOME=" + pluginHome.getAbsolutePath()
        };
        if (this.display != null) {
            env = ArrayUtils.add(env, "DISPLAY=" + this.display);
        }
        return env;
    }

    //-------------------------------------------------------------------------
    //
    //  Protected methods
    //
    //-------------------------------------------------------------------------

    protected CleanStream.CleanStreamType getErrorStream()
    {
        return CleanStream.CleanStreamType.ERROR;
    }

    protected abstract List<String> updateArguments(List<String> arguments);

    protected int processExecution(Process process, Logger outputLogger, boolean tolerateErrors) throws NativeProgramException
    {
        try
        {
            CleanStream cleanError = new CleanStream(
                    process.getErrorStream(),
                    outputLogger, getErrorStream(), myName()
            );

            CleanStream cleanOutput = new CleanStream(
                    process.getInputStream(),
                    outputLogger, CleanStream.CleanStreamType.INFO, myName()
            );

            cleanError.start();
            cleanOutput.start();

            int returnValue = process.waitFor();

            if (!tolerateErrors) {
                int errorCount = cleanError.getCount();
                if (returnValue == 0 && errorCount > 0) {
                    returnValue = errorCount;
                }
            }

            return returnValue;
        }
        catch (InterruptedException e)
        {
            throw new NativeProgramException("Program was interrupted", e);
        }
    }

    protected String myName() { return "abstract"; }

    public Set<String> getPath()
    {
        return path;
    }

    public String getInstalledPath()
    {
        return this.directory.getAbsolutePath();
    }

    public boolean getInitialized()
    {
        if (!initialized) { 
            if (artifact != null && isHaxelibProgram()) {
                File haxelibDirectory = HaxelibHelper.getHaxelibDirectoryForArtifactAndInitialize(artifact.getArtifactId(), artifact.getVersion(), logger);
                if (haxelibDirectory != null || haxelibDirectory.exists()) {
                    this.directory = haxelibDirectory;
                    initialized = true;
                }
            }
        }
        return initialized;
    }

    protected File getDestinationDirectoryForArtifact(Artifact artifact) throws NativeProgramException
    {
        return new File(pluginHome, getUniqueArtifactPath());
    }

    private String getUniqueArtifactPath()
    {
        return artifact.getArtifactId() + "-" + artifact.getVersion();
    }

    private File getDirectory(Artifact artifact) throws Exception
    {
        File destinationDirectory = getDestinationDirectoryForArtifact(artifact);

        if (!destinationDirectory.exists()
            || artifact.getFile().lastModified() > destinationDirectory.lastModified())
        {
            if (destinationDirectory.exists()) {
                FileUtils.deleteQuietly(destinationDirectory);
            }
            File unpackDir = new File(outputDirectory, getUniqueArtifactPath() + "-unpack");

            if (unpackDir.exists())
                FileUtils.deleteQuietly(unpackDir);

            UnpackHelper unpackHelper = new UnpackHelper() {};
            DefaultUnpackMethods unpackMethods = new DefaultUnpackMethods(logger);
            unpackHelper.unpack(unpackDir, artifact, unpackMethods, null);

            for (String fileName : unpackDir.list())
            {
                if (artifact.getType().equals("jar")) {
                    fileName = getUniqueArtifactPath();
                }
                File firstFile = new File(unpackDir, fileName);
                //FileUtils.moveDirectory(firstFile, destinationDirectory);
                firstFile.renameTo(destinationDirectory);
                if (!destinationDirectory.exists()) {
                    // manually move using shell call as last ditch
                    Process process = Runtime.getRuntime().exec(
                            "mv " + firstFile.getAbsolutePath() + " " + destinationDirectory.getAbsolutePath()
                    );
                }
                break;
            }

            if (destinationDirectory.exists()) {
                destinationDirectory.setLastModified(artifact.getFile().lastModified());
            }
            if (unpackDir.exists())
                FileUtils.deleteQuietly(unpackDir);
        }

        initialized = true;
        String directoryPath = destinationDirectory.getAbsolutePath();
        // Add current directory to path
        path.add(directoryPath);
        return destinationDirectory;
    }

    public void setDisplay(String display)
    {
        this.display = display;
    }

    protected boolean isWindows()
    {
        String systemName = System.getProperty("os.name");
        String preparedName = systemName.toLowerCase();

        return preparedName.indexOf("win") > -1;
    }
}