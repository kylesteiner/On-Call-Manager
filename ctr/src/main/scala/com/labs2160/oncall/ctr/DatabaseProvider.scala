package com.labs2160.oncall.ctr

import java.io.File
import java.util.Properties

import com.github.tototoshi.csv._
import com.labs2160.slacker.api._
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory

// SEE: https://github.com/tototoshi/scala-csv
class DatabaseProvider extends Resource {

    private val logger = LoggerFactory.getLogger(classOf[DatabaseProvider])

    private var DBDir:String = null
    private var CSVFileList = List[BasicNameValuePair]()

    override def setConfiguration(configuration: Properties) = {
        DBDir = Utils.getRequiredConfigParam(configuration, "DBDir")
        CSVFileList ::= new BasicNameValuePair("transactions.csv", "sellerUID,buyerUID,startDate,endDate")
        CSVFileList ::= new BasicNameValuePair("users.csv", "uid,name,email")
        CSVFileList ::= new BasicNameValuePair("userTotals.csv", "uid,total")
    }

    override def start = {
        createDBDir()

        for (fileConf <- CSVFileList) {
            createCSVFile(fileConf)
        }
    }

    override def shutdown = {
        // do nothing
    }

    // Create directory for CSV files
    private def createDBDir() = {
        val file = new File(DBDir)
        if (!file.exists) {
            if (file.mkdir) {
                logger.info(s"Created directory ${DBDir}")
            } else {
                throw new SecurityException("Failed to create directory" + DBDir)
            }
        }
    }

    private def createCSVFile(CSVConfig: BasicNameValuePair) = {
        val fileName = CSVConfig.getName
        val header = CSVConfig.getValue

        // Create CSV file if it doesn't exist
        val file = new File(DBDir + fileName)
        if (!file.exists) {
            if (file.createNewFile()){
                // Add header line
                val writer = CSVWriter.open(file)
                writer.writeRow(header.split(","))
                writer.close()
                logger.info(s"Created file ${fileName}")
            } else {
                throw new SecurityException("Failed to create file" + fileName)
            }
        }
    }

    def getTransactions() : List[Map[String,String]] = {
        return getTableWithHeader("transactions")
    }

    def getUsers() : List[Map[String,String]] = {
        return getTableWithHeader("users")
    }

    def getUserFromID(uid: String) : Map[String,String] = {
        val users = getUsers()
        for (user <- users) {
            if (user("uid") == uid) {
                return user
            }
        }
        throw new IllegalArgumentException(uid + " did not match any users")
    }

    def getUserTotals() : List[Map[String,String]] = {
        return getTableWithHeader("userTotals")
    }

    def getUserTotal(uid: String) : Int = {
        val totals = getUserTotals()
        for (total <- totals) {
            if (total("uid") == uid) {
                return total("total").toInt
            }
        }
        return 0
    }

    def addTransaction(sellerUID: String, buyerUID: String, startDate: String, endDate: String) = {
        addRow("transactions", List(sellerUID, buyerUID, startDate, endDate))
    }

    def updateUser(uid: String, name: String, email: String) = {
        addRow("users", List(uid, name, email))
    }

    def updateUserTotal(uid: String, hours: Int) = {
        addRow("userTotals", List(uid, hours.toString))
    }

    private def getTableWithHeader(table: String) : List[Map[String,String]] = {
        val reader = CSVReader.open(new File(DBDir + table + ".csv"))
        val rows: List[Map[String,String]] = reader.allWithHeaders
        reader.close
        return rows
    }

    private def getTableNoHeader(table: String) : List[List[String]] = {
        val reader = CSVReader.open(new File(DBDir + table + ".csv"))
        val rows: List[List[String]] = reader.all
        reader.close
        return rows
    }

    private def addRow(table: String, newRow: List[String]) = {
        var rows = getTableNoHeader(table)

        // Update row if found, otherwise append it
        var found = false
        for (i <- 0 to rows.size - 1) {
            val row = rows(i)
            if (row.head == newRow.head) {
                rows = rows.updated(i,newRow)
                found = true
            }
        }
        if (!found) rows = rows :+ newRow

        // Overwrite entire file with updated row
        val writer = CSVWriter.open(new File(DBDir + table + ".csv"))
        writer.writeAll(rows)
        writer.close()
    }
}