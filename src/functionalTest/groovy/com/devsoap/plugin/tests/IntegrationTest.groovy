/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.tests

import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths

/**
 * Base class for tests tests
 */
class IntegrationTest {

    private static final List<String> DEFAULT_ARGS = [
            '--stacktrace',
            '--warning-mode', 'all'
    ]

    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder()

    protected File buildFile

    protected File settingsFile

    protected long startTime

    @Before
    void setup() {
        startTime = System.currentTimeMillis()
        println "Running test in $projectDir.root"
        buildFile = makeBuildFile(projectDir.root)
        settingsFile = projectDir.newFile("settings.gradle")
    }

    @After
    void tearDown() {
        println "Test took ${(System.currentTimeMillis() - startTime)/1000L} seconds."
    }

    protected String getPluginDir() {
        File libsDir = Paths.get('.', 'build', 'libs').toFile()
        String escapedDir = libsDir.canonicalPath.replace("\\","\\\\")
        escapedDir
    }

    protected File makeBuildFile(File projectDir, boolean applyPluginToFile=true) {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.createNewFile()

        // Apply plugin to project
        buildFile << "buildscript {\n"
            buildFile << "repositories {\n"
                applyBuildScriptRepositories(buildFile)
            buildFile << "}\n"
        buildFile << "dependencies {\n"
            applyBuildScriptClasspathDependencies(buildFile)
            buildFile << "}\n"
        buildFile << "}\n"

        // Apply custom plugins{} block
        applyThirdPartyPlugins(buildFile)

        if ( applyPluginToFile ) {
            applyRepositories(buildFile)
            applyPlugin(buildFile)
            buildFile << "vaadin.logToConsole = true\n"
        }

        buildFile
    }

    protected void applyBuildScriptClasspathDependencies(File buildFile) {
        def projectVersion = System.getProperty('integrationTestProjectVersion')
        buildFile << "classpath group: 'org.codehaus.groovy.modules.http-builder', " +
                "name: 'http-builder', version: '0.7.1'\n"
        buildFile << "classpath group: 'com.devsoap.plugin', " +
                "name: 'gradle-vaadin-plugin', version: '$projectVersion'\n"
    }

    protected void applyBuildScriptRepositories(File buildFile) {
        String escapedDir = getPluginDir()
        buildFile << "mavenLocal()\n"
        buildFile << "mavenCentral()\n"
        buildFile << "flatDir dirs:file('$escapedDir')\n"
    }

    protected void applyThirdPartyPlugins(File buildFile) {
        if(!buildFile || !buildFile.exists()){
            throw new IllegalArgumentException("$buildFile does not exist or is null")
        }
    }

    protected void applyRepositories(File buildFile) {
        String escapedDir = getPluginDir()
        buildFile << """
            repositories {
                flatDir dirs:file('$escapedDir')
            }
        """.stripIndent()
    }

    protected void applyPlugin(File buildFile) {
        buildFile << "apply plugin:com.devsoap.plugin.GradleVaadinPlugin\n"
    }

    protected String runWithArguments(String... args) {
        GradleRunner runner = setupRunner(projectDir.root)
                .withArguments( DEFAULT_ARGS + (args as List))
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.build().output
    }

    protected String runFailureExpected() {
        GradleRunner runner = setupRunner().withArguments(DEFAULT_ARGS)
        println "Running gradle ${runner.arguments.join(' ')}"
        runner.buildAndFail().output
    }

    protected String runFailureExpected(String... args) {
        setupRunner()
                .withArguments(DEFAULT_ARGS + (args as List))
                .buildAndFail()
                .output
    }

    protected GradleRunner setupRunner(File projectDir = this.projectDir.root) {
        GradleRunner.create().withProjectDir(projectDir).withPluginClasspath()
    }
}