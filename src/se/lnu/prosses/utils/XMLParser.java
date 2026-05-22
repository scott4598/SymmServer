package se.lnu.prosses.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParser {

	public XMLParser() {
		// TODO Auto-generated constructor stub
	}

	protected Document document;

	protected ArrayList<Node> findTagElement(NodeList nodes, String tag) {
		ArrayList<Node> nodeList = new ArrayList<Node>();
		for(int index2 =0; index2 < nodes.getLength(); index2++)
			if(nodes.item(index2).getAttributes()!=null &&
			nodes.item(index2).getNodeName().equals(tag))
				nodeList.add(nodes.item(index2));
		return nodeList;
	}

	protected String getNodeAttribute(Node node, String attribute) {
		 return node.getAttributes().getNamedItem(attribute).getNodeValue();
	}

	protected ArrayList<Node> findNonNullChilds(Node node) {
		ArrayList<Node> nodeList = new ArrayList<Node>();
		for(int index2 =0; index2 < node.getChildNodes().getLength(); index2++)
			if(node.getChildNodes().item(index2).getAttributes()!=null)
				nodeList.add(node.getChildNodes().item(index2));
		return nodeList;
	}

	Node findTagElement(String element, String attr, String value){
		for(int index2 =0; index2 < document.getElementsByTagName(element).getLength(); index2++)
			if(document.getElementsByTagName(element).item(index2).getAttributes().getNamedItem(attr).getNodeValue().equals(value))
				return document.getElementsByTagName(element).item(index2);
		return null;
	}

	public boolean readXMLFile(String path) {
		File file = new File(path);
		if(!new File(path).exists()) {
			if(path.equals(""))
				Utils.log(getClass(),"The path to the XML file is blank!");
			else
				Utils.log(getClass(),"The xml file " + path + " does not exist!");
			return false;
		}
		Utils.log(this.getClass(), "Loading the source/sink file " + path);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			document = documentBuilder.parse(file);
		} catch (ParserConfigurationException|SAXException e) {
			Utils.logErr(getClass(), "Could not parse the xml file " + file);
			return false;
		} catch (FileNotFoundException es) {
			Utils.logErr(getClass(),  es.getMessage());
			Utils.logErr(getClass(), "Could not find the xml file " + file);
			return false;
		} catch (IOException e) {
			Utils.logErr(getClass(), "Could not load the xml file " + file);
			return false;
		}
		return true;
	}

}
