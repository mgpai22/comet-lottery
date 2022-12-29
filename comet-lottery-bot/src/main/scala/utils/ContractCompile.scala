package utils

import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ConstantsBuilder,
  ErgoContract,
  ErgoToken
}
import scorex.crypto.hash
import scorex.util.encode.Base16

class ContractCompile(ctx: BlockchainContext) {

  def compileWinnerContract(
      contract: String,
      comet: ErgoToken,
      oracleNFT: ErgoToken,
      timeToWaitMS: Long,
      winnerChance: Int,
      MaxMinerFee: Long
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("cometToken", comet.getId.getBytes)
        .item("timeToWaitMS", timeToWaitMS)
        .item("oracleNFT", oracleNFT.getId.getBytes)
        .item("winnerChance", winnerChance)
        .item("MaxMinerFee", MaxMinerFee)
        .build(),
      contract
    )
  }

  def compileProxyContract(
      contract: String,
      ticketContractSingleton: ErgoToken
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("ticketContractSingleton", ticketContractSingleton.getId.getBytes)
        .build(),
      contract
    )
  }

  def compileCollectionContract(
      contract: String,
      ticketContractSingleton: ErgoToken,
      winnerSelectionContract: Address,
      MaxMinerFee: Long
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item("ticketContractSingleton", ticketContractSingleton.getId.getBytes)
        .item(
          "winnerSelectionContractSigmaPropHash",
          hash.Blake2b256(winnerSelectionContract.asP2S().scriptBytes)
        )
        .item("MaxMinerFee", MaxMinerFee)
        .build(),
      contract
    )
  }

  def compileTicketContract(
      contract: String,
      collectionContract: Address,
      proxyContract: Address,
      winnerSelectionContract: Address,
      comet: ErgoToken,
      priceInComet: Long,
      MaxMinerFee: Long
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "collectionPoolSigmaPropHash",
          hash.Blake2b256(collectionContract.asP2S().scriptBytes)
        )
        .item(
          "proxySigmaPropHash",
          hash.Blake2b256(proxyContract.asP2S().scriptBytes)
        )
        .item(
          "winnerSelectionContractSigmaPropHash",
          hash.Blake2b256(winnerSelectionContract.asP2S().scriptBytes)
        )
        .item("cometToken", comet.getId.getBytes)
        .item("priceInComet", priceInComet)
        .item("MaxMinerFee", MaxMinerFee)
        .build(),
      contract
    )
  }
}
