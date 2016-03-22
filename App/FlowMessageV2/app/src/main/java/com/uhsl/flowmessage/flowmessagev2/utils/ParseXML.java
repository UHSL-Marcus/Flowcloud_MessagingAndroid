package com.uhsl.flowmessage.flowmessagev2.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Marcus on 16/03/2016.
 */
public class ParseXML {

    private Element root;

    /**
     * Constructor
     *
     * @param XML The XML string to be parsed
     * @param expectedRoot The expected name of the root node
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws BadXMLRootException
     */
    public ParseXML (String XML, String expectedRoot) throws ParserConfigurationException, IOException,
            SAXException, BadXMLRootException {

        XML = ParseXML.getValidXML(XML);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(XML)));

        doc.getDocumentElement().normalize();


        root = doc.getDocumentElement();

        if (!root.getNodeName().equals(expectedRoot))
            throw new BadXMLRootException();

        //System.out.println("XML:\n" + XML);
    }

    /**
     * Return the data under an element node as a string.
     * Could be text, could be more XML in string format if the node is not a final text nod
     *
     * @param elementName Name of the elements
     * @return The data under the element
     */
    public String getElementValue(String elementName) {
        Node n = root.getElementsByTagName(elementName).item(0);
        return getNodeValueAsString(n);
    }

    /**
     * Recursively loop through each node under initally passed element and build the data string
     *
     * @param n Current working node
     * @return String Data under node n
     */
    private String getNodeValueAsString(Node n) {
        String value = n.getNodeValue();
        if (n.hasChildNodes())
            value = n.getFirstChild().getNodeValue();

        if (value == null) {
            value = "";
            NodeList nl = n.getChildNodes();
            for(int i = 0; i < nl.getLength(); i++) {
                Node child = nl.item(i);
                value += "<" + child.getNodeName() + ">";
                value += getNodeValueAsString(child);
                value += "</" + child.getNodeName() + ">";
            }
        }
        return value;
    }

    /**
     * Static method to encapsulate XML data that lacks the  declaration data
     *
     * @param XML XML to validate
     * @return Valid XML
     */
    public static String getValidXML(String XML) {

        if (!XML.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
            XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + XML;

        return XML;
    }

    /**
     * Custom exception for when the root element doesn't match
     */
    public class BadXMLRootException extends Exception {
        public BadXMLRootException(){
            super("XML root element was not as expected");
        }
    }
}


