package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.shop.model.ShopPack;
import com.enosistudio.bruine.shop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/shop-packs")
public class AdminShopPackController {

    private final ShopService shopService;

    public AdminShopPackController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * Convertit un champ datetime-local (« 2026-07-05T14:30 ») en LocalDateTime, ou null si vide.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }

    private int eurosToCents(double euros) {
        return (int) Math.round(Math.max(euros, 0) * 100);
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("packs", shopService.findAllOrdered());
        return "admin/shop-packs";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name,
                         @RequestParam String emoji,
                         @RequestParam int points,
                         @RequestParam(defaultValue = "0") int bonusPoints,
                         @RequestParam double priceEuros,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         @RequestParam(defaultValue = "false") boolean popular,
                         @RequestParam(defaultValue = "0") int promoPercent,
                         @RequestParam(required = false) String promoStart,
                         @RequestParam(required = false) String promoEnd,
                         RedirectAttributes ra) {
        ShopPack pack = new ShopPack();
        pack.setName(name);
        pack.setEmoji(emoji);
        pack.setPoints(Math.max(points, 0));
        pack.setBonusPoints(Math.max(bonusPoints, 0));
        pack.setPriceCents(eurosToCents(priceEuros));
        pack.setSortOrder(sortOrder);
        pack.setPopular(popular);
        pack.setPromoPercent(Math.min(Math.max(promoPercent, 0), 100));
        pack.setPromoStart(parseDateTime(promoStart));
        pack.setPromoEnd(parseDateTime(promoEnd));
        shopService.save(pack);
        ra.addFlashAttribute("success", "Pack « " + name + " » créé.");
        return "redirect:/admin/shop-packs";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam String emoji,
                         @RequestParam int points,
                         @RequestParam(defaultValue = "0") int bonusPoints,
                         @RequestParam double priceEuros,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         @RequestParam(defaultValue = "false") boolean popular,
                         @RequestParam(defaultValue = "0") int promoPercent,
                         @RequestParam(required = false) String promoStart,
                         @RequestParam(required = false) String promoEnd,
                         RedirectAttributes ra) {
        shopService.findById(id).ifPresent(pack -> {
            pack.setName(name);
            pack.setEmoji(emoji);
            pack.setPoints(Math.max(points, 0));
            pack.setBonusPoints(Math.max(bonusPoints, 0));
            pack.setPriceCents(eurosToCents(priceEuros));
            pack.setSortOrder(sortOrder);
            pack.setPopular(popular);
            pack.setPromoPercent(Math.min(Math.max(promoPercent, 0), 100));
            pack.setPromoStart(parseDateTime(promoStart));
            pack.setPromoEnd(parseDateTime(promoEnd));
            shopService.save(pack);
        });
        ra.addFlashAttribute("success", "Pack mis à jour.");
        return "redirect:/admin/shop-packs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        shopService.deleteById(id);
        ra.addFlashAttribute("success", "Pack supprimé.");
        return "redirect:/admin/shop-packs";
    }
}
