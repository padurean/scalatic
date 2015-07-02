#Scalatic

**!IMPORTANT**: at this moment this project is a WiP.

Dead simple static blog generator written in Scala with minimum dependencies.

Uses GitHub API to render as HTML posts written in GitHub-flavored Markdwon
syntax.

Should be used together with the
[Github Markdown CSS](https://github.com/sindresorhus/github-markdown-css)
(a copy is included in the example from the _**test**_ folder)


##Usage

1. Create your blog folder structure

  **NOTE**: one can use the _**src/main/test/scala/scalatictest**_ folder
  as a starting point.

  The blog folder structure can reside anywhere on your machine
  and needs to look like this:

  ```
  blog            -> root folder of your blog; can have any name you want
    |_ new        -> any markdown source files that should be (re)rendered to HTML
    |_ source     -> contains any files that are not blog posts (e.g. html, css, js)
      |_ posts    -> all markdown source files that have already been rendered to HTML
    |_ target     -> this is where your generated blog will be saved

  ```

  Only the _**new**_ and _**source**_ folders need to be created -
  the _source/posts_ and _target_ folder are created automatically if they don't
  exist.

2. Clone the [Scalatic](https://github.com/padurean/scalatic) repo and run with

  `sbt "run /path/to/your/blog"`

  OR

  Download the [pre-built Scalatic jar](https://github.com/padurean) and run with

  `java -jar scalatic-0.1.0 /path/to/your/blog`


**NOTE**s:

  - To build Scalatic as one JAR, run `sbt one-jar`
