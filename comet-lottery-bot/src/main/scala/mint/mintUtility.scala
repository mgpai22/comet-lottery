package mint

import configs.{ServiceOwnerConfig, conf, serviceOwnerConf}
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.{Address, BlockchainContext, ContextVar, Eip4Token, ErgoToken, InputBox, OutBox, SignedTransaction}
import utils.{OutBoxes, TransactionHelper, explorerApi}

import java.util
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class mintUtility(val ctx: BlockchainContext, cometId: String, cometPrice:Long, ticketContract: String, singletonToken: String, collectionContract: String,
                  winnerSelectionContract: String, version: Long, distributionAddress: Address) {

  private val api = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  private val outBoxObj = new OutBoxes(ctx)
  private val ownerMnemonic = ""
  private val ownerTxHelper = new TransactionHelper(ctx = ctx, walletMnemonic = ownerMnemonic, mnemonicPassword = "")




//  private val proxySender = Address.create("3WwLBpX7D9eyaV9gWHeaSp22XwEvLQ9huEg9otg8rKXQw4xPtYzq")




  def buildLotteryMint(name: String, description: String, proxyInput: String, index: Long, timeStamp: Long, poolBoxID:String, ticketContractBoxId:String): SignedTransaction = {
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
    val buyerAmountPaid = 0.012 //change later
    inputValueIdeal.append((buyerAmountPaid * math.pow(10, 9).toLong).toLong)

    val change: Long = 0 //inputValue.sum - inputValueIdeal.sum
    val ticket: Eip4Token = outBoxObj.tokenHelper(input0, name, description, 1, 0)
    val newTicketContractBox: OutBox = outBoxObj.newTicketBox(new ErgoToken(singletonToken, 1), Address.create(ticketContract).toErgoContract, version, index, timeStamp, distributionAddress, 0.001)
    val proxyInputSentTxId: String = api.getBoxbyIDfromExplorer(input2.getId.toString).getTransactionId
    val proxySender = Address.create(api.getBoxesfromTransaction(proxyInputSentTxId).getInputs.get(0).getAddress)

    val buyerOutBox: OutBox = outBoxObj.NFToutBox(ticket, proxySender, change + 0.001)

    val paymentBoxValue: Double = (input0.getValue + (0.001 * math.pow(10, 9))) * math.pow(10, -9)

    val paymentBox: OutBox = outBoxObj.ticketPayoutBox(Address.create(collectionContract).toErgoContract, new ErgoToken(cometId, cometPrice + input0.getTokens.get(0).getValue), version, index, timeStamp: Long, paymentBoxValue)
    val OutBox: List[OutBox] = List(buyerOutBox, newTicketContractBox, paymentBox)
    val txHelper = new TransactionHelper(ctx = ctx, walletMnemonic = "", mnemonicPassword = "")
    val unsignedTx = txHelper.buildUnsignedTransactionWithDataInputs(inputs.asJava, OutBox, inputs.asJava)
    txHelper.signTransaction(unsignedTx)
  }

  def buildLotteryMintChained(name: String, description: String, proxyInput: String, index: Long, timeStamp: Long, poolBox: InputBox, ticketContractBox: InputBox): SignedTransaction = {
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

    val change: Long = inputValue.sum - inputValueIdeal.sum
    val ticket: Eip4Token = outBoxObj.tokenHelper(input0, name, description, 1, 0)
    val newTicketContractBox: OutBox = outBoxObj.newTicketBox(new ErgoToken(singletonToken, 1), Address.create(ticketContract).toErgoContract, version, index, timeStamp, distributionAddress, 0.001)
    val proxyInputSentTxId: String = api.getBoxbyIDfromExplorer(input2.getId.toString).getTransactionId
    val proxySender = Address.create(api.getBoxesfromTransaction(proxyInputSentTxId).getInputs.get(0).getAddress)

    val buyerOutBox: OutBox = outBoxObj.NFToutBox(ticket, proxySender, change + 0.001)

    val paymentBoxValue: Double = (input0.getValue + (0.001 * math.pow(10, 9))) * math.pow(10, -9)

    val paymentBox: OutBox = outBoxObj.ticketPayoutBox(Address.create(collectionContract).toErgoContract, new ErgoToken(cometId, cometPrice + input0.getTokens.get(0).getValue), version, index, timeStamp: Long, paymentBoxValue)
    val OutBox: List[OutBox] = List(buyerOutBox, newTicketContractBox, paymentBox)
    val txHelper = new TransactionHelper(ctx = ctx, walletMnemonic = "", mnemonicPassword = "")
    val unsignedTx = txHelper.buildUnsignedTransactionWithDataInputs(inputs.asJava, OutBox, inputs.asJava)
    txHelper.signTransaction(unsignedTx)
  }

  def initWinnerContract(collectionContractInput: InputBox, ticketContractInput: InputBox,
                         comet: ErgoToken, winningTicket: ErgoToken,
                         index: Long, amount: Double): SignedTransaction ={
    val winningTicketIssuerBox = winningTicket.getId.toString
    val issuerBox: ErgoBox = this.api.getErgoBoxfromID(winningTicketIssuerBox)
    val inputs: List[InputBox] = List(collectionContractInput.withContextVars(ContextVar.of(0.toByte, issuerBox)), ticketContractInput.withContextVars(ContextVar.of(0.toByte, issuerBox)))


    val outbox = this.outBoxObj.firstWinnerContractBox(issuerBox, Address.create(winnerSelectionContract).toErgoContract,
      Address.create(ticketContract).toErgoContract, Address.create(collectionContract).toErgoContract,
      new ErgoToken(singletonToken, 1), comet, winningTicket, distributionAddress, index, version, amount)



    val unsignedTx = ownerTxHelper.buildUnsignedTransaction(inputs.asJava, List(outbox))
    ownerTxHelper.signTransaction(unsignedTx)
  }

  def selectWinner(oracleBox: InputBox, winningTicketBox: InputBox, winnerContract: InputBox, winningTicket: ErgoToken, cometId: String, winnerAddress: Address, newIndex: Long, newVersion: Long, status: Boolean, timeStamp: Long): SignedTransaction = {
    val dataInputs = new util.ArrayList[InputBox]()
    dataInputs.add(oracleBox)
    dataInputs.add(winningTicketBox)
    val issuerBox: ErgoBox = this.api.getErgoBoxfromID(winningTicket.getId.toString)
    val inputBox: InputBox = winnerContract.withContextVars(ContextVar.of(0.toByte, issuerBox))

    val prizeValue = winnerContract.getTokens.get(0).getValue
    val cometWinner = new ErgoToken(cometId, (prizeValue * 0.9).toLong)
    val cometDistribution = new ErgoToken(cometId, (prizeValue * 0.1).toLong)
    val comet = new ErgoToken(cometId, prizeValue)

    val outBoxLoser = outBoxObj.loserOutBox(Address.create(ticketContract).toErgoContract, Address.create(collectionContract).toErgoContract, distributionAddress,
      new ErgoToken(singletonToken, 1), comet, newIndex, newVersion, timeStamp)


    val unsignedTxLoser = ownerTxHelper.buildUnsignedTransactionWithDataInputs(List(inputBox).asJava, outBoxLoser, dataInputs)

    val outBoxWinner = outBoxObj.lotteryWinnerBox(cometWinner, cometDistribution, winnerAddress, distributionAddress)

    val unsignedWinner = ownerTxHelper.buildUnsignedTransactionWithDataInputsWithTokensToBurn(List(inputBox).asJava, outBoxWinner, dataInputs, List(inputBox.getTokens.get(1)))

    if(status){
      return ownerTxHelper.signTransaction(unsignedWinner)
    }
    ownerTxHelper.signTransaction(unsignedTxLoser)
  }

}

object mintUtility{
  def getRandomNumberFromBoxID(boxID: String, range: Int): BigInt = {
    val boxIDHex: Array[Byte] = new ErgoToken(boxID, 1).getId.getBytes
    val randNumBytes: Array[Byte] = boxIDHex.slice(0, 15)
    val rand: BigInt = BigInt(randNumBytes)
    val rangeBI = BigInt(range)
    (((rand % rangeBI) + rangeBI) % rangeBI).toLong + 1L
  }

  def getChance(winningNumber: Int, amountTicketsSold: Int): Boolean = {
    ((BigInt(winningNumber - 1) % BigInt(amountTicketsSold)) * 1000) < BigInt(amountTicketsSold * 100)
  }
//  val bx = "28df3488741ff5896b8c4951a05ba9e724a8c7bdceb9470756506a130af7cc2a"
//  val rnge = 20
//  val num = getRandomNumberFromBoxID(bx, rnge)
//  println(num)
//  println(getChance(num.toInt, rnge))
//  println(((BigInt(num.toInt - 1) % BigInt(rnge)) * 1000))
}
