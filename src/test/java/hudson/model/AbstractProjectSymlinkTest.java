/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Oracle Corporation, Nikita Levyankov
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
package hudson.model;

import hudson.Functions;
import hudson.Util;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Shell;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test class that verifies symlinks creation for project
 * <p/>
 * Date: 11/14/11
 *
 * @author Nikita Levyankov
 */
public class AbstractProjectSymlinkTest extends HudsonTestCase {

    @Bug(1986)
    public void testBuildSymlinks() throws Exception {
        // If we're on Windows, don't bother doing this.
        if (Functions.isWindows()) {
            return;
        }

        FreeStyleProject job = createFreeStyleProject();
        job.getBuildersList().add(new Shell("echo \"Build #$BUILD_NUMBER\"\n"));
        FreeStyleBuild build = job.scheduleBuild2(0, new Cause.UserCause()).get();
        File lastSuccessful = new File(job.getRootDir(), "lastSuccessful"),
            lastStable = new File(job.getRootDir(), "lastStable");
        // First build creates links
        assertSymlinkForBuild(lastSuccessful, 1);
        assertSymlinkForBuild(lastStable, 1);
        FreeStyleBuild build2 = job.scheduleBuild2(0, new Cause.UserCause()).get();
        // Another build updates links
        assertSymlinkForBuild(lastSuccessful, 2);
        assertSymlinkForBuild(lastStable, 2);
        // Delete latest build should update links
        build2.delete();
        assertSymlinkForBuild(lastSuccessful, 1);
        assertSymlinkForBuild(lastStable, 1);
        // Delete all builds should remove links
        build.delete();
        assertFalse("lastSuccessful link should be removed", lastSuccessful.exists());
        assertFalse("lastStable link should be removed", lastStable.exists());
    }

    @Bug(2543)
    public void testSymlinkForPostBuildFailure() throws Exception {
        // If we're on Windows, don't bother doing this.
        if (Functions.isWindows()) {
            return;
        }

        // Links should be updated after post-build actions when final build result is known
        FreeStyleProject job = createFreeStyleProject();
        job.getBuildersList().add(new Shell("echo \"Build #$BUILD_NUMBER\"\n"));
        FreeStyleBuild build = job.scheduleBuild2(0, new Cause.UserCause()).get();
        assertEquals(Result.SUCCESS, build.getResult());
        File lastSuccessful = new File(job.getRootDir(), "lastSuccessful"),
            lastStable = new File(job.getRootDir(), "lastStable");
        // First build creates links
        assertSymlinkForBuild(lastSuccessful, 1);
        assertSymlinkForBuild(lastStable, 1);
        // Archive artifacts that don't exist to create failure in post-build action
        job.addPublisher(new ArtifactArchiver("*.foo", "", false));
        build = job.scheduleBuild2(0, new Cause.UserCause()).get();
        assertEquals(Result.FAILURE, build.getResult());
        // Links should not be updated since build failed
        assertSymlinkForBuild(lastSuccessful, 1);
        assertSymlinkForBuild(lastStable, 1);
    }

    private static void assertSymlinkForBuild(File file, int buildNumber)
        throws IOException, InterruptedException {
        assertTrue("should exist and point to something that exists", file.exists());
        assertTrue("should be symlink", Util.isSymlink(file));
        String s = FileUtils.readFileToString(new File(file, "log"));
        assertTrue("link should point to build #" + buildNumber + ", but link was: "
            + Util.resolveSymlink(file, TaskListener.NULL) + "\nand log was:\n" + s,
            s.contains("Build #" + buildNumber + "\n"));
    }
}
