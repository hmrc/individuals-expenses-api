package config.rewriters

import com.github.jknack.handlebars.{Handlebars, Options, Template}
import config.rewriters.DocumentationRewriters.CheckRewrite
import config.{AppConfig, FeatureSwitches}
import controllers.Rewriter

import javax.inject.{Inject, Singleton}
import scala.collection.mutable

@Singleton class OasFeatureRewriter @Inject() (implicit val appConfig: AppConfig) extends HandlebarsRewriter {

  private val fs = FeatureSwitches() // .featureSwitchConfig.entrySet.toMap.asJava

  hb.registerHelper("enabled", (featureName: String, _: Options) => {
    println(featureName)

    if (fs.isEnabled(featureName)) "true" else null

  })


  val rewriteOasFeature: (CheckRewrite, Rewriter) = (
    (version, _) => appConfig.endpointsEnabled(version),
    (path, filename, contents) => rewrite(path, filename, contents, fs)
  )

}

trait HandlebarsRewriter {
  implicit val appConfig: AppConfig

  protected val hb            = new Handlebars
  private val templateCache = mutable.Map[String, Template]()

  protected def rewrite(path: String, filename: String, contents: String, context: AnyRef): String = {
    val cacheKey = s"$path $filename"
    val template = compiledTemplate(cacheKey, contents)
    template.apply(context)
  }

  private def compiledTemplate(key: String, fileContents: String): Template = synchronized {
    templateCache.get(key) match {
      case Some(t) => t
      case None =>
        val t: Template = hb.compileInline(fileContents)
        templateCache.addOne(key -> t)
        t
    }
  }

}
