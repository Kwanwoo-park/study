if ('serviceWorker' in navigator) {
    window.addEventListener('load', function() {
        navigator.serviceWorker.register('/service-worker.js')
            .then(function (registration) {
                console.log('Service Worker 등록 성공:', registration.scope);
            })
            .catch(function (error) {
                console.log('Service Worker 등록 실패:', error);
            });
    });
}

self.addEventListener('fetch', function (event) {
  event.respondWith(
    caches.match(event.request).then(function (response) {
      return response || fetch(event.request);
    })
  );
});