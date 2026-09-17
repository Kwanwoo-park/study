(function () {
    'use strict';
    if (window.pageBackNavigationReady) return;
    window.pageBackNavigationReady = true;

    function hasPreviousPage() {
        if (window.history.length < 2 || !document.referrer) return false;
        try {
            const previous = new URL(document.referrer);
            const current = new URL(window.location.href);
            if (previous.origin !== current.origin) return false;
            if (previous.pathname === current.pathname && previous.search === current.search) return false;
            // The landing page redirects to login, and OAuth callback URLs cannot be revisited.
            if (['/', '/index', '/index.html'].includes(previous.pathname)
                    || previous.pathname.startsWith('/oauth2/')
                    || previous.pathname.startsWith('/login/oauth2/')) return false;
            // A signed-out visitor must not bounce between login and a protected page.
            if (current.pathname === '/member/login') {
                return ['/member/find', '/member/findByEmail', '/member/findByInfo', '/member/register', '/appeal']
                    .includes(previous.pathname) || previous.pathname === '/portfolio'
                    || previous.pathname.startsWith('/portfolio/');
            }
            return true;
        } catch (error) {
            return false;
        }
    }

    document.addEventListener('click', function (event) {
        const link = event.target.closest && event.target.closest('a[data-page-back]');
        if (!link || event.defaultPrevented || event.button !== 0
                || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        if (hasPreviousPage()) {
            try {
                window.history.back();
            } catch (error) {
                window.location.replace(link.href);
            }
        } else {
            window.location.replace(link.href);
        }
        // Without JavaScript, the anchor's href still provides the page-specific fallback.
    });
})();
