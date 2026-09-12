function copyDeckLink(btn) {
    navigator.clipboard.writeText(btn.dataset.deckMarkdown).then(function () {
        const original = btn.innerHTML;
        btn.innerHTML = '<i class="fa-solid fa-check"></i>&nbsp; Copié !';
        setTimeout(function () {
            btn.innerHTML = original;
        }, 1500);
    });
}

// pour la liste de jeu async
document.addEventListener('DOMContentLoaded', function () {
    const steamId = document.getElementById('games-section').dataset.steamid;

    fetch('/steam/profile/' + steamId + '/games')
        .then(r => r.json())
        .then(data => {
            // Mettre à jour le total
            const h = Math.floor(data.totalPlaytimeMinutes / 60);
            const m = data.totalPlaytimeMinutes % 60;
            document.getElementById('total-playtime').textContent = h + 'h ' + m + 'min';

            document.getElementById('games-loading').style.display = 'none';

            if (data.games.length === 0) {
                document.getElementById('games-empty').style.display = 'block';
                return;
            }

            const countEl = document.getElementById('games-count');
            countEl.textContent = data.games.length + ' jeu(x) avec plus d\'une heure de jeu';
            countEl.style.display = 'block';

            const list = document.getElementById('games-list');
            data.games.forEach(function (game, i) {
                const iconUrl = game.iconHash
                    ? 'https://media.steampowered.com/steamcommunity/public/images/apps/' + game.appId + '/' + game.iconHash + '.jpg'
                    : null;
                const hours = Math.floor(game.playtimeMinutes / 60);

                const row = document.createElement('div');
                row.className = 'game-row';
                row.style.cssText = 'display:flex;align-items:center;gap:0.75rem;padding:0.5rem 0.75rem;background:var(--bg-panel);border-radius:6px;animation-delay:' + (i * 25) + 'ms';

                if (iconUrl) {
                    const img = document.createElement('img');
                    img.src = iconUrl;
                    img.alt = game.name;
                    img.style.cssText = 'width:32px;height:32px;border-radius:4px;flex-shrink:0;';
                    row.appendChild(img);
                }

                const name = document.createElement('span');
                name.textContent = game.name;
                name.style.cssText = 'color:var(--text);font-size:0.9rem;flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';
                row.appendChild(name);

                const time = document.createElement('span');
                time.textContent = hours + 'h';
                time.style.cssText = 'color:var(--accent);font-size:0.85rem;flex-shrink:0;';
                row.appendChild(time);

                list.appendChild(row);
            });
        })
        .catch(function () {
            document.getElementById('games-loading').style.display = 'none';
            document.getElementById('games-empty').style.display = 'block';
            document.getElementById('total-playtime').textContent = '—';
        });
});
