package com.labs2160.oncall.ctr

import java.util.Properties

import com.labs2160.slacker.api._
import com.labs2160.slacker.api.annotation.ActionDescription
import org.joda.time.Period
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import org.slf4j.LoggerFactory

@ActionDescription(
    name = "On-Call Add Override",
    description = "Assign the time interval to the given person on PagerDuty",
    argsSpec = "<name> <start YYYY-MM-DD@HH:mm> <end YYYY-MM-DD@HH:mm>",
    argsExample = "pd-add-override steiner 2015-09-28@10:00 2015-09-29@10:00"
)
class AddOverrideAction extends Action {

    private val logger = LoggerFactory.getLogger(classOf[AddOverrideAction])

    private var database:DatabaseProvider = _
    private var api:PagerDutyProvider = _

    // For testing purposes
    def this(database: DatabaseProvider, api: PagerDutyProvider) {
        this()
        this.database = database
        this.api = api
    }

    override def setConfiguration(config: Properties) : Unit = {
        val resources = config.get("resources").asInstanceOf[java.util.Map[String, Resource]]
        this.database = resources.get("OnCallDB").asInstanceOf[DatabaseProvider]
        this.api = resources.get("PagerDuty").asInstanceOf[PagerDutyProvider]
    }

    override def execute(ctx: SlackerContext) : Boolean = {
        val args:Array[String] = ctx.getRequestArgs
        if (args == null || args.length != 3) {
            throw new NoArgumentsFoundException("4 arguments required")
        }

        val response = addOverride(args(0), args(1), args(2))
        ctx.setResponseMessage(response)
        return true
    }

    def addOverride(query: String, startDate: String, endDate: String): String = {
        // Get user data
        val buyer = api.getUserFromName(query)
        val (start_iso8601, start_pretty) = Utils.parseDate(startDate, "PST")
        val (end_iso8601, end_pretty) = Utils.parseDate(endDate, "PST")

        // Check if override exists
        if (api.getOverrides(start_iso8601.toString, end_iso8601.toString).size != 0) {
            throw new Exception("Override already exists between this time range")
        }

        // Update schedule
        val users = api.getSchedule(start_iso8601.toString, end_iso8601.toString)
        for (user <- users) {
            val parser: DateTimeFormatter = ISODateTimeFormat.dateTimeParser()
            val start = parser.parseDateTime(user("start"))
            val end = parser.parseDateTime(user("end"))
            val p = new Period(start, end)
            val total:Double = database.getUserTotal(user("uid")) - (p.getHours + p.getMinutes / 60.0)
            database.updateUserTotal(user("uid"), total)
        }

        // Override schedule
        api.postOverride(start_iso8601.toString, end_iso8601.toString, buyer("id"))
        val p = new Period(start_iso8601, end_iso8601)

        val hours:Double = p.getHours + p.getMinutes / 60.0
        database.updateUserTotal(buyer("id"), database.getUserTotal(buyer("id")) + hours)

        return s"${buyer("name")} is now on-call from ${start_pretty} to ${end_pretty}"
    }
}