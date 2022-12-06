package mock

import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoToken}
import utils.{InputBoxes, OutBoxes, TransactionHelper}

class sendToProxy(ctx: BlockchainContext, walletMnemonic: String, walletMnemonicPw: String, cometId: String, proxyContract: String) {
  private val txHelper = new TransactionHelper(ctx, walletMnemonic, walletMnemonicPw)
  private val input = new InputBoxes(ctx).getInputs(List(0.001), txHelper.senderAddress, List(List(cometId)), List(List(1)))
  private val outBox = new OutBoxes(ctx).mintBuyerBox(txHelper.senderAddress, Address.create(proxyContract).toErgoContract, new ErgoToken(cometId, 1), 0.001)
  private val unsignedTx = txHelper.buildUnsignedTransaction(input, List(outBox))
  private val signedTx = txHelper.signTransaction(unsignedTx)
  println(txHelper.sendTx(signedTx))
  println(signedTx.getOutputsToSpend.get(0).getId.toString)
}
