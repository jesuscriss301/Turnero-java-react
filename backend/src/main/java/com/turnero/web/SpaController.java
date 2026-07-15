package com.turnero.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
  @GetMapping({"/", "/login", "/register", "/admin", "/admin/**", "/reception", "/operator", "/display/**", "/q/**"})
  public String index() { return "forward:/index.html"; }
}
