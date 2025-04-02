//import play.api.libs.json._
//
//object JsonExample extends App {
//  val jsonStr =
//    """
//      {
//        "results": [
//          {
//            "id": 123,
//            "title": "Test Movie",
//            "genre_ids": [16, 35]
//          },
//          {
//            "id": 456,
//            "title": "Another Movie",
//            "genre_ids": [18]
//          }
//        ]
//      }
//    """
//
//  // 解析 JSON 字符串
//  val parsedJson = Json.parse(jsonStr)
//
//  // 获取 "results" 数组
//  val results: IndexedSeq[JsValue] = (parsedJson \ "results").asOpt[JsArray] match {
//    case Some(arr) => arr.value
//    case None =>
//      println("❌ 未找到结果数组")
//      IndexedSeq.empty
//  }
//
//  // 取第一个结果
//  val firstMovieOpt = results.headOption.flatMap(_.asOpt[JsObject])
//
//  // 获取 ID 和是否是动漫类型（16）
//  firstMovieOpt match {
//    case Some(movie) =>
//      val id = (movie \ "id").asOpt[Int].getOrElse(-1)
//      val genres = (movie \ "genre_ids").asOpt[JsArray].getOrElse(Json.arr())
//      val isAnimation = genres.value.exists {
//        case JsNumber(genreId) => genreId == 16
//        case _ => false
//      }
//
//      println(s"✅ 电影ID: $id")
//      println(s"🎬 类型: ${if (isAnimation) "动漫" else "真人"}")
//
//    case None =>
//      println("⚠️ 未找到电影数据")
//  }
//}
