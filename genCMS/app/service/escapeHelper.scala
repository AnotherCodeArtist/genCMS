package service

import java.util.regex.Pattern

import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers

object EscapeHelper {

  /**
   * Policy for the HTML sanitizer
   */
  val HTML_ID = Pattern.compile(
    "[a-zA-Z0-9\\:\\-_\\.]+")
  val HTML_CLASS = Pattern.compile(
    "[a-zA-Z0-9\\s,\\-_]+")
  val HTML_TITLE = Pattern.compile(
    "[\\p{L}\\p{N}\\s\\-_',:\\[\\]!\\./\\\\\\(\\)&]*")
  val ALIGN = Pattern.compile(
    "(?i)center|left|right|justify|char")
  // The 16 colors defined by the HTML Spec (also used by the CSS Spec)
  val COLOR_NAME = Pattern.compile(
    "(?:aqua|black|blue|fuchsia|gray|grey|green|lime|maroon|navy|olive|purple"
      + "|red|silver|teal|white|yellow)")
  // HTML/CSS Spec allows 3 or 6 digit hex to specify color
  val COLOR_CODE = Pattern.compile(
    "(?:#(?:[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3})?))")

  val NAME = Pattern.compile("[a-zA-Z0-9\\-_\\$]+")

  val HISTORY_BACK = Pattern.compile(
    "(?:javascript:)?\\Qhistory.go(-1)\\E")

  val ONSITE_URL = Pattern.compile(
    "(?:[\\p{L}\\p{N}\\\\\\.\\#@\\$%\\+&;\\-_~,\\?=/!]+|\\#(\\w)+)")

  val OFFSITE_URL = Pattern.compile(
    "\\s*(?:(?:ht|f)tps?://|mailto:)[\\p{L}\\p{N}]"
      + "[\\p{L}\\p{N}\\p{Zs}\\.\\#@\\$%\\+&;:\\-_~,\\?=/!\\(\\)]*\\s*")

  val PARAGRAPH = Pattern.compile(
    "(?:[\\p{L}\\p{N},'\\.\\s\\-_\\(\\)]|&[0-9]{2};)*")

  val NUMBER = Pattern.compile(
    "[+-]?(?:(?:[0-9]+(?:\\.[0-9]*)?)|\\.[0-9]+)")

  val NUMBER_OR_PERCENT = Pattern.compile(
    "[0-9]+%?")

  val VALIGN = Pattern.compile(
    "(?i)baseline|bottom|middle|top")

  val ONE_CHAR = Pattern.compile(
    ".?", Pattern.DOTALL)

  val POLICY_DEFINITION: PolicyFactory = new HtmlPolicyBuilder()
    .allowAttributes("id").matching(HTML_ID).globally()
    .allowAttributes("class").matching(HTML_CLASS).globally()
    .allowAttributes("lang").matching(Pattern.compile("[a-zA-Z]{2,20}"))
    .globally()
    .allowAttributes("title").matching(HTML_TITLE).globally()
    .allowStyling()
    .allowAttributes("align").matching(ALIGN).onElements("p")
    .allowAttributes("for").matching(HTML_ID).onElements("label")
    .allowStandardUrlProtocols()
    .allowAttributes("nohref").onElements("a")
    .allowAttributes("name").matching(NAME).onElements("a")
    .allowAttributes(
      "onfocus", "onblur", "onclick", "onmousedown", "onmouseup")
    .matching(HISTORY_BACK).onElements("a")
    .requireRelNofollowOnLinks()
    .allowAttributes("src").matching(ONSITE_URL)
    .onElements("img")
    .allowAttributes("src").matching(OFFSITE_URL)
    .onElements("img")
    .allowAttributes("name").matching(NAME)
    .onElements("img")
    .allowAttributes("alt").matching(PARAGRAPH)
    .onElements("img")
    .allowAttributes("border", "hspace", "vspace").matching(NUMBER)
    .onElements("img")
    .allowAttributes("border", "cellpadding", "cellspacing")
    .matching(NUMBER).onElements("table")
    .allowAttributes("bgcolor").matching(COLOR_CODE)
    .onElements("table")
    .allowAttributes("background").matching(ONSITE_URL)
    .onElements("table")
    .allowAttributes("align").matching(ALIGN)
    .onElements("table")
    .allowAttributes("noresize").matching(Pattern.compile("(?i)noresize"))
    .onElements("table")
    .allowAttributes("background").matching(ONSITE_URL)
    .onElements("td", "th", "tr")
    .allowAttributes("bgcolor").matching(COLOR_CODE)
    .onElements("td", "th")
    .allowAttributes("abbr").matching(PARAGRAPH)
    .onElements("td", "th")
    .allowAttributes("axis", "headers").matching(NAME)
    .onElements("td", "th")
    .allowAttributes("scope")
    .matching(Pattern.compile("(?i)(?:row|col)(?:group)?"))
    .onElements("td", "th")
    .allowAttributes("nowrap")
    .onElements("td", "th")
    .allowAttributes("height", "width").matching(NUMBER)
    .onElements("td", "th")
    .allowAttributes("height", "width").matching(NUMBER_OR_PERCENT)
    .onElements("table", "td", "th", "tr", "img")
    .allowAttributes("align").matching(ALIGN)
    .onElements("thead", "tbody", "tfoot", "img",
      "td", "th", "tr", "colgroup", "col")
    .allowAttributes("valign").matching(VALIGN)
    .onElements("thead", "tbody", "tfoot",
      "td", "th", "tr", "colgroup", "col")
    .allowAttributes("charoff").matching(NUMBER_OR_PERCENT)
    .onElements("td", "th", "tr", "colgroup", "col",
      "thead", "tbody", "tfoot")
    .allowAttributes("char").matching(ONE_CHAR)
    .onElements("td", "th", "tr", "colgroup", "col",
      "thead", "tbody", "tfoot")
    .allowAttributes("colspan", "rowspan").matching(NUMBER)
    .onElements("td", "th")
    .allowAttributes("span", "width").matching(NUMBER_OR_PERCENT)
    .onElements("colgroup", "col")
    .allowElements(
      "a", "label", "noscript", "h1", "h2", "h3", "h4", "h5", "h6",
      "p", "i", "b", "u", "strong", "em", "small", "big", "pre", "code",
      "cite", "samp", "sub", "sup", "strike", "center", "blockquote",
      "hr", "br", "col", "font", "map", "span", "div", "img",
      "ul", "ol", "li", "dd", "dt", "dl", "tbody", "thead", "tfoot",
      "table", "td", "th", "tr", "colgroup", "fieldset", "legend")
    .toFactory();
  val SIMPLE_POLICY = Sanitizers.BLOCKS.and(Sanitizers.FORMATTING).and(Sanitizers.IMAGES).and(Sanitizers.LINKS).and(Sanitizers.STYLES)
  /**
   * Escapes HTML entities in text only fields using the apache commons StringEscapeUtils
   */
  def escapeHTML(txt: String) = {
    escapeHtml4(txt)
  }

  /**
   * Prevents XSS using the OWASP Html Sanitizer
   */
  def sanitizeHTML(txt: String) = {
    SIMPLE_POLICY.sanitize(txt)
  }
}