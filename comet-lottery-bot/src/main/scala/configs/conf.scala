package configs

import com.google.gson.{Gson, GsonBuilder, JsonElement}

import java.io.{FileWriter, Writer}
import scala.io.Source

case class LotteryConfig(
                          Lottery: Config

                        )

case class Config(
                   version: Int,
                   initTransaction: String,
                   distributionAddress: String,
                   firstTime: String,
                   timeStamp: String,
                  ticketContract: TicketContract,
                  collectionContract: CollectionContract,
                  proxyContract: ProxyContract,
                  winnerSelectionContract: WinnerSelectionContract,
                   winningNFT: String,
                   winningAddress: String
                  )
case class TicketContract(
                         contract: String,
                         singleton: String,
                         initialBox: String
                         )
case class CollectionContract(
                             contract: String,
                             initialBox: String
                             )
case class ProxyContract(
                        contract: String
                        )
case class WinnerSelectionContract(
                                  contract: String
                                  )

case class ServiceOwnerConfig(
                             ownerMnemonic: String,
                             ownerMnemonicPw: String,
                             cometId: String,
                             cometTicketPrice: Int,
                             timeBeforeUnlockMS: Long,
                             distributionAddress: String,
                             singletonName: String,
                             singletonDesc: String,
                             ticketName: String,
                             ticketDesc: String,
                             oracleNFT: String,
                             nodeUrl: String,
                             apiUrl: String
                             )


class serviceOwnerConf(ownerMnemonic: String, ownerMnemonicPw: String, cometId: String, cometTicketPrice: Int, timeBeforeUnlockMS: Long, distributionAddress: String, singletonName: String, singletonDesc: String, ticketName: String, ticketDesc: String, oracleNFT: String,  nodeUrl: String, apiUrl: String){
  private val conf = ServiceOwnerConfig(ownerMnemonic, ownerMnemonicPw, cometId, cometTicketPrice, timeBeforeUnlockMS, distributionAddress, singletonName: String, singletonDesc: String, ticketName: String, ticketDesc: String, oracleNFT: String, nodeUrl, apiUrl)
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def write(filePath: String): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(this.conf))
    writer.close()
  }

  def read(filePath: String): LotteryConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[LotteryConfig])
  }
}

class conf(version: Int, ticketContract: String, singleton: String, initialTCBox: String, collectionContract: String, initialCCBox: String, proxyContract: String, winnerSelectionContract: String, distributionAddress: String, timeStamp: String){
  val ticketContractInstance: TicketContract = TicketContract(ticketContract, singleton, initialTCBox)
  val collectionContractInstance: CollectionContract = CollectionContract(collectionContract, initialCCBox)
  val proxyContractInstance: ProxyContract = ProxyContract(proxyContract)
  val winnerSelectionContractInstance: WinnerSelectionContract = WinnerSelectionContract(winnerSelectionContract)
  val conf = Config(version, "null", distributionAddress, "true", timeStamp, ticketContract = ticketContractInstance, collectionContract = collectionContractInstance, proxyContract = proxyContractInstance, winnerSelectionContract = winnerSelectionContractInstance, winningNFT = "null", winningAddress = "null")
  val newConfig: LotteryConfig = LotteryConfig(conf)
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def write(filePath: String): Unit ={
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(this.newConfig))
    writer.close()
  }

  def read(filePath: String): LotteryConfig ={
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[LotteryConfig])
  }

}

object conf{
  private val gson = new GsonBuilder().setPrettyPrinting().create()
  def read(filePath: String): LotteryConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[LotteryConfig])
  }

  def write(filePath: String, newConfig: LotteryConfig): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(newConfig))
    writer.close()
  }

  def writeTx(filePath: String, initTx: String): Unit ={
    val res = read(filePath)
    val conf = Config(res.Lottery.version, initTx, res.Lottery.distributionAddress, res.Lottery.firstTime, res.Lottery.timeStamp, ticketContract = res.Lottery.ticketContract, collectionContract = res.Lottery.collectionContract, proxyContract = res.Lottery.proxyContract, winnerSelectionContract = res.Lottery.winnerSelectionContract, winningNFT = res.Lottery.winningNFT, winningAddress = res.Lottery.winningAddress)
    val newConfig: LotteryConfig = LotteryConfig(conf)
    write(filePath, newConfig)
  }

  def writeFirstTime(filePath: String, firstTime: String): Unit = {
    val res = read(filePath)
    val conf = Config(res.Lottery.version, res.Lottery.initTransaction, res.Lottery.distributionAddress, firstTime, res.Lottery.timeStamp, ticketContract = res.Lottery.ticketContract, collectionContract = res.Lottery.collectionContract, proxyContract = res.Lottery.proxyContract, winnerSelectionContract = res.Lottery.winnerSelectionContract, winningNFT = res.Lottery.winningNFT, winningAddress = res.Lottery.winningAddress)
    val newConfig: LotteryConfig = LotteryConfig(conf)
    write(filePath, newConfig)
  }

  def writeV2(filePath: String, version: Int, ticketContractInitialBox: String, collectionContractInitialBox: String, timeStamp: Long): Unit = {
    val res = read(filePath)
    val ticketContract = TicketContract(res.Lottery.ticketContract.contract, res.Lottery.ticketContract.singleton, ticketContractInitialBox)
    val collectionContract = CollectionContract(res.Lottery.collectionContract.contract, collectionContractInitialBox)
    val conf = Config(version, "null", res.Lottery.distributionAddress, "true", timeStamp.toString, ticketContract = ticketContract, collectionContract = collectionContract, proxyContract = res.Lottery.proxyContract, winnerSelectionContract = res.Lottery.winnerSelectionContract, winningNFT = res.Lottery.winningNFT, winningAddress = res.Lottery.winningAddress)
    val newConfig: LotteryConfig = LotteryConfig(conf)
    write(filePath, newConfig)
  }

  def writeTSnull(filePath: String): Unit = {
    val res = read(filePath)
    val conf = Config(res.Lottery.version, res.Lottery.initTransaction, res.Lottery.distributionAddress, res.Lottery.firstTime, "null", ticketContract = res.Lottery.ticketContract, collectionContract = res.Lottery.collectionContract, proxyContract = res.Lottery.proxyContract, winnerSelectionContract = res.Lottery.winnerSelectionContract, winningNFT = "null", winningAddress = "null")
    val newConfig: LotteryConfig = LotteryConfig(conf)
    write(filePath, newConfig)
  }

  def writeWinner(filePath: String, winningNFT: String, winningAddress: String): Unit = {
    val res = read(filePath)
    val conf = Config(res.Lottery.version, res.Lottery.initTransaction, res.Lottery.distributionAddress, res.Lottery.firstTime, res.Lottery.timeStamp, ticketContract = res.Lottery.ticketContract, collectionContract = res.Lottery.collectionContract, proxyContract = res.Lottery.proxyContract, winnerSelectionContract = res.Lottery.winnerSelectionContract, winningNFT = winningNFT, winningAddress = winningAddress)
    val newConfig: LotteryConfig = LotteryConfig(conf)
    write(filePath, newConfig)
  }

}

object serviceOwnerConf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): ServiceOwnerConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ServiceOwnerConfig])
  }
}
