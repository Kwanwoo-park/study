(function (global) {
    const SWIPE_DURATION_MS = 180;

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

        element.dataset.imageSwipeBound = 'true';
        element.classList.add('swipeable-image');
        element.draggable = false;

        element.addEventListener('dragstart', (event) => event.preventDefault());
        element.addEventListener('pointerdown', startSwipe);
        element.addEventListener('pointermove', moveSwipe);
        element.addEventListener('pointerup', finishSwipe);
        element.addEventListener('pointercancel', cancelSwipe);

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

            if ((distance > 0 && !canMove('previous')) || (distance < 0 && !canMove('next'))) {
                distance *= 0.25;
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

            animateSlide(direction);
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

        function animateSlide(direction) {
            animating = true;
            const width = Math.max(element.clientWidth, 240);
            const exitX = direction === 'next' ? -width : width;
            const enterX = -exitX;

            element.style.transition = `transform ${SWIPE_DURATION_MS}ms ease, opacity ${SWIPE_DURATION_MS}ms ease`;
            element.style.transform = `translate3d(${exitX}px, 0, 0)`;
            element.style.opacity = '0.35';

            window.setTimeout(() => {
                const navigate = direction === 'next' ? settings.onNext : settings.onPrevious;
                if (typeof navigate === 'function') {
                    navigate();
                }

                element.style.transition = 'none';
                element.style.transform = `translate3d(${enterX}px, 0, 0)`;
                element.style.opacity = '0.35';

                window.requestAnimationFrame(() => {
                    window.requestAnimationFrame(() => {
                        element.style.transition = `transform ${SWIPE_DURATION_MS}ms ease, opacity ${SWIPE_DURATION_MS}ms ease`;
                        element.style.transform = 'translate3d(0, 0, 0)';
                        element.style.opacity = '1';

                        window.setTimeout(() => {
                            element.style.removeProperty('transition');
                            element.style.removeProperty('transform');
                            element.style.removeProperty('opacity');
                            animating = false;
                        }, SWIPE_DURATION_MS);
                    });
                });
            }, SWIPE_DURATION_MS);
        }

        function resetPosition() {
            element.style.transition = `transform ${SWIPE_DURATION_MS}ms ease`;
            element.style.transform = 'translate3d(0, 0, 0)';
            window.setTimeout(() => {
                element.style.removeProperty('transition');
                element.style.removeProperty('transform');
            }, SWIPE_DURATION_MS);
        }
    }

    global.initImageSwipe = initImageSwipe;
})(window);
