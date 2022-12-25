package main

import configs.{conf, serviceOwnerConf}
import json.Register.R4
import mint.{Client, DefaultNodeInfo, akkaFunctions}
import mock.sendToProxy
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.{Address, ErgoToken, ErgoValue}
import refund.refundFromProxy
import special.collection.Coll
import special.sigma.SigmaProp
import utils.explorerApi

import java.util.{List => JavaList}
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

object test extends App{
  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val lotteryFilePath = "lotteryConf"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private val lotteryConf = conf.read(lotteryFilePath + ".json")
  private val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  val txId = "45d036e1c1ac54bd89cf8282c57c6c5c61a0565e142b6571c7c1b5b3d9794846"
  val res = exp.getBoxesfromTransaction(txId)
  val winnerContractBox = res.getOutputs.get(0)
  val r9 = exp.getErgoBoxfromID(winnerContractBox.getBoxId).additionalRegisters(ErgoBox.R9).value.asInstanceOf[Coll[Long]](1)
  println(r9)
}

object addressTest extends App{
  val address = Address.create("3WwLBpX7D9eyaV9gWHeaSp22XwEvLQ9huEg9otg8rKXQw4xPtYzq")
  val pubKey = address.getPublicKey
  println(address)
  println(pubKey)
}

object sendToProxy extends App{
  val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  val buyerMnemonic = ""
  val cometId = "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b"
  val proxyContract = conf.read("lotteryConf.json").Lottery.proxyContract.contract
  val submit = new sendToProxy(ctx = ctx, walletMnemonic = buyerMnemonic, walletMnemonicPw = "", cometId = cometId, proxyContract = proxyContract)
}

object akkaTest extends App{
  val akka = new akkaFunctions()
  akka.main()
}
object refundTest extends App{
  val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  val senderAddress = Address.create("3WwVnxYXCTgvULrvfA2w8y21qVVqpjoRwScxwDsx7qSNehXnGVMQ")
  val inputBox = exp.getUnspentBoxFromMempool("9b727e46584a9e6179f7fa2f67f40bfa0494ae5d9562624a7527cd58af1f015b")
  val comet = new ErgoToken(serviceConf.cometId, 100)
  val buyerMnemonic = ""
  val buyerPw = ""
  val ref = new refundFromProxy(ctx, senderAddress = senderAddress, inputBox = List(inputBox), buyerMnemonic, buyerPw, comet, amount = 0.002)
}


object mathTest extends App{
  val num: Double = 0.027000000000000003
  val x = (math floor num * 1000) / 1000
  val bNum = math.BigDecimal(x)
  val y = bNum.underlying().stripTrailingZeros()
  println(y)
}

object test1 extends App{
  val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  val r4 = exp.getBoxbyIDfromExplorer("ad0e1fd878526b8ae0940d2846f61f73b6d9be8dc879c1a5b96552b1e1b7c46a").getAdditionalRegisters.get("R4").serializedValue
  println(r4)
}

object test2 extends App{
  val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  val r4 = exp.getUnspentBoxFromMempool("ad0e1fd878526b8ae0940d2846f61f73b6d9be8dc879c1a5b96552b1e1b7c46a").getRegisters.get(0).toHex
  println(r4)
}


object getTx extends App{
  val client: Client = new Client()
  client.setClient
  val ctx = client.getContext
  private val serviceFilePath = "serviceOwner.json"
  private val serviceConf = serviceOwnerConf.read(serviceFilePath)
  private val lotteryFilePath = "lotteryConf.json"
  private val lotteryConf = conf.read(lotteryFilePath)
  val exp = new explorerApi(DefaultNodeInfo(ctx.getNetworkType).explorerUrl)
  val r4 = exp.getAddressInfo(lotteryConf.Lottery.winnerSelectionContract.contract)
  val spentList = new ListBuffer[String]()
  for (i <- 0 until r4.size()) {
    val item = r4.get(i)
    if(item.getSpentTransactionId != null){
      spentList.append(item.getSpentTransactionId)
    }
  }
  for(tx <- spentList) {
    val boxes = exp.getBoxesfromTransaction(tx)
    val addy = boxes.getOutputs.get(0).getAddress
    if (addy == lotteryConf.Lottery.ticketContract.contract){
    val y = exp.getErgoBoxfromID(boxes.getOutputs.get(0).getBoxId).additionalRegisters(ErgoBox.R4).value.asInstanceOf[Long]
    println(y)
  }
  }
  val initOutput = exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs

  val init = {
    if(initOutput.get(0).getAddress != lotteryConf.Lottery.ticketContract.contract) exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs.get(1).getBoxId
    else exp.getBoxesfromTransaction(lotteryConf.Lottery.initTransaction).getOutputs.get(0).getBoxId
  }



  val einit = exp.getErgoBoxfromID(init).additionalRegisters(ErgoBox.R4).value.asInstanceOf[Long]
  println(einit)
}
