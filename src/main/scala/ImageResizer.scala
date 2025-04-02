import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D}
import java.io.File
import javax.imageio.ImageIO

object ImageResizer {
  val MIN_WIDTH = 640
  val MIN_HEIGHT = 640

  val validExtensions = Set("jpg", "jpeg", "png", "bmp", "webp")

  def main(args: Array[String]): Unit = {
    val folder = new File("D:/郑志德/")
//    val folder = new File("D:/MoviesData")
    if (!folder.exists() || !folder.isDirectory) {
      println(s"❌ 路径无效：${folder.getAbsolutePath}")
      return
    }

    processFolder(folder)
    println("\n✅ 所有图片处理完成！")
  }

  def processFolder(dir: File): Unit = {
    for (file <- dir.listFiles()) {
      if (file.isDirectory) {
        processFolder(file)
      } else if (isImage(file)) {
        processImage(file)
      }
    }
  }

  def isImage(file: File): Boolean = {
    val name = file.getName.toLowerCase
    validExtensions.exists(ext => name.endsWith("." + ext))
  }

  def processImage(file: File): Unit = {
    try {
      val original = ImageIO.read(file)
      if (original == null) {
        println(s"⚠️ 无法读取图像：${file.getName}")
        return
      }

      val w = original.getWidth
      val h = original.getHeight

      if (w >= MIN_WIDTH && h >= MIN_HEIGHT) {
        println(s"✅ 已满足：${file.getName} (${w}x${h})")
        return
      }

      // 等比例放大
      val scale = math.max(MIN_WIDTH.toDouble / w, MIN_HEIGHT.toDouble / h)
      val newW = (w * scale).ceil.toInt
      val newH = (h * scale).ceil.toInt

      val resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
      val g1 = resized.createGraphics()
      g1.drawImage(original, 0, 0, newW, newH, null)
      g1.dispose()

      // 创建黑色背景画布，并居中放置
      val finalW = math.max(newW, MIN_WIDTH)
      val finalH = math.max(newH, MIN_HEIGHT)

      val finalImage = new BufferedImage(finalW, finalH, BufferedImage.TYPE_INT_RGB)
      val g2 = finalImage.createGraphics()
      g2.setColor(Color.BLACK)
      g2.fillRect(0, 0, finalW, finalH)

      val x = (finalW - newW) / 2
      val y = (finalH - newH) / 2
      g2.drawImage(resized, x, y, null)
      g2.dispose()

      ImageIO.write(finalImage, "jpg", file)
      println(s"🔧 已修正：${file.getName} → ${finalW}x${finalH}")

    } catch {
      case e: Exception =>
        println(s"❌ 处理失败：${file.getName}，原因：${e.getMessage}")
    }
  }
}
