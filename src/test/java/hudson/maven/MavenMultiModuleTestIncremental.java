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
import hudson.model.Result;
import hudson.tasks.Maven.MavenInstallation;

import java.io.IOException;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.ExtractChangeLogSet;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;
import org.jvnet.hudson.test.HudsonTestCase;

import org.eclipse.hudson.legacy.maven.plugin.MavenBuild;
import org.eclipse.hudson.legacy.maven.plugin.MavenModuleSet;
import org.eclipse.hudson.legacy.maven.plugin.MavenModuleSetBuild;

/**
 * @author Andrew Bayer
 */
public class MavenMultiModuleTestIncremental extends HudsonTestCase {

    @Bug(7684)
    public void testRelRootPom() throws Exception {
        configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = createMavenProject();
        m.setRootPOM("parent/pom.xml");
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceWithChangesSCM(getClass().getResource("maven-multimod-rel-base.zip"),
						   getClass().getResource("maven-multimod-changes.zip")));
        
    	buildAndAssertSuccess(m);
            
    	// Now run a second build with the changes.
    	m.setIncrementalBuild(true);
        buildAndAssertSuccess(m);
            
    	MavenModuleSetBuild pBuild = m.getLastBuild();
    	ExtractChangeLogSet changeSet = (ExtractChangeLogSet) pBuild.getChangeSet();
            
    	assertFalse("ExtractChangeLogSet should not be empty.", changeSet.isEmptySet());
    
    	for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
    	    String parentModuleName = modBuild.getParent().getModuleName().toString();
    	    if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod:moduleA")) {
    	        assertEquals("moduleA should have Result.NOT_BUILT", Result.NOT_BUILT, modBuild.getResult());
    	    }
    	    else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod:moduleB")) {
    	        assertEquals("moduleB should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
    	    }
    	    else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod:moduleC")) {
    	        assertEquals("moduleC should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
    	    }
    	}	
    	
        long summedModuleDuration = 0;
        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            summedModuleDuration += modBuild.getDuration();
        }
        assertTrue("duration of moduleset build should be greater-equal than sum of the module builds",
                   pBuild.getDuration() >= summedModuleDuration);
    }

    /**
     * Module failures in build X should lead to those modules being re-run in build X+1, even if
     * incremental build is enabled and nothing changed in those modules.
     */
    @Bug(4152)
    public void testIncrementalMultiModWithErrorsMaven() throws Exception {
        configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = createMavenProject();
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceWithChangesSCM(getClass().getResource("maven-multimod-incr.zip"),
            getClass().getResource("maven-multimod-changes.zip")));

        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());

        // Now run a second build with the changes.
        m.setIncrementalBuild(true);
        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());

        MavenModuleSetBuild pBuild = m.getLastBuild();
        ExtractChangeLogSet changeSet = (ExtractChangeLogSet) pBuild.getChangeSet();

        assertFalse("ExtractChangeLogSet should not be empty.", changeSet.isEmptySet());
        assertEquals("Parent build should have Result.UNSTABLE", Result.UNSTABLE, pBuild.getResult());

        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            String parentModuleName = modBuild.getParent().getModuleName().toString();
            if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleA")) {
                assertEquals("moduleA should have Result.UNSTABLE", Result.UNSTABLE, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleB")) {
                assertEquals("moduleB should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleC")) {
                assertEquals("moduleC should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleD")) {
                assertEquals("moduleD should have Result.NOT_BUILT", Result.NOT_BUILT, modBuild.getResult());
            }
        }
    }

    /**
     * Test failures in a child module should lead to the parent being marked as unstable.
     */
    @Bug(4378)
    public void testMultiModWithTestFailuresMaven() throws Exception {
        configureDefaultMaven("apache-maven-2.2.1", MavenInstallation.MAVEN_21);
        MavenModuleSet m = createMavenProject();
        m.getReporters().add(new TestReporter());
        m.setScm(new ExtractResourceSCM(getClass().getResource("maven-multimod-incr.zip")));

        assertBuildStatus(Result.UNSTABLE, m.scheduleBuild2(0).get());

        MavenModuleSetBuild pBuild = m.getLastBuild();

        assertEquals("Parent build should have Result.UNSTABLE", Result.UNSTABLE, pBuild.getResult());

        for (MavenBuild modBuild : pBuild.getModuleLastBuilds().values()) {
            String parentModuleName = modBuild.getParent().getModuleName().toString();
            if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleA")) {
                assertEquals("moduleA should have Result.UNSTABLE", Result.UNSTABLE, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleB")) {
                assertEquals("moduleB should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleC")) {
                assertEquals("moduleC should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
            else if (parentModuleName.equals("org.jvnet.hudson.main.test.multimod.incr:moduleD")) {
                assertEquals("moduleD should have Result.SUCCESS", Result.SUCCESS, modBuild.getResult());
            }
        }
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
