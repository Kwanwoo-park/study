const slides = Array.from(document.querySelectorAll(".slide"));
const toc = document.getElementById("toc");
const progressBar = document.getElementById("progressBar");
const slideCounter = document.getElementById("slideCounter");
const prevBtn = document.getElementById("prevBtn");
const nextBtn = document.getElementById("nextBtn");
const savePdfBtn = document.getElementById("savePdfBtn");
const pdfSaveStatus = document.getElementById("pdfSaveStatus");

const mobileUaTokens = [
    "mobile", "android", "iphone", "ipad", "ipod",
    "blackberry", "windows phone", "opera mini", "iemobile"
];

let currentIndex = 0;

function applyMobileViewClass() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isMobile = mobileUaTokens.some((token) => userAgent.includes(token));
    document.body.classList.toggle("mobile-view", isMobile);
}

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

function savePortfolioPdf() {
    pdfSaveStatus.hidden = false;
    pdfSaveStatus.textContent = "인쇄 창에서 'PDF로 저장'을 선택해 주세요. 창이 열리지 않으면 브라우저의 인쇄·공유 메뉴를 이용해 주세요.";

    if (typeof window.print !== "function") {
        pdfSaveStatus.textContent = "이 환경에서는 인쇄를 지원하지 않습니다. Chrome 또는 Safari에서 이 페이지를 열어 PDF로 저장해 주세요.";
        return;
    }

    try {
        // Print CSS exposes every slide without changing the current screen position.
        window.print();
    } catch (error) {
        pdfSaveStatus.textContent = "인쇄 창을 열지 못했습니다. 브라우저의 인쇄·공유 메뉴를 이용하거나 Chrome 또는 Safari에서 다시 시도해 주세요.";
    }
}

prevBtn.addEventListener("click", prevSlide);
nextBtn.addEventListener("click", nextSlide);
savePdfBtn.addEventListener("click", savePortfolioPdf);

document.addEventListener("keydown", (event) => {
    if (event.key === "ArrowRight") {
        event.preventDefault();
        nextSlide();
    }

    if (event.key === "ArrowLeft") {
        event.preventDefault();
        prevSlide();
    }
});

applyMobileViewClass();
renderToc();
updateDeck();
