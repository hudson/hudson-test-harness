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

package hudson.slaves;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import junit.framework.Assert;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * This class tests that environment variables from node properties are applied,
 * and that the priority is maintained: parameters > slave node properties >
 * master node properties
 */
public class EnvironmentVariableRoundTripTest extends HudsonTestCase {

	private DumbSlave slave;
	private FreeStyleProject project;


	public void testFormRoundTripForMaster() throws Exception {
        hudson.getGlobalNodeProperties().replaceBy(
                Collections.singleton(new EnvironmentVariablesNodeProperty(
                        new Entry("KEY", "value"))));
		
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage(hudson, "configure");
		HtmlForm form = page.getFormByName("config");
		submit(form);
		
		Assert.assertEquals(1, hudson.getGlobalNodeProperties().toList().size());
		
		EnvironmentVariablesNodeProperty prop = hudson.getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
		Assert.assertEquals(1, prop.getEnvVars().size());
		Assert.assertEquals("value", prop.getEnvVars().get("KEY"));
	}

	public void testFormRoundTripForSlave() throws Exception {
		setVariables(slave, new Entry("KEY", "value"));
		
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage(slave, "configure");
		HtmlForm form = page.getFormByName("config");
		submit(form);
		
		Assert.assertEquals(1, slave.getNodeProperties().toList().size());
		
		EnvironmentVariablesNodeProperty prop = slave.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
		Assert.assertEquals(1, prop.getEnvVars().size());
		Assert.assertEquals("value", prop.getEnvVars().get("KEY"));
	}
	
	// //////////////////////// setup //////////////////////////////////////////

	public void setUp() throws Exception {
		super.setUp();
		slave = createSlave();
		project = createFreeStyleProject();
	}

	// ////////////////////// helper methods /////////////////////////////////

	private void setVariables(Node node, Entry... entries) throws IOException {
		node.getNodeProperties().replaceBy(
				Collections.singleton(new EnvironmentVariablesNodeProperty(
						entries)));

	}

}
