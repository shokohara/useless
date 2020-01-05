package com.github.shokohara.seed

import java.awt.{AWTException, Rectangle, Robot, Toolkit}
import java.io.{File, FileInputStream, IOException}

import javax.imageio.ImageIO
import sun.audio.{AudioPlayer, AudioStream}

object Hello2 {

  val r = new Robot
  val capture = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)

  def main(args: Array[String]): Unit = {
    val f_name = "/button57.wav"
    val in = getClass.getResourceAsStream(f_name)
    val as = new AudioStream(in)
    while (true) {
      try {
        Thread.sleep(120)
        val ext = "png"
        val path = s"Screenshot.$ext"
        val Image = r.createScreenCapture(capture)
        ImageIO.write(Image, ext, new File(path))
        AudioPlayer.player.start(as)
      } catch {
        case ex @ (_: AWTException | _: IOException | _: InterruptedException) => ex.printStackTrace()
      }
    }
  }
}
