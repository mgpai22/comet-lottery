package utils

import config.TestNetNodeConfig
import configs.serviceOwnerConf
import explorer.Explorer
import mint.DefaultNodeInfo
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.impl.{InputBoxImpl, ScalaBridge}
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import org.ergoplatform.restapi.client.{ApiClient, Asset, ErgoTransaction, ErgoTransactionOutput, InfoApi, Registers, TransactionsApi, UtxoApi}
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.JavaHelpers.JLongRType
import org.ergoplatform.appkit.{BlockchainContext, ErgoId, ErgoToken, ErgoType, ErgoValue, InputBox, NetworkType}
import org.ergoplatform.appkit.impl.ScalaBridge
import org.ergoplatform.explorer.client.model.{InputInfo, Items, OutputInfo, TransactionInfo}
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import retrofit2.Response
import scorex.utils.ByteArray
import sigmastate.Values.ByteArrayConstant
import special.collection.Coll

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util
import scala.collection.JavaConversions._
import scala.math.BigInt.javaBigInteger2bigInt
import java.util.List
import java.util

class explorerApi(apiUrl: String, nodeUrl: String = serviceOwnerConf.read("serviceOwner.json").nodeUrl) extends Explorer(nodeInfo = mint.DefaultNodeInfo(new network(serviceOwnerConf.read("serviceOwner.json").nodeUrl).getNetworkType)){

  def getExplorerApi(apiUrl: String): DefaultApi = {
    new ExplorerApiClient(apiUrl).createService(classOf[DefaultApi])
  }

  def buildNodeService(nodeUrl: String): ApiClient = {
    new ApiClient(nodeUrl)
    //    nodeClient.createService(classOf[UtxoApi])
    //    val anotherClient = nodeClient.createService(classOf[TransactionsApi])
    //    anotherClient.getUnconfirmedTransactions()
  }

  def getUnspentBoxFromTokenID(tokenId: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesUnspentBytokenidP1(tokenId, 0, 1).execute().body().getItems.get(0)
  }

  def getBoxesfromTransaction(txId: String): TransactionInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1TransactionsP1(txId).execute().body()
  }

//  def getAllBoxesByTokenId(tokenId: String): Unit ={
//    val api = this.getExplorerApi(this.apiUrl)
//    api.getApiV1BoxesBytokenidP1()
//  }

  def getAddressInfo(address: String): util.List[OutputInfo] = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesByaddressP1(address, 0, 100).execute().body().getItems
  }

  def getBoxesfromUnconfirmedTransaction(txId: String): ErgoTransaction = {
    val nodeService = this.buildNodeService(this.nodeUrl).createService(classOf[TransactionsApi])
    nodeService.getUnconfirmedTransactionById(txId).execute().body()
  }


  def getUnspentBoxFromMempool(boxId: String): InputBox = {
    val nodeService = this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null){
      return new InputBoxImpl(this.getErgoBoxfromID(boxId)).asInstanceOf[InputBox]
    }
    new InputBoxImpl(response).asInstanceOf[InputBox]
  }

  def getMem(boxId: String): Boolean = {
    val nodeService = this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null){
      return false
    }
    true
  }

  def getBoxbyIDfromExplorer(boxID: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesP1(boxID).execute().body()
  }

  def getErgoBoxfromID(boxID: String): ErgoBox = {
    val nodeService = this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response: ErgoTransactionOutput = nodeService.getBoxWithPoolById(boxID).execute().body()

    if(response == null){
      val box = this.getBoxbyIDfromExplorer(boxID)
      val tokens = new util.ArrayList[Asset](box.getAssets.size)
      for (asset <- box.getAssets) {
        tokens.add(new Asset().tokenId(asset.getTokenId).amount(asset.getAmount))
      }
      val registers = new Registers
      for (registerEntry <- box.getAdditionalRegisters.entrySet) {
        registers.put(registerEntry.getKey, registerEntry.getValue.serializedValue)
      }
      val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
        .ergoTree(box.getErgoTree)
        .boxId(box.getBoxId)
        .index(box.getIndex)
        .value(box.getValue)
        .transactionId(box.getTransactionId)
        .creationHeight(box.getCreationHeight)
        .assets(tokens)
        .additionalRegisters(registers)
      return ScalaBridge.isoErgoTransactionOutput.to(boxConversion)
    }
    val tokens = new util.ArrayList[Asset](response.getAssets.size)
    for (asset <- response.getAssets) {
      tokens.add(new Asset().tokenId(asset.getTokenId).amount(asset.getAmount))
    }
    val registers = new Registers
    for (registerEntry <- response.getAdditionalRegisters.entrySet()) {
      registers.put(registerEntry.getKey, registerEntry.getValue)
    }
    val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
      .ergoTree(response.getErgoTree)
      .boxId(response.getBoxId)
      .index(response.getIndex)
      .value(response.getValue)
      .transactionId(response.getTransactionId)
      .creationHeight(response.getCreationHeight)
      .assets(tokens)
      .additionalRegisters(registers)
    ScalaBridge.isoErgoTransactionOutput.to(boxConversion)
  }


  def getErgoBoxfromIDNoApi(box: InputInfo): ErgoBox = {

    val tokens = new util.ArrayList[Asset](box.getAssets.size)
    for (asset <- box.getAssets) {
      tokens.add(new Asset().tokenId(asset.getTokenId).amount(asset.getAmount))
    }
    val registers = new Registers
    for (registerEntry <- box.getAdditionalRegisters.entrySet) {
      registers.put(registerEntry.getKey, registerEntry.getValue.serializedValue)
    }
    val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
      .ergoTree(box.getErgoTree)
      .boxId(box.getBoxId)
      .index(box.getIndex)
      .value(box.getValue)
      .transactionId(null)
      .creationHeight(null)
      .assets(tokens)
      .additionalRegisters(registers)
    return ScalaBridge.isoErgoTransactionOutput.to(boxConversion)


  }

  def getWinningTicketWithR5(txId: String, tokenId: Long): String ={
    val api = this.getExplorerApi(this.apiUrl)
    val tx  = api.getApiV1TransactionsP1(txId).execute().body()
    val issuerR5 = getErgoBoxfromIDNoApi(tx.getInputs.get(0)).additionalRegisters(ErgoBox.R5).value.toString.toLong
    if(issuerR5 == tokenId){
      return tx.getInputs.get(0).getBoxId //issuer box id is tokenId
    }
    val newTx = tx.getOutputs.get(1).getSpentTransactionId
    if(newTx == null){
      return null
    }
    return getWinningTicketWithR5(newTx, tokenId)
  }

}
