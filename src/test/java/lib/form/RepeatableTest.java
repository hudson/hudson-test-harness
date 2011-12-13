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
 *    Alan Harder
 *     
 *
 *******************************************************************************/ 

package lib.form;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJob;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManagerImpl;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Alan.Harder@sun.com
 */
public class RepeatableTest extends HudsonTestCase {
    private JSONObject formData;
    private Class<?> bindClass;
    private List<?> bindResult;
    public List<Object> list = new ArrayList<Object>();
    public Integer minimum = null;

    public void doSubmitTest(StaplerRequest req) throws Exception {
        formData = req.getSubmittedForm();
        if (bindClass != null)
            bindResult = req.bindJSONToList(bindClass, formData.get("items"));
    }

    // ========================================================================

    private void doTestSimple() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testSimple");
        HtmlForm f = p.getFormByName("config");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("value one");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("value two");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("value three");
        f.getInputsByName("bool").get(2).click();
        submit(f);
    }

    public void testSimple() throws Exception {
        doTestSimple();
        JSONArray array = formData.getJSONArray("foos");
        assertEquals("value one", array.getJSONObject(0).get("txt"));
        assertEquals(false, array.getJSONObject(0).get("bool"));

        assertEquals("value two", array.getJSONObject(1).get("txt"));
        assertEquals(false, array.getJSONObject(1).get("bool"));

        assertEquals("value three", array.getJSONObject(2).get("txt"));
        assertEquals(true, array.getJSONObject(2).get("bool"));
    }

    // ========================================================================

    public static class Foo {
        public String txt;
        public boolean bool;
        @DataBoundConstructor
        public Foo(String txt, boolean bool) { this.txt = txt; this.bool = bool; }
        @Override public String toString() { return "foo:" + txt + ':' + bool; }
    }

    private void addData() {
        list.add(new Foo("existing one", true));
        list.add(new Foo("existing two", false));
    }

    public void testSimple_ExistingData() throws Exception {
        addData();
        doTestSimple();

        JSONArray array = formData.getJSONArray("foos");

        assertEquals("existing one", array.getJSONObject(0).get("txt"));
        assertEquals(true, array.getJSONObject(0).get("bool"));

        assertEquals("existing two", array.getJSONObject(1).get("txt"));
        assertEquals(false, array.getJSONObject(1).get("bool"));

        assertEquals("value one", array.getJSONObject(2).get("txt"));
        assertEquals(true, array.getJSONObject(2).get("bool"));

        assertEquals("value two", array.getJSONObject(3).get("txt"));
        assertEquals(false, array.getJSONObject(3).get("bool"));

        assertEquals("value three", array.getJSONObject(4).get("txt"));
        assertEquals(false, array.getJSONObject(4).get("bool"));
    }

    public void testMinimum() throws Exception {
        minimum = 3;
        HtmlPage p = createWebClient().goTo("self/testSimple");
        HtmlForm f = p.getFormByName("config");
        f.getInputByValue("").setValueAttribute("value one");
        f.getInputByValue("").setValueAttribute("value two");
        f.getInputByValue("").setValueAttribute("value three");
        try { f.getInputByValue(""); fail("?"); } catch (ElementNotFoundException expected) { }
        f.getInputsByName("bool").get(2).click();
        submit(f);

        JSONArray array = formData.getJSONArray("foos");
        assertEquals("value one", array.getJSONObject(0).get("txt"));
        assertEquals(false, array.getJSONObject(0).get("bool"));

        assertEquals("value two", array.getJSONObject(1).get("txt"));
        assertEquals(false, array.getJSONObject(1).get("bool"));

        assertEquals("value three", array.getJSONObject(2).get("txt"));
        assertEquals(true, array.getJSONObject(2).get("bool"));
    }

    public void testMinimum_ExistingData() throws Exception {
        addData();
        minimum = 3;
        HtmlPage p = createWebClient().goTo("self/testSimple");
        HtmlForm f = p.getFormByName("config");
        f.getInputByValue("").setValueAttribute("new one");
        try { f.getInputByValue(""); fail("?"); } catch (ElementNotFoundException expected) { }
        f.getInputsByName("bool").get(1).click();
        submit(f);

        JSONArray array = formData.getJSONArray("foos");
        assertEquals("existing one", array.getJSONObject(0).get("txt"));
        assertEquals(true, array.getJSONObject(0).get("bool"));

        assertEquals("existing two", array.getJSONObject(1).get("txt"));
        assertEquals(true, array.getJSONObject(1).get("bool"));

        assertEquals("new one", array.getJSONObject(2).get("txt"));
        assertEquals(false, array.getJSONObject(2).get("bool"));

    }

    // ========================================================================

    // hudson-behavior uniquifies radiobutton names so the browser properly handles each group,
    // then converts back to original names when submitting form.
    public void testRadio() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testRadio");
        HtmlForm f = p.getFormByName("config");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("txt one");
        f.getElementsByAttribute("INPUT", "type", "radio").get(1).click();
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("txt two");
        f.getElementsByAttribute("INPUT", "type", "radio").get(3).click();
        submit(f);
        JSONArray array = formData.getJSONArray("foos");
        assertEquals("txt one", array.getJSONObject(0).get("txt"));
        assertEquals("two", array.getJSONObject(0).get("radio"));

        assertEquals("txt two", array.getJSONObject(1).get("txt"));
        assertEquals("two", array.getJSONObject(1).get("radio"));

    }

    public static class FooRadio {
        public String txt, radio;
        public FooRadio(String txt, String radio) { this.txt = txt; this.radio = radio; }
    }

    public void testRadio_ExistingData() throws Exception {
        list.add(new FooRadio("1", "one"));
        list.add(new FooRadio("2", "two"));
        list.add(new FooRadio("three", "one"));
        HtmlPage p = createWebClient().goTo("self/testRadio");
        HtmlForm f = p.getFormByName("config");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("txt 4");
        f.getElementsByAttribute("INPUT", "type", "radio").get(7).click();
        submit(f);

        JSONArray array = formData.getJSONArray("foos");
        assertEquals("1", array.getJSONObject(0).get("txt"));
        assertEquals("one", array.getJSONObject(0).get("radio"));

        assertEquals("2", array.getJSONObject(1).get("txt"));
        assertEquals("two", array.getJSONObject(1).get("radio"));

        assertEquals("three", array.getJSONObject(2).get("txt"));
        assertEquals("one", array.getJSONObject(2).get("radio"));

        assertEquals("txt 4", array.getJSONObject(3).get("txt"));
        assertEquals("two", array.getJSONObject(3).get("radio"));

    }

    // hudson-behavior uniquifies radiobutton names so the browser properly handles each group,
    // then converts back to original names when submitting form.
    //TODO fix me
    public void ignore_testRadioBlock() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testRadioBlock");
        HtmlForm f = p.getFormByName("config");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("txt one");
        f.getInputByValue("").setValueAttribute("avalue do not send");
        f.getElementsByAttribute("INPUT", "type", "radio").get(1).click();
        f.getInputByValue("").setValueAttribute("bvalue");
        f.getButtonByCaption("Add").click();
        f.getInputByValue("").setValueAttribute("txt two");
        f.getElementsByAttribute("INPUT", "type", "radio").get(2).click();
        f.getInputByValue("").setValueAttribute("avalue two");
        submit(f);
        JSONArray array = formData.getJSONArray("foos");

        assertEquals("txt one", array.getJSONObject(0).get("txt"));
        JSONObject radio0 = array.getJSONObject(0).getJSONObject("radio");
        assertEquals("bvalue", radio0.get("b"));
        assertEquals("two", radio0.get("value"));

        assertEquals("txt two", array.getJSONObject(1).get("txt"));
        JSONObject radio1 = array.getJSONObject(1).getJSONObject("radio");
        assertEquals("avalue two", radio1.get("a"));
        assertEquals("one", radio1.get("value"));
    }

    // ========================================================================

    public static class Fruit implements ExtensionPoint, Describable<Fruit> {
        protected String name;
        private Fruit(String name) { this.name = name; }

        public Descriptor<Fruit> getDescriptor() {
            return Hudson.getInstance().getDescriptor(getClass());
        }
    }

    public static class FruitDescriptor extends Descriptor<Fruit> {
        public FruitDescriptor(Class<? extends Fruit> clazz) {
            super(clazz);
        }
        public String getDisplayName() {
            return clazz.getSimpleName();
        }
    }

    public static class Apple extends Fruit {
        private int seeds;
        @DataBoundConstructor public Apple(int seeds) { super("Apple"); this.seeds = seeds; }
        @Extension public static final FruitDescriptor D = new FruitDescriptor(Apple.class);
        @Override public String toString() { return name + " with " + seeds + " seeds"; }
    }
    public static class Banana extends Fruit {
        private boolean yellow;
        @DataBoundConstructor public Banana(boolean yellow) { super("Banana"); this.yellow = yellow; }
        @Extension public static final FruitDescriptor D = new FruitDescriptor(Banana.class);
        @Override public String toString() { return (yellow ? "Yellow" : "Green") + " " + name; }
    }

    public static class Fruity {
        public Fruit fruit;
        public String word;
        @DataBoundConstructor public Fruity(Fruit fruit, String word) {
            this.fruit = fruit;
            this.word = word;
        }
        @Override public String toString() { return fruit + " " + word; }
    }

    public DescriptorExtensionList<Fruit,Descriptor<Fruit>> getFruitDescriptors() {
        return hudson.<Fruit,Descriptor<Fruit>>getDescriptorList(Fruit.class);
    }

    //TODO fix me
    public void ignore_testDropdownList() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testDropdownList");
        HtmlForm f = p.getFormByName("config");
        f.getButtonByCaption("Add").click();
        waitForJavaScript(p);
        f.getInputByValue("").setValueAttribute("17"); // seeds
        f.getInputByValue("").setValueAttribute("pie"); // word
        f.getButtonByCaption("Add").click();
        waitForJavaScript(p);
        // select banana in 2nd select element:
        ((HtmlSelect)f.getElementsByTagName("select").get(1)).getOption(1).click();
        f.getInputsByName("yellow").get(1).click(); // checkbox
        f.getInputsByValue("").get(1).setValueAttribute("split"); // word
        String xml = f.asXml();
        bindClass = Fruity.class;
        submit(f);
        assertEquals(formData + "\n" + xml,
                     "[Apple with 17 seeds pie, Yellow Banana split]", bindResult.toString());
    }

    // ========================================================================

    public static class FooList {
        public String title;
        public Foo[] list = new Foo[0];
        @DataBoundConstructor public FooList(String title, Foo[] foo) {
            this.title = title;
            this.list = foo;
        }
        @Override public String toString() {
            StringBuilder buf = new StringBuilder("FooList:" + title + ":[");
            for (int i = 0; i < list.length; i++) {
                if (i > 0) buf.append(',');
                buf.append(list[i].toString());
            }
            buf.append(']');
            return buf.toString();
        }
    }
    //TODO fix me
    /** Tests nested repeatable and use of @DataBoundContructor to process formData */
    public void ignore_testNested() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testNested");
        HtmlForm f = p.getFormByName("config");
        try {
            clickButton(p, f, "Add");
            f.getInputByValue("").setValueAttribute("title one");
            clickButton(p,f,"Add Foo");
            f.getInputByValue("").setValueAttribute("txt one");
            clickButton(p,f,"Add Foo");
            f.getInputByValue("").setValueAttribute("txt two");
            f.getInputsByName("bool").get(1).click();
            clickButton(p, f, "Add");
            f.getInputByValue("").setValueAttribute("title two");
            f.getElementsByTagName("button").get(1).click(); // 2nd "Add Foo" button
            f.getInputByValue("").setValueAttribute("txt 2.1");
        } catch (Exception e) {
            System.err.println("HTML at time of failure:\n" + p.getBody().asXml());
            throw e;
        }
        bindClass = FooList.class;
        submit(f);
        assertEquals("[FooList:title one:[foo:txt one:false,foo:txt two:true], "
                     + "FooList:title two:[foo:txt 2.1:false]]", bindResult.toString());
    }

    private void clickButton(HtmlPage p, HtmlForm f, String caption) throws IOException {
        f.getButtonByCaption(caption).click();
        waitForJavaScript(p);
    }
    //TODO fix me
    public void ignore_testNestedRadio() throws Exception {
        HtmlPage p = createWebClient().goTo("self/testNestedRadio");
        HtmlForm f = p.getFormByName("config");
        try {
            clickButton(p, f, "Add");
            f.getElementsByAttribute("input", "type", "radio").get(1).click(); // outer=two
            f.getButtonByCaption("Add Moo").click();
            waitForJavaScript(p);
            f.getElementsByAttribute("input", "type", "radio").get(2).click(); // inner=inone
            f.getButtonByCaption("Add").click();
            waitForJavaScript(p);
            f.getElementsByAttribute("input", "type", "radio").get(4).click(); // outer=one
            Thread.sleep(500);
            f.getElementsByTagName("button").get(1).click(); // 2nd "Add Moo" button
            waitForJavaScript(p);
            f.getElementsByAttribute("input", "type", "radio").get(7).click(); // inner=intwo
            f.getElementsByTagName("button").get(1).click();
            waitForJavaScript(p);
            f.getElementsByAttribute("input", "type", "radio").get(8).click(); // inner=inone
        } catch (Exception e) {
            System.err.println("HTML at time of failure:\n" + p.getBody().asXml());
            throw e;
        }
        submit(f);
        assertEquals("[{\"moo\":{\"inner\":\"inone\"},\"outer\":\"two\"},"
                     + "{\"moo\":[{\"inner\":\"intwo\"},{\"inner\":\"inone\"}],\"outer\":\"one\"}]",
                     formData.get("items").toString());
    }

    /**
     * YUI internally partially relies on setTimeout/setInterval when we add a new chunk of HTML
     * to the page. So wait for the completion of it.
     *
     * <p>
     * To see where such asynchronous activity is happening, set a breakpoint to
     * {@link JavaScriptJobManagerImpl#addJob(JavaScriptJob, Page)} and look at the call stack.
     * Also see {@link #jsDebugger} at that time to see the JavaScript callstack.
     */
    private void waitForJavaScript(HtmlPage p) {
        p.getEnclosingWindow().getJobManager().waitForJobsStartingBefore(50);
    }
}
