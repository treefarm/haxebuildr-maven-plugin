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

public class PackageTypes {

    public static final String JAR = "jar";
    public static final String ZIP = "zip";
    public static final String TGZ = "tgz";
    public static final String TARGZ = "tar.gz";
    public static final String DEFAULT = JAR;

    @Nonnull
    public static final String getSDKArtifactPackaging(String classifier)
    {
        /*if (classifier.equals(OSClassifiers.WINDOWS))
        {
            return PackageTypes.ZIP;
        } else
        {
            return PackageTypes.TGZ;
        }*/
        return PackageTypes.DEFAULT;
    }
}
