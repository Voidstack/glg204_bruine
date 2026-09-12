(function () {

    // Burger menu pour mobile, menu de la navbar sur petit écran.
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.navbar-burger').forEach(burger => {
            burger.addEventListener('click', () => {
                const menu = document.getElementById(burger.dataset.target);
                burger.classList.toggle('is-active');
                menu.classList.toggle('is-active');
            });
        });
    });
})();


