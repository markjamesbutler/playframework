/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.filters.headers

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc.{ Action, Result }
import play.api.routing.Router
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import play.filters.hsts.HSTSFilter

object HSTSFilterSpec extends PlaySpecification {

  import HSTSFilter._

  sequential

  class Filters @Inject() (hstsHeadersFilter: HSTSFilter) extends HttpFilters {
    def filters = Seq(hstsHeadersFilter)
  }

  def configure(rawConfig: String) = {
    val typesafeConfig = ConfigFactory.parseString(rawConfig)
    Configuration(typesafeConfig)
  }

  def withApplication[T](result: Result, config: String)(block: => T): T = {
    running(new GuiceApplicationBuilder()
      .configure(configure(config))
      .overrides(
        bind[Router].to(Router.from {
          case _ => Action(result)
        }),
        bind[HttpFilters].to[Filters]
      ).build
    )(block)
  }

  "hsts headers" should {

    "work with default singleton apply method with all default options" in new WithApplication() {
      val filter = HSTSFilter()
      // Play.current is set at this point...
      val rh = FakeRequest()
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run()

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; preload")
    }

    "work with singleton apply method using configuration" in new WithApplication() {
      val filter = HSTSFilter(Configuration.reference)
      val rh = FakeRequest()
      val action = Action(Ok("success"))
      val result = filter(action)(rh).run()

      header(STRICT_TRANSPORT_SECURITY, result) must beSome("max-age=31536000; preload")
    }

    "strict transport security" should {

      "work with custom" in withApplication(Ok("hello"),
        """
          |play.filters.hsts.strictTransportSecurity="some strict transport security"
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(STRICT_TRANSPORT_SECURITY, result) must beSome("some strict transport security")
        }

      "work with none" in withApplication(Ok("hello"),
        """
          |play.filters.hsts.strictTransportSecurity=null
        """.stripMargin) {
          val result = route(FakeRequest()).get

          header(STRICT_TRANSPORT_SECURITY, result) must beNone
        }
    }
  }
}
