package mint
import configs.serviceOwnerConf
import node.BaseClient
import utils.network

class Client()
  extends BaseClient(nodeInfo = mint.DefaultNodeInfo(new network(serviceOwnerConf.read("serviceOwner.json").nodeUrl).getNetworkType)) {
}
