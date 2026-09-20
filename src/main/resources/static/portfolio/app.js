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

async function savePortfolioPdf() {
    if (savePdfBtn.disabled) return;
    savePdfBtn.disabled = true;
    pdfSaveStatus.hidden = false;
    pdfSaveStatus.textContent = "PDF를 준비하고 있습니다. 처음 저장할 때는 잠시 걸릴 수 있습니다.";

    try {
        const response = await fetch('/api/portfolio/pdf', {credentials: 'same-origin', cache: 'no-store'});
        if (!response.ok || !response.headers.get('Content-Type')?.toLowerCase().startsWith('application/pdf')) {
            throw new Error('PDF download failed');
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'study-portfolio.pdf';
        document.body.appendChild(link);
        try {
            link.click();
        } finally {
            link.remove();
            window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        }
        pdfSaveStatus.textContent = "PDF 다운로드를 요청했습니다. 브라우저의 다운로드 목록을 확인해 주세요.";
    } catch (error) {
        pdfSaveStatus.textContent = "PDF를 다운로드하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    } finally {
        savePdfBtn.disabled = false;
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
