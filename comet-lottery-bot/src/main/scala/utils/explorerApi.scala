package utils

import configs.serviceOwnerConf
import explorer.Explorer
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.impl.{InputBoxImpl, ScalaBridge}
import org.ergoplatform.explorer.client.model.{
  InputInfo,
  OutputInfo,
  TransactionInfo
}
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import org.ergoplatform.restapi.client._

import java.util
import scala.collection.JavaConversions._

class explorerApi(
    apiUrl: String,
    nodeUrl: String = serviceOwnerConf.read("serviceOwner.json").nodeUrl
) extends Explorer(
      nodeInfo = mint.DefaultNodeInfo(
        new network(
          serviceOwnerConf.read("serviceOwner.json").nodeUrl
        ).getNetworkType
      )
    ) {

  def getExplorerApi(apiUrl: String): DefaultApi = {
    new ExplorerApiClient(apiUrl).createService(classOf[DefaultApi])
  }

  def buildNodeService(nodeUrl: String): ApiClient = {
    new ApiClient(nodeUrl)
  }

  def getUnspentBoxFromTokenID(tokenId: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    val res =
      api.getApiV1BoxesUnspentBytokenidP1(tokenId, 0, 1).execute().body()
    try {
      res.getItems.get(0)
    } catch {
      case e: Exception => null
    }
  }

  def getBoxesFromTokenID(tokenId: String): OutputInfo = { //returns latest box the token has been in
    val api = this.getExplorerApi(this.apiUrl)
    var res = api.getApiV1BoxesBytokenidP1(tokenId, 0, 1).execute().body()
    val offset = res.getTotal - 1
    res = api.getApiV1BoxesBytokenidP1(tokenId, offset, 1).execute().body()
    try {
      res.getItems.get(0)
    } catch {
      case e: Exception => println(e); null
    }
  }

  def getBoxesfromTransaction(txId: String): TransactionInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1TransactionsP1(txId).execute().body()
  }

  def getAddressInfo(address: String): util.List[OutputInfo] = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesByaddressP1(address, 0, 100).execute().body().getItems
  }

  def getBoxesfromUnconfirmedTransaction(txId: String): ErgoTransaction = {
    val nodeService = this
      .buildNodeService(this.nodeUrl)
      .createService(classOf[TransactionsApi])
    nodeService.getUnconfirmedTransactionById(txId).execute().body()
  }

  def getUnspentBoxFromMempool(boxId: String): InputBox = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null) {
      return new InputBoxImpl(this.getErgoBoxfromID(boxId))
        .asInstanceOf[InputBox]
    }
    new InputBoxImpl(response).asInstanceOf[InputBox]
  }

  def getMem(boxId: String): Boolean = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null) {
      return false
    }
    true
  }

  def getBoxbyIDfromExplorer(boxID: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesP1(boxID).execute().body()
  }

  def getErgoBoxfromID(boxID: String): ErgoBox = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response: ErgoTransactionOutput =
      nodeService.getBoxWithPoolById(boxID).execute().body()

    if (response == null) {
      val box = this.getBoxbyIDfromExplorer(boxID)
      val tokens = new util.ArrayList[Asset](box.getAssets.size)
      for (asset <- box.getAssets) {
        tokens.add(
          new Asset().tokenId(asset.getTokenId).amount(asset.getAmount)
        )
      }
      val registers = new Registers
      for (registerEntry <- box.getAdditionalRegisters.entrySet) {
        registers.put(
          registerEntry.getKey,
          registerEntry.getValue.serializedValue
        )
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
      registers.put(
        registerEntry.getKey,
        registerEntry.getValue.serializedValue
      )
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

  def getWinningTicketWithR5(txId: String, tokenId: Long): String = {
    val api = this.getExplorerApi(this.apiUrl)
    val tx = api.getApiV1TransactionsP1(txId).execute().body()
    val issuerR5 = getErgoBoxfromIDNoApi(tx.getInputs.get(0))
      .additionalRegisters(ErgoBox.R5)
      .value
      .toString
      .toLong
    if (issuerR5 == tokenId) {
      return tx.getInputs.get(0).getBoxId //issuer box id is tokenId
    }
    val newTx = tx.getOutputs.get(1).getSpentTransactionId
    if (newTx == null) {
      return null
    }
    getWinningTicketWithR5(newTx, tokenId)
  }

}
