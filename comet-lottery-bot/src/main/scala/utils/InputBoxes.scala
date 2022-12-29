package utils

import org.ergoplatform.appkit._

import java.util
import scala.collection.mutable.ListBuffer

class InputBoxes(val ctx: BlockchainContext) {

  def getBoxById(id: String): Array[InputBox] ={
    this.ctx.getBoxesById(id)
  }

  def getInputs(amountList: List[Double], senderAddress: Address, tokenListParam: List[List[String]] = null, amountTokens: List[List[Long]] = null): util.List[InputBox] ={
    val amountTotal: Long = (Parameters.OneErg * amountList.sum).toLong
    var tokenList = new ListBuffer[ErgoToken]()
    var tokenList1 = new ListBuffer[ListBuffer[ErgoToken]]()
    var tList = new ListBuffer[ErgoToken]()
    var tokenList2 = new util.ArrayList[ErgoToken]()
    var tokenList3 = new util.ArrayList[ErgoToken]()
    var tokenAmountCounter = 0
    if(tokenListParam !=null && amountTokens == null){
      for(token <- tokenListParam){
        for(x <- token){
          val t: ErgoToken =  new ErgoToken(x, 1)
          tokenList.append(t)
        }
      }
    }
    else if(tokenListParam !=null && amountTokens != null){
      for(token <- tokenListParam){
        var tokenAmountCounterLocal = 0
        var tokenAmountList = amountTokens.apply(tokenAmountCounter)
        for(x <- token){
          var tokenAmm = tokenAmountList.apply(tokenAmountCounterLocal)
          tList.append(new ErgoToken(x, tokenAmm))
          tokenAmountCounterLocal = tokenAmountCounterLocal + 1
        }
        tokenAmountCounter = tokenAmountCounter + 1
        tokenList1.append(tList)
        tList = new ListBuffer[ErgoToken]()
      }
      var res: ListBuffer[ErgoToken] =  new ListBuffer[ErgoToken]()
      for(i <- tokenList1){
        res = res ++ i
      }
      for (i <- res) {
        tokenList2.add(i)
      }
    }
    if(tokenListParam !=null && amountTokens == null){
      for (i <- tokenList) {
        tokenList3.add(i)
      }
      return BoxOperations.createForSender(senderAddress, this.ctx).withAmountToSpend(amountTotal)
        .withTokensToSpend(tokenList3).withInputBoxesLoader(new ExplorerAndPoolUnspentBoxesLoader()).loadTop()
    }
    if(tokenListParam == null){
      return BoxOperations.createForSender(senderAddress, this.ctx).withAmountToSpend(amountTotal)
        .withInputBoxesLoader(new ExplorerAndPoolUnspentBoxesLoader()).loadTop()
    }
    return BoxOperations.createForSender(senderAddress, this.ctx).withAmountToSpend(amountTotal)
      .withTokensToSpend(tokenList2).withInputBoxesLoader(new ExplorerAndPoolUnspentBoxesLoader()).loadTop()
  }

}
