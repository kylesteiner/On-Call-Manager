package com.labs2160.oncall.ctr.actions

import java.util.Properties

import com.labs2160.oncall.ctr.resources.{PagerDutyProvider, DatabaseProvider}
import com.labs2160.slacker.api.{NoArgumentsFoundException, SlackerContext, Resource, Action}
import com.labs2160.slacker.api.annotation.ActionDescription
import org.slf4j.LoggerFactory

@ActionDescription(
    name = "On-Call Set User Total",
    description = "Manually set the on-call balance for a given user",
    argsSpec = "<name> <hours> <minutes>",
    argsExample = "steiner 24 10"
)
class SetUserTotalAction extends Action {

    private val logger = LoggerFactory.getLogger(classOf[AddOverrideAction])
    private var database:DatabaseProvider = _
    private var api:PagerDutyProvider = _

    // For testing purposes
    def this(database: DatabaseProvider, api: PagerDutyProvider) {
        this()
        this.database = database
        this.api = api
    }

    override def setComponents(resources: java.util.Map[String, Resource], config: Properties): Unit = {
        this.database = resources.get("OnCallDB").asInstanceOf[DatabaseProvider]
        this.api = resources.get("PagerDuty").asInstanceOf[PagerDutyProvider]
    }

    override def execute(ctx: SlackerContext): Boolean = {
        val args:Array[String] = ctx.getRequestArgs
        if (args == null || args.length != 3) {
            throw new NoArgumentsFoundException("3 arguments required")
        }

        val user = api.getUserFromName(args(0))
        val total: Double = args(1).toDouble + args(2).toDouble / 60
        database.updateUserTotal(user("id"), total)

        val response = s"User ${user("name")} now has a on-call balance of ${total.toString}"
        ctx.setResponseMessage(response)
        return true
    }

}
