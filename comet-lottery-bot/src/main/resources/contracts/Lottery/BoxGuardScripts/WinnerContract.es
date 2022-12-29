{
    //constants: cometToken, timeToWaitMS, oracleNFT, winnerChance, MaxMinerFee
    // blake2b256 hashes are used to save space

    val issuerBox = getVar[Box](0).get
    val selfTicketContract = SELF.R4[Coll[Byte]].get
    val selfCollectionContract = SELF.R5[Coll[Byte]].get
    val selfDistributionAddress = SELF.R6[SigmaProp].get.propBytes
    val version = SELF.R7[Coll[Long]].get(0)
    val totalSoldTickets = SELF.R7[Coll[Long]].get(1) - 1L
    val oracleBox = CONTEXT.dataInputs(0)
    val winnerTicketBox = CONTEXT.dataInputs(1) //box in which the winning ticket is inside of


    val validCometTokenInContract = SELF.tokens(0)._1 == cometToken
    val validOracleBox = oracleBox.tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == oracleNFT} //ensures the box comes from the oracle


    val totalSoldTicketBI = totalSoldTickets.toBigInt
    // generates a random number in range of the total sold tickets based on oracle box id
    val winNumber = ((((byteArrayToBigInt(oracleBox.id.slice(0, 15)).toBigInt % totalSoldTicketBI) + totalSoldTicketBI) % totalSoldTicketBI).toBigInt) + 1 // slice is (inclusive, exclusive)
    val chanceNumber = ((((byteArrayToBigInt(oracleBox.id.slice(0, 15)).toBigInt % 100) + 100) % 100).toBigInt) //generates number between 0 and 100 inclusive based on oracle box id
    val chance = chanceNumber <= winnerChance.toBigInt

    val winnerAmount = ((SELF.tokens(0)._2 * 900L) / 1000L) // 90% of comet goes to winner
    val distributionAmount = ((SELF.tokens(0)._2 * 100L) / 1000L) //10 % of comet does to distribution

    val properIssuerBox = issuerBox.propositionBytes == selfCollectionContract
    val properVersion = issuerBox.R4[Long].get == version
    val properNumber = issuerBox.R5[Long].get == winNumber

    val txFeeHash = fromBase16("e540cceffd3b8dd0f401193576cc413467039695969427df94454193dddfb375") //hashed address of txFee script
    val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (blake2b256(o.propositionBytes) == txFeeHash) o.value else 0L
    // the above var takes the sum of the value of all boxes that go to the txFee script and ensures that the total value is less than or equal to the MaxMinerFee
    //this prevents a bidding war
    }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

    val winnerBox = allOf(
    Coll(
         winnerTicketBox.tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == issuerBox.id}, //ensures box has winning ticket
         OUTPUTS(0).propositionBytes == winnerTicketBox.propositionBytes, //box must go to owner of ticket
         OUTPUTS(0).R4[Coll[Byte]].get == issuerBox.id, // winner box must have the winning NFT id (used for external api)
         OUTPUTS(0).tokens(0)._2 == winnerAmount, //box must have 90% of self ERGS minus fees
         OUTPUTS(1).tokens(0)._2 == distributionAmount, // box must have 10% of self ERGS
         OUTPUTS(1).propositionBytes == selfDistributionAddress //box must go to distribution
       )
       )
    val ifWinner = chance && winnerBox

    val currentTime = CONTEXT.headers(0).timestamp
    val minTime = currentTime + timeToWaitMS // this is the new time when the next winner will be selected
    val errorTime = 5 * 60 * 1000 // additional time can deviate by 5 minutes

    val timeOutput = OUTPUTS(0).R7[Long].get
    val timeOutputCondition = (timeOutput >= (minTime - errorTime)) && (timeOutput <= (minTime + errorTime)) // allows for a buffer since timestamps are based on the last block

    val timeOutputCollection = OUTPUTS(1).R6[Long].get
    val timeOutputConditionCollection = (timeOutputCollection >= (minTime - errorTime)) && (timeOutputCollection <= (minTime + errorTime))

    val newTicketContractBox = allOf(
    Coll(
        OUTPUTS(0).propositionBytes == selfTicketContract, //output creates new ticket contract
        OUTPUTS(0).R4[Long].get == version + 1L, // versioning increases by one
        OUTPUTS(0).R5[Long].get == 1L, //resets counter to one
        OUTPUTS(0).R6[SigmaProp].get.propBytes == selfDistributionAddress, // distribution address does not change
        timeOutputCondition,
        SELF.tokens(1)._1 == OUTPUTS(0).tokens(0)._1 //ensures new ticket contract has old ticket contract's singleton
       )
       )

    val newCollectionBox = allOf(
    Coll(
       OUTPUTS(1).R4[Long].get == version + 1L, // versioning increases by one
       OUTPUTS(1).R5[Long].get == 1L, //resets counter to one
       timeOutputConditionCollection,
       OUTPUTS(1).propositionBytes == selfCollectionContract, // ensures new collection contract
       OUTPUTS(1).tokens(0)._2 == SELF.tokens(0)._2 //pool gets the comet back
       )
       )

    val validate = allOf(
    Coll(
       validOracleBox,
       validCometTokenInContract,
       properIssuerBox,
       properVersion,
       properNumber,
       validMinerFee
       )
       )
    if (ifWinner) {
    // INPUT [WinnerContractBox  (SELF)] -> OUTPUTS [WinnerAddress, DistributionAddress, TxFee, ChangeAddress]
    // DataInput [OracleBox, WinnerTicketBox]
    // ContextVar [IssuerBox]
    sigmaProp( validate )
    }
    else {
    // INPUT [WinnerContractBox  (SELF)] -> OUTPUTS [TicketContractBox, CollectionContractBox, TxFee, ChangeAddress]
    // DataInput [OracleBox, WinnerTicketBox]
    // ContextVar [IssuerBox]
    sigmaProp( validate && newTicketContractBox && newCollectionBox && !chance ) // !chance ensures loser scenario only processes if there is no winner
    }
}
