package com.enosistudio.bruine.shop;

import com.enosistudio.bruine.shop.service.ShopService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.security.CurrentSteamUser;
import com.stripe.exception.StripeException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
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
    public ModelAndView page() {
        SteamUser user = currentSteamUser.require();
        ModelAndView mav = new ModelAndView("shop/shop");
        mav.addObject("packs", shopService.findAllOrdered());
        mav.addObject("history", shopService.findUserHistory(user.getId()));
        return mav;
    }

    /**
     * Lance le paiement d'un pack : crée une session Stripe Checkout (mode Sandbox)
     * et redirige l'utilisateur vers la page de paiement hébergée par Stripe.
     */
    @PostMapping("/checkout/{id}")
    public String checkout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SteamUser user = currentSteamUser.require();
        return shopService.findById(id).map(pack -> {
            try {
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                return "redirect:" + shopService.createCheckoutSession(user, pack, baseUrl);
            } catch (StripeException e) {
                redirectAttributes.addFlashAttribute("error", "Impossible de démarrer le paiement : " + e.getMessage());
                return "redirect:/shop";
            }
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Ce pack n'existe plus.");
            return "redirect:/shop";
        });
    }

    /**
     * Retour après paiement réussi : on valide la session et on crédite les points.
     */
    @GetMapping("/success")
    public String success(@RequestParam("session_id") String sessionId, RedirectAttributes redirectAttributes) {
        try {
            shopService.fulfillCheckout(sessionId).ifPresentOrElse(
                    purchase -> redirectAttributes.addFlashAttribute("success",
                            "Paiement accepté, " + purchase.getPointsCredited() + " points crédités !"),
                    () -> redirectAttributes.addFlashAttribute("error",
                            "Paiement non confirmé ou déjà pris en compte."));
        } catch (StripeException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la vérification du paiement : " + e.getMessage());
        }
        return "redirect:/shop";
    }

    /**
     * Retour après annulation du paiement sur Stripe.
     */
    @GetMapping("/cancel")
    public String cancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Paiement annulé, aucun point n'a été débité.");
        return "redirect:/shop";
    }
}
