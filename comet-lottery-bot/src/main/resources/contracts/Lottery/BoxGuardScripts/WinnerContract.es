  { //constants: cometToken, timeToWaitMS, and oracleNFT
    val issuerBox = getVar[Box](0).get
    val validIssuerBox = SELF.R4[Coll[Byte]].get == issuerBox.id
    val selfRaffleWinner = SELF.R5[Coll[Byte]].get
    val selfTicketContract = SELF.R6[Coll[Byte]].get
    val selfCollectionContract = SELF.R7[Coll[Byte]].get
    val selfDistributionAddress = SELF.R8[SigmaProp].get.propBytes
    val validCometTokenInContract = SELF.tokens(0)._1 == cometToken
    val oracleBox = CONTEXT.dataInputs(0)
    val validOracleBox = oracleBox.tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == oracleNFT} //ensures the box comes from the oracle
    val winnerTicketBox = CONTEXT.dataInputs(1)
    val totalSoldTickets = SELF.R9[Coll[Long]].get(1) - 1L
    val version = SELF.R9[Coll[Long]].get(0)
    val totalSoldTicketBI = totalSoldTickets.toBigInt
    val winNumber = ((((byteArrayToBigInt(oracleBox.id.slice(0, 15)).toBigInt % totalSoldTicketBI) + totalSoldTicketBI) % totalSoldTicketBI).toBigInt) + 1 // slice is (inclusive, exclusive)
    val chance = (((winNumber - 1).toBigInt % totalSoldTicketBI) * 1000) < ((totalSoldTickets * 100L).toBigInt) // represents 10% chance
    val winnerAmount = ((SELF.tokens(0)._2 * 900L) / 1000L) // 90% of comet goes to winner
    val distributionAmount = ((SELF.tokens(0)._2 * 100L) / 1000L) //10 % of comet does to distribution
    val properIssuerBox = issuerBox.propositionBytes == selfCollectionContract
    val properVersion = issuerBox.R4[Long].get == version
    val properNumber = issuerBox.R5[Long].get == winNumber
    val winnerBox = allOf(
    Coll(
         winnerTicketBox.tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == selfRaffleWinner}, //ensures box has winning ticket
         OUTPUTS(0).propositionBytes == winnerTicketBox.propositionBytes, //box must go to owner of ticket
         OUTPUTS(0).tokens(0)._2 == winnerAmount, //box must have 90% of self ERGS minus fees
         OUTPUTS(1).tokens(0)._2 == distributionAmount, // box must have 10% of self ERGS
         OUTPUTS(1).propositionBytes == selfDistributionAddress //box must go to distribution
       )
       )
    val ifWinner = chance && winnerBox

    val currentTime = CONTEXT.headers(0).timestamp
    val minTime = currentTime + timeToWaitMS
    val errorTime = 5 * 60 * 1000 // additional time can deviate by 5 minutes

    val timeOutput = OUTPUTS(0).R7[Long].get
    val timeOutputCondition = (timeOutput >= (minTime - errorTime)) && (timeOutput <= (minTime + errorTime))

    val timeOutputCollection = OUTPUTS(1).R6[Long].get
    val timeOutputConditionCollection = (timeOutputCollection >= (minTime - errorTime)) && (timeOutputCollection <= (minTime + errorTime))

    val collectionLoserBox = allOf(
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
       validIssuerBox
       )
       )
    if (ifWinner) {
    sigmaProp( validate )
    }
    else {
    sigmaProp( validate && collectionLoserBox && newCollectionBox && !chance ) // !chance ensures loser scenario only processes if there is no winner
    }
  }
