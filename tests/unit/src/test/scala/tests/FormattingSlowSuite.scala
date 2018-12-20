package tests

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object FormattingSlowSuite extends BaseSlowSuite("formatting") {

  testAsync("basic") {
    for {
      _ <- server.initialize(
        """|/.scalafmt.conf
           |maxColumn = 100
           |/a/src/main/scala/a/Main.scala
           |object FormatMe {
           | val x = 1  }
           |""".stripMargin,
        expectError = true
      )
      _ <- server.didOpen("a/src/main/scala/a/Main.scala")
      _ <- server.formatting("a/src/main/scala/a/Main.scala")
      // check that the file has been formatted
      _ = assertNoDiff(
        server.bufferContent("a/src/main/scala/a/Main.scala").get,
        """|object FormatMe {
           |  val x = 1
           |}""".stripMargin
      )
    } yield ()
  }

  testAsync("require-config") {
    for {
      _ <- server.initialize(
        """|/a/src/main/scala/a/Main.scala
           |object FormatMe {
           | val x = 1  }
           |""".stripMargin,
        expectError = true
      )
      _ <- server.didOpen("a/src/main/scala/a/Main.scala")
      _ <- server.formatting("a/src/main/scala/a/Main.scala")
      // check that the formatting request has been ignored
      _ = assertNoDiff(
        server.bufferContent("a/src/main/scala/a/Main.scala").get,
        """|object FormatMe {
           | val x = 1  }
           |""".stripMargin
      )
    } yield ()
  }

  testAsync("custom-config-path") {
    for {
      _ <- server.initialize(
        """|/project/.scalafmt.conf
           |maxColumn=100
           |/a/src/main/scala/a/Main.scala
           |object FormatMe {
           | val x = 1  }
           |""".stripMargin,
        expectError = true
      )
      _ <- server.didOpen("a/src/main/scala/a/Main.scala")
      _ <- {
        val config = new JsonObject
        config.add(
          "scalafmt-config-path",
          new JsonPrimitive("project/.scalafmt.conf")
        )
        server.didChangeConfiguration(config.toString)
      }
      _ <- server.formatting("a/src/main/scala/a/Main.scala")
      _ = assertNoDiff(
        server.bufferContent("a/src/main/scala/a/Main.scala").get,
        """|object FormatMe {
           |  val x = 1
           |}""".stripMargin
      )
    } yield ()
  }

  testAsync("version") {
    for {
      _ <- server.initialize(
        """|.scalafmt.conf
           |version=1.6.0-RC4
           |maxColumn=30
           |trailingCommas=never
           |/a/src/main/scala/a/Main.scala
           |case class User(
           |  name: String,
           |  age: Int,
           |)""".stripMargin,
        expectError = true
      )
      _ <- server.didOpen("a/src/main/scala/a/Main.scala")
      _ <- server.formatting("a/src/main/scala/a/Main.scala")
      // check that the file has been formatted respecting the trailing comma config (new in 1.6.0)
      _ = assertNoDiff(
        server.bufferContent("a/src/main/scala/a/Main.scala").get,
        """|case class User(
           |    name: String,
           |    age: Int
           |)""".stripMargin
      )
    } yield ()
  }

}
