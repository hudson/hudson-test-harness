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

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.tasks.Shell;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.PresetData;
import org.jvnet.hudson.test.recipes.PresetData.DataSet;

/**
 * @author Kohsuke Kawaguchi
 */
public class WipeWorkspaceTest extends HudsonTestCase {

    /**
     * Tests the workspace deletion.
     */
    public void testWipeWorkspace() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));

        FreeStyleBuild b = project.scheduleBuild2(0).get();

        assertTrue("Workspace should exist by now",
            b.getWorkspace().exists());

        // emulate the user behavior
        WebClient webClient = new WebClient();
        HtmlPage page = webClient.getPage(project);

        page = (HtmlPage) page.getFirstAnchorByText("Workspace").click();
        page = (HtmlPage) page.getFirstAnchorByText("Wipe Out Workspace").click();
        page = (HtmlPage) ((HtmlForm) page.getElementById("confirmation")).submit(null);

        assertFalse("Workspace should be gone by now",
            b.getWorkspace().exists());
    }


    @Bug(8950)
    public void testMissingWorkspaceGives404() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        assertNull(project.getSomeWorkspace());
        WebClient webClient = new WebClient();
        webClient.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = webClient.getPage(project, "ws/some/file");
        assertEquals(404, page.getWebResponse().getStatusCode());
    }

    /**
     * Makes sure that the workspace deletion is protected.
     */
    @PresetData(DataSet.NO_ANONYMOUS_READACCESS)
    public void testWipeWorkspaceProtected() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));

        FreeStyleBuild b = project.scheduleBuild2(0).get();

        assertTrue("Workspace should exist by now", b.getWorkspace().exists());

        // make sure that the action link is protected
        try {
            new WebClient().getPage(project, "doWipeOutWorkspace");
            fail("Should have failed");
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), 403);
        }
    }

    /**
     * Makes sure that the workspace deletion link is not provided
     * when the user doesn't have an access.
     */
    @PresetData(DataSet.ANONYMOUS_READONLY)
    public void testWipeWorkspaceProtected2() throws Exception {
        ((GlobalMatrixAuthorizationStrategy) hudson.getAuthorizationStrategy()).add(AbstractProject.WORKSPACE,
            "anonymous");

        // make sure that the deletion is protected in the same way
        testWipeWorkspaceProtected();

        // there shouldn't be any "wipe out workspace" link for anonymous user
        WebClient webClient = new WebClient();
        HtmlPage page = webClient.getPage(hudson.getItem("test0"));

        page = (HtmlPage) page.getFirstAnchorByText("Workspace").click();
        try {
            page.getFirstAnchorByText("Wipe Out Workspace");
            fail("shouldn't find a link");
        } catch (ElementNotFoundException e) {
            // OK
        }
    }
}
