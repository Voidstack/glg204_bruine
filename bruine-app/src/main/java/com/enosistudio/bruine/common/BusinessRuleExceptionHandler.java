package com.enosistudio.bruine.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * Affiche à l'utilisateur une action que les règles lui refusent.
 * Chaque contrôleur enveloppait auparavant ses appels dans le même try/catch pour reverser
 * le message en attribut flash. La règle est maintenant écrite une fois : le message est
 * déposé, et l'utilisateur revient sur la page d'où il venait.
 * La bannière est rendue par layout.html, donc le message s'affiche quelle que soit la page.
 */
@ControllerAdvice
public class BusinessRuleExceptionHandler {

    /**
     * Nom lu par la bannière d'erreur de layout.html.
     */
    public static final String FLASH_ATTRIBUTE = "errorMessage";

    private static final String FALLBACK = "/";

    @ExceptionHandler(BusinessRuleException.class)
    public String handleRefusedAction(BusinessRuleException refused,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
        redirectAttributes.addFlashAttribute(FLASH_ATTRIBUTE, refused.getMessage());
        return "redirect:" + previousPageOf(request);
    }

    /**
     * Page d'où venait la requête, réduite à son chemin.
     * <p>
     * L'en-tête Referer est fourni par le navigateur, donc jamais digne de confiance :
     * on refuse tout ce qui désigne un autre hôte, sans quoi une page hostile pourrait
     * transformer un refus en redirection vers son propre site.
     */
    private String previousPageOf(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return FALLBACK;
        }
        try {
            URI origin = URI.create(referer);
            if (origin.getHost() != null && !origin.getHost().equalsIgnoreCase(request.getServerName())) {
                return FALLBACK;
            }
            String path = origin.getPath();
            if (path == null || path.isBlank()) {
                return FALLBACK;
            }
            return origin.getQuery() == null ? path : path + "?" + origin.getQuery();
        } catch (IllegalArgumentException malformedReferer) {
            return FALLBACK;
        }
    }
}
