package initilization

import configs.{conf, serviceOwnerConf}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  ErgoToken,
  InputBox,
  Parameters,
  SignedTransaction
}
import utils.ContractCompile
import contracts.LotteryContracts
import utils.TransactionHelper

import java.util

class init(
    val ctx: BlockchainContext,
    comet: ErgoToken,
    priceInComet: Long,
    distributionAddress: Address,
    ownerMnemonic: String,
    ownerMnemonicPw: String = null,
    filePath: String,
    singletonName: String,
    singletonDesc: String,
    oracleNFT: ErgoToken,
    timeStamp: Long,
    timeToWaitMS: Long
) {
  private val compiler = new ContractCompile(ctx)
  private val MaxMinerFee = Parameters.MinFee * 3 //0.003 ERG
  private val serviceFilePath = "serviceOwner.json"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private val winnerSelectionContract: ErgoContract =
    compiler.compileWinnerContract(
      LotteryContracts.WinnerContract.contractScript,
      comet,
      oracleNFT,
      timeToWaitMS,
      serviceConf.winnerChance,
      MaxMinerFee
    )
  private val txHelper =
    new TransactionHelper(this.ctx, ownerMnemonic, ownerMnemonicPw)
  private val genesisTX: SignedTransaction = txHelper.sendToken(
    List(txHelper.senderAddress),
    List(0.01),
    List(List(comet.getId.toString)),
    List(List(1))
  )

  def initialize(): Unit = {
    val genesisOutBox: InputBox = genesisTX.getOutputsToSpend.get(0)
    val inputBoxList = new util.ArrayList[InputBox]()
    inputBoxList.add(genesisOutBox)
    txHelper.sendTx(genesisTX)
    val ticketContractSingletonTx: SignedTransaction = txHelper.createToken(
      txHelper.senderAddress,
      List(0.003),
      name = singletonName,
      description = singletonDesc,
      tokenAmount = 1,
      tokenDecimals = 0,
      inputBox = inputBoxList
    )
    val ticketContractSingletonOutBox: InputBox =
      ticketContractSingletonTx.getOutputsToSpend.get(0)
    inputBoxList.clear()
    inputBoxList.add(ticketContractSingletonOutBox)
    txHelper.sendTx(ticketContractSingletonTx)
    val ticketContractSingletonId: String = genesisOutBox.getId.toString
    val proxyContract: ErgoContract = compiler.compileProxyContract(
      LotteryContracts.ProxyContract.contractScript,
      new ErgoToken(ticketContractSingletonId, 1)
    )
    val collectionContract: ErgoContract = compiler.compileCollectionContract(
      LotteryContracts.CollectionContract.contractScript,
      new ErgoToken(ticketContractSingletonId, 1),
      winnerSelectionContract.toAddress,
      MaxMinerFee
    )
    val ticketContract: ErgoContract = compiler.compileTicketContract(
      LotteryContracts.TicketContract.contractScript,
      collectionContract.toAddress,
      proxyContract.toAddress,
      winnerSelectionContract.toAddress,
      comet,
      priceInComet,
      MaxMinerFee
    )
    val ticketContractTx: SignedTransaction = txHelper.initTicketContract(
      ticketContract,
      new ErgoToken(ticketContractSingletonId, 1),
      distributionAddress,
      timeStamp,
      txHelper.senderAddress,
      inputBox = inputBoxList
    )
    val ticketContractOutBox: InputBox =
      ticketContractSingletonTx.getOutputsToSpend.get(
        ticketContractSingletonTx.getOutputsToSpend.size() - 1
      )
    txHelper.sendTx(ticketContractTx)
    inputBoxList.clear()
    inputBoxList.add(ticketContractOutBox)
    val collectionContractTx: SignedTransaction =
      txHelper.initCollectionContract(
        collectionContract,
        comet,
        txHelper.senderAddress,
        inputBoxList,
        timeStamp: Long
      )
    txHelper.sendTx(collectionContractTx)
    val initialCollectionInput: String =
      collectionContractTx.getOutputsToSpend.get(0).getId.toString
    val initialTicketContractInput: String =
      ticketContractTx.getOutputsToSpend.get(0).getId.toString

    val confJson = new conf(
      1,
      ticketContract.toAddress.toString,
      ticketContractSingletonId,
      initialTicketContractInput,
      collectionContract.toAddress.toString,
      initialCollectionInput,
      proxyContract.toAddress.toString,
      winnerSelectionContract.toAddress.toString,
      distributionAddress.toString,
      timeStamp.toString
    )
    confJson.write(filePath)
  }
}
