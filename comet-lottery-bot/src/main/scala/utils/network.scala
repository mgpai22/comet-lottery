package utils

import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.restapi.client.{ApiClient, InfoApi}

class network(nodeUrl: String) {
  def buildNodeService(nodeUrl: String): ApiClient = {
    new ApiClient(nodeUrl)
  }

  def getNetworkType: NetworkType = {
    val nodeService = this.buildNodeService(this.nodeUrl).createService(classOf[InfoApi])
    val networkType = nodeService.getNodeInfo.execute().body().getNetwork.toLowerCase()
    if (networkType.equals("testnet")) {
      return NetworkType.TESTNET
    }
    NetworkType.MAINNET
  }

}
