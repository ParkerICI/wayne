// Almost all the stuff that was in this Munson file is now handled in cljs

document.addEventListener('DOMContentLoaded', (event) => {
    document
	.getElementById('info-tab-button-row')
	.addEventListener('click', function () {
	    const hero = document.querySelector('.hero');
	    const heroContent = document.querySelector('.hero-content');
	    console.log("foo", hero, this);
	    if (hero.style.height === '180px') {
		expandHero(hero, heroContent, this);
	    } else {
		collapseHero(hero, heroContent, this);
	    }
	});

    function expandHero(hero, heroContent, button) {
	hero.style.height = '504px';
	hero.style.padding = '60px 0 80px';
	var icon = button.children[0];
	var label = button.children[1];
	icon.src = '../assets/icons/merge-horizontal.svg';
	label.textContent = 'Hide overview';

	setTimeout(() => {
	    heroContent.style.display = '';
	    heroContent.classList.add('show');
	}, 100);
    }

    function collapseHero(hero, heroContent, button) {
	hero.style.height = '180px';
	hero.style.padding = '64px';
	var icon = button.children[0];
	var label = button.children[1];
	console.log("bar", icon, label);
	icon.src = '../assets/icons/show-info.svg';
	label.textContent = 'Show overview';
	heroContent.classList.remove('show');
	setTimeout(() => {
	    heroContent.style.display = 'none';
	}, 300);
    }
    
});
