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
*    Kohsuke Kawaguchi, Yahoo!, Inc.
 *     
 *
 *******************************************************************************/ 

package hudson.model;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Bug;

/**
 * @author Kohsuke Kawaguchi
 */
public class ApiTest extends HudsonTestCase {

    @Bug(2828)
    public void testXPath() throws Exception {
        new WebClient().goTo("api/xml?xpath=/*[1]","application/xml");
    }

    @Bug(3267)
    public void testWrappedZeroItems() throws Exception {
        Page page = new WebClient().goTo("api/xml?wrapper=root&xpath=/hudson/nonexistent", "application/xml");
        assertEquals("<root/>", page.getWebResponse().getContentAsString());
    }

    @Bug(3267)
    public void testWrappedOneItem() throws Exception {
        Page page = new WebClient().goTo("api/xml?wrapper=root&xpath=/hudson/view/name", "application/xml");
        assertEquals("<root><name>All</name></root>", page.getWebResponse().getContentAsString());
    }

    public void testWrappedMultipleItems() throws Exception {
        createFreeStyleProject();
        createFreeStyleProject();
        Page page = new WebClient().goTo("api/xml?wrapper=root&xpath=/hudson/job/name", "application/xml");
        assertEquals("<root><name>test0</name><name>test1</name></root>", page.getWebResponse().getContentAsString());
    }
}
