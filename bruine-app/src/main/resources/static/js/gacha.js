/* Le tirage et fait par le serveur ce script se contente de soumettre le
   formulaire de la page, et c'est Thymeleaf qui rend les cartes obtenues. */

const PACK_BAR_COLOR = {1: '#7dd3fc, #38bdf8', 5: '#c4b5fd, #a78bfa', 10: '#fde68a, #fbbf24'};
const SHAKES_NEEDED = 5;

let drag = null;
const mouse = {x: 0, y: 0, px: 0, py: 0};
const vel = {x: 0, y: 0};
const lerp = {rotZ: 0, skewX: 0, skewY: 0};
let rafId = null;

let shakeCount = 0;
let lastShakeTime = 0;
let lastVX = 0;

function clamp(v, mn, mx) {
    return Math.max(mn, Math.min(mx, v));
}

/* Bind pack cards */
function bindPackCards() {
    document.querySelectorAll('.pack-card').forEach(card => {
        const count = parseInt(card.dataset.count);
        card.addEventListener('pointermove', e => onHover(card, e));
        card.addEventListener('pointerleave', () => onLeave(card));
        card.addEventListener('pointerdown', e => startDrag(card, count, e));
    });
}

document.addEventListener('DOMContentLoaded', () => {
    bindPackCards();
    revealCards();
});

/* Révélation en cascade des cartes tirées */

/* Les cartes sont rendues par le serveur mais masquées par le CSS : on les découvre
   une à une, pour que le joueur voie son tirage se retourner plutôt que s'afficher. */
function revealCards() {
    document.querySelectorAll('#resultats .gacha-card').forEach((card, i) => {
        setTimeout(() => card.classList.add('revealed'), 80 + i * 120);
    });
}

/* Hover 3D tilt */
function onHover(card, e) {
    if (drag || card.classList.contains('pack-unavailable')) return;
    const r = card.getBoundingClientRect();
    const dx = ((e.clientX - r.left) / r.width - 0.5) * 2;
    const dy = ((e.clientY - r.top) / r.height - 0.5) * 2;
    card.style.transform = `perspective(520px) rotateX(${-dy * 13}deg) rotateY(${dx * 17}deg) scale(1.07) translateZ(6px)`;
    const sx = ((e.clientX - r.left) / r.width) * 100;
    const sy = ((e.clientY - r.top) / r.height) * 100;
    card.querySelector('.card-shine').style.background = `radial-gradient(circle at ${sx}% ${sy}%, rgba(255,255,255,0.28) 0%, transparent 58%)`;
}

function onLeave(card) {
    if (drag && drag.card === card) return;
    card.style.transition = 'transform 0.45s cubic-bezier(0.23,1,0.32,1)';
    card.style.transform = '';
    card.querySelector('.card-shine').style.background = '';
    setTimeout(() => {
        card.style.transition = '';
    }, 450);
}

/* Drag start */
function startDrag(card, count, e) {
    if (card.classList.contains('pack-unavailable') || drag) return;
    e.preventDefault();
    card.setPointerCapture(e.pointerId);
    const r = card.getBoundingClientRect();

    const clone = card.cloneNode(true);
    clone.classList.add('drag-clone');
    clone.style.width = r.width + 'px';
    clone.style.height = r.height + 'px';
    clone.style.left = r.left + 'px';
    clone.style.top = r.top + 'px';
    document.body.appendChild(clone);

    card.classList.add('ghost');
    drag = {card, clone, count, offX: e.clientX - r.left, offY: e.clientY - r.top, opened: false};

    mouse.x = mouse.px = e.clientX;
    mouse.y = mouse.py = e.clientY;
    vel.x = vel.y = 0;
    lerp.rotZ = lerp.skewX = lerp.skewY = 0;
    shakeCount = 0;
    lastVX = 0;
    lastShakeTime = 0;

    document.getElementById('shake-bar-inner').style.width = '0%';
    document.getElementById('shake-bar-inner').style.background = `linear-gradient(90deg, ${PACK_BAR_COLOR[count]})`;
    document.getElementById('shake-bar-label').textContent = 'Secouez !';
    document.getElementById('shake-bar-fixed').style.display = 'flex';

    rafId = requestAnimationFrame(animateDrag);
}

/* Drag physics loop */
function animateDrag() {
    if (!drag) return;
    const tRotZ = clamp(vel.x * 1.6, -24, 24);
    const tSkewX = clamp(vel.x * 0.9, -16, 16);
    const tSkewY = clamp(vel.y * 0.45, -9, 9);
    lerp.rotZ += (tRotZ - lerp.rotZ) * 0.22;
    lerp.skewX += (tSkewX - lerp.skewX) * 0.22;
    lerp.skewY += (tSkewY - lerp.skewY) * 0.22;
    vel.x *= 0.80;
    vel.y *= 0.80;
    drag.clone.style.transform = `perspective(600px) rotateZ(${lerp.rotZ}deg) skewX(${lerp.skewX}deg) skewY(${lerp.skewY}deg) scale(1.10)`;
    rafId = requestAnimationFrame(animateDrag);
}

/* Mouse move + shake detection */
document.addEventListener('pointermove', e => {
    if (!drag || drag.opened) return;

    mouse.px = mouse.x;
    mouse.py = mouse.y;
    mouse.x = e.clientX;
    mouse.y = e.clientY;
    vel.x = mouse.x - mouse.px;
    vel.y = mouse.y - mouse.py;

    drag.clone.style.left = (e.clientX - drag.offX) + 'px';
    drag.clone.style.top = (e.clientY - drag.offY) + 'px';

    const vx = vel.x;
    const now = Date.now();
    if (Math.abs(vx) > 5 && lastVX !== 0 && Math.sign(vx) !== Math.sign(lastVX) && now - lastShakeTime > 90) {
        lastShakeTime = now;
        shakeCount++;
        onShake();
    }
    if (Math.abs(vx) > 3) lastVX = vx;
});

/* Shake feedback */
function onShake() {
    const progress = Math.min(shakeCount / SHAKES_NEEDED, 1);
    document.getElementById('shake-bar-inner').style.width = (progress * 100) + '%';
    if (shakeCount >= SHAKES_NEEDED) {
        document.getElementById('shake-bar-label').textContent = '💥 Ouverture !';
        openPack();
    } else if (progress > 0.55) {
        document.getElementById('shake-bar-label').textContent = 'Encore un peu...';
    }
}

/* Pack open */
function openPack() {
    if (!drag || drag.opened) return;
    drag.opened = true;
    cancelAnimationFrame(rafId);

    const clone = drag.clone;
    const count = drag.count;
    clone.style.transition = 'none';
    clone.style.transformOrigin = 'center center';
    clone.style.animation = 'packExplode 0.42s ease forwards';

    setTimeout(() => {
        clone.remove();
        drag.card.classList.remove('ghost');
        document.getElementById('shake-bar-fixed').style.display = 'none';
        drag = null;
        submitPull(count);
    }, 420);
}

/* Pointer up, cancel if not opened */
document.addEventListener('pointerup', () => {
    if (!drag || drag.opened) return;
    cancelAnimationFrame(rafId);
    drag.clone.remove();
    drag.card.classList.remove('ghost');
    onLeave(drag.card);
    document.getElementById('shake-bar-fixed').style.display = 'none';
    drag = null;
});

/* Envoi du tirage */

/* Un POST de formulaire classique : la page revient rendue par le serveur, score et
   paquets indisponibles compris, ce qui évite de tenir un second état côté navigateur. */
function submitPull(count) {
    document.getElementById('spin-count').value = count;
    document.getElementById('spin-form').submit();
}
