package server

import java.nio.charset.StandardCharsets

import models.bff.outofgame.PlayerClasses
import scalatags.Text.all.*
import scalatags.Text.{svgAttrs, svgTags, tags2}
import services.i18n.{I18n, LandingPicture, Language}

/** The website's landing page, in each language of the game (its texts are the [[I18n]]'s): what
  * the game is, its classes and screenshots, where to download it (see [[ClientPackageRoutes]]),
  * that it is open source, its story, and the frequently asked questions.
  *
  * The pictures are the server's resources in `landing/` (see [[LandingAssets]]): one per
  * [[LandingPicture]] (e.g. `landing/fight.png`), and the animation of a fight,
  * `landing/battle.mp4` or `landing/battle.gif`. The screenshots missing are left out, and without
  * the animation, the page draws a fight with the shapes of the game.
  *
  * The `analytics.html` resource goes at the end of the page's head, as it is: the tags of an
  * analytics service, if any.
  */
object LandingPage {

  val gitHub: String       = "https://github.com/sherpal/BattleForFlatland"
  private val feedbackLink = s"$gitHub/issues"

  /** Where to read about each card of the open source section, in its order. */
  private val openSourceLinks =
    List(gitHub, s"$gitHub/blob/main/deploy.md", s"$gitHub/blob/main/make-a-boss.md")

  /** The downloads that the page offers, as (system, label). */
  private val downloads = List("windows" -> "Windows", "macos" -> "macOS", "linux" -> "Linux")

  /** The page in this language, made once. */
  def apply(language: Language): String = pages(language)

  private lazy val pages: Map[Language, String] =
    Language.values.map(language => language -> ("<!DOCTYPE html>" + page(language).render)).toMap

  private def resource(path: String): Option[String] =
    Option(getClass.getResourceAsStream(s"/$path")).map { stream =>
      try new String(stream.readAllBytes(), StandardCharsets.UTF_8)
      finally stream.close()
    }

  private lazy val analytics: String = resource("analytics.html").getOrElse("")

  /** The colours of the classes, as the tutorial gives them, and the High Priests' gold. */
  private def classColour(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "#e65a46"
    case PlayerClasses.Square => "#508ce6"
    case PlayerClasses.Pentagon => "#b96edc"
    case PlayerClasses.Hexagon => "#46be6e"
  }
  private val gold = "#ebbe46"

  private def sides(playerClass: PlayerClasses): Int = playerClass match {
    case PlayerClasses.Triangle => 3
    case PlayerClasses.Square => 4
    case PlayerClasses.Pentagon => 5
    case PlayerClasses.Hexagon => 6
  }

  // --- Shapes, in SVG ---

  /** A regular polygon of `n` sides, centred on (x, y), a vertex up (a side up for the square). */
  private def polygon(n: Int, x: Double, y: Double, radius: Double, colour: String): Frag = {
    val offset = -math.Pi / 2 + (if n == 4 then math.Pi / 4 else 0)
    val points = (0 until n).map { k =>
      val angle = offset + 2 * math.Pi * k / n
      // not in the server's locale, which may write "1,5"
      String.format(
        java.util.Locale.ROOT,
        "%.1f,%.1f",
        x + radius * math.cos(angle),
        y + radius * math.sin(angle)
      )
    }
    svgTags.polygon(
      svgAttrs.points := points.mkString(" "),
      svgAttrs.fill := colour,
      svgAttrs.fillOpacity := "0.9",
      svgAttrs.stroke := "#ffffff",
      svgAttrs.strokeOpacity := "0.35",
      svgAttrs.strokeWidth := "2",
      svgAttrs.strokeLinejoin := "round"
    )
  }

  private def classShape(playerClass: PlayerClasses, x: Double, y: Double, radius: Double): Frag =
    polygon(sides(playerClass), x, y, radius, classColour(playerClass))

  /** A High Priest (a circle, in gold). */
  private def highPriest(x: Double, y: Double, radius: Double): Frag = frag(
    svgTags.circle(
      svgAttrs.cx := x,
      svgAttrs.cy := y,
      svgAttrs.r := radius * 1.45,
      svgAttrs.fill := gold,
      svgAttrs.fillOpacity := "0.08"
    ),
    svgTags.circle(
      svgAttrs.cx := x,
      svgAttrs.cy := y,
      svgAttrs.r := radius,
      svgAttrs.fill := "#2a2416",
      svgAttrs.stroke := gold,
      svgAttrs.strokeWidth := "4"
    )
  )

  /** The team around a High Priest: the tank in front, the others behind, within the healer's range
    * (dashed).
    */
  private def battleArt(description: String): Frag = svgTags.svg(
    svgAttrs.viewBox := "0 0 400 300",
    role := "img",
    aria.label := description,
    svgTags.circle(
      svgAttrs.cx := 225,
      svgAttrs.cy := 160,
      svgAttrs.r := 130,
      svgAttrs.fill := "none",
      svgAttrs.stroke := classColour(PlayerClasses.Hexagon),
      svgAttrs.strokeOpacity := "0.35",
      svgAttrs.strokeWidth := "2",
      svgAttrs.strokeDasharray := "6 8"
    ),
    highPriest(170, 135, 58),
    classShape(PlayerClasses.Square, 82, 120, 30),
    classShape(PlayerClasses.Triangle, 248, 92, 24),
    classShape(PlayerClasses.Triangle, 262, 188, 24),
    classShape(PlayerClasses.Pentagon, 330, 70, 22),
    classShape(PlayerClasses.Hexagon, 318, 238, 24)
  )

  private def shapeIcon(n: Int, colour: String, size: Int): Frag = svgTags.svg(
    svgAttrs.viewBox := "0 0 64 64",
    svgAttrs.width := size,
    svgAttrs.height := size,
    aria.hidden := "true",
    polygon(n, 32, 33, 26, colour)
  )

  // --- The page ---

  private def page(language: Language): Frag = {
    val i18n = language.i18n
    html(
      lang := language.code,
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        tag("title")(s"Battle for Flatland: ${i18n.landingTagline}"),
        meta(name := "description", content := i18n.landingPitch),
        meta(attr("property") := "og:type", content := "website"),
        meta(attr("property") := "og:title", content := "Battle for Flatland"),
        meta(attr("property") := "og:description", content := i18n.landingPitch),
        meta(name := "theme-color", content := "#1c1e28"),
        link(rel := "icon", href := favicon),
        for other <- Language.values.toSeq
        yield link(rel := "alternate", attr("hreflang") := other.code, href := s"/${other.code}"),
        link(rel := "alternate", attr("hreflang") := "x-default", href := "/"),
        tag("style")(raw(css)),
        raw(analytics)
      ),
      body(
        header(language),
        tag("main")(
          hero(i18n),
          features(i18n),
          classes(i18n),
          screenshots(i18n),
          steps(i18n),
          openSource(i18n),
          story(i18n),
          faq(i18n),
          beta(i18n)
        ),
        footer(language),
        script(raw(detectSystem))
      )
    )
  }

  private def header(language: Language): Frag = tag("header")(cls := "top")(
    div(cls := "wrap top-bar")(
      a(cls := "brand", href := s"/${language.code}")(
        shapeIcon(3, gold, 28),
        span("Battle for Flatland")
      ),
      tag("nav")(cls := "top-links")(
        a(href := "#download")(language.i18n.landingDownload),
        a(href := gitHub, rel := "noopener")("GitHub"),
        languages(language)
      )
    )
  )

  /** The language, which opens the list of the others. */
  private def languages(current: Language): Frag = tag("details")(cls := "languages")(
    tag("summary")(current.nativeName),
    ul(
      for language <- Language.values.toSeq
      yield li(
        a(
          href := s"/${language.code}",
          lang := language.code,
          attr("hreflang") := language.code,
          if language == current then attr("aria-current") := "page" else ()
        )(language.nativeName)
      )
    )
  )

  private def hero(i18n: I18n): Frag = tags2.section(cls := "hero")(
    div(cls := "wrap hero-grid")(
      div(cls := "hero-text")(
        h1("Battle for Flatland"),
        p(cls := "tagline")(i18n.landingTagline),
        p(cls := "pitch")(i18n.landingPitch),
        div(id := "download", cls := "downloads")(
          for (system, label) <- downloads
          yield a(
            cls := "button download",
            href := s"/api/bff/client/$system",
            attr("data-system") := system
          )(i18n.downloadFor(label))
        ),
        p(cls := "small")(
          span(cls := "badge")("Beta"),
          " ",
          a(cls := "badge open", href := "#open-source")(i18n.landingOpenSourceBadge),
          " ",
          i18n.landingFreeAndOpen,
          " ",
          a(href := "/api/bff/client/love")(i18n.landingLoveFile),
          "."
        ),
        p(cls := "small muted")(i18n.landingUnsigned)
      ),
      div(cls := "hero-media")(animation(i18n))
    )
  )

  /** The animation of a fight: the video, the GIF, or else the drawing. */
  private def animation(i18n: I18n): Frag =
    LandingAssets(LandingAssets.video)
      .map { video =>
        tag("video")(
          attr("autoplay") := "",
          attr("muted") := "",
          attr("loop") := "",
          attr("playsinline") := "",
          attr("aria-label") := i18n.landingAnimation,
          src := video.url
        )
      }
      .orElse {
        LandingAssets(LandingAssets.gif).map(gif =>
          img(src := gif.url, alt := i18n.landingAnimation)
        )
      }
      .getOrElse(div(cls := "art")(battleArt(i18n.landingAnimation)))

  private def features(i18n: I18n): Frag = tags2.section(cls := "section")(
    div(cls := "wrap")(
      h2(i18n.landingFeaturesTitle),
      div(cls := "grid four")(
        for ((title, text), i) <- i18n.landingFeatures.zipWithIndex
        yield div(cls := "card")(
          shapeIcon(i + 3, gold, 40),
          h3(title),
          p(text)
        )
      )
    )
  )

  private def story(i18n: I18n): Frag = {
    val story = i18n.story
    tags2.section(cls := "section story")(
      div(cls := "wrap story-grid")(
        div(cls := "story-art")(
          svgTags.svg(
            svgAttrs.viewBox := "0 0 200 200",
            aria.hidden := "true",
            highPriest(100, 100, 62)
          )
        ),
        div(
          h2(story.heading),
          for paragraph <- story.paragraphs.init yield p(paragraph),
          story.paragraphs.lastOption.map(last => p(cls := "call")(last))
        )
      )
    )
  }

  private def classes(i18n: I18n): Frag = tags2.section(cls := "section")(
    div(cls := "wrap")(
      h2(i18n.landingClassesTitle),
      div(cls := "grid four")(
        for playerClass <- PlayerClasses.allChoices
        yield div(cls := "card class", style := s"--class-colour: ${classColour(playerClass)}")(
          svgTags.svg(
            svgAttrs.viewBox := "0 0 64 64",
            svgAttrs.width := 56,
            svgAttrs.height := 56,
            aria.hidden := "true",
            classShape(playerClass, 32, 33, 26)
          ),
          h3(i18n.playerClassName(playerClass)),
          p(cls := "role")(i18n.classRole(playerClass)),
          p(i18n.classSummary(playerClass))
        )
      )
    )
  )

  /** The screenshots that the server has (none: no section). */
  private def screenshots(i18n: I18n): Frag = {
    val pictures = LandingPicture.values.toSeq.flatMap { picture =>
      LandingAssets(LandingAssets.pictureName(picture)).map(picture -> _)
    }
    if pictures.isEmpty then frag()
    else
      tags2.section(cls := "section")(
        div(cls := "wrap")(
          h2(i18n.landingScreenshotsTitle),
          div(cls := "grid two")(
            for (picture, asset) <- pictures
            yield {
              val caption = i18n.landingCaption(picture)
              tag("figure")(cls := "shot")(
                a(href := asset.url, target := "_blank")(
                  img(src := asset.url, alt := caption, attr("loading") := "lazy")
                ),
                tag("figcaption")(caption)
              )
            }
          )
        )
      )
  }

  private def steps(i18n: I18n): Frag = tags2.section(cls := "section")(
    div(cls := "wrap")(
      h2(i18n.landingStepsTitle),
      ol(cls := "steps")(for step <- i18n.landingSteps yield li(step))
    )
  )

  private def openSource(i18n: I18n): Frag = tags2.section(id := "open-source", cls := "section")(
    div(cls := "wrap")(
      h2(i18n.landingOpenSourceTitle),
      p(cls := "lead")(i18n.landingOpenSourceText),
      div(cls := "grid three")(
        for ((title, text), link) <- i18n.landingOpenSource.zip(openSourceLinks)
        yield div(cls := "card")(
          h3(a(href := link, rel := "noopener")(title)),
          p(text)
        )
      ),
      div(cls := "actions")(
        a(cls := "button", href := gitHub, rel := "noopener")(i18n.landingSourceCode)
      )
    )
  )

  private def faq(i18n: I18n): Frag = tags2.section(id := "faq", cls := "section")(
    div(cls := "wrap")(
      h2(i18n.landingFaqTitle),
      div(cls := "faq")(
        for (question, answer) <- i18n.landingFaq
        yield tag("details")(tag("summary")(question), p(answer))
      )
    )
  )

  private def beta(i18n: I18n): Frag = tags2.section(cls := "section")(
    div(cls := "wrap")(
      div(cls := "beta")(
        h2(i18n.landingBetaTitle),
        p(i18n.landingBetaText),
        div(cls := "actions")(
          a(cls := "button", href := "#download")(i18n.landingDownload),
          a(cls := "button secondary", href := feedbackLink, rel := "noopener")(
            i18n.landingFeedback
          ),
          a(cls := "button secondary", href := gitHub, rel := "noopener")(i18n.landingSourceCode)
        )
      )
    )
  )

  private def footer(language: Language): Frag = tag("footer")(cls := "bottom")(
    div(cls := "wrap bottom-bar")(
      p(language.i18n.landingFooter, " ", a(href := gitHub, rel := "noopener")("GitHub")),
      p(cls := "footer-languages")(
        for other <- Language.values.toSeq
        yield a(href := s"/${other.code}", lang := other.code)(other.nativeName)
      ),
      p(cls := "trademark")(language.i18n.landingTrademark)
    )
  )

  // --- Assets ---

  private val favicon: String =
    "data:image/svg+xml," + java.net.URLEncoder
      .encode(
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><circle cx="32" cy="32" r="26" fill="#2a2416" stroke="#ebbe46" stroke-width="6"/></svg>""",
        StandardCharsets.UTF_8
      )
      .replace("+", "%20")

  /** Puts first the download for the visitor's system, if known. */
  private val detectSystem: String =
    """(function () {
      |  var ua = navigator.userAgent, system = null;
      |  if (/Windows/.test(ua)) system = "windows";
      |  else if (/Macintosh|Mac OS X/.test(ua) && !/iPhone|iPad/.test(ua)) system = "macos";
      |  else if (/Linux|X11/.test(ua) && !/Android/.test(ua)) system = "linux";
      |  var button = system && document.querySelector('.download[data-system="' + system + '"]');
      |  if (button) {
      |    button.classList.add("detected");
      |    button.parentNode.insertBefore(button, button.parentNode.firstChild);
      |    button.parentNode.classList.add("has-detected");
      |  }
      |})();""".stripMargin

  private val css: String =
    """:root {
      |  --background: #1c1e28; --panel: #282b38; --border: #50556980;
      |  --text: #ebebf0; --muted: #969baa; --gold: #ebbe46; --primary: #286ec8;
      |  --primary-hover: #3a82de; --radius: 12px;
      |}
      |* { box-sizing: border-box; }
      |html { scroll-behavior: smooth; }
      |[id] { scroll-margin-top: 72px; }
      |body {
      |  margin: 0; background: var(--background); color: var(--text);
      |  font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
      |  font-size: 17px; line-height: 1.6;
      |  background-image:
      |    radial-gradient(circle at 80% -10%, #ebbe4618, transparent 45%),
      |    radial-gradient(circle at -10% 40%, #508ce614, transparent 40%);
      |  background-repeat: no-repeat;
      |}
      |a { color: var(--gold); }
      |img, video, svg { display: block; max-width: 100%; }
      |h1, h2, h3 { line-height: 1.2; margin: 0 0 0.5em; }
      |h1 { font-size: clamp(2.4rem, 6vw, 4rem); letter-spacing: -0.02em; }
      |h2 { font-size: clamp(1.6rem, 3.5vw, 2.2rem); }
      |h3 { font-size: 1.2rem; }
      |p { margin: 0 0 1em; }
      |.wrap { width: 100%; max-width: 1120px; margin: 0 auto; padding: 0 16px; }
      |.small { font-size: 0.9rem; }
      |.muted { color: var(--muted); }
      |
      |/* Top bar */
      |.top { position: sticky; top: 0; z-index: 10; background: #1c1e28e6; backdrop-filter: blur(8px);
      |  border-bottom: 1px solid var(--border); }
      |.top-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px;
      |  min-height: 60px; flex-wrap: wrap; }
      |.brand { display: flex; align-items: center; gap: 10px; color: var(--text); font-weight: 700;
      |  text-decoration: none; font-size: 1.1rem; }
      |.top-links { display: flex; align-items: center; gap: 18px; }
      |.top-links > a { color: var(--text); text-decoration: none; font-size: 0.95rem; }
      |.top-links > a:hover { color: var(--gold); }
      |.languages { position: relative; font-size: 0.95rem; }
      |.languages summary { cursor: pointer; list-style: none; padding: 4px 10px;
      |  border: 1px solid var(--border); border-radius: 8px; }
      |.languages summary::-webkit-details-marker { display: none; }
      |.languages summary::after { content: " ▾"; color: var(--muted); }
      |.languages ul { position: absolute; right: 0; margin: 6px 0 0; padding: 6px; list-style: none;
      |  background: var(--panel); border: 1px solid var(--border); border-radius: 8px; min-width: 150px; }
      |.languages a { display: block; padding: 4px 10px; border-radius: 6px; color: var(--text);
      |  text-decoration: none; }
      |.languages a:hover { background: #ffffff12; }
      |.languages a[aria-current] { color: var(--gold); }
      |
      |/* Hero */
      |.hero { padding: 64px 0 40px; }
      |.hero-grid { display: grid; grid-template-columns: 1.05fr 1fr; gap: 48px; align-items: center; }
      |.tagline { font-size: clamp(1.2rem, 2.4vw, 1.5rem); color: var(--gold); font-weight: 600; }
      |.pitch { font-size: 1.1rem; color: #d4d5dc; }
      |.hero-media img, .hero-media video { width: 100%; border-radius: var(--radius);
      |  border: 1px solid var(--border); box-shadow: 0 20px 60px #00000080; }
      |.art { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius);
      |  padding: 16px; }
      |.art svg { width: 100%; height: auto; }
      |
      |/* Buttons */
      |.downloads, .actions { display: flex; flex-wrap: wrap; gap: 10px; margin: 28px 0 16px; }
      |.button { display: inline-block; padding: 12px 22px; border-radius: 10px; font-weight: 600;
      |  background: var(--primary); color: #fff; text-decoration: none; border: 1px solid transparent;
      |  transition: background 0.15s, transform 0.15s; }
      |.button:hover { background: var(--primary-hover); transform: translateY(-1px); }
      |.button.secondary, .downloads.has-detected .download:not(.detected) {
      |  background: transparent; border-color: var(--border); color: var(--text); }
      |.button.secondary:hover, .downloads.has-detected .download:not(.detected):hover {
      |  background: #ffffff10; }
      |.download.detected { background: var(--gold); color: #1c1e28; font-size: 1.05rem; }
      |.download.detected:hover { background: #f3cd65; }
      |.badge { display: inline-block; padding: 1px 8px; border-radius: 999px; font-size: 0.75rem;
      |  font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #1c1e28;
      |  background: var(--gold); vertical-align: 1px; }
      |.badge.open { color: var(--gold); background: transparent; border: 1px solid var(--gold);
      |  text-decoration: none; }
      |.badge.open:hover { background: #ebbe4626; }
      |
      |/* Sections */
      |.section { padding: 48px 0; }
      |.grid { display: grid; gap: 20px; }
      |.grid.four { grid-template-columns: repeat(4, 1fr); }
      |.grid.three { grid-template-columns: repeat(3, 1fr); }
      |.grid.two { grid-template-columns: repeat(2, 1fr); }
      |.lead { color: #d4d5dc; max-width: 68ch; margin-bottom: 24px; }
      |.card { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius);
      |  padding: 22px; }
      |.card svg { margin-bottom: 12px; }
      |.card p { color: #c9cad2; margin: 0; }
      |.card h3 a { color: var(--text); text-decoration-color: var(--gold); text-underline-offset: 4px; }
      |.card h3 a:hover { color: var(--gold); }
      |.card.class { border-top: 4px solid var(--class-colour); }
      |.card.class .role { color: var(--class-colour); font-weight: 600; margin-bottom: 8px; }
      |.story-grid { display: grid; grid-template-columns: 220px 1fr; gap: 40px; align-items: center;
      |  background: linear-gradient(135deg, #2a2416 0%, var(--panel) 60%); border: 1px solid #ebbe4640;
      |  border-radius: var(--radius); padding: 36px; }
      |.story p { color: #d4d5dc; max-width: 68ch; }
      |.story .call { color: var(--gold); font-weight: 700; font-size: 1.15rem; }
      |.shot { margin: 0; }
      |.shot img { width: 100%; aspect-ratio: 2 / 1; object-fit: contain; background: var(--panel);
      |  border-radius: var(--radius); border: 1px solid var(--border); }
      |.shot figcaption { color: var(--muted); font-size: 0.95rem; margin-top: 10px; }
      |.steps { counter-reset: step; list-style: none; padding: 0; margin: 0; display: grid; gap: 14px;
      |  grid-template-columns: repeat(4, 1fr); }
      |.steps li { counter-increment: step; background: var(--panel); border: 1px solid var(--border);
      |  border-radius: var(--radius); padding: 20px; }
      |.steps li::before { content: counter(step); display: flex; align-items: center;
      |  justify-content: center; width: 34px; height: 34px; border-radius: 50%; margin-bottom: 10px;
      |  background: #ebbe4626; color: var(--gold); font-weight: 700; }
      |.faq { display: grid; gap: 10px; max-width: 860px; }
      |.faq details { background: var(--panel); border: 1px solid var(--border);
      |  border-radius: var(--radius); }
      |.faq summary { cursor: pointer; list-style: none; padding: 16px 48px 16px 20px; font-weight: 600;
      |  position: relative; }
      |.faq summary::-webkit-details-marker { display: none; }
      |.faq summary::after { content: "+"; position: absolute; right: 20px; top: 50%;
      |  transform: translateY(-50%); color: var(--gold); font-size: 1.4rem; font-weight: 400; }
      |.faq details[open] summary::after { content: "−"; }
      |.faq summary:hover { color: var(--gold); }
      |.faq details p { margin: 0; padding: 0 20px 18px; color: #c9cad2; max-width: 75ch; }
      |.beta { text-align: center; padding: 44px 24px; border-radius: var(--radius);
      |  border: 1px solid #ebbe4660; background: radial-gradient(circle at 50% 0%, #ebbe4622, var(--panel) 70%); }
      |.beta p { max-width: 60ch; margin: 0 auto; color: #d4d5dc; }
      |.beta .actions { justify-content: center; margin-bottom: 0; }
      |
      |/* Footer */
      |.bottom { border-top: 1px solid var(--border); margin-top: 32px; padding: 24px 0;
      |  color: var(--muted); font-size: 0.9rem; }
      |.bottom-bar { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px 24px; }
      |.bottom p { margin: 0; }
      |.footer-languages a { color: var(--muted); margin-left: 12px; }
      |.footer-languages a:first-child { margin-left: 0; }
      |.bottom .trademark { flex-basis: 100%; font-size: 0.8rem; opacity: 0.8; }
      |
      |/* Narrower screens */
      |@media (max-width: 960px) {
      |  .grid.four, .steps { grid-template-columns: repeat(2, 1fr); }
      |  .grid.three { grid-template-columns: 1fr; }
      |  .hero-grid { grid-template-columns: 1fr; gap: 32px; }
      |  .hero { padding-top: 40px; }
      |}
      |@media (max-width: 640px) {
      |  body { font-size: 16px; }
      |  .grid.four, .grid.three, .grid.two, .steps { grid-template-columns: 1fr; }
      |  .story-grid { grid-template-columns: 1fr; padding: 24px; gap: 16px; }
      |  .story-art { max-width: 120px; }
      |  .top-links > a { display: none; }
      |  .downloads .button { flex: 1 1 100%; text-align: center; }
      |  .section { padding: 32px 0; }
      |}
      |@media (prefers-reduced-motion: reduce) {
      |  html { scroll-behavior: auto; }
      |  .button { transition: none; }
      |}
      |""".stripMargin
}
