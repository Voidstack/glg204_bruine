/* Mouvement partagé des cartes : inclinaison 3D au survol et glisser à inertie.
 *
 * Ce module ne connaît aucune page. Il anime la carte et signale les moments utiles ;
 * chaque page décide ensuite ce qu'un dépôt veut dire chez elle.
 *
 * API :
 *   CardMotion.enableHover(carte)
 *   CardMotion.enableDrag(carte, handlers)
 *   CardMotion.isOver(element, evenement)
 *   CardMotion.release()    interrompt le glisser sans dépôt et rend la copie
 *
 * handlers accepte trois fonctions, toutes facultatives :
 *   onStart(carte)          renvoyer false pour refuser le glisser
 *   onMove(evenement)       à chaque déplacement, pour surligner une cible
 *   onDrop(evenement, carte) au relâchement, une fois la copie retirée
 */
window.CardMotion = (function () {

    /* Une seule carte peut être glissée à la fois. */
    let drag = null;
    const mouse = {x: 0, y: 0, px: 0, py: 0};
    const vel = {x: 0, y: 0};
    const lerp = {rotZ: 0, rotX: 0, skewX: 0, skewY: 0};
    let rafId = null;

    function clamp(value, min, max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Vrai si le pointeur se trouve dans le rectangle de l'élément. */
    function isOver(element, event) {
        if (!element) return false;
        const r = element.getBoundingClientRect();
        return event.clientX >= r.left && event.clientX <= r.right
            && event.clientY >= r.top && event.clientY <= r.bottom;
    }

    function paintShine(card, background) {
        const shine = card.querySelector('.card-shine');
        if (shine) shine.style.background = background;
    }

    /* Survol : la carte s'incline vers le curseur et son reflet le suit. */
    function onHover(card, event) {
        if (drag) return;
        const r = card.getBoundingClientRect();
        const dx = ((event.clientX - r.left) / r.width - 0.5) * 2;
        const dy = ((event.clientY - r.top) / r.height - 0.5) * 2;

        card.style.transform = 'perspective(520px)'
            + ' rotateX(' + (-dy * 13) + 'deg)'
            + ' rotateY(' + (dx * 17) + 'deg)'
            + ' scale(1.07) translateZ(6px)';

        const sx = ((event.clientX - r.left) / r.width) * 100;
        const sy = ((event.clientY - r.top) / r.height) * 100;
        paintShine(card, 'radial-gradient(circle at ' + sx + '% ' + sy + '%,'
            + ' rgba(255,255,255,0.30) 0%, transparent 58%)');
    }

    function onLeave(card) {
        if (drag && drag.card === card) return;
        card.style.transition = 'transform 0.45s cubic-bezier(0.23,1,0.32,1)';
        card.style.transform = '';
        paintShine(card, '');
        setTimeout(function () { card.style.transition = ''; }, 450);
    }

    /* Glisser : une copie suit le curseur, l'originale reste en place et s'efface. */
    function startDrag(card, handlers, event) {
        if (drag) return;
        if (handlers.onStart && handlers.onStart(card) === false) return;

        event.preventDefault();
        card.setPointerCapture(event.pointerId);
        const r = card.getBoundingClientRect();

        const clone = card.cloneNode(true);
        clone.classList.add('drag-clone');
        clone.classList.remove('ghost');
        clone.style.width = r.width + 'px';
        clone.style.height = r.height + 'px';
        clone.style.left = r.left + 'px';
        clone.style.top = r.top + 'px';
        clone.style.transform = '';
        document.body.appendChild(clone);

        card.classList.add('ghost');

        drag = {
            card: card,
            clone: clone,
            handlers: handlers,
            offX: event.clientX - r.left,
            offY: event.clientY - r.top
        };

        mouse.x = mouse.px = event.clientX;
        mouse.y = mouse.py = event.clientY;
        vel.x = vel.y = 0;
        lerp.rotZ = lerp.rotX = lerp.skewX = lerp.skewY = 0;

        rafId = requestAnimationFrame(animate);
    }

    /* Déformation pilotée par la vitesse du curseur, lissée pour donner de l'inertie. */
    function animate() {
        if (!drag) return;

        const targetRotZ = clamp(vel.x * 1.6, -24, 24);
        const targetRotX = clamp(-vel.y * 1.6, -24, 24);
        const targetSkewX = clamp(vel.x * 0.9, -16, 16);
        const targetSkewY = clamp(vel.y * 0.45, -9, 9);

        lerp.rotZ += (targetRotZ - lerp.rotZ) * 0.22;
        lerp.rotX += (targetRotX - lerp.rotX) * 0.22;
        lerp.skewX += (targetSkewX - lerp.skewX) * 0.22;
        lerp.skewY += (targetSkewY - lerp.skewY) * 0.22;

        /* La vitesse retombe seule, elle se reconstruit au prochain déplacement. */
        vel.x *= 0.80;
        vel.y *= 0.80;

        drag.clone.style.transform = 'perspective(600px)'
            + ' rotateX(' + lerp.rotX + 'deg)'
            + ' rotateZ(' + lerp.rotZ + 'deg)'
            + ' skewX(' + lerp.skewX + 'deg)'
            + ' skewY(' + lerp.skewY + 'deg)'
            + ' scale(1.10)';

        rafId = requestAnimationFrame(animate);
    }

    function onPointerMove(event) {
        if (!drag) return;

        mouse.px = mouse.x;
        mouse.py = mouse.y;
        mouse.x = event.clientX;
        mouse.y = event.clientY;
        vel.x = mouse.x - mouse.px;
        vel.y = mouse.y - mouse.py;

        drag.clone.style.left = (mouse.x - drag.offX) + 'px';
        drag.clone.style.top = (mouse.y - drag.offY) + 'px';

        if (drag.handlers.onMove) drag.handlers.onMove(event);
    }

    function onPointerUp(event) {
        if (!drag) return;

        /* On libère l'état avant d'appeler la page : elle peut déplacer la carte. */
        const finished = drag;
        drag = null;

        cancelAnimationFrame(rafId);
        finished.clone.remove();
        finished.card.classList.remove('ghost');
        finished.card.style.transform = '';
        paintShine(finished.card, '');

        if (finished.handlers.onDrop) finished.handlers.onDrop(event, finished.card);
    }

    document.addEventListener('pointermove', onPointerMove);
    document.addEventListener('pointerup', onPointerUp);

    return {
        isOver: isOver,

        enableHover: function (card) {
            card.addEventListener('pointermove', function (e) { onHover(card, e); });
            card.addEventListener('pointerleave', function () { onLeave(card); });
        },

        enableDrag: function (card, handlers) {
            const bound = handlers || {};
            card.addEventListener('pointerdown', function (e) { startDrag(card, bound, e); });
        },

        /* La copie et la carte restent en l'état (copie affichée, carte effacée) : la page les termine. */
        release: function () {
            if (!drag) return null;
            const clone = drag.clone;
            drag = null;
            cancelAnimationFrame(rafId);
            return clone;
        }
    };
})();
