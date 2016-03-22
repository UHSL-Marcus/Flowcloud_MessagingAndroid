package com.uhsl.flowmessage.flowmessagev2.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Marcus on 17/03/2016.
 */
public class BuildXML {

    private Document doc;
    private Element root;
    private Element lastCreatedElement;

    /**
     * Constructor
     *
     * @param rootName Name of the root node
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public BuildXML(String rootName) throws ParserConfigurationException, IOException,
            SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        doc = builder.newDocument();

        root = doc.createElement(rootName);
        doc.appendChild(root);
        lastCreatedElement = root;
    }

    /**
     * Returns the last created non text element for further use. Non text is an element which has further elements nested.
     *
     * @return Last created non text element
     */
    public Element getLastCreatedElement() {
        return lastCreatedElement;
    }

    /**
     * Return the root element
     * @return Root element
     */
    public Element getRoot() {
        return root;
    }

    /**
     * Private function for neatness, adds a new element of passed name to passed parent node
     *
     * @param parent The parent node to nest the new element under
     * @param name The name of the new element
     * @return The newly created element
     */
    private Element addNewElementHidden(Element parent, String name) {
        Element e = doc.createElement(name);
        parent.appendChild(e);
        return e;
    }

    /**
     * Adds a new non text element
     *
     * @param parent The parent node to next the new element under
     * @param name The name of the new element
     * @return The newly created element
     */
    public Element addNewElement(Element parent, String name) {
        Element e = addNewElementHidden(parent, name);
        lastCreatedElement = e;
        return e;
    }

    /**
     * Add a new text element, aka a final element, one with no further elements nested under it.
     *
     * @param parent The parent nod to nest the new element under
     * @param name The name of the new element
     * @param text The text the new element to contain
     */
    public void addNewTextElement(Element parent, String name, String text) {
        Element e = addNewElementHidden(parent, name);
        e.appendChild(doc.createTextNode(text));
    }

    /**
     * Build the XML document into a string
     *
     * @return String String representation of the XML document
     * @throws TransformerException
     */
    public String buildXMLString() throws TransformerException{
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(doc);
        OutputStream os = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(os);

        transformer.transform(source, result);

        return os.toString();
    }
}
