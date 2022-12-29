{
    //constants: collectionPoolSigmaPropHash, proxySigmaPropHash, winnerSelectionContractSigmaPropHash type is Coll[Byte], cometToken, priceInComet, MaxMinerFee
    // blake2b256 hashes are used to save space

    val winnerSelection = blake2b256(OUTPUTS(0).propositionBytes) == winnerSelectionContractSigmaPropHash
    val timeStamp: Long = SELF.R7[Long].get
    val timePassed = CONTEXT.headers(0).timestamp >= timeStamp

    if (winnerSelection) {//if output goes to winner selection contract

        val txFeeHash = fromBase16("e540cceffd3b8dd0f401193576cc413467039695969427df94454193dddfb375") //hashed address of txFee script
        val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (blake2b256(o.propositionBytes) == txFeeHash) o.value else 0L
        // the above var takes the sum of the value of all boxes that go to the txFee script and ensures that the total value is less than or equal to the MaxMinerFee
        //this prevents a bidding war
        }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

        val transferToWinnerContract = allOf(
            Coll(
                OUTPUTS(0).R4[Coll[Byte]].get == SELF.propositionBytes, //r4 contains self address
                OUTPUTS(0).R6[SigmaProp].get.propBytes == SELF.R6[SigmaProp].get.propBytes, // r6 and SELF.R6 are the same distribution address
                OUTPUTS(0).R7[Coll[Long]].get == Coll(SELF.R4[Long].get, SELF.R5[Long].get), // r7 contains a coll of version, ticket index
                OUTPUTS(0).tokens(1)._1 == SELF.tokens(0)._1, // transfers singleton
                OUTPUTS(0).tokens(0)._1 == cometToken, //ensures output contains comet
                OUTPUTS(0).tokens(0)._2 == SELF.R8[Long].get, //ensures proper comet output
                validMinerFee
            )
        )
        // INPUTS [CollectionContractBox + comet, TicketContractBox (SELF) + singleton] -> OUTPUTS [WinnerContractBox + singleton + comet, TxFee, ChangeAddress]
        sigmaProp(transferToWinnerContract && timePassed) // goes to this sigmaProp if selecting winner
    }
    else {
        val ticketBox = OUTPUTS(0).tokens.size > 0 // nft must be there in first output
        val cometOutput = OUTPUTS(2).tokens(0)._1 == cometToken // the token paid is comet token
        val cometPoolValue = INPUTS(0).tokens(0)._2 // amount of comet based on how much box holds
        val cometPoolValueFromReg = SELF.R8[Long].get //amount of comet based on data stored in registers
        val validCometPoolValue = cometPoolValue == cometPoolValueFromReg //values should match
        val proxyUsed = blake2b256(INPUTS(2).propositionBytes) == proxySigmaPropHash //ensures that the proxy contract is used
        val properInput = blake2b256(INPUTS(0).propositionBytes) == collectionPoolSigmaPropHash //ensures pool is used as input
        val poolValue = INPUTS(0).value //amount of ERG in the collection pool
        val version: Long = SELF.R4[Long].get // current version
        val index: Long = SELF.R5[Long].get // current index
        val outBoxCheck = allOf(
            Coll(
                OUTPUTS(1).propositionBytes == SELF.propositionBytes, // creates new ticket contract (self)
                OUTPUTS(1).R4[Long].get == version, //version does not change, only winner contract can change it
                OUTPUTS(1).R7[Long].get == timeStamp, //timestamp does not change, only winner contract can change it
                OUTPUTS(1).R5[Long].get == index + 1L, // new index must be incremented by one
                OUTPUTS(1).R6[SigmaProp].get.propBytes == SELF.R6[SigmaProp].get.propBytes, // distribution address does not change
                OUTPUTS(1).tokens(0)._1 == SELF.tokens(0)._1, //singleton transfers to created ticketContract
                OUTPUTS(2).tokens(0)._2 == cometPoolValue + priceInComet, //new collection contract has old value + price of tickets
                cometOutput,
                validCometPoolValue,
                OUTPUTS(1).R8[Long].get == cometPoolValue + priceInComet, //data of comet amount gets written to register to new output
                blake2b256(OUTPUTS(2).propositionBytes) == collectionPoolSigmaPropHash, //payment box must go to pool
                OUTPUTS(2).R4[Long].get == version, //ensures correct version in collection contract
                OUTPUTS(2).R5[Long].get == index + 1L //ensures correct index in collection contract
            )
        )
        // INPUTS [CollectionContractBox + comet, TicketContractBox (SELF) + singleton, ProxyContractBuyerBox + comet] -> OUTPUTS [BuyerAddress + ticketNFT, TicketContractBox + singleton, CollectionContractBox + comet, TxFee, ChangeAddress]
        sigmaProp(ticketBox && proxyUsed && properInput && outBoxCheck) //goes to this sigmaProp if ticket is bought
    }

}
