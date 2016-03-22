package com.uhsl.flowmessage.flowmessagev2.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Marcus on 15/03/2016.
 */
public class EventFormat {

    public String type;
    public String body;

    /**
     * No argument constructor
     */
    public EventFormat() {};

    /**
     * Constructor to auto fill the fields
     *
     * @param type Message type (heartbeat etc)
     * @param body Body of the event, holds the event info
     */
    public EventFormat(String type, String body) {
        this.type = type;
        this.body =body;
    }

    /**
     * Parse an XML event into an EventFormat object
     *
     * @param input XML String to parse
     * @return EventFormat EventFormat object holding data from the XML event
     * @throws Exception A malformed XML string will cause an exception
     */
    public static EventFormat parseMessage(String input) throws Exception{

        EventFormat parsedEvent = new EventFormat();

        System.out.println("parse event\n" + input);

        try {
            ParseXML parseXML = new ParseXML(input, "event");

            parsedEvent.type = parseXML.getElementValue("type");
            parsedEvent.body = parseXML.getElementValue("body");



        } catch (Exception e) {
            //do something
            System.out.println("event parse error: " + e.toString() + " -> " + e.getMessage());
            throw new NullPointerException();
        }


        return  parsedEvent;
    }
}
