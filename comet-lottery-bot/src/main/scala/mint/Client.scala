package mint
import node.BaseClient
import mint.DefaultNodeInfo
import configs.{conf, serviceOwnerConf}
import org.ergoplatform.appkit.NetworkType
import utils.network

class Client()
  extends BaseClient(nodeInfo = mint.DefaultNodeInfo(new network(serviceOwnerConf.read("serviceOwner.json").nodeUrl).getNetworkType)) {
}
