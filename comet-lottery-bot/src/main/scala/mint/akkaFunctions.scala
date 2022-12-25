package mint
import akka.actor.ActorRef
import configs.{conf, serviceOwnerConf}
import initilization.init
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.{Address, ErgoToken, ErgoValue, InputBox}
import org.ergoplatform.explorer.client.model.OutputInfo
import special.collection.Coll
import utils.{explorerApi, fileOperations, masterAPI}

import java.util
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class akkaFunctions(follow: Boolean = false, serviceUrl: String = null, lotteryUrl: String = null) {
  class WinnerSelectionError(message: String) extends Exception(message)
  class DataChanged(message: String) extends Exception(message)
  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val lotteryFilePath = "lotteryConf"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private val lotteryConf = conf.read(lotteryFilePath + ".json")
  private val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  private val lotteryHistoryDir = "history/lottery/"
  private val serviceHistoryDir = "history/service/"


  def main(): Unit ={
    val config = lotteryConf.Lottery
//    println(config.timeStamp)
//    Thread.sleep(10000)

    if(config.timeStamp != "null" && !lotteryConf.Lottery.firstTime.toBoolean && !follow){
      val tokenID = lotteryConf.Lottery.ticketContract.singleton
      val singletonBoxAddress = exp.getUnspentBoxFromTokenID(tokenID).getAddress
      println("singleTon Box Address: " + singletonBoxAddress)
      println("In Mempool: " + exp.getMem(exp.getUnspentBoxFromTokenID(tokenID).getBoxId))

      if(((singletonBoxAddress != lotteryConf.Lottery.ticketContract.contract) && (singletonBoxAddress != lotteryConf.Lottery.winnerSelectionContract.contract)) || singletonBoxAddress == null){
        conf.writeTSnull(lotteryFilePath + ".json")
      }

    }

    if(config.timeStamp == "null"){
      println("trying to initialize")
      initialize()
      println("initialized")
    }
    else if (System.currentTimeMillis() >= config.timeStamp.toLong) {
      println("trying to process winner")
      processWinner()
    } else {
      println("proceeding with mint")
      mint()
    }
  }

  private def checkVersionAndTS(ticketBoxID: String): Unit ={
    val lotteryVersion = exp.getErgoBoxfromID(ticketBoxID).additionalRegisters(ErgoBox.R4).value.toString.toLong
    val lotteryTS = exp.getErgoBoxfromID(ticketBoxID).additionalRegisters(ErgoBox.R7).value.toString.toLong

    if (lotteryVersion > lotteryConf.Lottery.version){
      println("wrote to version")
      conf.writeVersion(lotteryFilePath + ".json", lotteryVersion.toInt)
    }

    if (lotteryTS > lotteryConf.Lottery.timeStamp.toLong){
      println("wrote to timestamp")
      conf.writeTS(lotteryFilePath + ".json", lotteryTS)
    }
  }

  def fixInitTxData(): Unit ={
    val lotteryVersion = lotteryConf.Lottery.version

    if(lotteryConf.Lottery.initTransaction == "null"){
      val initTx = getInitTx(lotteryVersion)
      if (initTx == null){
        println("issue with getting new initTx")
        conf.writeInitTx(lotteryFilePath + ".json", "null")
        throw new DataChanged("issue with getting new initTx")
      }
      conf.writeInitTx(lotteryFilePath + ".json", initTx)
      throw new DataChanged("")
    }

    val initOutput = exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs

    val init = {
      if(initOutput.get(0).getAddress != lotteryConf.Lottery.ticketContract.contract) exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs.get(1).getBoxId
      else exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs.get(0).getBoxId
    }
    var oldVersion = -1L
    try {
      oldVersion = exp.getErgoBoxfromID(init).additionalRegisters(ErgoBox.R4).value.asInstanceOf[Long]
    }

    if(oldVersion != lotteryVersion){
      val initTx = getInitTx(lotteryVersion)
      if (initTx == null){
        println("issue with getting new initTx")
        conf.writeInitTx(lotteryFilePath + ".json", "null")
        throw new DataChanged("issue with getting new initTx")
      }
      conf.writeInitTx(lotteryFilePath + ".json", initTx)
      throw new DataChanged("")
    }
  }

  private def getInitTx(version: Long): String ={
    val boxes = exp.getAddressInfo(lotteryConf.Lottery.winnerSelectionContract.contract)
    val spentList = new ListBuffer[String]()
    val initTxList = new ListBuffer[OutputInfo]()

    for (i <- 0 until boxes.size()) {
      val item = boxes.get(i)
      if(item.getSpentTransactionId != null){
        spentList.append(item.getSpentTransactionId)
      }
    }

    for(tx <- spentList) {
      val boxes = exp.getBoxesfromTransaction(tx)
      val outputAddress = boxes.getOutputs.get(0).getAddress
      if (outputAddress == lotteryConf.Lottery.ticketContract.contract){
//        val y = exp.getErgoBoxfromID(boxes.getOutputs.get(0).getBoxId).additionalRegisters(ErgoBox.R4).value.asInstanceOf[Long]
        initTxList.append(boxes.getOutputs.get(0))
      }
    }

    for(value <- initTxList){
      try {
        if (exp.getErgoBoxfromID(value.getBoxId).additionalRegisters(ErgoBox.R4).value.asInstanceOf[Long] == version) {
          return value.getSpentTransactionId
        }
      } catch {
        case e: Exception => println(e)
      }
    }
    null
  }

  def initialize(): Unit ={
    if(this.follow){
      val obj = new masterAPI
      val lotteryJson = obj.getMasterLottery(lotteryUrl)
      if(lotteryJson.Lottery.firstTime.toBoolean){
        println("Master has not processed any lottery tickets, awaiting completion")
        Thread.sleep(60000)
        return
      }
      if(lotteryConf.Lottery.ticketContract.singleton == lotteryJson.Lottery.ticketContract.singleton){
        println("Master has not update config, awaiting completion")
        Thread.sleep(60000)
        return
      }
      val json = obj.getService(serviceUrl)
      obj.overWriteServiceWithMasterConf("serviceOwner.json", json)
      obj.overWriteLotteryWithMasterConf("lotteryConf.json", lotteryJson)
      return
    }
    val cometId = serviceConf.cometId
    val comet = new ErgoToken(cometId, 1)
    val priceInComet = serviceConf.cometTicketPrice
    val timeStamp = System.currentTimeMillis() + serviceConf.timeBeforeUnlockMS
    val timeToWaitMS = serviceConf.timeBeforeUnlockMS
    val ownerMnemonic = serviceConf.ownerMnemonic
    val ownerMnemonicPw = serviceConf.ownerMnemonicPw
    val distributionAddress: Address = Address.create(serviceConf.distributionAddress)
    val oracleNFT = new ErgoToken(serviceConf.oracleNFT, 1)
    val initObj = new init(ctx, comet = comet, priceInComet = priceInComet, ownerMnemonic = ownerMnemonic, ownerMnemonicPw = ownerMnemonicPw, distributionAddress = distributionAddress, filePath = lotteryFilePath + ".json", singletonName = serviceConf.singletonName, singletonDesc = serviceConf.singletonDesc, oracleNFT = oracleNFT, timeStamp = timeStamp, timeToWaitMS = timeToWaitMS)
    initObj.initialize()
  }


  def mint(): String = {
    val proxyAddress = Address.create(lotteryConf.Lottery.proxyContract.contract)
    val validProxyBoxes = new util.ArrayList[String]()
    val boxes = client.getAllUnspentBox(proxyAddress)
    var counter = 0

    for (box <- boxes) {
      counter += 1
      if (!exp.isBoxInMemPool(box)) {
        val boxR4Hex = box.getRegisters.get(0).toHex
        val boxSentTxId: String = box.asInstanceOf[InputBoxImpl].getErgoBox.transactionId
        val boxSenderAddress = Address.create(exp.getBoxesfromTransaction(boxSentTxId).getInputs.get(0).getAddress)
        val senderAddressSigmaProp = ErgoValue.of(boxSenderAddress.getPublicKey).toHex
        if ((boxR4Hex == senderAddressSigmaProp) && (box.getValue >= (0.003 * math.pow(10, 9)).toLong)) {
          if(box.getTokens.get(0).getValue >= serviceConf.cometTicketPrice){
            validProxyBoxes.add(box.getId.toString)
          }
        }
      }
    }

    if (validProxyBoxes.isEmpty) {
      return null
    }
    val tokenID = lotteryConf.Lottery.ticketContract.singleton
    val txId = exp.getUnspentBoxFromTokenID(tokenID).getTransactionId
    val removedQuotes = txId.replace("\"", "")
    println("previous txId (queried for its outputs): " + removedQuotes)
    mintTicket(validProxyBoxes, txId)
  }

  private def mintTicket(proxyInput: util.ArrayList[String], txId: String): String = {
    val cometId = serviceConf.cometId
    val config = lotteryConf.Lottery
    val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
    val timeStamp = config.timeStamp.toLong

    val mintUtil = new mintUtility(ctx = ctx, ownerMnemonic = serviceConf.ownerMnemonic, mnemonicPassword = serviceConf.ownerMnemonicPw, cometId = cometId, cometPrice = serviceConf.cometTicketPrice, ticketContract = config.ticketContract.contract,
      singletonToken = config.ticketContract.singleton, collectionContract = config.collectionContract.contract, winnerSelectionContract = config.winnerSelectionContract.contract,
      version = config.version.toLong, distributionAddress = Address.create(serviceConf.distributionAddress))


//    val res = exp.getBoxesfromTransaction(txId)
//    var poolBoxID = config.collectionContract.initialBox
//    var ticketContractBoxId = config.ticketContract.initialBox
//    val firstTime: Boolean = config.firstTime.toBoolean
//
//    if (!firstTime) {
//      poolBoxID = res.getOutputs.get(2).getBoxId
//      ticketContractBoxId = res.getOutputs.get(1).getBoxId
//    } else if(config.version > 1){
//      poolBoxID = res.getOutputs.get(1).getBoxId
//      ticketContractBoxId = res.getOutputs.get(0).getBoxId
//    }

    val res = exp.getBoxesfromTransaction(txId)
    val firstTime: Boolean = config.firstTime.toBoolean

//    val (poolBoxID, ticketContractBoxId) = {
//      if (!firstTime) (res.getOutputs.get(2).getBoxId, res.getOutputs.get(1).getBoxId)
//      else if(config.version > 1) (res.getOutputs.get(1).getBoxId, res.getOutputs.get(0).getBoxId)
//      else (config.collectionContract.initialBox, config.ticketContract.initialBox) //brand new lottery scenario
//    }
      val (poolBoxID, ticketContractBoxId) = {
        if (config.version > 1 && res.getInputs.get(0).getAddress == config.winnerSelectionContract.contract) (res.getOutputs.get(1).getBoxId, res.getOutputs.get(0).getBoxId)
        else if(!firstTime) (res.getOutputs.get(2).getBoxId, res.getOutputs.get(1).getBoxId)
        else (config.collectionContract.initialBox, config.ticketContract.initialBox) //brand new lottery scenario
      }

    checkVersionAndTS(ticketContractBoxId)
    val indexInitial = exp.getErgoBoxfromID(ticketContractBoxId).additionalRegisters(ErgoBox.R5).value.toString.toLong + 1L

    val firstTxSigned = mintUtil.buildLotteryMint(serviceConf.ticketName + " " + (indexInitial - 1).toString + " v" + lotteryConf.Lottery.version, serviceConf.ticketDesc, proxyInput.get(0), indexInitial, timeStamp, poolBoxID, ticketContractBoxId)
    val firstTx = ctx.sendTransaction(firstTxSigned)
    println("ticket mint txn: " + firstTx.replace("\"", ""))
    proxyInput.remove(0)

    if (firstTime) {
      conf.writeTx(lotteryFilePath + ".json", firstTx.replace("\"", ""))
      conf.writeFirstTime(lotteryFilePath + ".json", false.toString)
    }

    val set = proxyInput.toSet

    var poolBox: InputBox = firstTxSigned.getOutputsToSpend.get(2)
    var ticketContractBox: InputBox = firstTxSigned.getOutputsToSpend.get(1)
    var index = indexInitial + 1L

    var loopTx: String = null
    Thread.sleep(1000)
    for (in <- set) {
      val loopTxSigned = mintUtil.buildLotteryMintChained(serviceConf.ticketName + " " + (index - 1).toString + " v" + lotteryConf.Lottery.version, serviceConf.ticketDesc, in, index, timeStamp, poolBox, ticketContractBox)
      loopTx = ctx.sendTransaction(loopTxSigned)
      index = index + 1
      poolBox = loopTxSigned.getOutputsToSpend.get(2)
      ticketContractBox = loopTxSigned.getOutputsToSpend.get(1)
      println("ticket mint txn: " + loopTx.replace("\"", ""))
      Thread.sleep(1500)
    }
    loopTx.replace("\"", "")
  }

  def handleProcessWinnerIndexError(): Unit ={
    println("index must be higher, not enough tickets have been bought")
    println("falling back to mint")
    mint()
  }

  def handleWinnerTxnError(): Unit ={
    println("Error with winner transaction, trying to handle")
    fixInitTxData()

    val tokenID = lotteryConf.Lottery.ticketContract.singleton
    val txId = exp.getUnspentBoxFromTokenID(tokenID).getTransactionId
    val res = exp.getBoxesfromTransaction(txId)
    val winnerContractBox = res.getOutputs.get(0)
    val winnerContractBoxInput = exp.getUnspentBoxFromMempool(winnerContractBox.getBoxId)
    val index = exp.getErgoBoxfromID(winnerContractBox.getBoxId).additionalRegisters(ErgoBox.R9).value.asInstanceOf[Coll[Long]](1)
    println("version from self: " + exp.getErgoBoxfromID(winnerContractBox.getBoxId).additionalRegisters(ErgoBox.R9).value.asInstanceOf[Coll[Long]](0))
    val oracleBoxId = exp.getUnspentBoxFromTokenID(serviceConf.oracleNFT).getBoxId
    val oracleBoxInput = exp.getUnspentBoxFromMempool(oracleBoxId)
    val initTx = lotteryConf.Lottery.initTransaction
    val winningId = mintUtility.getRandomNumberFromBoxID(oracleBoxId, (index - 1).toInt)
    val winningTicketId = exp.getWinningTicketWithR5(initTx, winningId.toLong) //root cause!!! Gets wrong version of ticket lol
    val winningBox = exp.getUnspentBoxFromTokenID(winningTicketId)
    val winningTicketBox = exp.getUnspentBoxFromMempool(winningBox.getBoxId)
    println("winning ticket number: " + winningId)
    val winnerAddress = Address.create(winningBox.getAddress)
    val winningTicket = new ErgoToken(winningTicketId, 1)
    val status = mintUtility.getChance(winningId.toInt, (index - 1).toInt)
    val newIndex = 1
    val newVersion = lotteryConf.Lottery.version + 1
    val timeStamp: Long = System.currentTimeMillis() + serviceConf.timeBeforeUnlockMS
    val outcomeTx = mintUtility.selectWinner(ctx = this.ctx, ownerMnemonic = serviceConf.ownerMnemonic, mnemonicPassword = serviceConf.ownerMnemonicPw,
      ticketContract = lotteryConf.Lottery.ticketContract.contract, collectionContract = lotteryConf.Lottery.collectionContract.contract, distributionAddress = Address.create(lotteryConf.Lottery.distributionAddress),
      singletonToken = lotteryConf.Lottery.ticketContract.singleton, oracleBox = oracleBoxInput, winningTicketBox = winningTicketBox, winnerContract = winnerContractBoxInput, winningTicket = winningTicket,
      cometId = serviceConf.cometId, winnerAddress = winnerAddress , newIndex = newIndex, newVersion= newVersion, status = status, timeStamp = timeStamp)
    val txn = ctx.sendTransaction(outcomeTx)
    val removedQuotes = txn.replace("\"", "")
    if (status) {
      println("Winner has been found: " + removedQuotes)
      conf.writeWinner(lotteryFilePath + ".json", winningTicketId, winnerAddress.toString)
      fileOperations.copyFile(lotteryFilePath + ".json", lotteryHistoryDir + "lottery_" + lotteryConf.Lottery.ticketContract.singleton +".json" )
      fileOperations.copyFile(serviceFilePath, serviceHistoryDir + "serviceOwner_" + lotteryConf.Lottery.ticketContract.singleton + ".json" )
      conf.writeTSnull(lotteryFilePath + ".json")
      conf.writeTSnull(lotteryFilePath + ".json")
      return
    }
    println("Loser this round: " + removedQuotes)
    Thread.sleep(120000)
//    conf.writeV2(lotteryFilePath + ".json", newVersion, timeStamp)
  }


  def processWinner(): Unit = {
    val config = lotteryConf.Lottery
    try {
      if (System.currentTimeMillis() >= config.timeStamp.toLong) {
        val singleton = config.ticketContract.singleton
        val latestTicketBox = exp.getUnspentBoxFromTokenID(singleton)

        if(latestTicketBox.getAddress != config.winnerSelectionContract.contract) {
          checkVersionAndTS(latestTicketBox.getBoxId)
        }


        val txId = latestTicketBox.getTransactionId
        val res = exp.getBoxesfromTransaction(txId)
        if (res.getOutputs.get(0).getAddress.equals(config.winnerSelectionContract.contract)){
          println("addresses compared!")
          throw new WinnerSelectionError("error found")
        }

        val poolBoxID = exp.getUnspentBoxFromMempool(res.getOutputs.get(2).getBoxId)
        val ticketContractBoxId = exp.getUnspentBoxFromMempool(res.getOutputs.get(1).getBoxId)

        val cometAmount = poolBoxID.getTokens.get(0).getValue
        println("good")
        val comet = new ErgoToken(serviceConf.cometId, cometAmount)
        val index = exp.getErgoBoxfromID(ticketContractBoxId.getId.toString).additionalRegisters(ErgoBox.R5).value.toString.toLong

        if (index < 2) {

          throw new RuntimeException("index must be higher, not enough tickets have been bought")
        }
//        val oracleBoxId = "415655d5064d9f09f00657b165442684968298446bba506c216101ddb467738d" //loser for v1
//        val oracleBoxId = "28df3488741ff5896b8c4951a05ba9e724a8c7bdceb9470756506a130af7cc2a" //winner at 20 buys aka 21 index
        fixInitTxData() //although this is called, the old data does not get refreshed, therefore, wrong registers get set for R4 during init

        val oracleBoxId = exp.getUnspentBoxFromTokenID(serviceConf.oracleNFT).getBoxId
        val initTx = config.initTransaction
        val winningId = mintUtility.getRandomNumberFromBoxID(oracleBoxId, (index - 1).toInt)
        val winningTicketId = exp.getWinningTicketWithR5(initTx, winningId.toLong)
        val winningTicket = new ErgoToken(winningTicketId, 1)


        val mintUtil = new mintUtility(ctx = ctx, ownerMnemonic = serviceConf.ownerMnemonic, mnemonicPassword = serviceConf.ownerMnemonicPw, cometId = serviceConf.cometId, cometPrice = serviceConf.cometTicketPrice, ticketContract = config.ticketContract.contract,
          singletonToken = config.ticketContract.singleton, collectionContract = config.collectionContract.contract, winnerSelectionContract = config.winnerSelectionContract.contract,
          version = config.version, distributionAddress = Address.create(serviceConf.distributionAddress))

        val amount = 0.006

        val initWinnerContractTx = mintUtil.initWinnerContract(poolBoxID, ticketContractBoxId, comet, winningTicket, index, amount)
        ctx.sendTransaction(initWinnerContractTx)

        try {

          val oracleBoxInput = exp.getUnspentBoxFromMempool(oracleBoxId)
          val winningBox = exp.getUnspentBoxFromTokenID(winningTicketId)
          val winningTicketBox = exp.getUnspentBoxFromMempool(winningBox.getBoxId)
          val winnerAddress = Address.create(winningBox.getAddress)
          val status = mintUtility.getChance(winningId.toInt, (index - 1).toInt)
          val winnerContractBox = initWinnerContractTx.getOutputsToSpend.get(0)
          val newIndex = 1
          val newVersion = config.version + 1
          val timeStamp: Long = System.currentTimeMillis() + serviceConf.timeBeforeUnlockMS
          val outcomeTx = mintUtil.selectWinner(oracleBoxInput, winningTicketBox, winnerContractBox, winningTicket, serviceConf.cometId, winnerAddress, newIndex, newVersion, status, timeStamp)
          val txn = ctx.sendTransaction(outcomeTx)
          val removedQuotes = txn.replace("\"", "")
          if (status) {
            println("Winner has been found: " + removedQuotes)
            conf.writeWinner(lotteryFilePath + ".json", winningTicketId, winnerAddress.toString)
            fileOperations.copyFile(lotteryFilePath + ".json", lotteryHistoryDir + "lottery_" + lotteryConf.Lottery.ticketContract.singleton +".json" )
            fileOperations.copyFile(serviceFilePath, serviceHistoryDir + "serviceOwner_" + lotteryConf.Lottery.ticketContract.singleton + ".json" )
            conf.writeTSnull(lotteryFilePath + ".json")
            return
          }
          println("Loser this round: " + removedQuotes)
//          conf.writeV2(lotteryFilePath + ".json", newVersion, timeStamp)
          Thread.sleep(120000)
        } catch {
          case e  => handleWinnerTxnError()
        }
      } else {
        val diff = config.timeStamp.toLong - System.currentTimeMillis()
        throw new RuntimeException("Adequate time has not passed, please wait for " + diff + " milliseconds more")
      }
    } catch {
      case e: IndexOutOfBoundsException => handleProcessWinnerIndexError()
      case e: RuntimeException => handleProcessWinnerIndexError()
      case e: WinnerSelectionError => handleWinnerTxnError()
    }
  }
}
