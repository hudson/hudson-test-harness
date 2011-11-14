/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Alan Harder
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

import hudson.security.ACL;
import hudson.tasks.BuildTrigger;
import hudson.tasks.MailMessageIdAction;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.acegisecurity.context.SecurityContextHolder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author Alan.Harder@sun.com
 */
public class DependencyGraphTest extends HudsonTestCase {

    /**
     * Tests triggering downstream projects with DependencyGraph.Dependency
     */
    public void testTriggerJob() throws Exception {
        hudson.quietPeriod = 3;
        Project p = createFreeStyleProject(),
            down1 = createFreeStyleProject(), down2 = createFreeStyleProject();
        // Add one standard downstream job:
        p.addPublisher(new BuildTrigger(Collections.singletonList(down1), Result.SUCCESS));
        // Add one downstream job with custom Dependency impl:
        p.getBuildersList().add(new TestDeclarer(Result.UNSTABLE, down2));
        hudson.rebuildDependencyGraph();
        // First build won't trigger down1 (Unstable doesn't meet threshold)
        // but will trigger down2 (build #1 is odd).
        Build b = (Build)p.scheduleBuild2(0, new Cause.UserCause()).get();
        String log = getLog(b);
        Queue.Item q = hudson.getQueue().getItem(down1);
        assertNull("down1 should not be triggered: " + log, q);
        assertNull("down1 should not be triggered: " + log, down1.getLastBuild());
        q = hudson.getQueue().getItem(down2);
        assertNotNull("down2 should be in queue (quiet period): " + log, q);
        Run r = (Run)q.getFuture().get(6, TimeUnit.SECONDS);
        assertNotNull("down2 should be triggered: " + log, r);
        assertNotNull("down2 should have MailMessageIdAction",
                      r.getAction(MailMessageIdAction.class));
        // Now change to success result..
        p.getBuildersList().replace(new TestDeclarer(Result.SUCCESS, down2));
        hudson.rebuildDependencyGraph();
        // ..and next build will trigger down1 (Success meets threshold),
        // but not down2 (build #2 is even)
        b = (Build)p.scheduleBuild2(0, new Cause.UserCause()).get();
        log = getLog(b);
        q = hudson.getQueue().getItem(down2);
        assertNull("down2 should not be triggered: " + log, q);
        assertEquals("down2 should not be triggered: " + log, 1,
                     down2.getLastBuild().getNumber());
        q = hudson.getQueue().getItem(down1);
        assertNotNull("down1 should be in queue (quiet period): " + log, q);
        r = (Run)q.getFuture().get(6, TimeUnit.SECONDS);
        assertNotNull("down1 should be triggered", r);
    }

    private static class TestDeclarer extends MockBuilder implements DependecyDeclarer {
        private AbstractProject down;
        private TestDeclarer(Result buildResult, AbstractProject down) {
            super(buildResult);
            this.down = down;
        }
        public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
            graph.addDependency(new DependencyGraph.Dependency(owner, down) {
                @Override
                public boolean shouldTriggerBuild(AbstractBuild build, TaskListener listener,
                                                  List<Action> actions) {
                    // Trigger for ODD build number
                    if (build.getNumber() % 2 == 1) {
                        actions.add(new MailMessageIdAction("foo"));
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Tests that all dependencies are found even when some projects have restricted visibility.
     */
    @LocalData @Bug(5265)
    public void testItemReadPermission() throws Exception {
        // Rebuild dependency graph as anonymous user:
        hudson.rebuildDependencyGraph();
        try {
            // Switch to full access to check results:
            SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
            // @LocalData for this test has jobs w/o anonymous Item.READ
            AbstractProject up = (AbstractProject)hudson.getItem("hiddenUpstream");
            assertNotNull("hiddenUpstream project not found", up);
            List<AbstractProject> down = hudson.getDependencyGraph().getDownstream(up);
            assertEquals("Should have one downstream project", 1, down.size());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}
