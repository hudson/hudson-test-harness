/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.tasks;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause.LegacyCodeCause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.ArtifactArchiverTest.CreateArtifact;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Verifies that the last successful and stable builds of a job will be kept if requested.
 */
public class LogRotatorTest extends HudsonTestCase {

    public void testSuccessVsFailure() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.setLogRotator(new LogRotator(-1, 2, -1, -1));
        assertEquals(Result.SUCCESS, build(project)); // #1
        project.getBuildersList().replaceBy(Collections.singleton(new FailureBuilder()));
        assertEquals(Result.FAILURE, build(project)); // #2
        assertEquals(Result.FAILURE, build(project)); // #3
        assertEquals(1, numberOf(project.getLastSuccessfulBuild()));
        project.getBuildersList().replaceBy(Collections.<Builder>emptySet());
        assertEquals(Result.SUCCESS, build(project)); // #4
        assertEquals(4, numberOf(project.getLastSuccessfulBuild()));
        assertEquals(null, project.getBuildByNumber(1));
        assertEquals(null, project.getBuildByNumber(2));
        assertEquals(3, numberOf(project.getLastFailedBuild()));
    }

    @Bug(2417)
    public void testStableVsUnstable() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.setLogRotator(new LogRotator(-1, 2, -1, -1));
        assertEquals(Result.SUCCESS, build(project)); // #1
        project.addPublisher(new TestsFail());

        assertEquals(Result.UNSTABLE, build(project)); // #2
        assertEquals(Result.UNSTABLE, build(project)); // #3
        assertEquals(1, numberOf(project.getLastStableBuild()));
        project.removePublisher(new TestsFail().getDescriptor());
        assertEquals(Result.SUCCESS, build(project)); // #4
        assertEquals(null, project.getBuildByNumber(1));
        assertEquals(null, project.getBuildByNumber(2));
    }

    @Bug(834)
    public void testArtifactDelete() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.setLogRotator(new LogRotator(-1, 6, -1, 2));
        project.addPublisher(new ArtifactArchiver("f", "", true));
        assertEquals("(no artifacts)", Result.FAILURE, build(project)); // #1
        assertFalse(project.getBuildByNumber(1).getHasArtifacts());
        project.getBuildersList().replaceBy(Collections.singleton(new CreateArtifact()));
        assertEquals(Result.SUCCESS, build(project)); // #2
        assertTrue(project.getBuildByNumber(2).getHasArtifacts());
        project.getBuildersList().replaceBy(Arrays.asList(new CreateArtifact(), new FailureBuilder()));
        assertEquals(Result.FAILURE, build(project)); // #3
        assertTrue(project.getBuildByNumber(2).getHasArtifacts());
        assertTrue(project.getBuildByNumber(3).getHasArtifacts());
        assertEquals(Result.FAILURE, build(project)); // #4
        assertTrue(project.getBuildByNumber(2).getHasArtifacts());
        assertTrue(project.getBuildByNumber(3).getHasArtifacts());
        assertTrue(project.getBuildByNumber(4).getHasArtifacts());
        assertEquals(Result.FAILURE, build(project)); // #5
        assertTrue(project.getBuildByNumber(2).getHasArtifacts());
        assertFalse("no better than #4", project.getBuildByNumber(3).getHasArtifacts());
        assertTrue(project.getBuildByNumber(4).getHasArtifacts());
        assertTrue(project.getBuildByNumber(5).getHasArtifacts());
        project.getBuildersList().replaceBy(Collections.singleton(new CreateArtifact()));
        assertEquals(Result.SUCCESS, build(project)); // #6
        assertFalse("#2 is still lastSuccessful until #6 is complete", project.getBuildByNumber(2).getHasArtifacts());
        assertFalse(project.getBuildByNumber(3).getHasArtifacts());
        assertFalse(project.getBuildByNumber(4).getHasArtifacts());
        assertTrue(project.getBuildByNumber(5).getHasArtifacts());
        assertTrue(project.getBuildByNumber(6).getHasArtifacts());
        assertEquals(Result.SUCCESS, build(project)); // #7
        assertEquals(null, project.getBuildByNumber(1));
        assertNotNull(project.getBuildByNumber(2));
        assertFalse("lastSuccessful was #6 for ArtifactArchiver", project.getBuildByNumber(2).getHasArtifacts());
        assertFalse(project.getBuildByNumber(3).getHasArtifacts());
        assertFalse(project.getBuildByNumber(4).getHasArtifacts());
        assertFalse(project.getBuildByNumber(5).getHasArtifacts());
        assertTrue(project.getBuildByNumber(6).getHasArtifacts());
        assertTrue(project.getBuildByNumber(7).getHasArtifacts());
        assertEquals(Result.SUCCESS, build(project)); // #8
        assertEquals(null, project.getBuildByNumber(2));
        assertNotNull(project.getBuildByNumber(3));
        assertFalse(project.getBuildByNumber(3).getHasArtifacts());
        assertFalse(project.getBuildByNumber(4).getHasArtifacts());
        assertFalse(project.getBuildByNumber(5).getHasArtifacts());
        assertFalse(project.getBuildByNumber(6).getHasArtifacts());
        assertTrue(project.getBuildByNumber(7).getHasArtifacts());
        assertTrue(project.getBuildByNumber(8).getHasArtifacts());
    }


    static Result build(FreeStyleProject project) throws Exception {
        return project.scheduleBuild2(0, new LegacyCodeCause()).get(10, TimeUnit.SECONDS).getResult();
    }

    private static int numberOf(Run<?,?> run) {
        return run != null ? run.getNumber() : -1;
    }

    static class TestsFail extends Publisher {
        public @Override boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
            build.setResult(Result.UNSTABLE);
            return true;
        }

        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }

        @Extension
        public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
            public DescriptorImpl() {
            }

            public String getDisplayName() {
                return "TestsFail";
            }

            @Override
            public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }
        }

    }

}
