  {
  //constants: ticketContractSingleton
  val buyerPK = SELF.R4[SigmaProp].get
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
  if(INPUTS.size == 1){
    managerRefund
  }
  else {
    val ticketTokenId = INPUTS(1).tokens(0)._1 // ticket contract singleton
    val correctReceiver = OUTPUTS(0).propositionBytes == SELF.R4[SigmaProp].get.propBytes //makes sure nft gets to who ever sent this
    val validSale = ticketTokenId == ticketContractSingleton
    sigmaProp(validSale && correctReceiver)
  }
  }
