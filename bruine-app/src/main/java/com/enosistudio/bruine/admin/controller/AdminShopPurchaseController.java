package com.enosistudio.bruine.admin.controller;

import com.enosistudio.bruine.shop.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/shop-purchases")
public class AdminShopPurchaseController {

    private final ShopService shopService;

    public AdminShopPurchaseController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("purchases", shopService.findAllHistory());
        return "admin/shop-purchases";
    }

    /**
     * Historique filtré sur un utilisateur précis.
     */
    @GetMapping("/user/{userId}")
    public String byUser(@PathVariable Long userId, Model model) {
        model.addAttribute("purchases", shopService.findUserHistory(userId));
        return "admin/shop-purchases";
    }
}
