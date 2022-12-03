package mint
import node.{DefaultNodeInfo, MainNetNodeExplorerInfo, NodeInfo, TestNetNodeExplorerInfo}
import org.ergoplatform.appkit.NetworkType
import configs.{conf, serviceOwnerConf}


case class DefaultNodeInfo( networkType: NetworkType) extends NodeInfo(
  mainNetNodeExplorerInfo = MainNetNodeExplorerInfo(
    mainnetNodeUrl = serviceOwnerConf.read("serviceOwner.json").nodeUrl,
    mainnetExplorerUrl = serviceOwnerConf.read("serviceOwner.json").apiUrl
  ),
  testNetNodeExplorerInfo = TestNetNodeExplorerInfo(
    testnetNodeUrl = serviceOwnerConf.read("serviceOwner.json").nodeUrl,
    testnetExplorerUrl = serviceOwnerConf.read("serviceOwner.json").apiUrl
  ),
  networkType = networkType
)
