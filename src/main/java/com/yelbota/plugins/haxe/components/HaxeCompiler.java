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
import com.yelbota.plugins.haxe.components.nativeProgram.HaxelibNativeProgram;
import com.yelbota.plugins.haxe.utils.CompileTarget;
import com.yelbota.plugins.haxe.utils.HarMetadata;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

@Component(role = HaxeCompiler.class)
public final class HaxeCompiler {

    @Requirement(hint = "haxe")
    private NativeProgram haxe;

    @Requirement(hint = "haxelib")
    private HaxelibNativeProgram haxelib;

    @Requirement
    private Logger logger;

    private File outputDirectory;

    public void compileHxml(MavenProject project, File hxml, File workingDirectory) throws Exception
    {
        List<String> args;
        int returnValue;

        // get the list of libs in the hxml        
        String haxelibToInstall;
        Set<String> haxelibsToInstall = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new FileReader(hxml.getAbsolutePath()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            haxelibToInstall = StringUtils.substringAfter(line, "-lib ");
            if (!StringUtils.startsWith(line, "#")
                    && haxelibToInstall.length() > 0) {
                haxelibsToInstall.add(haxelibToInstall);
            }
        }

        // go through the list and if they are not installed, install them now
        Iterator<String> it = haxelibsToInstall.iterator();
        while(it.hasNext()) {
            haxelibToInstall = it.next();

            args = new ArrayList<String>();
            args.add("path");
            args.add(haxelibToInstall);
            returnValue = haxelib.execute(args);

            if (returnValue > 0) {
                args = new ArrayList<String>();
                args.add("install");
                args.add(haxelibToInstall);
                logger.info("Installing haxelib '"+haxelibToInstall+"'");
                returnValue = haxelib.execute(args, logger);
                if (returnValue > 0) {
                    throw new Exception("Haxelib was installing '"+haxelibToInstall+"', but has encountered an error and cannot proceed.");
                }
            }
        }

        // now that the libs are installed, compile the project
        args = new ArrayList<String>();
        args.add(hxml.getAbsolutePath());

        logger.info("Building '"+hxml.getName()+"'");
        returnValue = haxe.execute(args, workingDirectory);
        if (returnValue > 0) {
            throw new Exception("Haxe compiler was building '"+hxml.getName()+"', but has encountered an error and cannot proceed.");
        }
    }

    public void compile(MavenProject project, Map<CompileTarget, String> targets, String main, boolean debug, boolean includeTestSources, boolean verbose) throws Exception
    {
        compile(project, targets, main, debug, includeTestSources, verbose, null);
    }

    public void compile(MavenProject project, Map<CompileTarget, String> targets, String main, boolean debug, boolean includeTestSources, boolean verbose, List<String> additionalArguments) throws Exception
    {
        List<String> args = new ArrayList<String>();

        for (String sourceRoot : project.getCompileSourceRoots()) {
            addSourcePath(args, sourceRoot);
        }

        if (includeTestSources) {
            for (String sourceRoot : project.getTestCompileSourceRoots()) {
                addSourcePath(args, sourceRoot);
            }
        }

        addLibs(args, project);
        addHars(args, project, targets.keySet());
        addMain(args, main);
        addDebug(args, debug);

        if (additionalArguments != null)
            args.addAll(additionalArguments);

        for (CompileTarget target : targets.keySet()) {
            String output = targets.get(target);
            List<String> argsClone = new ArrayList<String>();
            argsClone.addAll(args);
            addTarget(argsClone, target);
            argsClone.add(output);
            haxe.execute(argsClone);
        }
    }

    private void addLibs(List<String> argumentsList, MavenProject project)
    {
        for (Artifact artifact : project.getArtifacts())
        {
            if (artifact.getType().equals(HaxeFileExtensions.HAXELIB)
                || artifact.getType().equals(HaxeFileExtensions.POM_HAXELIB))
            {
                String haxelibId = artifact.getArtifactId() + ":" + artifact.getVersion();
                argumentsList.add("-lib");
                argumentsList.add(haxelibId);
            }
        }
    }

    private void addTarget(List<String> args, CompileTarget target)
    {
        switch (target)
        {
            case java: {
                args.add("-java");
                break;
            }
            case neko: {
                args.add("-neko");
                break;
            }
        }
    }

    private void addDebug(List<String> argumentsList, boolean debug)
    {
        if (debug)
            argumentsList.add("-debug");
    }

    private void addHars(List<String> argumentsList, MavenProject project, Set<CompileTarget> targets)
    {
        File dependenciesDirectory = new File(outputDirectory, "dependencies");

        if (!dependenciesDirectory.exists())
            dependenciesDirectory.mkdir();

        for (Artifact artifact: project.getArtifacts())
        {
            if (artifact.getType().equals(HaxeFileExtensions.HAR))
            {
                File harUnpackDirectory = new File(dependenciesDirectory, artifact.getArtifactId());

                if (!harUnpackDirectory.exists())
                {
                    harUnpackDirectory.mkdir();
                    ZipUnArchiver unArchiver = new ZipUnArchiver();
                    unArchiver.enableLogging(logger);
                    unArchiver.setSourceFile(artifact.getFile());
                    unArchiver.setDestDirectory(harUnpackDirectory);
                    unArchiver.extract();
                }

                try
                {
                    File metadataFile = new File(harUnpackDirectory, HarMetadata.METADATA_FILE_NAME);
                    JAXBContext jaxbContext = JAXBContext.newInstance(HarMetadata.class, CompileTarget.class);
                    HarMetadata metadata = (HarMetadata) jaxbContext.createUnmarshaller().unmarshal(metadataFile);

                    if (!metadata.target.containsAll(targets))
                        logger.warn("Dependency " + artifact + " is not compatible with your compile targets.");
                }
                catch (JAXBException e)
                {
                    logger.warn("Can't read " + artifact + "metadata", e);
                }

                addSourcePath(argumentsList, harUnpackDirectory.getAbsolutePath());
            }
        }
    }

    private void addMain(List<String> argumentsList, String main)
    {
        argumentsList.add("-main");
        argumentsList.add(main);
    }

    private void addSourcePath(List<String> argumentsList, String sourcePath)
    {
        argumentsList.add("-cp");
        argumentsList.add(sourcePath);
    }

    public void setOutputDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }
}
