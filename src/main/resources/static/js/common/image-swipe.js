(function (global) {
    const SWIPE_DURATION_MS = 180;
    const preloadedImageSources = new Set();

    function preloadAdjacentImages(images, currentIndex) {
        if (!Array.isArray(images) || images.length <= 1) return;

        [currentIndex - 1, currentIndex + 1]
            .filter(index => index >= 0 && index < images.length)
            .map(index => typeof images[index] === 'string' ? images[index] : images[index] && images[index].imgSrc)
            .filter(source => source && !preloadedImageSources.has(source))
            .forEach(source => {
                preloadedImageSources.add(source);
                const image = new Image();
                image.src = source;
            });
    }

    function initImageSwipe(element, options) {
        if (!element || element.dataset.imageSwipeBound === 'true') {
            return;
        }

        const settings = options || {};
        let pointerId = null;
        let startX = 0;
        let currentX = 0;
        let dragging = false;
        let animating = false;
        let dragIncomingImage = null;
        let dragDirection = null;

        element.dataset.imageSwipeBound = 'true';
        element.classList.add('swipeable-image');
        element.draggable = false;

        element.addEventListener('dragstart', (event) => event.preventDefault());
        element.addEventListener('pointerdown', startSwipe);
        element.addEventListener('pointermove', moveSwipe);
        element.addEventListener('pointerup', finishSwipe);
        element.addEventListener('pointercancel', cancelSwipe);
        element.imageSwipeNavigate = navigateWithAnimation;

        function navigateWithAnimation(direction) {
            if (animating || !canMove(direction)) return false;
            animateSlide(direction);
            return true;
        }

        function startSwipe(event) {
            if (animating || !event.isPrimary || (event.pointerType === 'mouse' && event.button !== 0)) {
                return;
            }

            pointerId = event.pointerId;
            startX = event.clientX;
            currentX = startX;
            dragging = true;
            element.classList.add('is-swiping');
            element.style.transition = 'none';
            element.setPointerCapture(pointerId);
        }

        function moveSwipe(event) {
            if (!dragging || event.pointerId !== pointerId) {
                return;
            }

            currentX = event.clientX;
            let distance = currentX - startX;
            const direction = distance < 0 ? 'next' : 'previous';

            if (!canMove(direction)) {
                distance *= 0.25;
                removeDragIncomingImage();
            } else {
                prepareDragIncomingImage(direction, distance);
            }

            element.style.transform = `translate3d(${distance}px, 0, 0)`;
        }

        function finishSwipe(event) {
            if (!dragging || event.pointerId !== pointerId) {
                return;
            }

            currentX = event.clientX;
            const distance = currentX - startX;
            const threshold = Math.min(90, Math.max(45, element.clientWidth * 0.16));
            releasePointer();

            if (Math.abs(distance) < threshold) {
                resetPosition();
                return;
            }

            const direction = distance < 0 ? 'next' : 'previous';
            if (!canMove(direction)) {
                resetPosition();
                return;
            }

            const incomingImage = dragIncomingImage;
            dragIncomingImage = null;
            dragDirection = null;
            animateSlide(direction, incomingImage);
        }

        function cancelSwipe(event) {
            if (!dragging || event.pointerId !== pointerId) {
                return;
            }

            releasePointer();
            resetPosition();
        }

        function releasePointer() {
            if (pointerId !== null && element.hasPointerCapture(pointerId)) {
                element.releasePointerCapture(pointerId);
            }
            pointerId = null;
            dragging = false;
            element.classList.remove('is-swiping');
        }

        function canMove(direction) {
            const predicate = direction === 'next' ? settings.canNext : settings.canPrevious;
            return typeof predicate !== 'function' || predicate();
        }

        function animateSlide(direction, preparedIncomingImage) {
            animating = true;
            const width = element.offsetWidth;
            const exitX = direction === 'next' ? -width : width;
            const enterX = -exitX;
            const sourceProvider = direction === 'next' ? settings.getNextSource : settings.getPreviousSource;
            const incomingSource = typeof sourceProvider === 'function' ? sourceProvider() : null;
            const incomingImage = preparedIncomingImage
                || (incomingSource ? createIncomingImage(incomingSource, enterX) : null);

            element.style.transition = `transform ${SWIPE_DURATION_MS}ms ease`;
            if (incomingImage) {
                incomingImage.style.transition = `transform ${SWIPE_DURATION_MS}ms ease`;
            }

            window.requestAnimationFrame(() => {
                element.style.transform = `translate3d(${exitX}px, 0, 0)`;
                if (incomingImage) {
                    incomingImage.style.transform = 'translate3d(0, 0, 0)';
                }
            });

            window.setTimeout(() => {
                const navigate = direction === 'next' ? settings.onNext : settings.onPrevious;
                if (typeof navigate === 'function') navigate();

                if (incomingImage) incomingImage.remove();
                element.style.removeProperty('transition');
                element.style.removeProperty('transform');
                animating = false;
            }, SWIPE_DURATION_MS);
        }

        function prepareDragIncomingImage(direction, distance) {
            if (dragDirection !== direction) {
                removeDragIncomingImage();
                const sourceProvider = direction === 'next' ? settings.getNextSource : settings.getPreviousSource;
                const source = typeof sourceProvider === 'function' ? sourceProvider() : null;
                const adjacentOffset = direction === 'next' ? element.offsetWidth : -element.offsetWidth;
                dragIncomingImage = source ? createIncomingImage(source, adjacentOffset + distance) : null;
                if (dragIncomingImage) dragIncomingImage.style.transition = 'none';
                dragDirection = direction;
                return;
            }

            if (dragIncomingImage) {
                const adjacentOffset = direction === 'next' ? element.offsetWidth : -element.offsetWidth;
                dragIncomingImage.style.transform = `translate3d(${adjacentOffset + distance}px, 0, 0)`;
            }
        }

        function removeDragIncomingImage() {
            if (dragIncomingImage) dragIncomingImage.remove();
            dragIncomingImage = null;
            dragDirection = null;
        }

        function createIncomingImage(source, initialX) {
            const parent = element.parentElement;
            if (!parent) return null;

            const incomingImage = element.cloneNode(false);
            incomingImage.removeAttribute('id');
            incomingImage.removeAttribute('data-image-swipe-bound');
            incomingImage.removeAttribute('tabindex');
            incomingImage.setAttribute('aria-hidden', 'true');
            incomingImage.src = source;
            incomingImage.style.position = 'absolute';
            incomingImage.style.left = `${element.offsetLeft}px`;
            incomingImage.style.top = `${element.offsetTop}px`;
            incomingImage.style.width = `${element.offsetWidth}px`;
            incomingImage.style.height = `${element.offsetHeight}px`;
            incomingImage.style.margin = '0';
            incomingImage.style.pointerEvents = 'none';
            incomingImage.style.transition = `transform ${SWIPE_DURATION_MS}ms ease`;
            incomingImage.style.transform = `translate3d(${initialX}px, 0, 0)`;
            parent.append(incomingImage);
            return incomingImage;
        }

        function resetPosition() {
            removeDragIncomingImage();
            element.style.transition = `transform ${SWIPE_DURATION_MS}ms ease`;
            element.style.transform = 'translate3d(0, 0, 0)';
            window.setTimeout(() => {
                element.style.removeProperty('transition');
                element.style.removeProperty('transform');
            }, SWIPE_DURATION_MS);
        }
    }

    function animateImageSwipe(element, direction) {
        return Boolean(element
            && typeof element.imageSwipeNavigate === 'function'
            && element.imageSwipeNavigate(direction));
    }

    global.initImageSwipe = initImageSwipe;
    global.animateImageSwipe = animateImageSwipe;
    global.preloadAdjacentImages = preloadAdjacentImages;
})(window);
