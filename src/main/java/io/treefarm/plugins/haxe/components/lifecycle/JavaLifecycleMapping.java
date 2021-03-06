/**
 * Copyright (C) 2013 https://github.com/treefarm/haxebuildr-maven-plugin
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
package io.treefarm.plugins.haxe.components.lifecycle;

import io.treefarm.plugins.haxe.utils.HaxeFileExtensions;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = LifecycleMapping.class, hint = HaxeFileExtensions.JAR )
public class JavaLifecycleMapping extends AbstractHaxeLifecycleMapping implements LifecycleMapping
{
    public String getCompiler()
    {
        return "io.treefarm.plugins:haxebuildr-maven-plugin:compileJava";
    }

}