package com.uhsl.flowmessage.flowmessagev2.utils;

import org.w3c.dom.Element;


/**
 * Created by Marcus on 02/03/2016.
 */
public class MessageFormat {
    public String messageID;
    public String senderID;
    public String senderType;
    public String type;
    public String body;

    /**
     * No argument constructor
     */
    public MessageFormat() {}

    /**
     * Constructor to auto fill the fields
     *
     * @param messageID ID of the message
     * @param senderID ID of the sender
     * @param senderType Type of sender (Device or User)
     * @param type Message type (text message, command, etc)
     * @param body Body of the message, holds the message info
     */
    public MessageFormat(String messageID, String senderID, String senderType, String type, String body) {
        this.messageID = messageID;
        this.senderID = senderID;
        this.senderType = senderType;
        this.type = type;
        this.body =body;
    }

    /**
     * Build the message in the correct XML format
     *
     * @param input MessageFormat object holding all the information
     * @return String The generated XML
     */
    public static String buildMessage(MessageFormat input) {

        String xmlString = null;

        System.out.println("Build message");

        try {
            BuildXML buildXML = new BuildXML("message");

            Element root = buildXML.getRoot();

            buildXML.addNewTextElement(root, "messageID", input.messageID);
            buildXML.addNewTextElement(root, "senderID", input.senderID);
            buildXML.addNewTextElement(root, "senderType", input.senderType);
            buildXML.addNewTextElement(root, "type", input.type);
            buildXML.addNewTextElement(root, "body", input.body);


            xmlString = buildXML.buildXMLString();

        } catch (Exception e) {
            // do something
            System.out.println("build error: " + e.toString() + " -> " + e.getMessage());
        }

        if (xmlString != null)
            System.out.println(xmlString);

        return xmlString;
    }

    /**
     * Parse an XML message into a MessageFormat object
     *
     * @param input XML String to parse
     * @return MessageFormat MessageFormat object holding data from the XML message
     * @throws Exception A malformed XML string will cause an exception
     */
    public static MessageFormat parseMessage(String input) throws Exception{

        MessageFormat parsedMessage = new MessageFormat();


        try {
            ParseXML parseXML = new ParseXML(input, "message");

            parsedMessage.messageID = parseXML.getElementValue("messageID");
            parsedMessage.senderID = parseXML.getElementValue("senderID");
            parsedMessage.senderType = parseXML.getElementValue("senderType");
            parsedMessage.type = parseXML.getElementValue("type");
            parsedMessage.body = parseXML.getElementValue("body");


        } catch (Exception e) {
            //do something
            System.out.println("parse error: " + e.toString() + " -> " + e.getMessage());
            throw new NullPointerException();
        }


        return  parsedMessage;
    }

}
