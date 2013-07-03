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
package com.yelbota.plugins.haxe;

import com.yelbota.plugins.haxe.components.HaxeCompiler;
import com.yelbota.plugins.haxe.components.NativeBootstrap;
import com.yelbota.plugins.haxe.components.HaxeCompiler;
import com.yelbota.plugins.haxe.components.OpenFLCompiler;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;

import com.yelbota.plugins.haxe.utils.HaxeFileExtensions;

import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.io.File;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.xml.sax.*;
import org.w3c.dom.*;

public abstract class AbstractCompileMojo extends AbstractHaxeMojo {

    /**
     *  Main class
     */
    @Parameter
    protected String main;

    /**
     * Compile in debug mode
     */
    @Parameter
    protected boolean debug;

    /**
     * Compile with verbose output
     */
    @Parameter
    protected boolean verbose;

    @Component
    protected HaxeCompiler compiler;

    @Component
    protected OpenFLCompiler openflCompiler;

    @Parameter(required = false)
    protected List<String> compilerFlags;

    @Parameter
    protected String nmml;

    @Component(hint = HaxeFileExtensions.HAXELIB)
    protected ArtifactHandler haxelibHandler;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();

        compiler.setOutputDirectory(outputDirectory);
        if (openflIsActive()) {
            openflCompiler.setOutputDirectory(outputDirectory);
        }
    }

    protected boolean openflIsActive()
    {
        return nmml != null;
    }

    @Override
    protected void initialize(MavenProject project, ArtifactRepository localRepository) throws Exception
    {
        if (openflIsActive()) {
            File nmmlFile = new File(outputDirectory.getParentFile(), nmml);
            if (nmmlFile.exists()) {
                nmml = nmmlFile.getAbsolutePath();
                Document dom;
                // Make an  instance of the DocumentBuilderFactory
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                try {
                    // use the factory to take an instance of the document builder
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    // parse using the builder to get the DOM mapping of the    
                    // XML file
                    dom = db.parse(nmmlFile.getAbsolutePath());

                    Element doc = dom.getDocumentElement();

                    NodeList nl;

                    nl = doc.getElementsByTagName("haxelib");
                    String haxelibName;
                    String haxelibVersion;
                    if (nl.getLength() > 0) {
                        Set<Artifact> dependencies = project.getDependencyArtifacts();

                        for (int i = 0; i < nl.getLength(); i++) {
                            haxelibVersion = "";
                            Node node = nl.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                Element element = (Element) node;
                                haxelibName = element.getAttribute("name");
                                if (element.hasAttribute("version")) {
                                    haxelibVersion = element.getAttribute("version");
                                }
                                Artifact artifact = new DefaultArtifact(
                                    "org.haxe.lib",
                                    haxelibName,
                                    haxelibVersion,
                                    Artifact.SCOPE_COMPILE,
                                    "haxelib",
                                    "",
                                    haxelibHandler);
                                
                                //dependencies.add(artifact);
                            }
                        }
                    }
                } catch (ParserConfigurationException pce) {
                    System.out.println(pce.getMessage());
                } catch (SAXException se) {
                    System.out.println(se.getMessage());
                } catch (IOException ioe) {
                    System.err.println(ioe.getMessage());
                }
            } else {
                nmml = null;
            }
        }

        super.initialize(project, localRepository);
    }
}
