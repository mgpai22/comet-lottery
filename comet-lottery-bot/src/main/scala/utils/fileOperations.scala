package utils

import java.io.File
import java.nio.file.{Files, Path, Paths}
object fileOperations{
  def copyFile(sourcePath: String, destinationPath: String): Unit ={
    val sPath = Paths.get(sourcePath)
    val dPath = Paths.get(destinationPath)
    Files.copy(sPath, dPath)
  }

  def deleteFile(path: String): Unit ={
    val filePath = Paths.get(path)
    Files.delete(filePath)
  }

  def getLargestFileName(dir: String): Int = {
    val files = new File(dir).listFiles
    val names = files.filter(_.getName.contains("_")).map(_.getName)
    if (names.nonEmpty) {
      val numbers = names.map(_.split("_+")(1))
      val validNumbers = numbers.map(_.replace(".json", ""))
      if (validNumbers.nonEmpty) {
        val maxNumber = validNumbers.map(_.toInt).max
        if (names.exists(_.contains(maxNumber.toString))) {
          maxNumber
        } else {
          -1
        }
      } else {
        -1
      }
    } else {
      -1
    }
  }
}
