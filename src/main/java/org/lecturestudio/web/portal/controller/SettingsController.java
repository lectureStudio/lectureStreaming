package org.lecturestudio.web.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	@RequestMapping()
	public String settingsIndex() {
		return "settings";
	}

}
