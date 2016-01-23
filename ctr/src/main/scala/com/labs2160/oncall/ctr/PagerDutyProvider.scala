package com.labs2160.oncall.ctr

import java.util.Properties
import javax.ws.rs.client.{ClientBuilder, Entity, WebTarget}
import javax.ws.rs.core.Response.Status._

import com.labs2160.slacker.api._
import org.apache.http.message.BasicNameValuePair
import org.json.JSONObject
import org.slf4j.LoggerFactory
import play.api.libs.json._

class PagerDutyProvider extends Resource {

    private val logger = LoggerFactory.getLogger(classOf[PagerDutyProvider])

    lazy val client = ClientBuilder.newClient()

    var apiUri:String = _
    var apiKey:String = _
    var scheduleID:String = _

    override def setConfiguration(configuration: Properties) = {
        apiUri = Utils.getRequiredConfigParam(configuration, "apiUri")
        apiKey = Utils.getRequiredConfigParam(configuration, "apiKey")
        scheduleID = Utils.getRequiredConfigParam(configuration, "scheduleID")
    }

    override def start = { }

    override def shutdown = { }

    /**
      * See https://developer.pagerduty.com/documentation/rest/schedules/overrides/create
      *
      * @param start The start date and time for the override.
      * @param end   The end date and time for the override.
      * @param uid   The ID of the user who will be on call for the duration of the override.
      */
    def postOverride(start: String, end: String, uid: String) = {
        var parameterList = List[BasicNameValuePair]()
        parameterList ::= new BasicNameValuePair("start", start)
        parameterList ::= new BasicNameValuePair("end", end)
        parameterList ::= new BasicNameValuePair("user_id", uid)
        postRequest("/schedules/" + scheduleID + "/overrides", parameterList, "override")
    }

    def getOverrides(start: String, end: String): List[JsObject] = {
        var parameterList = List[BasicNameValuePair]()
        parameterList ::= new BasicNameValuePair("since", start)
        parameterList ::= new BasicNameValuePair("until", end)
        val result = getRequest("/schedules/" + scheduleID + "/overrides", parameterList)
        val overrideJS : List[JsObject] = (result \ "overrides").as[List[JsObject]]
        return overrideJS
    }

    /**
      * See https://developer.pagerduty.com/documentation/rest/users/list
      *
      * @return list of all users where each item is a map of user data
      */
    def getUsers() : List[Map[String, String]] = {
        var parameterList = List[BasicNameValuePair]()
        parameterList ::= new BasicNameValuePair("limit", "9999999")

        // Send get request for user
        val result = getRequest("/users", parameterList)
        val usersJS : List[JsObject] = (result \ "users").as[List[JsObject]]

        // Convert map from String -> JVValue to Map String -> String
        var users = List[Map[String, String]]()
        for (user <- usersJS) {
            val map = user.value.map { case(k, v) => k -> v.toString.replaceAll("^\"|\"$", "") }
            users ::= map.toMap
        }
        return users
    }

    /**
      * See https://developer.pagerduty.com/documentation/rest/users/list
      *
      * @param query matches name or email to user
      * @return map of user data
      */
    def getUserFromName(query: String) : Map[String, String] = {
        var parameterList = List[BasicNameValuePair]()
        parameterList ::= new BasicNameValuePair("query", query)

        // Send get request for user
        val result = getRequest("/users", parameterList)
        val users : List[JsObject] = (result \ "users").as[List[JsObject]]
        if (users.length == 0) {
            throw new IllegalArgumentException(query + " did not match any users")
        } else if (users.length > 1) {
            throw new IllegalArgumentException(query + " matched multiple users")
        }

        // Convert map from String -> JVValue to Map String -> String
        val user = users.head.value map { case(k, v) => k -> v.toString.replaceAll("^\"|\"$", "") }
        return user.toMap
    }

    /**
      * https://developer.pagerduty.com/documentation/rest/schedules/users
      *
      * @param startDate
      * @param endDate
      * @return
      */
    def getSchedule(startDate: String, endDate: String): List[Map[String, String]] = {
        var parameterList = List[BasicNameValuePair]()
        parameterList ::= new BasicNameValuePair("since", startDate)
        parameterList ::= new BasicNameValuePair("until", endDate)

        // Send get request for schedule
        val result = getRequest("/schedules/" + scheduleID + "/entries", parameterList)
        val entriesJS : List[JsObject] = (result \ "entries").as[List[JsObject]]

        // Convert map from String -> JVValue to Map String -> String
        var users = List[Map[String, String]]()
        for (entry <- entriesJS) {
            val userMap = scala.collection.mutable.Map[String,String]()
            userMap.put("uid", (entry \ "user" \ "id").as[String])
            userMap.put("start", (entry \ "start").as[String])
            userMap.put("end", (entry \ "end").as[String])
            users ::= userMap.toMap
        }
        return users
    }

    private def postRequest(path: String, parameterList: List[BasicNameValuePair], parentParam: String) = {
        val pdTarget: WebTarget = client.target(apiUri + path)

        // Set up request body
        val parentData = new JSONObject
        val childData = new JSONObject
        for (p <- parameterList) {
            childData.put(p.getName, p.getValue)
        }
        parentData.put(parentParam, childData)

        // Issue POST request and check response
        logger.info(s"POST request with URI: ${pdTarget.getUri} and payload: ${parentData.toString}")
        val response = pdTarget.request()
            .header("Content-type", "application/json")
            .header("Authorization", "Token token=" + apiKey)
            .post(Entity.entity(parentData.toString, "application/json"))
        val result = (response != null && response.getStatus() == CREATED.getStatusCode())
        if (!result) throw new InvalidRequestException(s"Status: ${response.getStatus}")
        val s = response.readEntity(classOf[String])
    }

    private def getRequest(path: String, parameterList: List[BasicNameValuePair]): JsValue = {
        var pdTarget: WebTarget = client.target(apiUri + path)

        // Add parameters
        for (p <- parameterList) {
            pdTarget = pdTarget.queryParam(p.getName, p.getValue)
        }

        // Issue request and return response
        logger.info(s"GET request with URI: ${pdTarget.getUri}")
        val response = pdTarget.request()
            .header("Content-type", "application/json")
            .header("Authorization", "Token token=" + apiKey)
            .get()
        val result = (response != null && response.getStatus() == OK.getStatusCode())
        if (!result) throw new InvalidRequestException(s"Status: ${response.getStatus}")
        val s = response.readEntity(classOf[String])
        return play.api.libs.json.Json.parse(s)
    }
}