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

package hudson.console;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.FilePath;
import hudson.Launcher;
import hudson.MarkupText;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.triggers.SCMTrigger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.TestExtension;

/**
 * @author Kohsuke Kawaguchi
 */
public class PollingOutputTest extends HudsonTestCase {

    /**
     * Places a triple dollar mark at the specified position.
     */
    public static final class DollarMark extends ConsoleNote<Object> {
        public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
            text.addMarkup(charPos,"$$$");
            return null;
        }

        @TestExtension
        public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {
            public String getDisplayName() {
                return "Dollar mark";
            }
        }
    }
    /**
     * Makes sure that annotations in the polling output is handled correctly.
     */
    public void testPollingOutput() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new PollingSCM());
        SCMTrigger t = new SCMTrigger("@daily");
        t.start(p,true);
        p.addTrigger(t);

        buildAndAssertSuccess(p);

        // poll now
        t.new Runner().run();

        HtmlPage log = createWebClient().getPage(p, "scmPollLog");
        String text = log.asText();
        assertTrue(text, text.contains("$$$hello from polling"));
    }

    public static class PollingSCM extends SingleFileSCM {
        public PollingSCM() throws UnsupportedEncodingException {
            super("abc", "def");
        }

        @Override
        protected PollingResult compareRemoteRevisionWith(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
            listener.annotate(new DollarMark());
            listener.getLogger().println("hello from polling");
            return new PollingResult(Change.NONE);
        }

        @TestExtension
        public static final class DescriptorImpl extends SCMDescriptor<PollingSCM> {
            public DescriptorImpl() {
                super(PollingSCM.class, RepositoryBrowser.class);
            }

            @Override
            public String getDisplayName() {
                return "Test SCM";
            }
        }
    }
}
