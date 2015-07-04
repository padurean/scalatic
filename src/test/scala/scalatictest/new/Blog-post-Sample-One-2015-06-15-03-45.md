#Blog post Sample One

Some wise words here :)


## Some random Scala code just to see how it looks

```scala
object Scalatic {
  private def validateArgs(scalaticArgs: Array[String])
      : Option[(String,String,String)] =
    scalaticArgs match {
      case Array(aPath) =>
        Some((noEndSlash(aPath), defaultSource, defaultTarget))
      case Array(aPath, aSource) =>
        Some((noEndSlash(aPath), noEndSlash(aSource), defaultTarget))
      case Array(aPath, aSource, aTarget, _*) =>
        Some((noEndSlash(aPath), noEndSlash(aSource), noEndSlash(aTarget)))
      case _ =>
        None
    }
}
```


##Some section

1. Some ordered list item one

2. Some ordered list item two


## Any GitHub flavored markdown should work

```json
{
  some: "json",
  should: ["be", "highlighted", "nicely"]
  coolstuff: {
    yeah: "!"
  }
}
```
