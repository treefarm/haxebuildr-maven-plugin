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

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;

import com.yelbota.plugins.haxe.utils.CleanStream;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgramException;

@Component(role = NativeProgram.class, hint = "munit")
public final class MUnitNativeProgram extends AbstractNativeProgram {

    @Requirement(hint = "haxe")
    private NativeProgram haxe;

    @Requirement(hint = "neko")
    private NativeProgram neko;

    @Requirement(hint = "haxelib")
    private HaxelibNativeProgram haxelib;

    @Override
    public void initialize(Artifact artifact, File outputDirectory, File pluginHome, Set<String> path)
    {
		super.initialize(artifact, outputDirectory.getParentFile(), pluginHome, path);

		try
        {
        	haxelib.execute("set", artifact.getArtifactId(), artifact.getVersion());
        }
        catch (NativeProgramException e)
        {
            System.out.println("Unable to set version for haxelib '"+artifact.getArtifactId()+"'. " + e);
        }
	}

    @Override
    protected CleanStream.CleanStreamType getErrorStream()
    {
        return CleanStream.CleanStreamType.DEBUG;
    }

    @Override
    protected List<String> updateArguments(List<String> arguments)
    {
        List<String> list = new ArrayList<String>();

        // run MUnit via haxelib
        File executable = new File(haxelib.getInstalledPath(), isWindows() ? "haxelib.exe" : "haxelib");
        list.add(executable.getAbsolutePath());
        list.add("run");
        list.add("munit");
        list.addAll(arguments);

        return list;
    }

    @Override
    protected String myName() { return "munit"; }

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
                "OPENFLPATH=" + openflHome
        };
        return ArrayUtils.addAll(super.getEnvironment(), env);
    }
}
