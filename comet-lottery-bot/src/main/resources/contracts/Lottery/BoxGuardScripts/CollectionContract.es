  {
  //constants: ticketContractSingleton, winnerSelectionContractSigmaProp
    val winnerSelection = OUTPUTS(0).propositionBytes == winnerSelectionContractSigmaProp
    val timeStamp: Long = SELF.R6[Long].get
    val timePassed = CONTEXT.headers(0).timestamp >= timeStamp
    if (winnerSelection) {//if output goes to winner selection contract
    val issuerBox = getVar[Box](0).get
    val transferToWinnerContract = allOf(
    Coll(
        issuerBox.propositionBytes == SELF.propositionBytes, // issuer box owned by collection contract
        OUTPUTS(0).R4[Coll[Byte]].get == issuerBox.id,
        OUTPUTS(0).R4[Coll[Byte]].get == OUTPUTS(0).R5[Coll[Byte]].get, // id of the issuer box and winner ticket token match
        OUTPUTS(0).R6[Coll[Byte]].get == INPUTS(1).propositionBytes, // r6 contains self address
        OUTPUTS(0).R7[Coll[Byte]].get == SELF.propositionBytes, // r7 contains collection contract address
        OUTPUTS(0).R8[SigmaProp].get.propBytes == INPUTS(1).R6[SigmaProp].get.propBytes, // r8 and r6 are the same distribution address
        OUTPUTS(0).tokens(0)._2 == SELF.tokens(0)._2 // value of self tokens transferred
       )
       )
       sigmaProp(transferToWinnerContract && timePassed) // goes to this sigmaProp if selecting winner
    }
    else {
    val ticketContractToken = OUTPUTS(1).tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == ticketContractSingleton} //ensures ticket contract's singleton is in output
    val index: Long = SELF.R5[Long].get
    val newIndex = OUTPUTS(2).R5[Long].get == index + 1L // new index must be incremented by one
    val timeStampSame = OUTPUTS(2).R6[Long].get == timeStamp
    val newPool = OUTPUTS(2).propositionBytes == SELF.propositionBytes //ensures output recreates pool
    sigmaProp(ticketContractToken && newPool && newIndex && timeStampSame) //goes to this sigmaProp if ticket is bought
    }
  }
