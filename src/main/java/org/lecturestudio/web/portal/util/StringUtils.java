package org.lecturestudio.web.portal.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Safelist;

import org.owasp.esapi.ESAPI;

public class StringUtils {

	public static final String cleanHtml(String input) {
		input = ESAPI.encoder().canonicalize(input).replaceAll("\0", "");

		OutputSettings outputSettings = new OutputSettings();
		outputSettings.prettyPrint(false);
		outputSettings.escapeMode(EscapeMode.xhtml);

		Safelist safelist = Safelist.relaxed()
			.addTags("font")
			.addAttributes("a", "target")
			.addAttributes("div", "align")
			.addAttributes("p", "align")
			.addAttributes("font", "color");

		return Jsoup.clean(input, "", safelist, outputSettings);
	}

}
