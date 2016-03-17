package play.filters.hsts

import javax.inject.{ Provider, Inject, Singleton }

import play.api.inject.Module
import play.api.{ Environment, PlayConfig, Configuration }
import play.api.mvc._

/**
 * This class set the HTTP Strict Transport Security (HSTS) header on the HTTP Request.
 *
 * <ul>
 *   <li>{{play.filters.hsts.strictTransportSecurity}} - sets strictTransportSecurity. Some("max-age=31536000; preload") by default.
 * </ul>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6797">HTTP Strict Transport Security</a>
 */
object HSTSFilter {

  val STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security"

  def apply(config: HSTSConfig = HSTSConfig()): HSTSFilter = {
    new HSTSFilter(config)
  }

  def apply(config: Configuration): HSTSFilter = {
    new HSTSFilter(HSTSConfig.fromConfiguration(config))
  }
}

case class HSTSConfig(strictTransportSecurity: Option[String] = Some("max-age=31536000; preload"))

object HSTSConfig {

  def fromConfiguration(conf: Configuration): HSTSConfig = {
    val config = PlayConfig(conf).get[PlayConfig]("play.filters.hsts")

    HSTSConfig(
      strictTransportSecurity = config.get[Option[String]]("strictTransportSecurity"))
  }
}

@Singleton
class HSTSFilter @Inject() (config: HSTSConfig) extends EssentialFilter {
  import HSTSFilter._

  protected def headers(request: RequestHeader): Seq[(String, String)] = config.strictTransportSecurity.map(STRICT_TRANSPORT_SECURITY -> _).toSeq

  def apply(next: EssentialAction) = EssentialAction { req =>
    import play.api.libs.iteratee.Execution.Implicits.trampoline
    next(req).map(_.withHeaders(headers(req): _*))
  }
}

/**
 * Provider for HSTSConfig.
 * @param configuration
 */
@Singleton
class HSTSConfigProvider @Inject() (configuration: Configuration) extends Provider[HSTSConfig] {
  lazy val get = HSTSConfig.fromConfiguration(configuration)
}

/**
 * HSTS Module.
 */
class HSTSModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[HSTSConfig].toProvider[HSTSConfigProvider],
    bind[HSTSFilter].toSelf
  )
}

/**
 * Components for HSTS Filter.
 */
trait HSTSComponents {
  def configuration: Configuration

  lazy val hstsConfig: HSTSConfig = HSTSConfig.fromConfiguration(configuration)
  lazy val hstsFilter: HSTSFilter = HSTSFilter(hstsConfig)
}