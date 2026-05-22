package se.lnu.prosses.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLSourceSinkParser extends XMLParser {


	public XMLSourceSinkParser() {
	}

	private void addElement(String[] attributes, Node tag, HashMap<String, List<String[]>> output) {
		String[] elementsValues = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++)
			elementsValues[i] = tag.getAttributes().getNamedItem(attributes[i]).getNodeValue().replaceAll(";", "")
			.trim();

		if (output.get(tag.getNodeName()) == null)
			output.put(tag.getNodeName(), new ArrayList<String[]>());
		//System.err.println(tag.getNodeName()+" " + elementsValues[0] + ":" + elementsValues[1]);
		output.get(tag.getNodeName()).add(elementsValues);
	}

	// String sourceChilds =
	// document.getElementsByTagName(data).item(0).getChildNodes().item(1).getAttributes().getNamedItem("class")


	public HashMap<String, List<String[]>> getSinkOrSources(String data, HashMap<String, List<String[]>> output) {
		if (document == null) {
			Utils.logErr(getClass(), "The XML file is not loaded or is empty");
			return null;
		}

		NodeList allSourceChilds = document.getElementsByTagName(data);
		//.item(0).getChildNodes();
		for(int index1 =0; index1 < allSourceChilds.getLength(); index1++) {
			NodeList sourceChilds = allSourceChilds.item(index1).getChildNodes();
			for (int index = 0; index < sourceChilds.getLength(); index++) {
				if (sourceChilds.item(index).getAttributes() != null) {
					if (sourceChilds.item(index).getNodeName().equals(Utils.XMLField))
						addElement(new String[] { "class", "name" }, sourceChilds.item(index), output);
					else if (sourceChilds.item(index).getNodeName().equals(Utils.XMLParameter))
						addElement(new String[] { "class", "method", "parameter" }, sourceChilds.item(index), output);
					else if (sourceChilds.item(index).getNodeName().equals(Utils.XMlReturnValue))
						addElement(new String[] { "class", "method" }, sourceChilds.item(index), output);
					else
						Utils.logErr(getClass(), "Unknown type: " + sourceChilds.item(index).getNodeName());
				}
			}
		}
		return output;
	}

	public HashMap<String, List<String[]>> getAllSinksOrSources(HashMap<String, List<String[]>> output, boolean isHigh, boolean isSource) {
		if (document == null) {
			Utils.logErr(getClass(), "The XML file is not loaded or is empty");
			return null;
		}
		NodeList allSourcesSinkTypes = document.getElementsByTagName("assign");
		for(int index1 =0; index1 < allSourcesSinkTypes.getLength(); index1++) {
			String domain = allSourcesSinkTypes.item(index1).getAttributes().getNamedItem("domain").getNodeValue();
			if(isHigh?domain.equals("high"):domain.equals("low")) {
				String handle = allSourcesSinkTypes.item(index1).getAttributes().getNamedItem("handle").getNodeValue();
				Node handleNode = findTagElement("assignable","handle",handle);
				if(handleNode!=null) {
					NodeList handleChilds = handleNode.getChildNodes();
					ArrayList<Node> categoryNodes = findTagElement(handleChilds, "category");
					if(categoryNodes==null || categoryNodes.size()==0) {
						handleSourceSinkNodes(handleChilds, isSource, output);			
					}
					else
						for(Node node:categoryNodes) {
							handleSourceSinkNodes(node.getChildNodes(), isSource, output);
						}
				} else
				{
					Utils.logErr(getClass(), "No  Handle Is Defined For The Tag "+ handle);
				}
			}
		}
		return output;
	}

	private void handleSourceSinkNodes(NodeList handleChilds, boolean isSource, HashMap<String, List<String[]>> output) {
		ArrayList<Node> sinkSourceNodes = findTagElement(handleChilds, isSource?"source":"sink");
		for(Node node: sinkSourceNodes) {
			for(Node ss : findNonNullChilds(node)){
				if (ss.getNodeName().equals(Utils.XMLField))
					addElement(new String[] { "class", "name" }, ss, output);
				else if (ss.getNodeName().equals(Utils.XMLParameter))
					addElement(new String[] { "class", "method", "parameter" }, ss, output);
				else if (ss.getNodeName().equals(Utils.XMlReturnValue))
					addElement(new String[] { "class", "method" }, ss, output);
				else if (ss.getNodeName().equals(Utils.XMlMethod))
					addElement(new String[] { "class", "method" }, ss, output);
				else if (ss.getNodeName().equals(Utils.XMLReference))
					addElement(new String[] { "class", "method" }, ss, output);
				else
					Utils.logErr(getClass(), "Unknown type: " + ss.getNodeName());
			}
		}
	}


}
