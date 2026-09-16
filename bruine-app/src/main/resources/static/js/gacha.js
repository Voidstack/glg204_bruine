/* Le tirage est fait par le serveur : ce script fait secouer un paquet, puis soumet le
   formulaire de la page, et c'est Thymeleaf qui rend les cartes obtenues. Le survol et le
   glisser viennent de card-motion.js. */
(function () {
    const PACK_BAR_COLOR = {1: '#7dd3fc, #38bdf8', 5: '#c4b5fd, #a78bfa', 10: '#fde68a, #fbbf24'};
    const SHAKES_NEEDED = 5;
    const SHAKE_DISTANCE = 25;

    const bar = document.getElementById('shake-bar-fixed');
    const barInner = document.getElementById('shake-bar-inner');
    const barLabel = document.getElementById('shake-bar-label');

    let pack = null;
    let opening = false;
    let shakeCount = 0;
    let lastShakeTime = 0;
    let anchorX = null;
    let direction = 0;

    document.querySelectorAll('.pack-card:not(.pack-unavailable)').forEach(card => {
        CardMotion.enableHover(card);
        CardMotion.enableDrag(card, {onStart: startShaking, onMove: detectShake, onDrop: hideShakeBar});
    });
    revealCards();

    /* Les cartes sont rendues par le serveur mais masquées par le CSS : on les découvre
       une à une, pour que le joueur voie son tirage se retourner plutôt que s'afficher. */
    function revealCards() {
        document.querySelectorAll('#resultats .gacha-card').forEach((card, i) => {
            setTimeout(() => card.classList.add('revealed'), 80 + i * 120);
        });
    }

    function startShaking(card) {
        if (opening) return false;
        pack = card;
        shakeCount = 0;
        lastShakeTime = 0;
        anchorX = null;
        direction = 0;

        barInner.style.width = '0%';
        barInner.style.background = `linear-gradient(90deg, ${PACK_BAR_COLOR[card.dataset.count]})`;
        barLabel.textContent = 'Secouez !';
        bar.style.display = 'flex';
    }

    /* Une secousse est un changement de sens horizontal après SHAKE_DISTANCE pixels : mesurée en distance
       et non entre deux événements, dont la fréquence varie (plus élevée DevTools ouverts). */
    function detectShake(event) {
        if (anchorX === null) {
            anchorX = event.clientX;
            return;
        }
        const dx = event.clientX - anchorX;
        if (Math.abs(dx) < SHAKE_DISTANCE) return;

        const now = Date.now();
        if (direction !== 0 && Math.sign(dx) !== direction && now - lastShakeTime > 90) {
            lastShakeTime = now;
            shakeCount++;
            onShake();
        }
        direction = Math.sign(dx);
        anchorX = event.clientX;
    }

    function onShake() {
        const progress = Math.min(shakeCount / SHAKES_NEEDED, 1);
        barInner.style.width = (progress * 100) + '%';
        if (shakeCount >= SHAKES_NEEDED) {
            barLabel.textContent = '💥 Ouverture !';
            openPack();
        } else if (progress > 0.55) {
            barLabel.textContent = 'Encore un peu...';
        }
    }

    function openPack() {
        const clone = CardMotion.release();
        if (!clone) return;
        opening = true;

        clone.style.transition = 'none';
        clone.style.transformOrigin = 'center center';
        clone.style.animation = 'packExplode 0.42s ease forwards';

        setTimeout(() => {
            clone.remove();
            pack.classList.remove('ghost');
            hideShakeBar();
            submitPull(pack.dataset.count);
        }, 420);
    }

    function hideShakeBar() {
        bar.style.display = 'none';
    }

    /* Un POST de formulaire classique : la page revient rendue par le serveur, score et
       paquets indisponibles compris, ce qui évite de tenir un second état côté navigateur. */
    function submitPull(count) {
        document.getElementById('spin-count').value = count;
        document.getElementById('spin-form').submit();
    }
})();
