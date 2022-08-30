package org.lecturestudio.web.portal.controller;

import javax.validation.Valid;

import org.lecturestudio.web.portal.model.p2p.P2PDemoForm;
import org.lecturestudio.web.portal.service.P2PDemoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class P2PDemoController {

	@Autowired
	private P2PDemoService p2pDemoService;


	@GetMapping(value = "/p2p-demo")
	public String p2pDemo(Model model) {
		P2PDemoForm demoForm = new P2PDemoForm();
		demoForm.setServerBandwidth(1000);
		demoForm.setSuperPeerBandwidthThreshold(100);
		demoForm.setNumServers(1);
		demoForm.setNumPeers(5);
		demoForm.setNumSuperPeers(2);
		demoForm.setMaxSuperPeersClients(3);
		demoForm.setDocumentSize(1);

		
		model.addAttribute("demoForm", demoForm);

		return "p2p-demo";
	}

	@PostMapping("/p2p-demo")
	public String addCourse(Authentication authentication, @Valid P2PDemoForm demoForm, BindingResult result, Model model) {
		if (result.hasErrors()) {
			return "p2p-demo";
		}

		model.addAttribute("demoForm", demoForm);

		p2pDemoService.start(authentication.getName(), demoForm);

		return "redirect:/p2p-demo";
	}
}
