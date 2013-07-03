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

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.io.FileUtils;

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.yelbota.plugins.haxe.utils.HaxelibHelper;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgramException;

@Component(role = OpenFLNativeProgram.class, hint = "openfl")
public final class OpenFLNativeProgram extends AbstractNativeProgram {

    @Requirement(hint = "haxe")
    private NativeProgram haxe;

    @Requirement(hint = "neko")
    private NativeProgram neko;

    @Requirement(hint = "haxelib")
    private HaxelibNativeProgram haxelib;

    private File nmeDirectory;

    public void initialize(Artifact artifact, File outputDirectory, File pluginHome, Set<String> path, File nmeDirectory, File openflNativeDirectory)
    {
        if (nmeDirectory != null && nmeDirectory.exists()) {
            this.nmeDirectory = nmeDirectory;
        }
        initialize(artifact, outputDirectory, pluginHome, path);

        if (this.nmeDirectory != null
                && openflNativeDirectory != null && openflNativeDirectory.exists()) {
            File nmeNDLLDirectory = new File(nmeDirectory, "ndll");
            File openflNDLLDirectory = new File(openflNativeDirectory, "ndll");

            /*for (String fileName : nmeNDLLDirectory.list())
            {
                File ndllDirectory = new File(nmeNDLLDirectory, fileName);
                if (ndllDirectory.isDirectory()) {
                    FileUtils.copyDirectory(ndllDirectory);
                }
            }*/
            try {
                FileUtils.copyDirectory(nmeNDLLDirectory, openflNDLLDirectory);
            } catch (IOException e) {
                logger.error("Unable to copy NDLL files from NME to OpenFL Native");
            }
        }
    }

    @Override
    public void initialize(Artifact artifact, File outputDirectory, File pluginHome, Set<String> path)
    {
		super.initialize(artifact, outputDirectory, pluginHome, path);

        path.add("/bin");
        path.add("/usr/bin");

		try
        {
        	haxelib.execute("set", artifact.getArtifactId(), 
                HaxelibHelper.getCleanVersionForHaxelibArtifact(artifact.getVersion()));
        }
        catch (NativeProgramException e)
        {
            logger.error("Unable to set version for haxelib '"+artifact.getArtifactId()+"'. " + e);
        }
	}

    @Override
    protected String myName() { return "openfl"; }

	@Override
    protected File getDestinationDirectoryForArtifact(Artifact artifact) throws NativeProgramException
    {
        return HaxelibHelper.getHaxelibDirectoryForArtifactAndInitialize(artifact.getArtifactId(), artifact.getVersion(), logger);
    }

    @Override
    protected List<String> updateArguments(List<String> arguments)
    {
        List<String> list = new ArrayList<String>();

        // run OpenFL via haxelib
        File executable = new File(haxelib.getInstalledPath(), isWindows() ? "haxelib.exe" : "haxelib");
        list.add(executable.getAbsolutePath());
        list.add("run");
        list.add("openfl");
        list.addAll(arguments);

        return list;
    }

    @Override
    protected String[] getEnvironment()
    {
    	String haxeHome = haxe.getInstalledPath();
    	String nekoHome = neko.getInstalledPath();
    	String openflHome = getInstalledPath();
        String[] env = new String[]{
                "HAXEPATH=" + haxeHome,
                "NEKOPATH=" + nekoHome,
                "DYLD_LIBRARY_PATH=" + nekoHome + ":.",
                "LD_LIBRARY_PATH=" + nekoHome + ":.",
                "HAXE_LIBRARY_PATH=" + haxeHome + "/std:.",
                "HAXE_STD_PATH=" + haxeHome + "/std:.",
                "OPENFLPATH=" + openflHome,
                "PATH=" + StringUtils.join(path.iterator(), ":"),
                /*"OPENFL_LOAD_DEBUG=1",*/
                "HOME=" + pluginHome.getAbsolutePath()
        };
        if (this.nmeDirectory != null) {
            env = ArrayUtils.add(env, "NMEPATH=" + nmeDirectory.getAbsolutePath());
        }

        return env;
    }
}
