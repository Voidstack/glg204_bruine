/* Level Up - drag & drop des cartes vers la Steam Machine + conversion en XP */
(function () {
    const queue = document.getElementById('machine-queue');
    const xpPreview = document.getElementById('xp-preview');
    const activateBtn = document.getElementById('activate-btn');
    const machineBody = document.getElementById('machine-body');
    const resultEl = document.getElementById('machine-result');
    const convertForm = document.getElementById('convert-form');

    // Page sans machine (inventaire vide) : rien à câbler.
    if (!machineBody) return;

    if (resultEl.classList.contains('show')) {
        setTimeout(() => resultEl.classList.remove('show'), 3500);
    }

    // pending[]: {rewardId, finish, xp, name, emoji}
    const pending = [];
    // usedCount: how many of each slot are already in the machine
    const usedCount = {};

    /* Cartes : survol et glisser vers la machine */
    /* Le mouvement vient de card-motion.js. Ici on ne décrit que la règle du level up :
       une carte lâchée sur la machine rejoint la chambre de combustion. */
    document.querySelectorAll('.card-slot').forEach(slot => {
        const card = slot.querySelector('.inv-card');
        if (!card) return;

        CardMotion.enableHover(card);
        CardMotion.enableDrag(card, {
            onStart: () => !isExhausted(slot),
            onMove: e => machineBody.classList.toggle('drag-over', CardMotion.isOver(machineBody, e)),
            onDrop: e => {
                machineBody.classList.remove('drag-over');
                if (CardMotion.isOver(machineBody, e)) loadIntoMachine(slot);
            }
        });
    });

    /** Vrai si tous les exemplaires de cette carte sont déjà dans la machine. */
    function isExhausted(slot) {
        const {rewardId, finish, count} = slot.dataset;
        return (usedCount[slotKey(rewardId, finish)] ?? 0) >= parseInt(count);
    }

    function loadIntoMachine(slot) {
        if (isExhausted(slot)) return;

        const {rewardId, finish, xp, count, name, emoji} = slot.dataset;
        const key = slotKey(rewardId, finish);

        usedCount[key] = (usedCount[key] ?? 0) + 1;
        pending.push({rewardId, finish, xp: parseInt(xp), name, emoji});

        renderQueue();
        updateSlotDisplay(rewardId, finish, parseInt(count));
    }

    /* Queue rendering */
    function renderQueue() {
        // rebuild queue items
        queue.querySelectorAll('.queue-item').forEach(n => n.remove());

        pending.forEach((item, idx) => {
            const div = document.createElement('div');
            div.className = 'queue-item';
            div.innerHTML = `
                <span class="queue-emoji">${item.emoji}</span>
                <div class="queue-name">${item.name}</div>
                <span class="queue-xp">+${item.xp}</span>
                <button class="queue-remove" title="Retirer" data-idx="${idx}">&#10005;</button>`;
            queue.appendChild(div);
        });

        // remove buttons
        queue.querySelectorAll('.queue-remove').forEach(btn => {
            btn.addEventListener('click', () => removeFromQueue(parseInt(btn.dataset.idx)));
        });

        convertForm.querySelectorAll('input[name^="items["]').forEach(n => n.remove());
        pending.forEach((item, idx) => {
            addHidden(`items[${idx}].rewardId`, item.rewardId);
            addHidden(`items[${idx}].finish`, item.finish);
        });

        // XP total
        const total = pending.reduce((s, i) => s + i.xp, 0);
        const prev = parseInt(xpPreview.textContent) || 0;
        xpPreview.textContent = total + ' XP';
        if (total !== prev) {
            xpPreview.classList.remove('bump');
            void xpPreview.offsetWidth;
            xpPreview.classList.add('bump');
        }

        // button state
        if (pending.length > 0) {
            activateBtn.disabled = false;
            activateBtn.classList.add('ready');
            machineBody.classList.add('active');
        } else {
            activateBtn.disabled = true;
            activateBtn.classList.remove('ready');
            machineBody.classList.remove('active');
        }
    }

    function removeFromQueue(idx) {
        const item = pending[idx];
        const key = slotKey(item.rewardId, item.finish);
        usedCount[key] = Math.max(0, (usedCount[key] ?? 1) - 1);
        pending.splice(idx, 1);

        // find original count from slot
        const slot = document.getElementById(`slot-${item.rewardId}-${item.finish}`);
        const count = slot ? parseInt(slot.dataset.count) : 1;
        updateSlotDisplay(item.rewardId, item.finish, count);
        renderQueue();
    }

    /* Slot display (exhausted state) */
    function updateSlotDisplay(rewardId, finish, totalCount) {
        const key = slotKey(rewardId, finish);
        const used = usedCount[key] ?? 0;
        const slot = document.getElementById(`slot-${rewardId}-${finish}`);
        if (!slot) return;
        const remaining = totalCount - used;
        slot.dataset.exhausted = remaining <= 0 ? 'true' : 'false';
        // update count badge
        // Le compteur est rendu par le fragment de carte partagé, on le cherche dans la carte.
        const countEl = slot.querySelector('.card-count');
        if (countEl) countEl.textContent = '×' + remaining;
    }

    /* Activate : la conversion est un POST de formulaire classique, la page revient rechargée */
    convertForm.addEventListener('submit', e => {
        if (pending.length === 0) {
            e.preventDefault();
            return;
        }
        activateBtn.classList.remove('ready');
        activateBtn.classList.add('firing');
    });

    /* Helpers */
    function slotKey(rewardId, finish) {
        return `${rewardId}_${finish}`;
    }

    function addHidden(name, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        convertForm.appendChild(input);
    }
})();
