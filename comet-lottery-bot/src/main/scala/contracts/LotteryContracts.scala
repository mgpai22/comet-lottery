package contracts

import enumeratum._

import scala.collection.immutable
import scala.io.Source

sealed trait LotteryContracts extends EnumEntry {
  // Top Folder
  val domain: String = ""
  // Sub Folder
  val domainType: String = ""
  val contractType: ContractType = ContractTypes.None
  val fileExtension: String = ".es"
  val dirName: String = "contracts"
  val version: Long = 0

  lazy val fileName: String = if (version <= 1) {
    this.toString + fileExtension
  } else {
    this.toString + s"_v$version" + fileExtension
  }
  lazy val contractScript: String = get()

  def getPath: String =
    List(dirName, domain, domainType, contractType.plural, fileName)
      .filter(_.nonEmpty)
      .mkString("/")

  def get(): String = {
    val getViaPath: () => String = () => {
      val fullPath: String = getPath
      try {
        val contractSource =
          Source.fromResource(fullPath)

        val contractString = contractSource.mkString
        contractSource.close()

        contractString
      } catch {
        case _: NullPointerException =>
          throw new NullPointerException(s"$fullPath not found")
      }
    }

    val contractString: String = getViaPath()

    contractString
  }
}

object LotteryContracts extends Enum[LotteryContracts] {
  val values: immutable.IndexedSeq[LotteryContracts] = findValues
  case object TicketContract extends TicketBoxGuardScriptContract
  case object CollectionContract extends CollectionBoxGuardScriptContract
  case object WinnerContract extends WinnerContractBoxGuardScriptContract
  case object ProxyContract extends ProxyContractBoxGuardScriptContract
}


sealed trait TicketContract extends LotteryContracts {
  override val domain: String = "Lottery"
}
sealed trait CollectionContract extends LotteryContracts {
  override val domain: String = "Lottery"
}
sealed trait WinnerContract extends LotteryContracts {
  override val domain: String = "Lottery"
}
sealed trait ProxyContract extends LotteryContracts {
  override val domain: String = "Lottery"
}
//</editor-fold>

//<editor-fold desc="Detailed Contract Types">
/**
 * // ===== Detailed Level Contracts =====
 */
sealed trait TicketBoxGuardScriptContract extends TicketContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}
sealed trait CollectionBoxGuardScriptContract extends CollectionContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}
sealed trait WinnerContractBoxGuardScriptContract extends WinnerContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}
sealed trait ProxyContractBoxGuardScriptContract extends ProxyContract {
  override val contractType: ContractType = ContractTypes.ProxyContract
}
//</editor-fold>

//<editor-fold desc="Contract Type Enum">
/**
 * Describes the different contract types as Enums
 */
sealed trait ContractType extends EnumEntry { val plural: String }

object ContractTypes extends Enum[ContractType] {
  val values: immutable.IndexedSeq[ContractType] = findValues

  case object ProxyContract extends ContractType {
    override val plural = "ProxyContracts"
  }

  case object BoxGuardScript extends ContractType {
    override val plural = "BoxGuardScripts"
  }
  case object None extends ContractType { override val plural = "" }
}
//</editor-fold>
