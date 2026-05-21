(function () {
  const apply = (theme) => {
    const next = theme === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    document.querySelectorAll("[data-theme-label]").forEach((node) => {
      node.textContent = next === "light" ? "浅色" : "深色";
    });
  };
  const initial = new URLSearchParams(location.search).get("theme")
    || "dark";
  apply(initial);
  window.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        const next = document.documentElement.dataset.theme === "light" ? "dark" : "light";
        apply(next);
      });
    });
  });
})();
