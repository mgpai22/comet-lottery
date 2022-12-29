package mint

import org.ergoplatform.{ErgoBox, ErgoScriptPredef}
import org.ergoplatform.appkit._
import scorex.crypto.encode.Base16
import utils.{OutBoxes, TransactionHelper, explorerApi}

import java.util
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class mintUtility(
    val ctx: BlockchainContext,
    ownerMnemonic: String,
    mnemonicPassword: String,
    cometId: String,
    cometPrice: Long,
    ticketContract: String,
    singletonToken: String,
    collectionContract: String,
    winnerSelectionContract: String,
    version: Long,
    distributionAddress: Address
) {
  private val txPropBytes =
    Base16.decode(ErgoScriptPredef.feeProposition(720).bytesHex).get
  private val api = new explorerApi(
    DefaultNodeInfo(ctx.getNetworkType).explorerUrl
  )
  private val outBoxObj = new OutBoxes(ctx)
  private val ownerTxHelper = new TransactionHelper(
    ctx = ctx,
    walletMnemonic = ownerMnemonic,
    mnemonicPassword = mnemonicPassword
  )

  def convertERGLongToDouble(num: Long): Double = {
    val value = num * math.pow(10, -9)
    val x = (math floor value * 1000) / 1000
    val bNum = math.BigDecimal(x)
    val finalNum = bNum.underlying().stripTrailingZeros()
    finalNum.toString.toDouble
  }

  def buildLotteryMint(
      name: String,
      description: String,
      proxyInput: String,
      index: Long,
      timeStamp: Long,
      poolBoxID: String,
      ticketContractBoxId: String
  ): SignedTransaction = {
    val inputs: ListBuffer[InputBox] = new ListBuffer[InputBox]()
    val inputValue: ListBuffer[Long] = new ListBuffer[Long]()
    val inputValueIdeal: ListBuffer[Long] = new ListBuffer[Long]()
    val input0: InputBox = api.getUnspentBoxFromMempool(poolBoxID)
    val input1: InputBox = api.getUnspentBoxFromMempool(ticketContractBoxId)
    val input2: InputBox = api.getUnspentBoxFromMempool(proxyInput)

    inputs.append(input0)
    inputs.append(input1)
    inputs.append(input2)
    for (input <- inputs) {
      inputValue.append(input.getValue)
    }
    inputValueIdeal.append(input0.getValue)
    inputValueIdeal.append(input1.getValue)
    val buyerAmountPaid = 0.003
    inputValueIdeal.append((buyerAmountPaid * math.pow(10, 9).toLong).toLong)

    val change: Double =
      this.convertERGLongToDouble(inputValue.sum - inputValueIdeal.sum)
    val ticket: Eip4Token =
      outBoxObj.tokenHelper(input0, name, description, 1, 0)
    val newTicketContractBox: OutBox = outBoxObj.newTicketBox(
      new ErgoToken(singletonToken, 1),
      Address.create(ticketContract).toErgoContract,
      version,
      index,
      timeStamp,
      cometPrice + input0.getTokens.get(0).getValue,
      distributionAddress,
      0.001
    )
    val r4 = input2.getRegisters.get(0).toHex
    val prop =
      ErgoValue.fromHex(r4).getValue.asInstanceOf[special.sigma.SigmaProp]
    val proxySender = new org.ergoplatform.appkit.SigmaProp(prop)
      .toAddress(this.ctx.getNetworkType)

    val buyerOutBox: OutBox =
      outBoxObj.NFToutBox(ticket, proxySender, change + 0.001)

    val paymentBoxValue: Double =
      (input0.getValue + (0.001 * math.pow(10, 9))) * math.pow(10, -9)

    val paymentBox: OutBox = outBoxObj.ticketPayoutBox(
      Address.create(collectionContract).toErgoContract,
      new ErgoToken(cometId, cometPrice + input0.getTokens.get(0).getValue),
      version,
      index,
      timeStamp: Long,
      paymentBoxValue
    )
    val OutBox: List[OutBox] =
      List(buyerOutBox, newTicketContractBox, paymentBox)
    val unsignedTx = ownerTxHelper.buildUnsignedTransaction(
      inputs.asJava,
      OutBox
    )
    ownerTxHelper.signTransaction(unsignedTx)
  }

  def buildLotteryMintChained(
      name: String,
      description: String,
      proxyInput: String,
      index: Long,
      timeStamp: Long,
      poolBox: InputBox,
      ticketContractBox: InputBox
  ): SignedTransaction = {
    val inputs: ListBuffer[InputBox] = new ListBuffer[InputBox]()
    val inputValue: ListBuffer[Long] = new ListBuffer[Long]()
    val inputValueIdeal: ListBuffer[Long] = new ListBuffer[Long]()
    val input0: InputBox = poolBox
    val input1: InputBox = ticketContractBox
    val input2: InputBox = api.getUnspentBoxFromMempool(proxyInput)

    inputs.append(input0)
    inputs.append(input1)
    inputs.append(input2)
    for (input <- inputs) {
      inputValue.append(input.getValue)
    }
    inputValueIdeal.append(input0.getValue)
    inputValueIdeal.append(input1.getValue)
    val buyerAmountPaid = 0.003
    inputValueIdeal.append((buyerAmountPaid * math.pow(10, 9).toLong).toLong)

    val change: Double =
      this.convertERGLongToDouble(inputValue.sum - inputValueIdeal.sum)
    val ticket: Eip4Token =
      outBoxObj.tokenHelper(input0, name, description, 1, 0)
    val newTicketContractBox: OutBox = outBoxObj.newTicketBox(
      new ErgoToken(singletonToken, 1),
      Address.create(ticketContract).toErgoContract,
      version,
      index,
      timeStamp,
      cometPrice + input0.getTokens.get(0).getValue,
      distributionAddress,
      0.001
    )
    val r4 = input2.getRegisters.get(0).toHex
    val prop =
      ErgoValue.fromHex(r4).getValue.asInstanceOf[special.sigma.SigmaProp]
    val proxySender = new org.ergoplatform.appkit.SigmaProp(prop)
      .toAddress(this.ctx.getNetworkType)

    val buyerOutBox: OutBox =
      outBoxObj.NFToutBox(ticket, proxySender, change + 0.001)

    val paymentBoxValue: Double =
      (input0.getValue + (0.001 * math.pow(10, 9))) * math.pow(10, -9)

    val paymentBox: OutBox = outBoxObj.ticketPayoutBox(
      Address.create(collectionContract).toErgoContract,
      new ErgoToken(cometId, cometPrice + input0.getTokens.get(0).getValue),
      version,
      index,
      timeStamp: Long,
      paymentBoxValue
    )
    val OutBox: List[OutBox] =
      List(buyerOutBox, newTicketContractBox, paymentBox)
    val unsignedTx = ownerTxHelper.buildUnsignedTransaction(
      inputs.asJava,
      OutBox
    )
    ownerTxHelper.signTransaction(unsignedTx)
  }

  def initWinnerContract(
      collectionContractInput: InputBox,
      ticketContractInput: InputBox,
      comet: ErgoToken,
      winningTicket: ErgoToken,
      index: Long,
      amount: Double
  ): SignedTransaction = {
    val winningTicketIssuerBox = winningTicket.getId.toString
    val issuerBox: ErgoBox = this.api.getErgoBoxfromID(winningTicketIssuerBox)
    val inputs: List[InputBox] = List(
      collectionContractInput,
      ticketContractInput
    )

    val outbox = this.outBoxObj.firstWinnerContractBox(
      issuerBox,
      Address.create(winnerSelectionContract).toErgoContract,
      Address.create(ticketContract).toErgoContract,
      Address.create(collectionContract).toErgoContract,
      new ErgoToken(singletonToken, 1),
      comet,
      winningTicket,
      distributionAddress,
      index,
      version,
      amount
    )

    val unsignedTx =
      ownerTxHelper.buildUnsignedTransaction(inputs.asJava, List(outbox), 0.003)
    ownerTxHelper.signTransaction(unsignedTx)
  }

  def selectWinner(
      oracleBox: InputBox,
      winningTicketBox: InputBox,
      winnerContract: InputBox,
      winningTicket: ErgoToken,
      cometId: String,
      winnerAddress: Address,
      newIndex: Long,
      newVersion: Long,
      status: Boolean,
      timeStamp: Long
  ): SignedTransaction = {
    val dataInputs = new util.ArrayList[InputBox]()
    dataInputs.add(oracleBox)
    dataInputs.add(winningTicketBox)
    val issuerBox: ErgoBox =
      this.api.getErgoBoxfromID(winningTicket.getId.toString)
    val inputBox: InputBox =
      winnerContract.withContextVars(
        ContextVar.of(0.toByte, issuerBox)
      )
    val prizeValue = winnerContract.getTokens.get(0).getValue
    val cometWinner = new ErgoToken(cometId, (prizeValue * 0.9).toLong)
    val cometDistribution = new ErgoToken(cometId, (prizeValue * 0.1).toLong)
    val comet = new ErgoToken(cometId, prizeValue)

    val outBoxLoser = outBoxObj.loserOutBox(
      Address.create(ticketContract).toErgoContract,
      Address.create(collectionContract).toErgoContract,
      distributionAddress,
      new ErgoToken(singletonToken, 1),
      comet,
      newIndex,
      newVersion,
      timeStamp
    )

    val unsignedTxLoser = ownerTxHelper.buildUnsignedTransactionWithDataInputs(
      List(inputBox).asJava,
      outBoxLoser,
      dataInputs,
      0.003
    )

    val outBoxWinner = outBoxObj.lotteryWinnerBox(
      cometWinner,
      issuerBox,
      cometDistribution,
      winnerAddress,
      distributionAddress
    )

    val unsignedWinner =
      ownerTxHelper.buildUnsignedTransactionWithDataInputsWithTokensToBurn(
        List(inputBox).asJava,
        outBoxWinner,
        dataInputs,
        List(inputBox.getTokens.get(1)),
        0.003
      )

    if (status) {
      return ownerTxHelper.signTransaction(unsignedWinner)
    }
    ownerTxHelper.signTransaction(unsignedTxLoser)
  }

}

object mintUtility {
  private val txPropBytes =
    Base16.decode(ErgoScriptPredef.feeProposition(720).bytesHex).get
  def getRandomNumberFromBoxID(boxID: String, range: Int): BigInt = {
    val boxIDHex: Array[Byte] = new ErgoToken(boxID, 1).getId.getBytes
    val randNumBytes: Array[Byte] = boxIDHex.slice(0, 15)
    val rand: BigInt = BigInt(randNumBytes)
    val rangeBI = BigInt(range)
    (((rand % rangeBI) + rangeBI) % rangeBI).toLong + 1L
  }

  def getChance(winningId: BigInt, chancePercent: BigInt): Boolean = {
    // winningID must be between 0 and 100 inclusive
    winningId <= chancePercent
  }

  def selectWinner(
      ctx: BlockchainContext,
      ownerMnemonic: String,
      mnemonicPassword: String,
      ticketContract: String,
      collectionContract: String,
      distributionAddress: Address,
      singletonToken: String,
      oracleBox: InputBox,
      winningTicketBox: InputBox,
      winnerContract: InputBox,
      winningTicket: ErgoToken,
      cometId: String,
      winnerAddress: Address,
      newIndex: Long,
      newVersion: Long,
      status: Boolean,
      timeStamp: Long
  ): SignedTransaction = {
    val outBoxObj = new OutBoxes(ctx)
    val api = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
    val ownerTxHelper = new TransactionHelper(
      ctx = ctx,
      walletMnemonic = ownerMnemonic,
      mnemonicPassword = mnemonicPassword
    )
    val dataInputs = new util.ArrayList[InputBox]()
    dataInputs.add(oracleBox)
    dataInputs.add(winningTicketBox)
    val issuerBox: ErgoBox = api.getErgoBoxfromID(winningTicket.getId.toString)
    val inputBox: InputBox =
      winnerContract.withContextVars(
        ContextVar.of(0.toByte, issuerBox)
      )

    val prizeValue = winnerContract.getTokens.get(0).getValue
    val cometWinner = new ErgoToken(cometId, (prizeValue * 0.9).toLong)
    val cometDistribution = new ErgoToken(cometId, (prizeValue * 0.1).toLong)
    val comet = new ErgoToken(cometId, prizeValue)

    val outBoxLoser = outBoxObj.loserOutBox(
      Address.create(ticketContract).toErgoContract,
      Address.create(collectionContract).toErgoContract,
      distributionAddress,
      new ErgoToken(singletonToken, 1),
      comet,
      newIndex,
      newVersion,
      timeStamp
    )

    val unsignedTxLoser = ownerTxHelper.buildUnsignedTransactionWithDataInputs(
      List(inputBox).asJava,
      outBoxLoser,
      dataInputs,
      0.003
    )

    val outBoxWinner = outBoxObj.lotteryWinnerBox(
      cometWinner,
      issuerBox,
      cometDistribution,
      winnerAddress,
      distributionAddress
    )

    val unsignedWinner =
      ownerTxHelper.buildUnsignedTransactionWithDataInputsWithTokensToBurn(
        List(inputBox).asJava,
        outBoxWinner,
        dataInputs,
        List(inputBox.getTokens.get(1)),
        0.003
      )

    if (status) {
      return ownerTxHelper.signTransaction(unsignedWinner)
    }
    ownerTxHelper.signTransaction(unsignedTxLoser)
  }
}
