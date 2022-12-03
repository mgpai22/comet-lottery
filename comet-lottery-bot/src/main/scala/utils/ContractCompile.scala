package utils

import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoContract, ErgoToken}

class ContractCompile(ctx: BlockchainContext) {

  def compileWinnerContract(contract: String, comet: ErgoToken, oracleNFT: ErgoToken, timeToWaitMS: Long): ErgoContract = {
    this.ctx.compileContract(ConstantsBuilder.create().item("cometToken", comet.getId.getBytes).item("timeToWaitMS", timeToWaitMS).item("oracleNFT", oracleNFT.getId.getBytes)
      .build(),
      contract)
  }

  def compileProxyContract(contract: String, ticketContractSingleton: ErgoToken): ErgoContract ={
    this.ctx.compileContract(ConstantsBuilder.create().item("ticketContractSingleton", ticketContractSingleton.getId.getBytes).build(),
      contract)
  }

  def compileCollectionContract(contract: String, ticketContractSingleton: ErgoToken, winnerSelectionContract: Address): ErgoContract ={
    this.ctx.compileContract(ConstantsBuilder.create().item("ticketContractSingleton", ticketContractSingleton.getId.getBytes).item(
      "winnerSelectionContractSigmaProp", winnerSelectionContract.asP2S().scriptBytes).build(),
      contract)
  }

  def compileTicketContract(contract: String, collectionContract: Address, proxyContract: Address, winnerSelectionContract: Address, comet: ErgoToken, priceInComet: Long): ErgoContract ={
    this.ctx.compileContract(ConstantsBuilder.create().item("collectionPoolSigmaProp", collectionContract.asP2S().scriptBytes).item("proxySigmaProp",
      proxyContract.asP2S().scriptBytes)
      .item("winnerSelectionContractSigmaProp", winnerSelectionContract.asP2S().scriptBytes).item("cometToken",
      comet.getId.getBytes).item(
      "priceInComet", priceInComet).build(),
      contract)
  }
}
