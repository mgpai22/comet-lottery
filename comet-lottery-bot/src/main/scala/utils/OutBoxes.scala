package utils

import org.ergoplatform.appkit.{Address, BlockchainContext, Eip4Token, ErgoContract, ErgoToken, ErgoType, ErgoValue, InputBox, JavaHelpers, OutBox, Parameters}
import org.ergoplatform.appkit.impl.{Eip4TokenBuilder, ErgoTreeContract}
import boxes.Box
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.JavaHelpers.JLongRType
import special.collection.Coll

import java.util
import scala.collection.mutable.ListBuffer

class OutBoxes(ctx: BlockchainContext){

  private def getAmount(amount: Double): Long ={
    (amount * Parameters.OneErg).toLong
  }
  private val txBuilder = this.ctx.newTxBuilder()
  private val minAmount = this.getAmount(0.001)

  def pictureNFTHelper(inputBox: InputBox, name: String, description: String, imageLink: String, sha256: Array[Byte]): Eip4Token ={
    val tokenID = inputBox.getId.toString
    Eip4TokenBuilder.buildNftPictureToken(
      tokenID,
      1,
      name,
      description, 0,
      sha256,
      imageLink
    )

  }

  def tokenHelper(inputBox: InputBox, name: String, description: String, tokenAmount: Long, tokenDecimals: Int): Eip4Token ={
    new Eip4Token(inputBox.getId.toString, tokenAmount, name, description, tokenDecimals)
  }

  def NFToutBox(nft: Eip4Token, receiver: Address, amount: Double = 0.001): OutBox = {
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .mintToken(nft)
      .contract(new ErgoTreeContract(receiver.getErgoAddress.script, this.ctx.getNetworkType))
      .build()
  }

  def tokenOutBox(token: Eip4Token, receiver: Address, amount: Double = 0.001): OutBox = {
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .mintToken(token)
      .contract(new ErgoTreeContract(receiver.getErgoAddress.script, this.ctx.getNetworkType))
      .build()
  }

  def tokenSendOutBox(receiver: List[Address],  amountList: List[Double], tokens: List[List[String]], amountTokens: List[List[Long]] = null): List[OutBox] ={
    val outbx  = new ListBuffer[OutBox]()
    var amountCounter = 0
    var tokenList1 = new ListBuffer[ListBuffer[ErgoToken]]()
    var tokenList2 = new util.ArrayList[ErgoToken]()
    var tokenAmountCounter = 0
    val tokenList = new ListBuffer[ErgoToken]()
    var tList  = new ListBuffer[ErgoToken]()
    if (amountTokens == null){
      for (token <- tokens) {
        for (x <- token) {
          val t: ErgoToken = new ErgoToken(x, 1)
          tokenList.append(t)
        }
      }
    } else{
      for (token <- tokens) {
        var tokenAmountCounterLocal = 0
        var tokenAmountList = amountTokens.apply(tokenAmountCounter)
        for (x <- token) {
          var tokenAmm = tokenAmountList.apply(tokenAmountCounterLocal)
          tList.append(new ErgoToken(x, tokenAmm))
          tokenAmountCounterLocal = tokenAmountCounterLocal + 1
        }
        tokenAmountCounter = tokenAmountCounter + 1
        tokenList1.append(tList)
        tList = new ListBuffer[ErgoToken]()
      }
    }
    for (address: Address <- receiver){
      var erg = getAmount(amountList.apply(amountCounter))
      var box = this.ctx.newTxBuilder().outBoxBuilder()
        .value(erg)
        .tokens(tokenList1.apply(amountCounter).toArray:_*)
        .contract(new ErgoTreeContract(address.getErgoAddress.script, this.ctx.getNetworkType))
        .build()
      outbx.append(box)
      amountCounter +=1
    }
    outbx.toList
  }

  def initTicketContractBox(singleTon: ErgoToken, contract: ErgoContract, distributionAddress: Address, timeStamp: Long, amount: Double = 0.001): OutBox={
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(singleTon)
      .registers(ErgoValue.of(1L), ErgoValue.of(1L), ErgoValue.of(distributionAddress.getPublicKey), ErgoValue.of(timeStamp))
      .contract(contract)
      .build()
  }

  def initCollectionContractBox(comet: ErgoToken, contract: ErgoContract, timeStamp: Long, amount: Double = 0.001): OutBox = {
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(comet)
      .registers(ErgoValue.of(1L), ErgoValue.of(1L), ErgoValue.of(timeStamp))
      .contract(contract)
      .build()
  }

  def newTicketBox(singleTon: ErgoToken, contract: ErgoContract, version: Long, index: Long, timeStamp: Long, distributionAddress: Address, amount: Double): OutBox ={
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(singleTon)
      .registers(ErgoValue.of(version), ErgoValue.of(index), ErgoValue.of(distributionAddress.getPublicKey), ErgoValue.of(timeStamp))
      .contract(contract)
      .build()
  }

  def ticketPayoutBox(collectionContract: ErgoContract, comet: ErgoToken, version: Long, counter: Long, timeStamp: Long, amount: Double): OutBox ={
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(comet)
      .registers(ErgoValue.of(version), ErgoValue.of(counter), ErgoValue.of(timeStamp))
      .contract(collectionContract)
      .build()
  }

  def mintBuyerBox(senderAddress: Address, proxyContract: ErgoContract, comet: ErgoToken, amount: Double = 0.012): OutBox ={
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(comet)
      .registers(ErgoValue.of(senderAddress.getPublicKey))
      .contract(proxyContract)
      .build()
  }

  def firstWinnerContractBox(issuerBox: ErgoBox, winnerSelectionContract:  ErgoContract, ticketContract: ErgoContract,
                             collectionContract: ErgoContract, singleton: ErgoToken, comet: ErgoToken, winningTicket: ErgoToken,
                             distributionAddress: Address, index: Long, version: Long, amount: Double): OutBox ={
    def buildLongColl(long1: java.lang.Long, long2: java.lang.Long): String = {
      val list: Array[java.lang.Long] = Array(long1, long2)
      val myColl: Coll[java.lang.Long] = special.collection.Builder.DefaultCollBuilder.fromArray(list)
      val typ: ErgoType[Coll[java.lang.Long]] = ErgoType.collType(ErgoType.longType())
      ErgoValue.of[Coll[java.lang.Long]](myColl, typ).toHex
    }
    this.txBuilder.outBoxBuilder()
      .value(getAmount(amount))
      .tokens(comet, singleton)
      .registers(ErgoValue.of(issuerBox.id), ErgoValue.of(winningTicket.getId.getBytes), ErgoValue.of(ticketContract.toAddress.asP2S.scriptBytes),
        ErgoValue.of(collectionContract.toAddress.asP2S.scriptBytes), ErgoValue.of(distributionAddress.getPublicKey),
        ErgoValue.fromHex(buildLongColl(version, index)))
      .contract(winnerSelectionContract)
      .build()
  }

  def loserOutBox(ticketContract: ErgoContract, collectionContract: ErgoContract, distributionAddress: Address, singleton: ErgoToken, comet: ErgoToken, newIndex: Long, version: Long, timeStamp: Long): List[OutBox] ={
    val box1 = this.txBuilder.outBoxBuilder()
      .value(minAmount)
      .tokens(singleton)
      .registers(ErgoValue.of(version), ErgoValue.of(newIndex), ErgoValue.of(distributionAddress.getPublicKey), ErgoValue.of(timeStamp))
      .contract(ticketContract)
      .build()

    val box2 = this.txBuilder.outBoxBuilder()
      .value(minAmount)
      .tokens(comet)
      .registers(ErgoValue.of(version), ErgoValue.of(newIndex), ErgoValue.of(timeStamp))
      .contract(collectionContract)
      .build()

    List(box1, box2)
  }

  def lotteryWinnerBox(cometWinner: ErgoToken, cometDistribution: ErgoToken, winnerAddress: Address, distributionAddress:Address): List[OutBox] ={
    val box1 = this.txBuilder.outBoxBuilder()
      .value(minAmount)
      .tokens(cometWinner)
      .registers(ErgoValue.of(0L), ErgoValue.of(0L), ErgoValue.of(0L), ErgoValue.of(0L))
      .contract(new ErgoTreeContract(winnerAddress.getErgoAddress.script, this.ctx.getNetworkType))
      .build()

    val box2 = this.txBuilder.outBoxBuilder()
      .value(minAmount)
      .tokens(cometDistribution)
      .registers(ErgoValue.of(0L), ErgoValue.of(0L), ErgoValue.of(0L), ErgoValue.of(0L))
      .contract(new ErgoTreeContract(distributionAddress.getErgoAddress.script, this.ctx.getNetworkType))
      .build()

    List(box1, box2)
  }

}
