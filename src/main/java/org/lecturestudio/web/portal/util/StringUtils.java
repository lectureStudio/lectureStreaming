package org.lecturestudio.web.portal.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;
import org.owasp.esapi.ESAPI;

public class StringUtils {

	public static final String cleanHtml(String input) {
		input = ESAPI.encoder().canonicalize(input).replaceAll("\0", "");

		OutputSettings outputSettings = new OutputSettings();
		outputSettings.prettyPrint(false);

		return Jsoup.clean(input, "", Whitelist.relaxed(), outputSettings);
	}

}
