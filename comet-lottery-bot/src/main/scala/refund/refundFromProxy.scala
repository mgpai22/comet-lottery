package refund

import org.ergoplatform.appkit._
import utils.{OutBoxes, TransactionHelper}

import scala.collection.JavaConverters._

class refundFromProxy(ctx: BlockchainContext, senderAddress: Address, inputBox: List[InputBox], buyerMnemonic: String, buyerPw: String,  comet: ErgoToken, amount: Double) {
  private val txHelper = new TransactionHelper(ctx, buyerMnemonic, buyerPw)

  def returnUnsigned(): UnsignedTransaction = {
    val outBox = new OutBoxes(ctx).refundBox(comet, senderAddress = senderAddress, amount)
    txHelper.buildUnsignedTransaction(inputBox.asJava, List(outBox))
  }

}
