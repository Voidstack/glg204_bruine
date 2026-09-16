package com.enosistudio.bruine.legal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/mentions-legales")
    public String mentions() {
        return "legal/mentions";
    }

    @GetMapping("/politique-confidentialite")
    public String privacy() {
        return "legal/privacy";
    }
}