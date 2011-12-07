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
#         Kohsuke Kawaguchi
#
#**************************************************************************/ 

package hudson.matrix;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import java.io.IOException;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;


public class MatrixProjectTest extends HudsonTestCase {

    void assertRectangleTable(MatrixProject p) throws IOException, SAXException {
        HtmlPage html = createWebClient().getPage(p);
        HtmlTable table = (HtmlTable) html.selectSingleNode("id('matrix')/table");

        // remember cells that are extended from rows above.
        //def rowSpans = [:];
        int masterWidth;
        for (HtmlTableRow r : table.getRows()) {
            // TODO - rewrite the following groovy in Java
//            int width = r.cells*.columnSpan.sum() + rowSpans.values().sum(0);
//            if (masterWidth == null)
//                masterWidth = width;
//            else
//                assertEquals(masterWidth,width);
//
//            for (c in r.cells)
//                rowSpans[c.rowSpan] = (rowSpans[c.rowSpan]?:0)+c.columnSpan
//            // shift rowSpans by one
//            def nrs =[:]
//            rowSpans.each { k,v -> if(k>1) nrs[k-1]=v }
//            rowSpans = nrs
        }
    }

    @Bug(4245)
    void testLayout1() throws IOException, SAXException {
        // 5*5*5*5*5 matrix
        MatrixProject p = createMatrixProject();
        // TODO: Convert the following groovy to Java
//        p.axes = new AxisList(
//            'a','b','c','d','e'].collect { name -> new TextAxis(name, (1..4)*.toString() ) }
//        );
        assertRectangleTable(p);
    }

    @Bug(4245)
    void testLayout2() throws IOException, SAXException {
        // 2*3*4*5*6 matrix
        MatrixProject p = createMatrixProject();
        // TODO: Convert the following groovy to Java
//        p.axes = new AxisList(
//            (2..6).collect { n -> new TextAxis("axis${n}", (1..n)*.toString() ) }
//        );
        assertRectangleTable(p);
    }

    /**
     * Makes sure that the configuration correctly roundtrips.
     */
    public void testConfigRoundtrip() throws IOException, Exception {
//        hudson.getJDKs().addAll(
//                new JDK("jdk1.7","somewhere"),
//                new JDK("jdk1.6","here"),
//                new JDK("jdk1.5","there")
//                );
//
//        List<Slave> slaves = (0..2).collect { createSlave() };

        MatrixProject p = createMatrixProject();
        
        String[] jdks = {"jdk1.6","jdk1.5"};
        
        p.getAxes().add(new JDKAxis(jdks));
        
//        p.axes.add(new LabelAxis("label1",slaves[0].nodeName, slaves[1].nodeName]));
//        p.axes.add(new LabelAxis("label2",[slaves[2].nodeName])); // make sure single value handling works OK
        
        AxisList o = new AxisList(p.getAxes());
        configRoundtrip(p);
        AxisList n = p.getAxes();

        assertEquals(o.size(),n.size());
        
//        (0 ..< (o.size())).each { i ->
//            def oi = o[i];
//            def ni = n[i];
//            assertSame(oi.class,ni.class);
//            assertEquals(oi.name,ni.name);
//            assertEquals(oi.values,ni.values);
//        }
    }

    public void testLabelAxes() throws IOException {
        MatrixProject p = createMatrixProject();

//        List<Slave> slaves = (0..<4).collect { createSlave() }
//
//        p.axes.add(new LabelAxis("label1",[slaves[0].nodeName, slaves[1].nodeName]));
//        p.axes.add(new LabelAxis("label2",[slaves[2].nodeName, slaves[3].nodeName]));

        System.out.println(p.getLabels());
        assertEquals(4,p.getLabels().size());
        assertTrue(p.getLabels().contains(hudson.getLabel("slave0&&slave2")));
        assertTrue(p.getLabels().contains(hudson.getLabel("slave1&&slave2")));
        assertTrue(p.getLabels().contains(hudson.getLabel("slave0&&slave3")));
        assertTrue(p.getLabels().contains(hudson.getLabel("slave1&&slave3")));
    }

    /**
     * Quiettng down Hudson causes a dead lock if the parent is running but children is in the queue
     */
    @Bug(4873)
    void testQuietDownDeadlock() {
//        def p = createMatrixProject();
//        p.axes = new AxisList(new TextAxis("foo","1","2"));
//        p.runSequentially = true; // so that we can put the 2nd one in the queue
//
//        OneShotEvent firstStarted = new OneShotEvent();
//        OneShotEvent buildCanProceed = new OneShotEvent();
//
//        p.getBuildersList().add( [perform:{ AbstractBuild build, Launcher launcher, BuildListener listener ->
//            firstStarted.signal();
//            buildCanProceed.block();
//            return true;
//        }] as TestBuilder );
//        Future f = p.scheduleBuild2(0)
//
//        // have foo=1 block to make sure the 2nd configuration is in the queue
//        firstStarted.block();
//        // enter into the quiet down while foo=2 is still in the queue
//        hudson.doQuietDown();
//        buildCanProceed.signal();
//
//        // make sure foo=2 still completes. use time out to avoid hang
//        assertBuildStatusSuccess(f.get(10,TimeUnit.SECONDS));
//
//        // MatrixProject scheduled after the quiet down shouldn't start
//        try {
//            Future g = p.scheduleBuild2(0)
//            g.get(3,TimeUnit.SECONDS)
//            fail()
//        } catch (TimeoutException e) {
//            // expected
//        }        
    }
}
