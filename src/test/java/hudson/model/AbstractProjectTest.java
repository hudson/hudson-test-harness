/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
*
*    Kohsuke Kawaguchi
 *     
 *
 *******************************************************************************/ 

package hudson.model;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import hudson.scm.NullSCM;
import hudson.Launcher;
import hudson.FilePath;
import hudson.util.StreamTaskListener;
import hudson.util.OneShotEvent;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Bug;

import java.util.concurrent.Future;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractProjectTest extends HudsonTestCase {
    //TODO find the reason
    public void ignore_testConfigRoundtrip() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        Label l = hudson.getLabel("foo && bar");
        project.setAssignedLabel(l);
        configRoundtrip(project);

        assertEquals(l,project.getAssignedLabel());
    }

    /**
     * Tests the &lt;optionalBlock @field> round trip behavior by using {@link AbstractProject#concurrentBuild}
     */
    public void testOptionalBlockDataBindingRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        for( boolean b : new boolean[]{true,false}) {
            p.setConcurrentBuild(b);
            submit(new WebClient().getPage(p,"configure").getFormByName("config"));
            assertEquals(b,p.isConcurrentBuild());
        }
    }

    /**
     * Tests round trip configuration of the blockBuildWhenUpstreamBuilding field
     */
    @Bug(4423)
    public void testConfiguringBlockBuildWhenUpstreamBuildingRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();        
        p.setBlockBuildWhenUpstreamBuilding(false);
        
        HtmlForm form = new WebClient().getPage(p, "configure").getFormByName("config");
        HtmlInput input = form.getInputByName("blockBuildWhenUpstreamBuilding");
        assertFalse("blockBuildWhenUpstreamBuilding check box is checked.", input.isChecked());
        
        input.setChecked(true);
        submit(form);        
        assertTrue("blockBuildWhenUpstreamBuilding was not updated from configuration form", p.blockBuildWhenUpstreamBuilding());
        
        form = new WebClient().getPage(p, "configure").getFormByName("config");
        input = form.getInputByName("blockBuildWhenUpstreamBuilding");
        assertTrue("blockBuildWhenUpstreamBuilding check box is not checked.", input.isChecked());
    }

    /**
     * Unless the concurrent build option is enabled, polling and build should be mutually exclusive
     * to avoid allocating unnecessary workspaces.
     */
    @Bug(4202)
    public void testPollingAndBuildExclusion() throws Exception {
        final OneShotEvent sync = new OneShotEvent();

        final FreeStyleProject p = createFreeStyleProject();
        FreeStyleBuild b1 = buildAndAssertSuccess(p);

        p.setScm(new NullSCM() {
            @Override
            public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) {
                try {
                    sync.block();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }

            /**
             * Don't write 'this', so that subtypes can be implemented as anonymous class.
             */
            private Object writeReplace() { return new Object(); }
            
            @Override public boolean requiresWorkspaceForPolling() {
                return true;
            }
        });
        Thread t = new Thread() {
            @Override public void run() {
                p.pollSCMChanges(StreamTaskListener.fromStdout());
            }
        };
        try {
            t.start();
            Future<FreeStyleBuild> f = p.scheduleBuild2(0);

            // add a bit of delay to make sure that the blockage is happening
            Thread.sleep(3000);

            // release the polling
            sync.signal();

            FreeStyleBuild b2 = assertBuildStatusSuccess(f);

            // they should have used the same workspace.
            assertEquals(b1.getWorkspace(), b2.getWorkspace());
        } finally {
            t.interrupt();
        }
    }

}
