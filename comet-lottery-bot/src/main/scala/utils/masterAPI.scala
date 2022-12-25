package utils
import java.io.{File, FileWriter, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import com.google.gson.{Gson, GsonBuilder, JsonParser}
import configs.{LotteryConfig, conf, serviceOwnerConf}
import sun.security.util.FilePaths

case class MasterServiceOwnerConfig(
                               cometId: String,
                               cometTicketPrice: Int,
                               timeBeforeUnlockMS: Long,
                               distributionAddress: String,
                               singletonName: String,
                               singletonDesc: String,
                               ticketName: String,
                               ticketDesc: String,
                               oracleNFT: String,
                             )


class masterAPI {

  def getMasterLottery(lotteryUrl: String): LotteryConfig ={
    val url = new URL(lotteryUrl)
    val connection: HttpURLConnection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val reader = new InputStreamReader(connection.getInputStream)
    val gson = new GsonBuilder().setPrettyPrinting().create()
    gson.fromJson(reader, classOf[LotteryConfig])
  }

  def getService(serviceUrl: String): MasterServiceOwnerConfig ={
    val url = new URL(serviceUrl)
    val connection: HttpURLConnection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val reader = new InputStreamReader(connection.getInputStream)
    val gson = new GsonBuilder().setPrettyPrinting().create()
    gson.fromJson(reader, classOf[MasterServiceOwnerConfig])
  }

  def overWriteLotteryWithMasterConf(filePath: String, json: LotteryConfig): Unit ={
    conf.write(filePath, json)
  }

  def overWriteServiceWithMasterConf(filePath: String, json: MasterServiceOwnerConfig): Unit = {
    val ownerMnemonic = serviceOwnerConf.read(filePath).ownerMnemonic
    val ownerMnemonicPw = serviceOwnerConf.read(filePath).ownerMnemonicPw
    val nodeUrl = serviceOwnerConf.read(filePath).nodeUrl
    val apiUrl = serviceOwnerConf.read(filePath).apiUrl

    val serviceOwnerConfig = new serviceOwnerConf(
      ownerMnemonic,
      ownerMnemonicPw,
      json.cometId,
      json.cometTicketPrice,
      json.timeBeforeUnlockMS,
      json.distributionAddress,
      json.singletonName,
      json.singletonDesc,
      json.ticketName,
      json.ticketDesc,
      json.oracleNFT,
      nodeUrl,
      apiUrl
    )
    serviceOwnerConfig.write(filePath)
  }


}

object apiTest extends App{
  val url = new URL("https://api.cometgag.com/lottery")
  val connection: HttpURLConnection = url.openConnection().asInstanceOf[HttpURLConnection]
  connection.setRequestMethod("GET")
  val reader = new InputStreamReader(connection.getInputStream)
  private val gson = new GsonBuilder().setPrettyPrinting().create()
  val json: LotteryConfig = gson.fromJson(reader, classOf[LotteryConfig])
  println(json.Lottery.version)
  conf.write("data.json", json)
}

object classTest extends App{
  val obj = new masterAPI
  val json = obj.getService("https://api.cometgag.com/service")
  obj.overWriteServiceWithMasterConf("serviceOwner.json", json)
}
