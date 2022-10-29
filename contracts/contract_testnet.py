ticket_script = """ 
  {
  //constants: collectionPoolSigmaProp, proxySigmaProp, winnerSelectionContractSigmaProp type is Coll[Byte], cometToken, priceInComet, ownerPk
    val winnerSelection = OUTPUTS(0).propositionBytes == winnerSelectionContractSigmaProp 
    if (winnerSelection) {//if output goes to winner selection contract
    val transferToWinnerContract = allOf(
    Coll(
       OUTPUTS(0).R4[Box].get.propositionBytes == INPUTS(0).propositionBytes, // issuer box owned by collection contract
       OUTPUTS(0).R4[Box].get.id == OUTPUTS(0).R5[Coll[Byte]].get, // id of the issuer box and winner ticket token match
       OUTPUTS(0).R6[Coll[Byte]].get == SELF.propositionBytes, //INPUTS(1) // r6 contains self address
       OUTPUTS(0).R7[Coll[Byte]].get == INPUTS(0).propositionBytes, // r7 contains collection contract address
       OUTPUTS(0).R8[SigmaProp].get.propBytes == SELF.R6[SigmaProp].get.propBytes, // r8 and r6 are the same distribution address
       OUTPUTS(0).R9[Coll[Long]].get == Coll(SELF.R4[Long].get, SELF.R5[Long].get), // r9 contains a coll of version and ticket index
       OUTPUTS(0).tokens(1)._1 == SELF.tokens(0)._1 // transfers singleton
       )
       )
        sigmaProp(transferToWinnerContract && ownerPk) // goes to this sigmaProp if selecting winner
        // ownerPk ensures only owner can end raffle/start the drawing process
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
"""

winner_script = """ 
  { //constants: cometToken
    val issuerBox = SELF.R4[Box].get
    val selfRaffleWinner = SELF.R5[Coll[Byte]].get
    val selfTicketContract = SELF.R6[Coll[Byte]].get
    val selfCollectionContract = SELF.R7[Coll[Byte]].get
    val selfDistributionAddress = SELF.R8[SigmaProp].get.propBytes
    val validCometTokenInContract = SELF.tokens(0)._1 == cometToken
    val oracleBox = CONTEXT.dataInputs(0) 
    val validOracleBox = true //box must also be verified to contain the oracle nft (not possible on testnet)
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
    
    val collectionLoserBox = allOf(
    Coll(
        OUTPUTS(0).propositionBytes == selfTicketContract, //output creates new ticket contract
        OUTPUTS(0).R4[Long].get == version + 1L, // versioning increases by one
        OUTPUTS(0).R5[Long].get == 1L, //resets counter to one
        SELF.tokens(1)._1 == OUTPUTS(0).tokens(0)._1 //ensures new ticket contract has old ticket contract's singleton
       )
       )
       
    val newCollectionBox = allOf(
    Coll(
       OUTPUTS(1).R4[Long].get == version + 1L, // versioning increases by one
       OUTPUTS(1).R5[Long].get == 1L, //resets counter to one
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
       properNumber
       )
       )
    if (ifWinner) {
    sigmaProp( validate )
    }
    else {
    sigmaProp( validate && collectionLoserBox && newCollectionBox && !chance ) // !chance ensures loser scenario only processes if there is no winner
    }
  }
"""

collection_script = """ 
  {
  //constants: ticketContractSingleton, winnerSelectionContractSigmaProp, ownerPk
    val winnerSelection = OUTPUTS(0).propositionBytes == winnerSelectionContractSigmaProp
    if (winnerSelection) {//if output goes to winner selection contract
    val transferToWinnerContract = allOf(
    Coll(
        OUTPUTS(0).R4[Box].get.propositionBytes == SELF.propositionBytes, // issuer box owned by collection contract
        OUTPUTS(0).R4[Box].get.id == OUTPUTS(0).R5[Coll[Byte]].get, // id of the issuer box and winner ticket token match
        OUTPUTS(0).R6[Coll[Byte]].get == INPUTS(1).propositionBytes, // r6 contains self address
        OUTPUTS(0).R7[Coll[Byte]].get == SELF.propositionBytes, // r7 contains collection contract address
        OUTPUTS(0).R8[SigmaProp].get.propBytes == INPUTS(1).R6[SigmaProp].get.propBytes, // r8 and r6 are the same distribution address
        OUTPUTS(0).tokens(0)._2 == SELF.tokens(0)._2 // value of self tokens transferred 
       )
       )
       sigmaProp(transferToWinnerContract && ownerPk) // goes to this sigmaProp if selecting winner
       // ownerPk ensures only owner can end raffle/start the drawing process
    }
    else {
    val ticketContractToken = OUTPUTS(1).tokens.exists{ (t: (Coll[Byte], Long)) => t._1 == ticketContractSingleton} //ensures ticket contract's singleton is in output
    val index: Long = SELF.R5[Long].get
    val newIndex = OUTPUTS(2).R5[Long].get == index + 1L // new index must be incremented by one
    val newPool = OUTPUTS(2).propositionBytes == SELF.propositionBytes //ensures output recreates pool
    sigmaProp(ticketContractToken && newPool && newIndex) //goes to this sigmaProp if ticket is bought
    }
  }
"""
proxyScript = """ 
  {
  //constants: ticketContractSingleton

    val ticketTokenId = INPUTS(1).tokens(0)._1 // ticket contract singleton
    val buyerPK = SELF.R4[SigmaProp].get 
    val correctReceiver = OUTPUTS(0).propositionBytes == SELF.R4[SigmaProp].get.propBytes //makes sure nft gets to who ever sent this
    val validSale = ticketTokenId == ticketContractSingleton
    val managerRefund = {
        val refundBox = OUTPUTS(0)
        val valueValid = SELF.tokens(0)._2 == refundBox.tokens(0)._2 // value of self minus fees goes to buyer
        val propsValid = refundBox.propositionBytes == SELF.R4[SigmaProp].get.propBytes //makes sure box goes to buyer

        val validManagerRefund = allOf(
        Coll(
          valueValid,
          propsValid
        )
        )
      sigmaProp( validManagerRefund && buyerPK)
    }
    sigmaProp(validSale && correctReceiver) || managerRefund
  }
"""