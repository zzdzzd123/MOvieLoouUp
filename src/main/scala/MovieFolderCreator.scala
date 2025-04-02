import java.io.{File, PrintWriter}
import scala.io.Source

object MovieFolderCreator {
  def main(args: Array[String]): Unit = {
    // ✅ 从 movie_list.txt 读取电影名
    val inputFile = "movie_list.txt"
    val movieList = Source.fromFile(inputFile, "UTF-8").getLines().map(_.trim).filter(_.nonEmpty).toList

    // ✅ 设置文件夹父目录（你可以修改为你想保存的位置）
    val basePath = "D:\\MoviesData"  // ⚠️ 修改为你想创建的根目录

    val baseDir = new File(basePath)
    if (!baseDir.exists()) {
      baseDir.mkdirs()
      println(s"✅ 创建根目录：$basePath")
    }

    // ✅ 为每个电影名创建文件夹
    for (title <- movieList) {
      val safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_") // 替换非法字符
      val movieDir = new File(baseDir, safeTitle)

      if (!movieDir.exists()) {
        val success = movieDir.mkdirs()
        if (success) {
          println(s"📁 创建成功：${movieDir.getAbsolutePath}")
        } else {
          println(s"❌ 创建失败：${movieDir.getAbsolutePath}")
        }
      } else {
        println(s"⚠️ 已存在：${movieDir.getAbsolutePath}")
      }
    }

    println("\n✅ 所有文件夹创建完成！")
  }
}
