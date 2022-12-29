{
    //constants: ticketContractSingleton, winnerSelectionContractSigmaPropHash, MaxMinerFee
    // blake2b256 hashes are used to save space

    val winnerSelection = blake2b256(OUTPUTS(0).propositionBytes) == winnerSelectionContractSigmaPropHash
    val timeStamp: Long = SELF.R6[Long].get
    val timePassed = CONTEXT.headers(0).timestamp >= timeStamp
    if (winnerSelection) {//if output goes to winner selection contract

    val transferToWinnerContract = allOf(
    Coll(
        OUTPUTS(0).R5[Coll[Byte]].get == SELF.propositionBytes, // r5 contains collection (self) contract address
        OUTPUTS(0).tokens(0)._2 == SELF.tokens(0)._2, // all comet transferred
        OUTPUTS(0).tokens(1)._1 == ticketContractSingleton // output includes singleton from TicketContractBox
       )
       )
       // INPUTS [CollectionContractBox, TicketContractBox] -> OUTPUTS [WinnerContractBox + singleton + comet, TxFee, ChangeAddress]
       sigmaProp(transferToWinnerContract && timePassed) // goes to this sigmaProp if selecting winner
    }
    else {
    val ticketContractToken = OUTPUTS(1).tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == ticketContractSingleton} //ensures ticket contract's singleton is in output
    val index: Long = SELF.R5[Long].get
    val newIndex = OUTPUTS(2).R5[Long].get == index + 1L // new index must be incremented by one
    val timeStampSame = OUTPUTS(2).R6[Long].get == timeStamp //timestamp does not change, only winner contract can change it
    val newPool = OUTPUTS(2).propositionBytes == SELF.propositionBytes //ensures output recreates pool
    // INPUTS [CollectionContractBox, TicketContractBox, ProxyContractBuyerBox] -> OUTPUTS [BuyerAddress + ticketNFT, TicketContractBox + singleton, CollectionContractBox + comet, TxFee, ChangeAddress]
    sigmaProp(ticketContractToken && newPool && newIndex && timeStampSame) //goes to this sigmaProp if ticket is bought
    }
}
