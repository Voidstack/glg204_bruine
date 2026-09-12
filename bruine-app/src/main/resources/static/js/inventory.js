/* Inventaire : les cartes réagissent au curseur comme ailleurs sur le site.
 *
 * Il n'y a rien à déposer ici, le glisser sert donc uniquement à manipuler la carte :
 * elle se soulève, suit le curseur, puis revient à sa place au relâchement. Le mouvement
 * lui-même vient de card-motion.js, partagé avec l'éditeur de deck et le level up.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.inv-card').forEach(card => {
        CardMotion.enableHover(card);
        CardMotion.enableDrag(card);
    });
});
