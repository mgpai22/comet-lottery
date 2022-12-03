package mock

import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoToken}
import utils.{InputBoxes, OutBoxes, TransactionHelper}

class sendToProxy(ctx: BlockchainContext, senderAddress: Address, walletMnemonic: String, walletMnemonicPw: String, cometId: String, proxyContract: String) {
  private val txHelper = new TransactionHelper(ctx, walletMnemonic, walletMnemonicPw)
  private val input = new InputBoxes(ctx).getInputs(List(0.012), senderAddress, List(List(cometId)), List(List(50)))
  private val outBox = new OutBoxes(ctx).mintBuyerBox(senderAddress, Address.create(proxyContract).toErgoContract, new ErgoToken(cometId, 100))
  private val unsignedTx = txHelper.buildUnsignedTransaction(input, List(outBox))
  private val signedTx = txHelper.signTransaction(unsignedTx)
  println(txHelper.sendTx(signedTx))
  println(signedTx.getOutputsToSpend.get(0).getId.toString)

}
