document.addEventListener('DOMContentLoaded', function () {
  const accordionItems = document.querySelectorAll('.accordion-item');

  accordionItems.forEach((item) => {
    const icon = item.querySelector('.accordion-icon');

    item.addEventListener('click', function () {
      item.classList.toggle('active');

      if (item.classList.contains('active')) {
        icon.src = '../assets/icons/minus.svg';
      } else {
        icon.src = '../assets/icons/plus.svg';
      }

      accordionItems.forEach((otherItem) => {
        if (otherItem !== item) {
          otherItem.classList.remove('active');
          otherItem.querySelector('.accordion-icon').src =
            '../assets/icons/plus.svg';
        }
      });
    });
  });
});

