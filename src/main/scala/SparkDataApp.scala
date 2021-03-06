import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SparkDataApp {
  def main(args: Array[String]) {

    var Exitcode = 0
    val StartedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").format(LocalDateTime.now)
    val log = ("Started", StartedTime)
    var LogList = List(log.toString().replace("(", "").replace(")", ""))

    val spark = SparkSession
      .builder()
      .appName("Spark Hive Example")
      // .master("local[*]")
      .config("spark.sql.warehouse.dir", "/tmp")
      .config("spark.hadoop.validateOutputSpecs", "false")
      .config("spark.debug.maxToStringFields", 100)
      .config("spark.scheduler.mode", "FAIR")
      .enableHiveSupport()
      .getOrCreate()
    //spark.sparkContext.setLogLevel("ERROR")

    //try {
      val processingTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").format(LocalDateTime.now)
      val log1 = ("InProcessing", processingTime)
      LogList = log1.toString().replace("(", "").replace(")", "") :: LogList

      val inputPath = args(0)
      val job_input = spark.sparkContext.textFile(inputPath)
      val job_properties = job_input.collect().toList.flatMap(x => x.split("=")).grouped(2).collect { case List(k, v) => k -> v }.toMap

      val SRC_FILE_PATH = job_properties("SRC_FILE_PATH")
      val SRC_FILE_STATS_NM = job_properties("SRC_FILE_STATS_NM")
      val SRC_FILE_NM = job_properties("SRC_FILE_NM")
      val SRC_FILE_SCHEMA_NM = job_properties("SRC_FILE_SCHEMA_NM")
      val tbl_id = job_properties("tbl_id")
      val TBL_NAME = job_properties("TBL_NAME")
      val Hive_Path=job_properties("HIVE_PATH")

      //val datapath=("file://"+properties.get("SRC_FILE_PATH")+"/"+properties.get("SRC_FILE_NM"))
      val datapath = (SRC_FILE_PATH + "/" + SRC_FILE_NM)
      println(datapath)
      // val schemapath=("file://"+properties.get("SRC_FILE_PATH")+"/"+properties.get("SRC_FILE_SCHEMA_NM"))
      val schemapath = (SRC_FILE_PATH + "/" + SRC_FILE_SCHEMA_NM)
      println(schemapath)

      println("Proceeding with data file")

      val dataDF1 = spark.read
        //.schema(input6)
        .option("delimiter", "\u0007")
        .option("ignoreLeadingWhiteSpace", "True")
        .option("ignoreTrailingWhiteSpace", "True")
        .option("multiline", "True")
        .option("escape", "\u000D")
        .csv(datapath)
      //   .option("path",datapath)

      val dftbl = TBL_NAME + "_DF"


      println("Data file Found")
      println("Proceeding with schema file")
      val input = spark.sparkContext.textFile(schemapath).coalesce(1).mapPartitions(_.drop(3)).filter(line => !(line.contains(")")))

      // println("Schema file found")

      val input2 = input.map { x =>
        val w = x.split(":")
        val columnName = w(0).trim()
        val raw = w(1).trim()
        (columnName, raw)
      }

      val input3 = input2.map { x =>
        val x2 = x._2.replaceAll(";", "")
        (x._1, x2)
      }

      val input4 = input3.map { x =>
        val pattern1 = ".*int\\d{0,}".r
        val pattern2 = ".*string\\[.*\\]".r
        val pattern3 = ".*timestamp\\[.*\\]".r
        val pattern4 = ".*date\\d{0,}".r
        val raw1 = pattern1 replaceAllIn(x._2, "int")
        val raw2 = pattern2 replaceAllIn(raw1, "string")
        val raw3 = pattern3 replaceAllIn(raw2, "timestamp")
        val raw4 = pattern4 replaceAllIn(raw3, "date")
        val raw5 = x._1 + " " + raw4
        raw5
      }

      val tblname = TBL_NAME
      spark.sql("drop table if exists " + tblname)
      val input5 = "create external table if not exists " + tblname + "(" + input4.collect().toList.mkString(",") + ") stored as parquetfile location" + " '" + Hive_Path + "'"
      //val input5 = "create external table if not exists " + tblname + "(" + input4.collect().toList.mkString(",") + ") stored as parquetfile Location "+"+ Hive_Path +"
      print(input5)
      //Table created in hive default database

      spark.sql(input5)
      dataDF1.write.insertInto(tblname.toString)
      //spark.sql("insert into table "+ tblname + " select * from " + dftbl )

      val completedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").format(LocalDateTime.now)
      val log2 = ("Completed", completedTime)
      LogList = log2.toString().replace("(", "").replace(")", "") :: LogList

    /*} catch {
      case e: Throwable =>
        println("File Not Found")
        val failedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss").format(LocalDateTime.now)
        Exitcode = 1
        val log3 = ("failed", failedTime)
        LogList = log3.toString().replace("(", "").replace(")", "") :: LogList
    } finally {*/
      //spark.sparkContext.parallelize(LogList).saveAsTextFile("/Users/cklekkala/IdeaProjects/untitled1/injecti.log")
      //   spark.sparkContext.parallelize(LogList).coalesce(1,false).saveAsTextFile(args(0)+"/SimpleApp.log")

 //   }
    spark.stop()
  }
}

