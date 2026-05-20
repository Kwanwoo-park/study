const slides = Array.from(document.querySelectorAll(".slide"));
const toc = document.getElementById("toc");
const progressBar = document.getElementById("progressBar");
const slideCounter = document.getElementById("slideCounter");
const prevBtn = document.getElementById("prevBtn");
const nextBtn = document.getElementById("nextBtn");

let currentIndex = 0;

function renderToc() {
    slides.forEach((slide, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = `${String(index + 1).padStart(2, "0")} ${slide.dataset.title}`;
        button.addEventListener("click", () => goToSlide(index));
        toc.appendChild(button);
    });
}

function updateDeck() {
    slides.forEach((slide, index) => {
        slide.classList.toggle("active", index === currentIndex);
    });

    Array.from(toc.children).forEach((button, index) => {
        button.classList.toggle("active", index === currentIndex);
        button.setAttribute("aria-current", index === currentIndex ? "step" : "false");
    });

    const progress = ((currentIndex + 1) / slides.length) * 100;
    progressBar.style.width = `${progress}%`;
    slideCounter.textContent = `${currentIndex + 1} / ${slides.length}`;
    prevBtn.disabled = currentIndex === 0;
    nextBtn.disabled = currentIndex === slides.length - 1;
}

function goToSlide(index) {
    currentIndex = Math.max(0, Math.min(index, slides.length - 1));
    updateDeck();
}

function nextSlide() {
    goToSlide(currentIndex + 1);
}

function prevSlide() {
    goToSlide(currentIndex - 1);
}

prevBtn.addEventListener("click", prevSlide);
nextBtn.addEventListener("click", nextSlide);

document.addEventListener("keydown", (event) => {
    if (event.key === "ArrowRight" || event.key === "PageDown" || event.key === " ") {
        event.preventDefault();
        nextSlide();
    }

    if (event.key === "ArrowLeft" || event.key === "PageUp") {
        event.preventDefault();
        prevSlide();
    }

    if (event.key === "Home") {
        event.preventDefault();
        goToSlide(0);
    }

    if (event.key === "End") {
        event.preventDefault();
        goToSlide(slides.length - 1);
    }
});

let touchStartX = 0;

document.addEventListener("touchstart", (event) => {
    touchStartX = event.changedTouches[0].screenX;
}, { passive: true });

document.addEventListener("touchend", (event) => {
    const deltaX = event.changedTouches[0].screenX - touchStartX;

    if (Math.abs(deltaX) < 60) {
        return;
    }

    if (deltaX < 0) {
        nextSlide();
    } else {
        prevSlide();
    }
}, { passive: true });

renderToc();
updateDeck();
