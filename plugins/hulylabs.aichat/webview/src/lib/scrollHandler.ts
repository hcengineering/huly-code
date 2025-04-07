function initScrollHandler() {
  const scrollableElements = document.querySelectorAll('*');
  scrollableElements.forEach(element => {
    if (getComputedStyle(element).overflow === 'auto' || getComputedStyle(element).overflow === 'scroll' ||
        getComputedStyle(element).overflowY === 'auto' || getComputedStyle(element).overflowY === 'scroll') {
      let scrollingTimeout: number;
      element.addEventListener('scroll', () => {
        element.classList.add('scrolling');
        clearTimeout(scrollingTimeout);
        scrollingTimeout = window.setTimeout(() => {
          element.classList.remove('scrolling');
        }, 1000);
      });
    }
  });
}

export default initScrollHandler;
