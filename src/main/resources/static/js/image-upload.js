function toWebP(file, quality) {
    return new Promise(function (resolve) {
        if (file.type === 'image/webp' || file.type === 'image/gif') {
            resolve(null);
            return;
        }
        var url = URL.createObjectURL(file);
        var img = new Image();
        img.onload = function () {
            URL.revokeObjectURL(url);
            try {
                var canvas = document.createElement('canvas');
                canvas.width  = img.naturalWidth;
                canvas.height = img.naturalHeight;
                canvas.getContext('2d').drawImage(img, 0, 0);
                canvas.toBlob(function (blob) {
                    resolve(blob ? new File([blob], 'image.webp', { type: 'image/webp' }) : null);
                }, 'image/webp', quality);
            } catch (e) {
                resolve(null);
            }
        };
        img.onerror = function () { URL.revokeObjectURL(url); resolve(null); };
        img.src = url;
    });
}

/**
 * Initializes image preview, SHA-256 hash computation, and submit button
 * idempotency guard for a file input element.
 *
 * @param {Object} config
 * @param {string} config.fileInputSelector   - CSS selector for <input type="file">
 * @param {string} config.previewSelector     - CSS selector for the <img> preview element
 * @param {string} config.hashInputSelector   - CSS selector for the hidden hash <input>
 * @param {string} config.submitSelector      - CSS selector for the submit button
 * @param {string} [config.labelTextSelector] - CSS selector for label text span (optional)
 * @param {Element} [root]                    - Scope root element (defaults to document)
 */
function initImageUpload(config, root) {
    var scope       = root || document;
    var fileInput   = scope.querySelector(config.fileInputSelector);
    var previewImg  = scope.querySelector(config.previewSelector);
    var hashInput   = scope.querySelector(config.hashInputSelector);
    var submitBtn   = scope.querySelector(config.submitSelector);
    var existingImg = config.existingImageSelector
                       ? scope.querySelector(config.existingImageSelector)
                       : null;
    var labelText   = config.labelTextSelector
                       ? scope.querySelector(config.labelTextSelector)
                       : null;

    if (!fileInput || fileInput.dataset.imageUploadInitialized === 'true') return;
    fileInput.dataset.imageUploadInitialized = 'true';

    fileInput.addEventListener('change', async function () {
        var file = this.files[0];
        if (existingImg) existingImg.classList.add('hidden');
        if (previewImg)  previewImg.classList.add('hidden');
        if (hashInput)   hashInput.value = '';
        if (labelText)   labelText.textContent = file ? file.name : '사진을 선택하세요';
        if (!file) return;

        var converted = await toWebP(file, 0.85);
        var effectiveFile = file;

        if (converted) {
            try {
                var dt = new DataTransfer();
                dt.items.add(converted);
                fileInput.files = dt.files;
                effectiveFile = converted;
            } catch (e) {
                // DataTransfer 미지원 환경 — 원본 유지
            }
        }

        if (previewImg) {
            var objUrl = URL.createObjectURL(effectiveFile);
            previewImg.onload = function () { URL.revokeObjectURL(objUrl); };
            previewImg.src = objUrl;
            previewImg.classList.remove('hidden');
        }

        if (hashInput && window.crypto && crypto.subtle) {
            try {
                var buf    = await effectiveFile.arrayBuffer();
                var digest = await crypto.subtle.digest('SHA-256', buf);
                hashInput.value = Array.from(new Uint8Array(digest))
                    .map(function (b) { return b.toString(16).padStart(2, '0'); })
                    .join('');
            } catch (err) {
                console.warn('SHA-256 hash computation failed:', err);
            }
        }

        if (typeof config.onImageReady === 'function') {
            config.onImageReady(effectiveFile);
        }
    });

    if (submitBtn) {
        submitBtn.addEventListener('click', function () {
            setTimeout(function () { submitBtn.disabled = true; }, 0);
        });
        submitBtn.addEventListener('htmx:afterRequest', function (evt) {
            if (evt.detail && !evt.detail.successful) {
                submitBtn.disabled = false;
            }
        });
    }
}

function createImageAnalysisHandler(config, root) {
    var scope = root || document;
    var currentFile = null;
    var retryAttempt = 0;
    var ctrl = null;

    function find(selector) {
        return selector ? scope.querySelector(selector) : null;
    }

    function setBusy(isBusy) {
        var fileInput = find(config.fileInputSelector);
        var submitBtn = find(config.submitSelector);
        var statusSpan = find(config.statusSelector);
        var fileLabel = find(config.fileLabelSelector);
        var retryBtn = find(config.retryButtonSelector);

        if (fileInput) fileInput.disabled = isBusy;
        if (submitBtn) submitBtn.disabled = isBusy;
        if (retryBtn) {
            retryBtn.disabled = isBusy || !currentFile;
            if (isBusy) retryBtn.classList.add('hidden');
        }
        if (fileLabel) fileLabel.classList.toggle('opacity-50', isBusy);

        if (statusSpan) {
            statusSpan.classList.toggle('hidden', !isBusy);
            if (isBusy) statusSpan.textContent = 'AI 분석 중...';
        }
    }

    function showRetryButton() {
        var retryBtn = find(config.retryButtonSelector);
        if (!retryBtn) return;
        retryBtn.classList.remove('hidden');
        retryBtn.disabled = !currentFile;
    }

    async function analyze(file, attempt) {
        if (!file) return;
        currentFile = file;
        if (ctrl) ctrl.abort();
        var requestCtrl = new AbortController();
        ctrl = requestCtrl;

        var nameInput = find(config.nameInputSelector);
        var descInput = find(config.descriptionInputSelector);
        var statusSpan = find(config.statusSelector);

        setBusy(true);
        var dotTimer = statusSpan ? setInterval(function () {
            var dots = (statusSpan.textContent.match(/\./g) || []).length;
            statusSpan.textContent = 'AI 분석 중' + '.'.repeat(dots % 3 + 1);
        }, 1000) : null;

        try {
            var fd = new FormData();
            fd.append('imageFile', file);
            fd.append('retryAttempt', String(attempt));
            if (attempt > 0) {
                fd.append('previousName', nameInput ? nameInput.value : '');
                fd.append('previousDescription', descInput ? descInput.value : '');
            }

            var res = await fetch('/images/analyze', {
                method: 'POST',
                body: fd,
                signal: requestCtrl.signal
            });
            if (!res.ok) return;

            var data = await res.json();
            if (nameInput && data.name) nameInput.value = data.name;
            if (descInput && data.description) descInput.value = data.description;
            retryAttempt = attempt;
            showRetryButton();
        } catch (e) {
            if (e.name !== 'AbortError') console.warn('Image analysis failed:', e);
        } finally {
            if (dotTimer) clearInterval(dotTimer);
            if (ctrl === requestCtrl) {
                setBusy(false);
            }
        }
    }

    var retryBtn = find(config.retryButtonSelector);
    if (retryBtn && retryBtn.dataset.imageAnalysisRetryInitialized !== 'true') {
        retryBtn.dataset.imageAnalysisRetryInitialized = 'true';
        retryBtn.addEventListener('click', function () {
            analyze(currentFile, retryAttempt + 1);
        });
    }

    return function (file) {
        retryAttempt = 0;
        currentFile = file;
        var retryBtn = find(config.retryButtonSelector);
        if (retryBtn) {
            retryBtn.classList.add('hidden');
            retryBtn.disabled = true;
        }
        analyze(file, 0);
    };
}
