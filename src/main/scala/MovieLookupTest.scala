import scalaj.http._
import play.api.libs.json._
import java.io._
import javax.imageio.ImageIO
import scala.io.Source

//最終版 就用這個，這個失敗會重試，並且程序不會中斷。
//666
object MovieLookupTest {
  val apiKey = "f3f756b9be4ad6dadd56d58a3bba074a" // ⚠️ 替换为你的 TMDb Key
  val imageBase = "https://image.tmdb.org/t/p/original"

  // ✅ CSV 转义函数
  def escapeCSV(str: String): String = {
    if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
      "\"" + str.replace("\"", "\"\"") + "\""
    } else str
  }

  // ✅ 下载头像图片（带超时 + 重试）
  def downloadWithRetry(url: String, maxRetries: Int = 3): Option[Array[Byte]] = {
    var attempt = 0
    while (attempt < maxRetries) {
      try {
        val resp = Http(url).timeout(10000, 30000).asBytes
        return Some(resp.body)
      } catch {
        case _: java.net.SocketTimeoutException =>
          println(s"⏱ 第 ${attempt + 1} 次下载超时，重试中...")
          attempt += 1
        case e: Exception =>
          println(s"❌ 下载失败：${e.getMessage}")
          return None
      }
    }
    None
  }

  def main(args: Array[String]): Unit = {
    val movieList: List[(String, String, String)] = Source.fromFile("movie_list.txt", "UTF-8")
      .getLines()
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { line =>
        val pattern = "(.*)\\((\\d{4})\\)".r
        line match {
          case pattern(name, year) => (name.trim, year, line)
          case _ => (line, "", line)
        }
      }.toList

//    val desktopPath = "D:\\MovieData2"
    val desktopPath = "D:\\MovieDataSpark"

    val filePath = desktopPath + "\\movies_cast.csv"
    val writer = new PrintWriter(new File(filePath), "UTF-8")

    println(s"📁 输出文件将保存到：$filePath")

    val header = "电影名称,电影类别," + (1 to 10).flatMap(i => Seq(s"主演$i-演员", s"主演$i-角色")).mkString(",")
    writer.println(header)

    for ((title, year, folderNameRaw) <- movieList) {
      try {
        println(s"\n🎬 正在处理：$title ($year)")

        val baseRequest = Http("https://api.themoviedb.org/3/search/movie")
          .param("api_key", apiKey)
          .param("query", title)

        val searchResponse = {
          if (year.nonEmpty) baseRequest.param("year", year).asString
          else baseRequest.asString
        }

        if (searchResponse.body.trim.isEmpty) {
          println(s"❌ 空响应，跳过：$title")
          val row = (folderNameRaw +: "未知" +: Seq.fill(20)("查询失败")).map(escapeCSV).mkString(",")
          writer.println(row)
          writer.flush() // 确保每次写入都立即保存
        } else {
          val searchJson = Json.parse(searchResponse.body)
          val movieOpt = (searchJson \ "results").asOpt[JsArray].flatMap(_.value.headOption)
          val movieIdOpt = movieOpt.flatMap(result => (result \ "id").asOpt[Int])

          // ✅ 判断是否为动画/动漫
          val genreLabel: String = movieOpt match {
            case Some(movie) =>
              val genreIds = (movie \ "genre_ids").asOpt[JsArray].getOrElse(Json.arr())
              val isAnimation = genreIds.value.exists {
                case JsNumber(id) => id == 16
                case _ => false
              }
              if (isAnimation) "动漫" else "真人"
            case None => "未知"
          }

          val castInfo: Seq[String] = movieIdOpt match {
            case Some(movieId) =>
              val creditsResponse = Http(s"https://api.themoviedb.org/3/movie/$movieId/credits")
                .param("api_key", apiKey)
                .asString

              if (creditsResponse.body.trim.isEmpty) {
                println(s"⚠️ 演员数据为空：$title")
                Seq.fill(20)("无")
              } else {
                val creditsJson = Json.parse(creditsResponse.body)
                val allActors = (creditsJson \ "cast").as[JsArray].value
                val actorsWithPhoto = allActors
                  .filter(actor => (actor \ "profile_path").asOpt[String].isDefined)
                  .take(10)

                val folderName = folderNameRaw.replaceAll("[\\\\/:*?\"<>|]", "_")
                val movieFolder = new File(desktopPath, folderName)
                movieFolder.mkdirs()

                actorsWithPhoto.flatMap { actor =>
                  val name = (actor \ "name").asOpt[String].getOrElse(" ")
                  val role = (actor \ "character").asOpt[String].getOrElse(" ")
                  val profilePath = (actor \ "profile_path").asOpt[String].get
                  val imageUrl = imageBase + profilePath
//                  val outputFile = new File(movieFolder, name.replaceAll("\\s+", "_") + ".jpg")
                  val outputFile = new File(movieFolder, name+".jpg")
                  downloadWithRetry(imageUrl) match {
                    case Some(imageBytes) =>
                      try {
                        val bais = new java.io.ByteArrayInputStream(imageBytes)
                        val bufferedImage = ImageIO.read(bais)
                        if (bufferedImage != null) {
                          ImageIO.write(bufferedImage, "jpg", outputFile)
                          println(s"✅ 已保存：${outputFile.getName}")
                        } else {
                          println(s"⚠️ 无法解析图像：$name")
                        }
                      } catch {
                        case e: Exception =>
                          println(s"❌ 保存失败：${e.getMessage}")
                      }
                    case None =>
                      println(s"❌ 最终下载失败：$imageUrl")
                  }

                  Seq(name, role)
                }.toList
              }

            case None =>
              println(s"⚠️ 找不到电影：$title")
              Seq.fill(20)("未找到")
          }

          val padded = castInfo.padTo(20, "无")
          val escapedRow = (folderNameRaw +: genreLabel +: padded).map(escapeCSV).mkString(",")
          writer.println(escapedRow)
          writer.flush() // 确保每次写入都立即保存
        }
      } catch {
        case e: Exception =>
          println(s"❌ 处理失败：$title，错误信息：${e.getMessage}")
          val row = (folderNameRaw +: "未知" +: Seq.fill(20)("处理失败")).map(escapeCSV).mkString(",")
          writer.println(row)
          writer.flush() // 确保每次写入都立即保存
      }
    }

    writer.close()
    println("\n✅ 所有数据与图片采集完成！")
  }
}
