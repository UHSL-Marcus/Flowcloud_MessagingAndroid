package com.uhsl.flowmessage.flowmessagev2.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Marcus on 16/03/2016.
 */
public class HeartbeatFormat {
    public Date timestamp;
    public long uptime;

    /**
     * No argument constructor
     */
    public HeartbeatFormat() {};

    /**
     * Constructor to auto fill the fields
     *
     * @param timestamp heartbeat timestamp
     * @param uptime uptime of the device
     */
    public HeartbeatFormat(Date timestamp, long uptime) {
        this.timestamp = timestamp;
        this.uptime = uptime;
    }

    /**
     * Parse XML heartbeat data into a HeartbeatFormat object
     *
     * @param XML XML String to parse
     * @return HeartbeatFormat EventFormat object holding data from the XML event
     */
    public static HeartbeatFormat parseHeartbeat(String XML) {
        HeartbeatFormat parsedHeartbeat = new HeartbeatFormat();


        try {
            ParseXML parseXML = new ParseXML(XML, "heartbeat");

            DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
            parsedHeartbeat.timestamp = format.parse(parseXML.getElementValue("timestamp"));
            parsedHeartbeat.uptime = Long.valueOf(parseXML.getElementValue("uptime"));

        } catch (Exception e) {
            //do something
            System.out.println("heartbeat error: " + e.toString() + " -> " + e.getMessage());
            throw new NullPointerException();
        }


        return  parsedHeartbeat;

    }
}
