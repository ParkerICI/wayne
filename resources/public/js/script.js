// Almost all the stuff that was in this Munson file is now handled in cljs


document.addEventListener('DOMContentLoaded', (event) => {
  document
    .getElementById('info-tab-button-row')
    .addEventListener('click', function () {
      const hero = document.querySelector('.hero');
      const heroContent = document.querySelector('.hero-content');
      const icon = document.querySelector('#info-tab-button-row img');

      if (hero.style.height === '180px') {
        expandHero(hero, heroContent, icon);
      } else {
        collapseHero(hero, heroContent, icon);
      }
    });


  function expandHero(hero, heroContent, icon) {
    hero.style.height = '504px';
    hero.style.padding = '60px 0 80px';
    icon.src = '../assets/icons/merge-horizontal.svg';
    setTimeout(() => {
      heroContent.style.display = '';
      heroContent.classList.add('show');
    }, 100);
  }

  function collapseHero(hero, heroContent, icon) {
    hero.style.height = '180px';
    hero.style.padding = '64px';
    icon.src = '../assets/icons/show-info.svg';
    heroContent.classList.remove('show');
    setTimeout(() => {
      heroContent.style.display = 'none';
    }, 300);
  }

});
