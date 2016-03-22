package com.uhsl.flowmessage.flowmessagev2.utils;

/**
 * Created by Marcus on 16/03/2016.
 */
public class KVSValueFormat {
    public String type;
    public String body;

    /**
     * No argument constructor
     */
    public KVSValueFormat() {};

    /**
     * Constructor to auto fill the fields
     *
     * @param type Value type (heartbeat etc)
     * @param body Body of the value, holds the main data
     */
    public KVSValueFormat(String type, String body) {
        this.type = type;
        this.body =body;
    }

    /**
     * Parse an XML KVS value into an KVSValueFormat object
     *
     * @param input XML String to parse
     * @return KVSValueFormat KVSValueFormat object holding data from the XML value
     * @throws NullPointerException A malformed XML string will cause an exception
     */
    public static KVSValueFormat parseValue(String input) throws NullPointerException{

        KVSValueFormat parsedValue = new KVSValueFormat();


        try {
            ParseXML parseXML = new ParseXML(input, "value");

            parsedValue.type = parseXML.getElementValue("type");
            parsedValue.body = parseXML.getElementValue("body");

        } catch (Exception e) {
            //do something
            System.out.println("KVS value parse error: " + e.toString() + " -> " + e.getMessage());
            throw new NullPointerException();
        }


        return  parsedValue;
    }
}
