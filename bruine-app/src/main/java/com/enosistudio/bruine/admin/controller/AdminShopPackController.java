package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.shop.dto.ShopPackFormDTO;
import com.enosistudio.bruine.shop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shop-packs")
public class AdminShopPackController {

    private final ShopService shopService;

    public AdminShopPackController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("packs", shopService.findAllOrdered());
        return "admin/shop-packs";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute ShopPackFormDTO form, RedirectAttributes redirectAttributes) {
        shopService.create(form);
        redirectAttributes.addFlashAttribute("success", "Pack « " + form.name() + " » créé.");
        return "redirect:/admin/shop-packs";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute ShopPackFormDTO form,
                         RedirectAttributes redirectAttributes) {
        shopService.update(id, form);
        redirectAttributes.addFlashAttribute("success", "Pack mis à jour.");
        return "redirect:/admin/shop-packs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        shopService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Pack supprimé.");
        return "redirect:/admin/shop-packs";
    }
}
