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

import javax.annotation.Nonnull;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import com.yelbota.plugins.haxe.utils.HaxelibHelper;
import com.yelbota.plugins.haxe.utils.PackageTypes;
import com.yelbota.plugins.haxe.utils.OSClassifiers;
import com.yelbota.plugins.haxe.components.nativeProgram.HaxelibNativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.OpenFLNativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgramException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.*;

@Component(role = NativeBootstrap.class)
public class NativeBootstrap {

    //-------------------------------------------------------------------------
    //
    //  Injection
    //
    //-------------------------------------------------------------------------

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement(hint = "haxe")
    private NativeProgram haxe;

    @Requirement(hint = "neko")
    private NativeProgram neko;

    @Requirement(hint = "haxelib")
    private HaxelibNativeProgram haxelib;

    @Requirement(hint = "openfl")
    private OpenFLNativeProgram openfl;

    @Requirement(hint = "hxcpp")
    private NativeProgram hxcpp;

    @Requirement(hint = "munit")
    private NativeProgram munit;

    @Requirement(hint = "chxdoc")
    private NativeProgram chxdoc;

    @Requirement
    private Logger logger;

    //-------------------------------------------------------------------------
    //
    //  Fields
    //
    //-------------------------------------------------------------------------

    private MavenProject project;

    private ArtifactRepository localRepository;

    //-------------------------------------------------------------------------
    //
    //  Public
    //
    //-------------------------------------------------------------------------

    public void initialize(MavenProject project, ArtifactRepository localRepository) throws Exception
    {
        this.project = project;
        this.localRepository = localRepository;

        Map<String, Plugin> pluginMap = project.getBuild().getPluginsAsMap();
        Plugin plugin = pluginMap.get("com.yelbota.plugins:haxe-maven-plugin");
        File pluginHome = initializePluginHome(project, plugin);

        if (!pluginHome.exists())
            pluginHome.mkdirs();

        initializePrograms(project, pluginHome, plugin.getDependencies());
        initializeHaxelib(pluginHome);
    }

    private File initializePluginHome(MavenProject project, Plugin plugin) throws Exception
    {
        Artifact pluginArtifact = resolveArtifact(repositorySystem.createPluginArtifact(plugin), false);
        //String pluginHomeName = plugin.getArtifactId() + "-" + plugin.getVersion();
        String pluginHomeName = "home";
        return new File(pluginArtifact.getFile().getParentFile(), pluginHomeName);
    }

    public void clean(MavenProject project) throws Exception
    {
        Map<String, Plugin> pluginMap = project.getBuild().getPluginsAsMap();
        Plugin plugin = pluginMap.get("com.yelbota.plugins:haxe-maven-plugin");
        File pluginHome = initializePluginHome(project, plugin);
        if (pluginHome.exists()) {
            logger.info("Deleting " + pluginHome.getAbsolutePath());
            FileUtils.deleteQuietly(pluginHome);
        }
    }

    //-------------------------------------------------------------------------
    //
    //  Private methods
    //
    //-------------------------------------------------------------------------

    private void initializePrograms(MavenProject project, File pluginHome, List<Dependency> pluginDependencies) throws Exception
    {
        Map<String, Artifact> artifactsMap = new HashMap<String, Artifact>();
        Set<String> path = new HashSet<String>();
        File outputDirectory = getOutputDirectory();

        // Add java to PATH
        path.add(new File(System.getProperty("java.home"), "bin").getAbsolutePath());

        for (Dependency dependency : pluginDependencies)
        {
            String artifactKey = dependency.getGroupId() + ":" + dependency.getArtifactId();

            if (artifactKey.equals(HAXE_COMPILER_KEY) 
                || artifactKey.equals(NEKO_KEY)
                || artifactKey.equals(NME_KEY)
                || artifactKey.equals(HXCPP_KEY)
                || StringUtils.startsWith(artifactKey, OPENFL_KEY))
            {
            /*    String classifier = OSClassifiers.getDefaultClassifier();
                String packaging = PackageTypes.getSDKArtifactPackaging(classifier);
                if (artifactKey.equals(OPENFL_KEY)) classifier = null;

                Artifact artifact = repositorySystem.createArtifactWithClassifier(
                        dependency.getGroupId(), 
                        dependency.getArtifactId(), 
                        dependency.getVersion(),
                        packaging,
                        classifier
                );

                Artifact resolvedArtifact = resolveArtifact(artifact, true);
                boolean resolvedLocally = (resolvedArtifact != null);
                if (!resolvedLocally) {
                    resolvedArtifact = resolveArtifact(artifact, false);
                }
                if (resolvedArtifact != null) {
                    resolvedArtifact.setVersion(artifact.getVersion());
                    artifactsMap.put(artifactKey, resolvedArtifact);
                }*/

                Artifact artifact = repositorySystem.createArtifactWithClassifier(
                        dependency.getGroupId(), 
                        dependency.getArtifactId(), 
                        dependency.getVersion(),
                        dependency.getType(),
                        dependency.getClassifier()
                );
                Artifact resolvedArtifact = resolveArtifact(artifact, false);
                if (resolvedArtifact != null) {
                    artifactsMap.put(artifactKey, resolvedArtifact);
                }
            }
        }

        if (artifactsMap.get(NEKO_KEY) == null)
        {
            throw new Exception(String.format(
                    "Neko Runtime dependency (%s) not found in haxe-maven-plugin dependencies",
                    NEKO_KEY));
        }

        if (artifactsMap.get(HAXE_COMPILER_KEY) == null)
        {
            throw new Exception(String.format(
                    "Haxe Compiler dependency (%s) not found in haxe-maven-plugin dependencies",
                    HAXE_COMPILER_KEY));
        }

        neko.initialize(artifactsMap.get(NEKO_KEY), outputDirectory, pluginHome, path);
        haxe.initialize(artifactsMap.get(HAXE_COMPILER_KEY), outputDirectory, pluginHome, path);
        haxelib.initialize(artifactsMap.get(HAXE_COMPILER_KEY), outputDirectory, pluginHome, path);
        HaxelibHelper.setHaxelib(haxelib);

        Iterator<Artifact> iterator;
        Set<Artifact> projectDependencies = project.getDependencyArtifacts();
        if (projectDependencies != null) {
            iterator = projectDependencies.iterator();
            while(iterator.hasNext()) {
                Artifact a = iterator.next();

                if (a.getType().equals(HaxeFileExtensions.HAXELIB)) {
                    File haxelibDirectory = HaxelibHelper.getHaxelibDirectoryForArtifact(a.getArtifactId(), a.getVersion());

                    if (haxelibDirectory != null && haxelibDirectory.exists()) {
                        iterator.remove();
                    }
                } else {
                    if (a.getGroupId().equals(HaxelibHelper.HAXELIB_GROUP_ID)) {
                        /*String packaging = PackageTypes.getSDKArtifactPackaging(OSClassifiers.getDefaultClassifier());
                        Artifact artifact = repositorySystem.createArtifactWithClassifier(
                            a.getGroupId(), a.getArtifactId(), a.getVersion(),
                            packaging, null
                        );
                        Artifact resolvedArtifact = resolveArtifact(artifact, true);
                        boolean resolvedLocally = (resolvedArtifact != null);
                        if (!resolvedLocally) {
                            resolvedArtifact = resolveArtifact(artifact, false);
                        }
                        if (resolvedArtifact != null) {
                            HaxelibHelper.injectPomHaxelib(resolvedArtifact, outputDirectory, logger, resolvedLocally);
                            iterator.remove();
                        }*/
                        /*Artifact resolvedArtifact = resolveArtifact(a, false);
                        if (resolvedArtifact != null) {
                            HaxelibHelper.injectPomHaxelib(a, outputDirectory, logger);
                        }*/

                        Artifact artifact = repositorySystem.createArtifactWithClassifier(
                            a.getGroupId(),
                            a.getArtifactId(),
                            a.getVersion(),
                            a.getType(),
                            null
                        );

                        Artifact resolvedArtifact = resolveArtifact(artifact, false);
                        if (resolvedArtifact != null && resolvedArtifact.getFile() != null) {
                            HaxelibHelper.injectPomHaxelib(
                                a.getArtifactId(),
                                a.getVersion(),
                                a.getType(),
                                a.getFile(),
                                logger
                            );
                        }
                    }
                }

                if (a.getArtifactId().equals(MUNIT_ID)) {
                    munit.initialize(a, outputDirectory, pluginHome, path);
                }

                if (a.getArtifactId().equals(CHXDOC_ID)) {
                    chxdoc.initialize(a, outputDirectory, pluginHome, path);
                }
            }
        }

        Iterator<String> mapIterator = artifactsMap.keySet().iterator();
        while (mapIterator.hasNext()) {
            String key = mapIterator.next();
            if (key.equals(NME_KEY)
                    || StringUtils.startsWith(key, OPENFL_KEY)) {
                if (projectDependencies != null) {
                    iterator = projectDependencies.iterator();
                    while(iterator.hasNext()) {
                        Artifact a = iterator.next();
                        if (a.getType().equals(HaxeFileExtensions.HAXELIB)
                            && StringUtils.startsWith(a.getArtifactId(), OPENFL_ARTIFACT_ID_PREFIX)
                            && (a.getVersion() == null 
                                || a.getVersion().equals("")
                                || a.getVersion().equals(artifactsMap.get(key).getVersion()))) {
                            iterator.remove();
                        }
                    }
                }
                // inject all openfl accessory libs
                Artifact a = artifactsMap.get(key);
                Artifact resolvedArtifact = resolveArtifact(a, false);
                if (resolvedArtifact != null && resolvedArtifact.getFile() != null) {
                    HaxelibHelper.injectPomHaxelib(
                        a.getArtifactId(),
                        a.getVersion(),
                        a.getType(),
                        a.getFile(),
                        logger
                    );
                }
            }
        }

        if (artifactsMap.get(OPENFL_KEY) != null) {
            File nmeDirectory = null;
            Artifact nmeArtifact = artifactsMap.get(NME_KEY);
            if (nmeArtifact != null) {
                nmeDirectory = HaxelibHelper.getHaxelibDirectoryForArtifact(nmeArtifact.getArtifactId(), nmeArtifact.getVersion());
            }

            File openflNativeDirectory = null;
            Artifact openflNativeArtifact = artifactsMap.get(OPENFL_GROUP + OPENFL_ARTIFACT_ID_PREFIX + OPENFL_NATIVE_SUFFIX);
            if (openflNativeArtifact != null) {
                openflNativeDirectory = HaxelibHelper.getHaxelibDirectoryForArtifact(openflNativeArtifact.getArtifactId(), openflNativeArtifact.getVersion());
            }

            openfl.initialize(artifactsMap.get(OPENFL_KEY), outputDirectory, pluginHome, path, nmeDirectory, openflNativeDirectory);
        }

        if (artifactsMap.get(HXCPP_KEY) != null) {
            logger.info("initializing hxcpp");
            hxcpp.initialize(artifactsMap.get(HXCPP_KEY), outputDirectory, pluginHome, path);
        }
    }
    
    private void initializeHaxelib(File pluginHome) throws Exception
    {
        // Add haxelib virtual repository.
        project.getRemoteArtifactRepositories().add(
                new MavenArtifactRepository(HaxelibHelper.HAXELIB_URL, "http://"+HaxelibHelper.HAXELIB_URL,
                new HaxelibRepositoryLayout(),
                new ArtifactRepositoryPolicy(false, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE),
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE)
        ));
    }

    @Nonnull
    private File getOutputDirectory()
    {
        File outputDirectory = new File(project.getBuild().getDirectory());

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        else if (!outputDirectory.isDirectory()) {
            outputDirectory.delete();
            outputDirectory.mkdirs();
        }

        return outputDirectory;
    }

    @Nonnull
    private Artifact resolveArtifact(Artifact artifact, boolean localOnly) throws Exception
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();

        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        if (!localOnly) {
            request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        }
        //request.setResolveRoot( true );
        //request.setResolveTransitively( false );
        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);

        if (!resolutionResult.isSuccess())
        {
            String expectedPackageType = PackageTypes.DEFAULT;
            if (artifact.getType().equals(expectedPackageType)) {
                artifact = repositorySystem.createArtifactWithClassifier(
                        artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                        expectedPackageType, artifact.getClassifier());
                request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setLocalRepository(localRepository);
                if (!localOnly) {
                    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
                }
                request.setResolveRoot( true );
                request.setResolveTransitively( false );
                resolutionResult = repositorySystem.resolve(request);
                if (resolutionResult.isSuccess()) {
                    return artifact;
                }
            }
            if (!localOnly) {
                String message = "Failed to resolve artifact " + artifact;
                throw new Exception(message);
            } else {
                artifact = null;
            }
        }

        return artifact;
    }

    private static final String HAXE_COMPILER_KEY = "org.haxe.compiler:haxe-compiler";
    private static final String NEKO_KEY = "org.nekovm:nekovm";
    private static final String NME_KEY = "org.haxenme:nme";
    private static final String HXCPP_KEY = "org.haxe.lib:hxcpp";
    private static final String OPENFL_ARTIFACT_ID_PREFIX = "openfl";
    private static final String OPENFL_NATIVE_SUFFIX = "-native";
    private static final String OPENFL_GROUP = "org.openfl:";
    private static final String OPENFL_KEY = OPENFL_GROUP + OPENFL_ARTIFACT_ID_PREFIX;
    private static final String MUNIT_ID = "munit";
    private static final String CHXDOC_ID = "chxdoc";

    private class HaxelibRepositoryLayout extends DefaultRepositoryLayout {

        @Override
        public String getId()
        {
            return "haxelib";
        }
    }
    
}
