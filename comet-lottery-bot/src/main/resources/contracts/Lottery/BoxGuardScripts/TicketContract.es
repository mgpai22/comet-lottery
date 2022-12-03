{
    //constants: collectionPoolSigmaProp, proxySigmaProp, winnerSelectionContractSigmaProp type is Coll[Byte], cometToken, priceInComet
    val winnerSelection = OUTPUTS(0).propositionBytes == winnerSelectionContractSigmaProp
    val timeStamp: Long = SELF.R7[Long].get
    val timePassed = CONTEXT.headers(0).timestamp >= timeStamp
    if (winnerSelection) {//if output goes to winner selection contract
        val issuerBox = getVar[Box](0).get
        val transferToWinnerContract = allOf(
            Coll(
                issuerBox.propositionBytes == INPUTS(0).propositionBytes, // issuer box owned by collection contract
                OUTPUTS(0).R4[Coll[Byte]].get == issuerBox.id, //ensure R4 contain issuer box's id
                OUTPUTS(0).R4[Coll[Byte]].get == OUTPUTS(0).R5[Coll[Byte]].get, // id of the issuer box and winner ticket token match
                OUTPUTS(0).R6[Coll[Byte]].get == SELF.propositionBytes, //INPUTS(1) // r6 contains self address
                OUTPUTS(0).R7[Coll[Byte]].get == INPUTS(0).propositionBytes, // r7 contains collection contract address
                OUTPUTS(0).R8[SigmaProp].get.propBytes == SELF.R6[SigmaProp].get.propBytes, // r8 and r6 are the same distribution address
                OUTPUTS(0).R9[Coll[Long]].get == Coll(SELF.R4[Long].get, SELF.R5[Long].get), // r9 contains a coll of version, ticket index
                OUTPUTS(0).tokens(1)._1 == SELF.tokens(0)._1 // transfers singleton
            )
        )
        sigmaProp(transferToWinnerContract && timePassed) // goes to this sigmaProp if selecting winner
    }
    else {
        val ticketBox = OUTPUTS(0).tokens.size > 0 // nft must be there in first output
        val cometOutput = OUTPUTS(2).tokens(0)._1 == cometToken // the token paid is comet token
        val cometPoolValue = INPUTS(0).tokens(0)._2
        val proxyUsed = INPUTS(2).propositionBytes == proxySigmaProp //ensures that the proxy contract is used
        val properInput = INPUTS(0).propositionBytes == collectionPoolSigmaProp //ensures pool is used as input
        val poolValue = INPUTS(0).value //amount of ERG in the collection pool
        val version: Long = SELF.R4[Long].get // current version
        val index: Long = SELF.R5[Long].get // current index
        val outBoxCheck = allOf(
            Coll(
                OUTPUTS(1).propositionBytes == SELF.propositionBytes, // creates new ticket contract (self)
                OUTPUTS(1).R4[Long].get == version, //version does not change, only winner contract can change
                OUTPUTS(1).R7[Long].get == timeStamp,
                OUTPUTS(1).R5[Long].get == index + 1L, // new index must be incremented by one
        OUTPUTS(1).R6[SigmaProp].get.propBytes == SELF.R6[SigmaProp].get.propBytes, // distribution address does not change
        OUTPUTS(1).tokens(0)._1 == SELF.tokens(0)._1, //new ticket contract box has the same token as the current contract
        OUTPUTS(2).tokens(0)._2 >= cometPoolValue + priceInComet, //new collection contract has old value + price of tickets
            cometOutput,
        OUTPUTS(2).propositionBytes == collectionPoolSigmaProp, //payment box must go to pool
        OUTPUTS(2).R4[Long].get == version, //ensures correct version in collection contract
        OUTPUTS(2).R5[Long].get == index + 1L //ensures correct index in collection contract
    )
    )
        sigmaProp((ticketBox && proxyUsed && properInput && outBoxCheck )) //goes to this sigmaProp if ticket is bought
    }

}
