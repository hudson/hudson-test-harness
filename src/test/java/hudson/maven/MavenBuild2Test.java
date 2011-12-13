/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     
 *
 *******************************************************************************/

package hudson.maven;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.tasks.Maven.MavenInstallation;
import java.io.File;
import java.io.IOException;
import org.eclipse.hudson.legacy.maven.plugin.MavenBuild;
import org.eclipse.hudson.legacy.maven.plugin.MavenModuleSet;
import org.eclipse.hudson.legacy.maven.plugin.MavenModuleSetBuild;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class MavenBuild2Test extends HudsonTestCase {

    @Bug(value = 8390)
    public void testMaven2BuildWrongInheritence() throws Exception {

        MavenModuleSet m = createMavenProject();
        MavenInstallation mavenInstallation = configureDefaultMaven();
        m.setMaven(mavenInstallation.getName());
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("incorrect-inheritence-testcase.zip")));
        m.setGoals("clean validate");
        MavenModuleSetBuild mmsb = buildAndAssertSuccess(m);
        assertFalse(mmsb.getProject().getModules().isEmpty());
    }

    @Bug(value = 8445)
    public void testMaven2SeveralModulesInDirectory() throws Exception {

        MavenModuleSet m = createMavenProject();
        MavenInstallation mavenInstallation = configureDefaultMaven();
        m.setMaven(mavenInstallation.getName());
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("several-modules-in-directory.zip")));
        m.setGoals("clean validate");
        MavenModuleSetBuild mmsb = buildAndAssertSuccess(m);
        assertFalse(mmsb.getProject().getModules().isEmpty());
    }

    @Email("https://groups.google.com/d/msg/hudson-users/Xhw00UopVN0/FA9YqDAIsSYJ")
    public void testMavenWithDependencyVersionInEnvVar() throws Exception {

        MavenModuleSet m = createMavenProject();
        MavenInstallation mavenInstallation = configureDefaultMaven();
        ParametersDefinitionProperty parametersDefinitionProperty =
                new ParametersDefinitionProperty(new StringParameterDefinition("JUNITVERSION", "3.8.2"));

        m.addProperty(parametersDefinitionProperty);
        m.setMaven(mavenInstallation.getName());
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("envars-maven-project.zip")));
        m.setGoals("clean test-compile");
        MavenModuleSetBuild mmsb = buildAndAssertSuccess(m);
        assertFalse(mmsb.getProject().getModules().isEmpty());
    }

    private static class TestReporter extends MavenReporter {

        @Override
        public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            assertNotNull(build.getProject().getWorkspace());
            assertNotNull(build.getWorkspace());
            return true;
        }
    }
}
