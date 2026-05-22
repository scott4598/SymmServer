package se.lnu.prosses.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import se.lnu.prosses.core.ProjectHelper;
import se.lnu.prosses.core.ProjectHelper.SecuritySignature;

public class XMLSecstubsParser extends XMLParser {

    public XMLSecstubsParser() {
    }

    public void getSecstubs(Hashtable<String, ArrayList<SecuritySignature>> securitySignatures) {
	if (document == null) {
	    Utils.logErr(getClass(), "The XML file is not loaded or is empty");
	    return;
	}

	NodeList allMethods = document.getElementsByTagName("method");
	// .item(0).getChildNodes();
	for (int index1 = 0; index1 < allMethods.getLength(); index1++) {
	    SecuritySignature securitySignature = new ProjectHelper().new SecuritySignature();
	    for (Node methodChild : super.findNonNullChilds(allMethods.item(index1)))
		if (methodChild.getNodeName().equals("methodSignature")) {
		    String signtaure = getNodeAttribute(methodChild, "signature");
		    if (securitySignature.extractMethodSignature(signtaure)) {
			if (securitySignatures.get(securitySignature.method) == null)
			    securitySignatures.put(securitySignature.method, new ArrayList<SecuritySignature>());
			securitySignatures.get(securitySignature.method).add(securitySignature);
		    }
		} else if (methodChild.getNodeName().equals("confidentiality"))
		    extractSecstubs(methodChild, securitySignature);
	}
    }

    private void extractSecstubs(Node item, SecuritySignature securitySignature) {
	ArrayList<Node> methodChanges = super.findNonNullChilds(item);
	HashMap<String, Object> output = new HashMap<String, Object>();
	for (Node methodChange : methodChanges) {
	    if (methodChange.getNodeName().equals("methodChanges"))
		extractMethodChanges(methodChange, output);
	    else if (methodChange.getNodeName().equals("methodReturns"))
		extractMethodreturns(methodChange, output);
	    else if (methodChange.getNodeName().equals("methodOutputs"))
		extractLevels(methodChange, output, "output");
	    else if (methodChange.getNodeName().equals("methodAssumes"))
		extractAssumes(methodChange, output, "assume");

	}
	populate(output, securitySignature);
    }

    // pars, this, ret

    private void populate(HashMap<String, Object> output, SecuritySignature securitySignature) {
	/*
	 * Object field —> “return .*;" returns args or object fields —> return *;
	 * returns args or this —> return @; new that does not depend on args/object of
	 * at most level—> return new_X; new that does not depend on args/objects —>
	 * return new
	 * 
	 * changes only args with leats upper bound of args/object—> @<~@; or @<~L/H;
	 * changes base with leats upper bound of args/object —> ".*<~@;" or ".*<~L/H;”
	 * changes both this and args with leats upper bound of args/object —> "*<~@;"
	 * changes none —> “-<~;”
	 * 
	 * does not assume the integrity of args/object —> -! assumes the integrity of
	 * args/object —> @! assumes the integrity of object —> .*!
	 */

	if (!(boolean) output.get("parsNondefault") && !(boolean) output.get("thisNondefault"))
	    securitySignature.secSignature.add("-<~");
	else if ((boolean) output.get("parsNondefault")
		&& ((String) output.get("parsLevel")).equals("least upper bound")
		&& (boolean) output.get("thisNondefault")
		&& ((String) output.get("thisLevel")).equals("least upper bound"))
	    securitySignature.secSignature.add("*<~@");
	else {
	    if ((boolean) output.get("parsNondefault") && ((String) output.get("parsLevel")).equals("least upper bound")
		    && !(boolean) output.get("thisNondefault"))
		securitySignature.secSignature.add("@<~@");
	    if ((boolean) output.get("parsNondefault") && ((String) output.get("parsLevel")).equals("low")
		    && !(boolean) output.get("thisNondefault"))
		securitySignature.secSignature.add("@<~L");
	    if ((boolean) output.get("parsNondefault") && ((String) output.get("parsLevel")).equals("high")
		    && !(boolean) output.get("thisNondefault"))
		securitySignature.secSignature.add("@<~H");

	    if (!(boolean) output.get("parsNondefault") && (boolean) output.get("thisNondefault")
		    && ((String) output.get("thisLevel")).equals("least upper bound"))
		securitySignature.secSignature.add(".*<~@");
	    if (!(boolean) output.get("parsNondefault") && (boolean) output.get("thisNondefault")
		    && ((String) output.get("thisLevel")).equals("low"))
		securitySignature.secSignature.add(".*<~L");
	    if (!(boolean) output.get("parsNondefault") && (boolean) output.get("thisNondefault")
		    && ((String) output.get("thisLevel")).equals("high"))
		securitySignature.secSignature.add(".*<~H");
	}

	// depends on args, object fields
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("new")
		&& ((String) output.get("retLevel")).equals("low"))
	    securitySignature.secSignature.add("return new_L");
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("new")
		&& ((String) output.get("retLevel")).equals("high"))
	    securitySignature.secSignature.add("return new_H");
	// does not depend on args, object fields
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("new")
		&& ((String) output.get("retLevel")).equals(""))
	    securitySignature.secSignature.add("return new");
	// returns an object fields
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("thisObjectFields")
		&& ((String) output.get("retLevel")).equals("high"))
	    securitySignature.secSignature.add("return .*H");
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("thisObjectFields")
		&& ((String) output.get("retLevel")).equals("low"))
	    securitySignature.secSignature.add("return .*L");
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("thisObjectFields")
		&& ((String) output.get("retLevel")).equals("least upper bound"))
	    securitySignature.secSignature.add("return .*");
	// return on of its args or this
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("new")
		&& ((String) output.get("retLevel")).equals("argumentsOrThisObejct"))
	    securitySignature.secSignature.add("return @X");
	// anything is an object field or args
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("anything")
		&& ((String) output.get("retLevel")).equals("least upper bound"))
	    securitySignature.secSignature.add("return *");
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("anything")
		&& ((String) output.get("retLevel")).equals("low"))
	    securitySignature.secSignature.add("return *L");
	if ((boolean) output.get("retNondefault") && ((String) output.get("retStatement")).equals("anything")
		&& ((String) output.get("retLevel")).equals("high"))
	    securitySignature.secSignature.add("return new(%(*))");

	if ((boolean) output.get("outputNondefault") && ((String) output.get("outputLevel")).equals("high"))
	    securitySignature.secSignature.add("output_H");
	if ((boolean) output.get("outputNondefault") && ((String) output.get("outputLevel")).equals("low"))
	    securitySignature.secSignature.add("output_L");

	/*
	 * if(!(boolean)output.get("assumeNondefault")) securitySignature.secSignature
	 * += "-!"; if((boolean)output.get("assumeNondefault") &&
	 * ((String)output.get("assumes")).equals("argumentAndThisObject"))
	 * securitySignature.secSignature += "@!";
	 * if((boolean)output.get("assumeNondefault") &&
	 * ((String)output.get("assumes")).equals("thisObject"))
	 * securitySignature.secSignature += ".*!";
	 */
	// securitySignature.secSignature =
	// securitySignature.secSignature.substring(0,securitySignature.secSignature.length()-1);

    }

    /*
     * private void extractMethodoutputs(Node methodChange, HashMap<String, Object>
     * output) { boolean outputExecuted = false; String outputLevel = null;
     * extractLevels(changesParameterChilds, output);
     * System.out.println(outputExecuted + " " + outputLevel); }
     */

    private void extractAssumes(Node node, HashMap<String, Object> output, String base) {
	for (Node child : super.findNonNullChilds(node))
	    if (child.getNodeName().equals("executed")) {
		output.put(base + "Nondefault",
			(super.getNodeAttribute(child, "isExecuted").equals("no") ? false : true));
	    } else if (child.getNodeName().equals("assumes")) {
		output.put(base + "Assumes", super.getNodeAttribute(child, "assumes"));
	    }
    }

    private void extractLevels(Node node, HashMap<String, Object> output, String base) {
	for (Node child : super.findNonNullChilds(node))
	    if (child.getNodeName().equals("executed")) {
		output.put(base + "Nondefault",
			(super.getNodeAttribute(child, "isExecuted").equals("no") ? false : true));
	    } else if (child.getNodeName().equals("level")) {
		output.put(base + "Level", super.getNodeAttribute(child, "to"));
	    }
    }

    private void extractMethodreturns(Node node, HashMap<String, Object> output) {
	ArrayList<Node> methodChanges = super.findNonNullChilds(node);
	for (Node methodChange : methodChanges) {
	    if (methodChange.getNodeName().equals("executed")) {
		output.put("retNondefault",
			(super.getNodeAttribute(methodChange, "isExecuted").equals("no") ? false : true));
	    } else if (methodChange.getNodeName().equals("returnStatement")) {
		output.put("retStatement", super.getNodeAttribute(methodChange, "returns"));
	    } else if (methodChange.getNodeName().equals("level")) {
		output.put("retLevel", super.getNodeAttribute(methodChange, "to"));
	    }
	}
    }

    private void extractMethodChanges(Node item, HashMap<String, Object> output) {
	ArrayList<Node> methodChanges = super.findNonNullChilds(item);
	for (Node methodChange : methodChanges) {
	    if (methodChange.getNodeName().equals("changesParameter")) {
		extractLevels(methodChange, output, "pars");
	    } else if (methodChange.getNodeName().equals("changesThisObject")) {
		extractLevels(methodChange, output, "this");
	    }
	}
    }
}
