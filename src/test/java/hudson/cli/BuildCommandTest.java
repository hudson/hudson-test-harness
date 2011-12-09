/**************************************************************************
#
# Copyright (C) 2004-2009 Oracle Corporation
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#         
#
#**************************************************************************/
package hudson.cli;

import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import org.jvnet.hudson.test.HudsonTestCase;
import hudson.tasks.Shell;
import hudson.util.OneShotEvent;
import org.jvnet.hudson.test.TestBuilder;
import hudson.model.AbstractBuild;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.ParametersAction;
import java.io.IOException;

/**
 * {@link BuildCommand} test.
 *
 * @author Kohsuke Kawaguchi
 */


public class BuildCommandTest extends HudsonTestCase {

    /**
     * Just schedules a build and return.
     */
    public void testAsync() throws IOException, InterruptedException {
        FreeStyleProject p = createFreeStyleProject();
        final OneShotEvent started = new OneShotEvent();
        final OneShotEvent completed = new OneShotEvent();
        TestBuilder testBuilder = new TestBuilder() {

            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
                started.signal();
                completed.block();
                return true;
            }
        };
        p.getBuildersList().add(testBuilder);

        // this should be asynchronous
        assertEquals(0, new CLI(getURL()).execute("build", p.getName()));
        started.block();
        assertTrue(p.getBuildByNumber(1).isBuilding());
        completed.signal();
    }

    /**
     * Tests synchronous execution.
     */
    public void testSync() throws IOException, InterruptedException {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new Shell("sleep 3"));

        new CLI(getURL()).execute("build", "-s", p.getName());
        assertFalse(p.getBuildByNumber(1).isBuilding());
    }

    public void testParameters() throws IOException, InterruptedException, Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("key", null)));

        new CLI(getURL()).execute("build", "-s", "-p", "key=foobar", p.getName());
        FreeStyleBuild b = assertBuildStatusSuccess(p.getBuildByNumber(1));
        ParameterValue parameter = b.getAction(ParametersAction.class).getParameter("key");
        assertNotNull(parameter);
        assertTrue(parameter instanceof StringParameterValue);
        assertEquals("foobar", ((StringParameterValue) parameter).value);
    }
}
