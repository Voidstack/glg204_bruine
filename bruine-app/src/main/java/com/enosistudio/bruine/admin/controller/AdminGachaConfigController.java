package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.gacha.dto.GachaConfigFormDTO;
import com.enosistudio.bruine.gacha.service.GachaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/gacha-config")
public class AdminGachaConfigController {

    private final GachaService gachaService;

    public AdminGachaConfigController(GachaService gachaService) {
        this.gachaService = gachaService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("gachaConfig", gachaService.currentConfig());
        return "admin/gacha-config";
    }

    @PostMapping
    public String save(@ModelAttribute GachaConfigFormDTO form, RedirectAttributes redirectAttributes) {
        gachaService.saveConfig(form);
        redirectAttributes.addFlashAttribute("success", "Configuration sauvegardée.");
        return "redirect:/admin/gacha-config";
    }
}
