package utils
import com.google.gson.GsonBuilder
import configs.{LotteryConfig, conf, serviceOwnerConf}

import java.io.InputStreamReader
import java.net.{HttpURLConnection, URL}

case class MasterServiceOwnerConfig(
    cometId: String,
    cometTicketPrice: Int,
    winnerChance: Int,
    timeBeforeUnlockMS: Long,
    distributionAddress: String,
    singletonName: String,
    singletonDesc: String,
    ticketName: String,
    ticketDesc: String,
    oracleNFT: String
)

class masterAPI {

  def getMasterLottery(lotteryUrl: String): LotteryConfig = {
    val url = new URL(lotteryUrl)
    val connection: HttpURLConnection =
      url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val reader = new InputStreamReader(connection.getInputStream)
    val gson = new GsonBuilder().setPrettyPrinting().create()
    gson.fromJson(reader, classOf[LotteryConfig])
  }

  def getService(serviceUrl: String): MasterServiceOwnerConfig = {
    val url = new URL(serviceUrl)
    val connection: HttpURLConnection =
      url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    val reader = new InputStreamReader(connection.getInputStream)
    val gson = new GsonBuilder().setPrettyPrinting().create()
    gson.fromJson(reader, classOf[MasterServiceOwnerConfig])
  }

  def overWriteLotteryWithMasterConf(
      filePath: String,
      json: LotteryConfig
  ): Unit = {
    conf.write(filePath, json)
  }

  def overWriteServiceWithMasterConf(
      filePath: String,
      json: MasterServiceOwnerConfig
  ): Unit = {
    val ownerMnemonic = serviceOwnerConf.read(filePath).ownerMnemonic
    val ownerMnemonicPw = serviceOwnerConf.read(filePath).ownerMnemonicPw
    val nodeUrl = serviceOwnerConf.read(filePath).nodeUrl
    val apiUrl = serviceOwnerConf.read(filePath).apiUrl

    val serviceOwnerConfig = new serviceOwnerConf(
      ownerMnemonic,
      ownerMnemonicPw,
      json.cometId,
      json.cometTicketPrice,
      json.winnerChance,
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
