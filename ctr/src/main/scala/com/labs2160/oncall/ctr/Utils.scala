package com.labs2160.oncall.ctr

import java.util.{TimeZone, Properties}

import org.joda.time.DateTime

object Utils {
    def getRequiredConfigParam(configuration: Properties, key: String): String = {
        val value: String = configuration.getProperty(key)
        if (value == null || value.trim.length == 0) {
            throw new IllegalStateException("Configuration parameter \"" + key + "\" must be specified")
        }
        return value
    }

    def parseDate(arg: String, timeZone: String) = {
        val format = new java.text.SimpleDateFormat("yyyy-MM-dd:HH-z")
        val timeZoneID:String = TimeZone.getTimeZone(timeZone).getID
        val date = format.parse(arg + "-" + timeZoneID)
        val dateTime = new DateTime(date);
        (dateTime, date)
    }
}