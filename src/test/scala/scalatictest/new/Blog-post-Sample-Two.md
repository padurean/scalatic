#Blog post Sample Two

Some wise words here :)


## Some random Scala code just to see how it looks

```scala
object Scalatic {
  def render(file: Path, header: String, footer: String): String = {
    val srcFilePath: String = file.toString
    println(s"\nRendering $srcFilePath ...")
    val markdown = stringFromFile(srcFilePath)
    val html = Http(GHMDRendererUrl)
      .postData(markdown)
      .header("Content-Type", "text/plain")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(10000))
      .asString.body
    val htmlFull = s"$header\n$html\n$footer"
    htmlFull
  }
}
```


##Some section

1. Some ordered list item one

2. Some ordered list item two
