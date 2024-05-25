document.addEventListener('DOMContentLoaded', (event) => {
  document
    .getElementById('info-tab-button-row')
    .addEventListener('click', function () {
      const hero = document.querySelector('.hero');
      const heroContent = document.querySelector('.hero-content');
      const icon = document.querySelector('#info-tab-button-row img');

      if (hero.style.height === '60px') {
        expandHero(hero, heroContent, icon);
      } else {
        collapseHero(hero, heroContent, icon);
      }
    });

  function expandHero(hero, heroContent, icon) {
    hero.style.height = '396px';
    hero.style.padding = '60px 0 80px';
    icon.src = '../assets/icons/layout-navbar-collapse.svg';
    setTimeout(() => {
      heroContent.style.display = '';
      heroContent.classList.add('show');
    }, 100);
  }

  function collapseHero(hero, heroContent, icon) {
    hero.style.height = '60px';
    hero.style.padding = '0';
    icon.src = '../assets/icons/layout-navbar-expand.svg';
    heroContent.classList.remove('show');
    setTimeout(() => {
      heroContent.style.display = 'none';
    }, 300);
  }

  document.querySelectorAll('.dataset-tag').forEach((tag) => {
    tag.addEventListener('click', function () {
      if (this.classList.contains('dataset-tag-active')) {
        deactivateTag(this);
      } else {
        deactivateAllTags();
        activateTag(this);
      }
      toggleSelectedFilterView();
    });
  });

  function deactivateAllTags() {
    document.querySelectorAll('.dataset-tag').forEach((tag) => {
      tag.classList.remove('dataset-tag-active');
      const icon = tag.getAttribute('data-icon');
      tag.querySelector('.icon').src = `../assets/icons/${icon}-icon.svg`;
    });
  }

  function activateTag(tag) {
    tag.classList.add('dataset-tag-active');
    const activeIcon = tag.getAttribute('data-icon');
    tag.querySelector(
      '.icon'
    ).src = `../assets/icons/${activeIcon}-icon-blue.svg`;

    const text = tag.textContent.trim();
    document.getElementById('active-tag-text').textContent = text;
  }

  function deactivateTag(tag) {
    tag.classList.remove('dataset-tag-active');
    const icon = tag.getAttribute('data-icon');
    tag.querySelector('.icon').src = `../assets/icons/${icon}-icon.svg`;

    // Clear the text inside the <p> element or set it to default
    document.getElementById('active-tag-text').textContent = 'Default text';
  }

  function toggleSelectedFilterView() {
    const selectedFilterView = document.querySelector('.selected-filter-view');
    const activeTags = document.querySelectorAll('.dataset-tag-active');
    if (activeTags.length === 0) {
      selectedFilterView.style.display = 'none';
    } else {
      selectedFilterView.style.display = 'block';
    }
  }

  // Initially hide the selected-filter-view element
  toggleSelectedFilterView();

  const selectWrapper = document.querySelector('.custom-select-wrapper');
  const selectBox = document.querySelector('.custom-select');
  const selectElement = selectBox.querySelector('select');
  const selectedDisplay = document.createElement('div');
  selectedDisplay.className = 'selected-display';
  selectedDisplay.textContent =
    selectElement.options[selectElement.selectedIndex].textContent;
  selectBox.insertBefore(selectedDisplay, selectElement);

  const selectContent = document.createElement('div');
  selectContent.className = 'custom-select-content';

  const options = selectElement.options;
  for (let option of options) {
    const optionDiv = document.createElement('div');
    optionDiv.textContent = option.textContent;
    optionDiv.setAttribute('data-value', option.value);
    selectContent.appendChild(optionDiv);
  }
  selectBox.appendChild(selectContent);

  selectBox.addEventListener('click', function () {
    selectBox.classList.toggle('open');
  });

  selectContent.addEventListener('click', function (e) {
    if (e.target.hasAttribute('data-value')) {
      const selectedValue = e.target.getAttribute('data-value');
      selectElement.value = selectedValue;
      selectedDisplay.textContent = e.target.textContent;
      selectBox.classList.remove('open');
    }
  });

  document.addEventListener('click', function (e) {
    if (!selectBox.contains(e.target)) {
      selectBox.classList.remove('open');
    }
  });
});
