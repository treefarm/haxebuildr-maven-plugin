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
package com.yelbota.plugins.haxe.components.repository;

import org.sonatype.aether.resolution.VersionRequest;

import com.yelbota.plugins.haxe.utils.PackageTypes;
import com.yelbota.plugins.haxe.utils.HaxelibHelper;
import com.yelbota.plugins.haxe.utils.OSClassifiers;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.HaxelibNativeProgram;
import com.yelbota.plugins.haxe.components.nativeProgram.NativeProgramException;
import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.*;
import org.sonatype.aether.transfer.ArtifactTransferException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Requirement;
import com.yelbota.plugins.nd.UnpackHelper;
import com.yelbota.plugins.nd.utils.DefaultUnpackMethods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class HaxelibRepositoryConnector implements RepositoryConnector {

    @Requirement
    private RepositorySystem repositorySystem;

    //-------------------------------------------------------------------------
    //
    //  Fields
    //
    //-------------------------------------------------------------------------

    private final RemoteRepository repository;

    private final RepositoryConnector defaultRepositoryConnector;

    private final RepositorySystemSession session;

    private final NativeProgram haxelib;

    private final Logger logger;

    //-------------------------------------------------------------------------
    //
    //  Public methods
    //
    //-------------------------------------------------------------------------

    public HaxelibRepositoryConnector(RemoteRepository repository, RepositoryConnector defaultRepositoryConnector, NativeProgram haxelib, Logger logger, RepositorySystemSession session)
    {
        this.repository = repository;
        this.defaultRepositoryConnector = defaultRepositoryConnector;
        this.haxelib = haxelib;
        this.logger = logger;
        this.session = session;
    }

    @Override
    public void get(Collection<? extends ArtifactDownload> artifactDownloads, Collection<? extends MetadataDownload> metadataDownloads)
    {
        if (artifactDownloads == null)
        {
            defaultRepositoryConnector.get(artifactDownloads, metadataDownloads);
        }
        else
        {
            ArrayList<ArtifactDownload> normalArtifacts = new ArrayList<ArtifactDownload>();
            ArrayList<ArtifactDownload> haxelibArtifacts = new ArrayList<ArtifactDownload>();

            // Separate artifacts collection. Get haxelib artifacts and all others.
            for (ArtifactDownload artifactDownload : artifactDownloads)
            {
                Artifact artifact = artifactDownload.getArtifact();
                if (artifact.getExtension().equals(HaxeFileExtensions.HAXELIB)) {
                    haxelibArtifacts.add(artifactDownload);
                } else {
                    /*if (artifact.getGroupId().equals("org.haxe.lib")) {
                        injectPomForHaxelib(artifactDownload);
                    } else if (artifact.getClassifier().equals(HaxeFileExtensions.HAXELIB)) {
                        // the POM for these is already accounted for
                    } else {*/

                    if (artifact.getGroupId().equals(HaxelibHelper.HAXELIB_GROUP_ID)
                          && artifact.getExtension().equals("pom")) {
                        injectPomForHaxelib(artifactDownload);
                    } else {
                        normalArtifacts.add(artifactDownload);
                    }
                }
            }

            // Get normal artifacts
            defaultRepositoryConnector.get(normalArtifacts, metadataDownloads);

            for (ArtifactDownload artifactDownload : normalArtifacts)
            {
                Artifact artifact = artifactDownload.getArtifact();
                if (artifact.getGroupId().equals(HaxelibHelper.HAXELIB_GROUP_ID)
                        && !artifact.getExtension().equals("pom")) {
                    HaxelibHelper.injectPomHaxelib(artifactDownload, logger);
                }
            }

            getHaxelibs(haxelibArtifacts);
        }
    }

    private void getHaxelibs(List<ArtifactDownload> haxelibArtifacts)
    {
        for (ArtifactDownload artifactDownload : haxelibArtifacts)
        {
            Artifact artifact = artifactDownload.getArtifact();
            File haxelibDirectory = HaxelibHelper.getHaxelibDirectoryForArtifact(artifact.getArtifactId(), artifact.getVersion());
            if (!haxelibDirectory.exists()) {
                logger.info("Resolving " + artifact);
                try
                {
                    int code;
                    if (artifact.getVersion() == null || artifact.getVersion() == "") {
                        code = haxelib.execute(
                            "install",
                            artifact.getArtifactId()
                        );
                    } else {
                        code = haxelib.execute(
                            "install",
                            artifact.getArtifactId(),
                            artifact.getVersion()
                        );
                    }

                    if (code > 0)
                    {
                        artifactDownload.setException(new ArtifactTransferException(
                                artifact, repository, "Can't resolve artifact " + artifact.toString()));
                    //} else {
                    //    injectPomForHaxelib(artifactDownload);
                    }
                }
                catch (NativeProgramException e)
                {
                    artifactDownload.setException(new ArtifactTransferException(
                            artifact, repository, e));
                }
            }
        }
    }

    private void injectPomForHaxelib(ArtifactDownload artifactDownload)
    {
        Artifact artifact = artifactDownload.getArtifact();
        String pomPath = artifactDownload.getFile().getAbsolutePath().replace(
            artifact.getExtension(), "pom");
        File artifactFile = new File(pomPath);

        // TODO Need custom dependency resolver so enforcer does not bother
        // checking for poms for these dependencies which originate from
        // haxelib repository.
        if (!artifactFile.exists()) {
            try
            {
                if (!artifactFile.getParentFile().exists()) {
                    artifactFile.getParentFile().mkdirs();
                }
                artifactFile.createNewFile();
                String version = HaxelibHelper.getSnapshotVersionForHaxelibArtifact(artifact.getVersion());

                FileWriter fileWriter = new FileWriter(artifactFile);
                String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                    "\n         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                    "\n         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                    "\n  <modelVersion>4.0.0</modelVersion>" +
                    "\n" +
                    "\n  <groupId>" + artifact.getGroupId() + "</groupId>" +
                    "\n  <artifactId>" + artifact.getArtifactId() + "</artifactId>" +
                    "\n  <version>" + version + "</version>" +
                    "\n" +
                    "\n  <packaging>" + PackageTypes.DEFAULT + "</packaging>" +
                    "\n</project>" +
                    "\n";
                fileWriter.write(content);
                fileWriter.close();
            }
            catch (IOException e)
            {
                artifactDownload.setException(new ArtifactTransferException(
                        artifact, repository, "POM generation failed for haxelib " + artifact.toString()));
            }
        }
    }

    @Override
    public void put(Collection<? extends ArtifactUpload> artifactUploads, Collection<? extends MetadataUpload> metadataUploads)
    {
        defaultRepositoryConnector.put(artifactUploads, metadataUploads);
        // TODO Deploying to http://lib.haxe.org. Need to define haxelib packaging?
    }

    @Override
    public void close()
    {
    }
}
