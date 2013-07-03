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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component(role = NativeProgram.class, hint = "haxe", isolatedRealm = true)
public final class HaxeNativeProgram extends AbstractNativeProgram {

    @Requirement(hint = "neko")
    private NativeProgram neko;

    @Override
    protected List<String> updateArguments(List<String> arguments)
    {
        List<String> list = new ArrayList<String>();
        File executable = new File(directory, isWindows() ? "haxe.exe" : "haxe");
        list.add(executable.getAbsolutePath());
        list.addAll(arguments);

        return list;
    }

    @Override
    protected String myName() { return "haxe"; }

    @Override
    protected String[] getEnvironment()
    {
    	String haxeHome = getInstalledPath();
    	String nekoHome = neko.getInstalledPath();
        String[] env = new String[]{
                "HAXEPATH=" + haxeHome,
                "NEKOPATH=" + nekoHome,
                "DYLD_LIBRARY_PATH=" + nekoHome + ":.",
                "LD_LIBRARY_PATH=" + nekoHome + ":.",
                "HAXE_LIBRARY_PATH=" + haxeHome + "/std:.",
                "HAXE_STD_PATH=" + haxeHome + "/std:.",
                "PATH=" + StringUtils.join(path.iterator(), ":"),
                "HOME=" + pluginHome.getAbsolutePath()
        };
        return env;
    }
}

