import {readFile, mkdir, writeFile, rename} from 'node:fs/promises';
import {existsSync} from 'node:fs';
import {resolve, dirname, extname, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {chromium} from 'playwright';
import {getDocument} from 'pdfjs-dist/legacy/build/pdf.mjs';

const toolDirectory = dirname(fileURLToPath(import.meta.url));
const root = resolve(toolDirectory, '../..');
const staticRoot = resolve(process.argv[3] || resolve(root, 'src/main/resources/static'));
const fontRoot = resolve(toolDirectory, 'node_modules/@fontsource/noto-sans-kr');
const output = resolve(process.argv[2] || resolve(root, 'output/pdf/portfolio.pdf'));
const origin = 'http://portfolio.build';
const mimeTypes = {'.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.png': 'image/png', '.woff2': 'font/woff2', '.woff': 'font/woff'};

// Install during deployment, never download dependencies in a visitor's request.
if (!existsSync(chromium.executablePath())) {
    throw new Error('Chromium is missing. Run: node tools/portfolio-pdf/node_modules/playwright/cli.js install chromium');
}

const browser = await chromium.launch({headless: true});
try {
    const page = await browser.newPage({viewport: {width: 1280, height: 900}, locale: 'ko-KR', serviceWorkers: 'block'});
    page.setDefaultTimeout(30000);
    const failures = [];
    page.on('pageerror', error => failures.push(error.message));
    await page.route('**/*', async route => {
        const url = new URL(route.request().url());
        const isFont = url.pathname.startsWith('/_pdf-fonts/');
        const directory = isFont ? fontRoot : staticRoot;
        const path = decodeURIComponent(isFont ? url.pathname.slice('/_pdf-fonts/'.length) : url.pathname.slice(1));
        const file = resolve(directory, path);
        if (url.origin !== origin || !file.startsWith(directory + sep)) {
            failures.push(`Disallowed PDF resource: ${url.origin}${url.pathname}`);
            return route.abort();
        }
        try {
            await route.fulfill({body: await readFile(file), contentType: mimeTypes[extname(file)] || 'application/octet-stream'});
        } catch (error) {
            failures.push(`Missing PDF resource: ${url.pathname}`);
            await route.fulfill({status: 404, body: 'Not found'});
        }
    });
    await page.emulateMedia({media: 'print'});
    await page.goto(`${origin}/portfolio/index.html`, {waitUntil: 'load'});
    for (const weight of [400, 700, 900]) {
        const css = (await readFile(resolve(fontRoot, `${weight}.css`), 'utf8')).replaceAll('./files/', `${origin}/_pdf-fonts/files/`);
        await page.addStyleTag({content: css});
    }
    await page.addStyleTag({content: 'body { font-family: "Noto Sans KR", sans-serif !important; }'});
    await page.evaluate(async () => {
        await document.fonts.ready;
        await Promise.all(Array.from(document.images, image => image.decode()));
    });
    if (failures.length) throw new Error(failures.join('\n'));

    const slides = await page.locator('.slide').evaluateAll(elements => elements.map(element => ({title: element.dataset.title, text: element.innerText})));
    const bytes = await page.pdf({preferCSSPageSize: true, printBackground: true, tagged: true});
    const loadingTask = getDocument({data: new Uint8Array(bytes), useSystemFonts: false});
    const document = await loadingTask.promise;
    try {
        if (document.numPages !== slides.length) throw new Error(`PDF layout overflow: ${document.numPages} pages for ${slides.length} slides.`);
        for (let index = 0; index < slides.length; index++) {
            const pdfPage = await document.getPage(index + 1);
            const text = (await pdfPage.getTextContent()).items.map(item => item.str || '').join('');
            const normalize = value => value.normalize('NFKC').replace(/\s/g, '');
            if (!normalize(text).includes(normalize(slides[index].text))) throw new Error(`PDF text is missing or reordered on slide ${index + 1}: ${slides[index].title}`);
        }
    } finally {
        await loadingTask.destroy();
    }
    await mkdir(dirname(output), {recursive: true});
    await writeFile(`${output}.tmp`, bytes);
    await rename(`${output}.tmp`, output);
    console.log(`Generated ${output} (${slides.length} pages, ${bytes.length} bytes; text verified).`);
} finally {
    await browser.close();
}
