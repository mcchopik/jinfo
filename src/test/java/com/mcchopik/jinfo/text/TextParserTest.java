package com.mcchopik.jinfo.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextParserTest {

    private final TextParser parser = new TextParser();

    @Test
    void plainTextWithoutFormatting() {
        ParsedProfile p = parser.parse("Привет, я Вася", Lang.RU);
        assertEquals("Привет, я Вася", p.plainText());
        assertEquals(1, p.parts().size());
        assertTrue(p.parts().get(0) instanceof Part.Text);
    }

    @Test
    void colorCodeApplied() {
        ParsedProfile p = parser.parse("&cКрасный", Lang.RU);
        Part.Text t = (Part.Text) p.parts().get(0);
        assertEquals("#FF5555", t.style().colorHex());
        assertEquals("Красный", t.text());
    }

    @Test
    void sectionSignColorApplied() {
        ParsedProfile p = parser.parse("\u00A7aЗелёный", Lang.RU);
        Part.Text t = (Part.Text) p.parts().get(0);
        assertEquals("#55FF55", t.style().colorHex());
    }

    @Test
    void hexColorApplied() {
        ParsedProfile p = parser.parse("&#12AB34Цвет", Lang.RU);
        Part.Text t = (Part.Text) p.parts().get(0);
        assertEquals("#12AB34", t.style().colorHex());
        assertEquals("Цвет", t.text());
    }

    @Test
    void boldItalicUnderlineStrike() {
        ParsedProfile p = parser.parse("&l&o&n&mX", Lang.RU);
        Part.Text t = (Part.Text) p.parts().get(0);
        assertTrue(t.style().bold());
        assertTrue(t.style().italic());
        assertTrue(t.style().underline());
        assertTrue(t.style().strikethrough());
    }

    @Test
    void obfuscationIsIgnored() {
        ParsedProfile p = parser.parse("&kСекрет", Lang.RU);
        Part.Text t = (Part.Text) p.parts().get(0);
        assertEquals("Секрет", t.text());
        // никакого флага обфускации нет — просто обычный текст
        assertFalse(t.style().bold());
    }

    @Test
    void servicePlaceholderWithHandleBuildsUrl() {
        ParsedProfile p = parser.parse("{youtube} @vasya", Lang.RU);
        Part.Link link = firstLink(p);
        assertEquals("https://youtube.com/@vasya", link.url());
        assertEquals("Ютуб", link.label());
        assertTrue(link.verified());
    }

    @Test
    void servicePlaceholderWithFullUrl() {
        ParsedProfile p = parser.parse("{twitch} https://twitch.tv/vasya", Lang.EN);
        Part.Link link = firstLink(p);
        assertEquals("https://twitch.tv/vasya", link.url());
        assertEquals("Twitch", link.label());
    }

    @Test
    void servicePlaceholderWrongDomainBecomesRaw() {
        ParsedProfile p = parser.parse("{youtube} https://evil.com/@vasya", Lang.RU);
        assertTrue(p.parts().stream().anyMatch(x -> x instanceof Part.Raw));
        assertEquals(0, p.linkCount());
    }

    @Test
    void universalLinkAnyHttpsDomain() {
        ParsedProfile p = parser.parse("{link:Мой сайт} https://example.com/page", Lang.RU);
        Part.Link link = firstLink(p);
        assertEquals("example.com", java.net.URI.create(link.url()).getHost());
        assertEquals("Мой сайт", link.label());
        assertFalse(link.verified());
    }

    @Test
    void javascriptLinkRejected() {
        ParsedProfile p = parser.parse("{link:bad} javascript:alert(1)", Lang.RU);
        assertEquals(0, p.linkCount());
        assertTrue(p.parts().stream().anyMatch(x -> x instanceof Part.Raw));
    }

    @Test
    void httpLinkRejected() {
        ParsedProfile p = parser.parse("{link:bad} http://example.com", Lang.RU);
        assertEquals(0, p.linkCount());
    }

    @Test
    void localhostAndIpRejected() {
        assertEquals(0, parser.parse("{link:x} https://localhost/a", Lang.RU).linkCount());
        assertEquals(0, parser.parse("{link:x} https://127.0.0.1/a", Lang.RU).linkCount());
        assertEquals(0, parser.parse("{link:x} https://192.168.0.1/a", Lang.RU).linkCount());
    }

    @Test
    void userinfoRejected() {
        assertEquals(0, parser.parse("{link:x} https://user@example.com/a", Lang.RU).linkCount());
    }

    @Test
    void shortenerGetsWarning() {
        ParsedProfile p = parser.parse("{link:short} https://bit.ly/abc", Lang.RU);
        Part.Link link = firstLink(p);
        assertTrue(link.warnings().contains("shortener"));
    }

    @Test
    void sanitizeRemovesZeroWidthAndBidi() {
        String s = TextParser.sanitize("a\u200Bb\u202Ec");
        assertEquals("abc", s);
    }

    @Test
    void repeatedCharsCollapsed() {
        assertEquals("aaaa", TextParser.sanitize("aaaaaaaaaa"));
    }

    @Test
    void tooManyLinksFlagged() {
        String text = "{youtube} a {youtube} b {youtube} c {youtube} d "
                + "{youtube} e {youtube} f {youtube} g";
        ParsedProfile p = parser.parse(text, Lang.RU);
        assertEquals(7, p.linkCount());
        assertTrue(p.overLimits());
        assertTrue(p.warnings().contains("too_many_links"));
    }

    @Test
    void tooLongFlagged() {
        String longText = "a".repeat(501);
        ParsedProfile p = parser.parse(longText, Lang.RU);
        assertTrue(p.overLimits());
        assertTrue(p.warnings().contains("too_long"));
    }

    @Test
    void htmlEscapesText() {
        ParsedProfile p = parser.parse("<script>alert(1)</script>", Lang.RU);
        String html = HtmlRenderer.render(p, HtmlRenderer.Mode.TOOLTIP);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void htmlLinkHasDataUrlAndColorClass() {
        ParsedProfile p = parser.parse("{youtube} vasya", Lang.RU);
        String html = HtmlRenderer.render(p, HtmlRenderer.Mode.FULL);
        assertTrue(html.contains("svc-youtube"));
        assertTrue(html.contains("data-url=\"https://youtube.com/@vasya\""));
        assertTrue(html.contains("role=\"button\""));
    }

    @Test
    void tooltipLinksNotButtons() {
        ParsedProfile p = parser.parse("{youtube} vasya", Lang.RU);
        String html = HtmlRenderer.render(p, HtmlRenderer.Mode.TOOLTIP);
        assertFalse(html.contains("role=\"button\""));
    }

    private static Part.Link firstLink(ParsedProfile p) {
        List<Part> parts = p.parts();
        for (Part part : parts) {
            if (part instanceof Part.Link l) {
                return l;
            }
        }
        throw new AssertionError("no link found in: " + p.plainText());
    }
}