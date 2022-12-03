package mint

import configs.{conf, serviceOwnerConf}
import initilization.init
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.{Address, ErgoToken, ErgoValue, InputBox}
import utils.explorerApi

import java.util
import scala.collection.JavaConversions._

class akkaFunctions {
  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val lotteryFilePath = "lotteryConf"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private val lotteryConf = conf.read(lotteryFilePath + ".json")
  private val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)


  def main(): Unit ={
    val config = lotteryConf.Lottery
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

  def initialize(): Unit ={
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
          validProxyBoxes.add(box.getId.toString)
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

    val mintUtil = new mintUtility(ctx = ctx, cometId = cometId, cometPrice = serviceConf.cometTicketPrice, ticketContract = config.ticketContract.contract,
      singletonToken = config.ticketContract.singleton, collectionContract = config.collectionContract.contract, winnerSelectionContract = config.winnerSelectionContract.contract,
      version = config.version.toLong, distributionAddress = Address.create(serviceConf.distributionAddress))


    val res = exp.getBoxesfromTransaction(txId)
    var poolBoxID = config.collectionContract.initialBox
    var ticketContractBoxId = config.ticketContract.initialBox
    val firstTime: Boolean = config.firstTime.toBoolean

    if (!firstTime) {
      poolBoxID = res.getOutputs.get(2).getBoxId
      ticketContractBoxId = res.getOutputs.get(1).getBoxId
    }


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

//  def handleWinnerTxnError(): Unit ={
//    val tokenID = lotteryConf.Lottery.ticketContract.singleton
//    val txId = exp.getUnspentBoxFromTokenID(tokenID).getTransactionId
//    val res = exp.getBoxesfromTransaction(txId)
//    val winnerContractBox = res.getOutputs.get(0)
//    val index = -1 //exp.getErgoBoxfromID(winnerContractBox.getBoxId).additionalRegisters(ErgoBox.R9).value
//    val oracleBoxId = exp.getUnspentBoxFromTokenID(serviceConf.oracleNFT).getBoxId
//    val oracleBoxInput = exp.getUnspentBoxFromMempool(oracleBoxId)
//    val initTx = lotteryConf.Lottery.initTransaction
//    val winningId = mintUtility.getRandomNumberFromBoxID(oracleBoxId, (index - 1).toInt)
//    val winningTicketId = exp.getWinningTicketWithR5(initTx, winningId.toLong)
//    val winningBox = exp.getUnspentBoxFromTokenID(winningTicketId)
//    val winningTicketBox = exp.getUnspentBoxFromMempool(winningBox.getBoxId)
//    val winnerAddress = Address.create(winningBox.getAddress)
//    val status = mintUtility.getChance(winningId.toInt, (index - 1).toInt)
//    val newIndex = 1
//    val newVersion = lotteryConf.Lottery.version + 1
//    val timeStamp: Long = System.currentTimeMillis() + serviceConf.timeBeforeUnlockMS
//    val outcomeTx = mintUtil.selectWinner(oracleBoxInput, winningTicketBox, winnerContractBox, winningTicket, serviceConf.cometId, winnerAddress, newIndex, newVersion, status, timeStamp, lotteryFilePath + ".json")
//    val txn = ctx.sendTransaction(outcomeTx)
//    val removedQuotes = txn.replace("\"", "")
//    if (status) {
//      println("Winner has been found: " + removedQuotes)
//      return
//    }
//    println("Loser this round: " + removedQuotes)
//    conf.writeV2(lotteryFilePath + ".json", newVersion, outcomeTx.getOutputsToSpend.get(0).getId.toString, outcomeTx.getOutputsToSpend.get(1).getId.toString, timeStamp)
//  }

  def processWinner(): Unit = {
    val config = lotteryConf.Lottery
    try {
      if (System.currentTimeMillis() >= config.timeStamp.toLong) {
        val singleton = config.ticketContract.singleton
        val latestTicketBox = exp.getUnspentBoxFromTokenID(singleton)
        val txId = latestTicketBox.getTransactionId
        val res = exp.getBoxesfromTransaction(txId)
        val poolBoxID = exp.getUnspentBoxFromMempool(res.getOutputs.get(2).getBoxId)
        val ticketContractBoxId = exp.getUnspentBoxFromMempool(res.getOutputs.get(1).getBoxId)
        val cometAmount = poolBoxID.getTokens.get(0).getValue
        val comet = new ErgoToken(serviceConf.cometId, cometAmount)
        val index = exp.getErgoBoxfromID(ticketContractBoxId.getId.toString).additionalRegisters(ErgoBox.R5).value.toString.toLong
        if (index < 2) {
          throw new RuntimeException("index must be higher, not enough tickets have been bought")
        }
//        val oracleBoxId = "415655d5064d9f09f00657b165442684968298446bba506c216101ddb467738d" //loser for v1
//        val oracleBoxId = "28df3488741ff5896b8c4951a05ba9e724a8c7bdceb9470756506a130af7cc2a" //winner at 20 buys aka 21 index
        val oracleBoxId = exp.getUnspentBoxFromTokenID(serviceConf.oracleNFT).getBoxId
        val initTx = config.initTransaction
        val winningId = mintUtility.getRandomNumberFromBoxID(oracleBoxId, (index - 1).toInt)
        val winningTicketId = exp.getWinningTicketWithR5(initTx, winningId.toLong)
        val winningTicket = new ErgoToken(winningTicketId, 1)


        val mintUtil = new mintUtility(ctx = ctx, cometId = serviceConf.cometId, cometPrice = 100, ticketContract = config.ticketContract.contract,
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
            conf.writeTSnull(lotteryFilePath + ".json")
            return
          }
          println("Loser this round: " + removedQuotes)
          conf.writeV2(lotteryFilePath + ".json", newVersion, outcomeTx.getOutputsToSpend.get(0).getId.toString, outcomeTx.getOutputsToSpend.get(1).getId.toString, timeStamp)
        } catch {
          case e  => println("Error with winner transaction")
        }
      } else {
        val diff = config.timeStamp.toLong - System.currentTimeMillis()
        throw new RuntimeException("Adequate time has not passed, please wait for " + diff + " milliseconds more")
      }
    } catch {
      case e: IndexOutOfBoundsException => handleProcessWinnerIndexError()
      case e: RuntimeException => handleProcessWinnerIndexError()
    }
  }
}
