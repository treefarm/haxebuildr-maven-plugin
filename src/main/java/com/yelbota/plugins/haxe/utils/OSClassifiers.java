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
package com.yelbota.plugins.haxe.utils;

import javax.annotation.Nonnull;

public class OSClassifiers {

    public static final String OSX = "osx";
    public static final String WINDOWS = "windows";
    public static final String LINUX = "linux";

    @Nonnull
    public static final String getDefaultClassifier() throws Exception
    {
        String systemName = System.getProperty("os.name");
        String preparedName = systemName.toLowerCase();

        if (preparedName.indexOf("win") > -1)
        {
            return WINDOWS;
        } else if (preparedName.indexOf("lin") > -1)
        {
            String arch = System.getProperty("os.arch");
            if (arch.indexOf("64") > -1)
            {
                return LINUX + "64";
            }
            return LINUX;
        } else if (preparedName.indexOf("mac") > -1)
        {
            return OSX;
        } else
        {
            throw new Exception(systemName + " is not supported");
        }
    }
}