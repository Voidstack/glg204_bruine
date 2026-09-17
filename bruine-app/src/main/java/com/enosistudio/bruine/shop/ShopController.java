package com.enosistudio.bruine.shop;

import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.shop.model.ShopPack;
import com.enosistudio.bruine.shop.service.ShopService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final CurrentSteamUser currentSteamUser;
    private final ShopService shopService;

    public ShopController(CurrentSteamUser currentSteamUser, ShopService shopService) {
        this.currentSteamUser = currentSteamUser;
        this.shopService = shopService;
    }

    @GetMapping
    public String page(Model model) {
        SteamUser user = currentSteamUser.require();
        model.addAttribute("packs", shopService.findAllOrdered());
        model.addAttribute("history", shopService.findUserHistory(user.getId()));
        return "shop/shop";
    }

    /**
     * Lance le paiement d'un pack : crée une session Stripe Checkout (mode Sandbox)
     * et redirige l'utilisateur vers la page de paiement hébergée par Stripe.
     */
    @PostMapping("/checkout/{id}")
    public String checkout(@PathVariable Long id) throws StripeException {
        SteamUser user = currentSteamUser.require();
        ShopPack pack = shopService.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Ce pack n'existe plus."));
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return "redirect:" + shopService.createCheckoutSession(user, pack, baseUrl);
    }

    /**
     * Retour après paiement réussi : on valide la session et on crédite les points.
     */
    @GetMapping("/success")
    public String success(@RequestParam("session_id") String sessionId,
                          RedirectAttributes redirectAttributes) throws StripeException {
        shopService.fulfillCheckout(sessionId).ifPresent(
                purchase -> redirectAttributes.addFlashAttribute("successMessage",
                        "Paiement accepté, " + purchase.getPointsCredited() + " points crédités !"));
        return "redirect:/shop";
    }

    /**
     * Retour après annulation du paiement sur Stripe.
     */
    @GetMapping("/cancel")
    public String cancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Paiement annulé, aucun point n'a été débité.");
        return "redirect:/shop";
    }

    @ExceptionHandler(StripeException.class)
    public String onStripeFailure(StripeException panneStripe, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Le paiement n'a pas abouti : " + panneStripe.getMessage());
        return "redirect:/shop";
    }
}
