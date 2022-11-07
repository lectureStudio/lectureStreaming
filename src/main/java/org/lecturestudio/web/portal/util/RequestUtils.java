package org.lecturestudio.web.portal.util;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

public class RequestUtils {

	/**
	 * Returns the referer URL retrieved from the provided request.
	 *
	 * @param request The {@link HttpServletRequest} from which to retrieve the referer.
	 * 
	 * @return Optional with the referer URL, null as value if no referer found.
	 */
	public static Optional<String> getPreviousPage(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader("Referer"));
	}

}
