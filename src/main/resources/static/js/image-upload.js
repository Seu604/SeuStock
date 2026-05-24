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

    if (!fileInput) return;

    fileInput.addEventListener('change', async function () {
        var file = this.files[0];
        if (existingImg) existingImg.classList.add('hidden');
        if (previewImg)  previewImg.classList.add('hidden');
        if (hashInput)   hashInput.value = '';
        if (labelText)   labelText.textContent = file ? file.name : '사진을 선택하세요';
        if (!file) return;

        if (previewImg) {
            var reader = new FileReader();
            reader.onload = function (e) {
                previewImg.src = e.target.result;
                previewImg.classList.remove('hidden');
            };
            reader.readAsDataURL(file);
        }

        if (hashInput && window.crypto && crypto.subtle) {
            try {
                var buf    = await file.arrayBuffer();
                var digest = await crypto.subtle.digest('SHA-256', buf);
                hashInput.value = Array.from(new Uint8Array(digest))
                    .map(function (b) { return b.toString(16).padStart(2, '0'); })
                    .join('');
            } catch (err) {
                console.warn('SHA-256 hash computation failed:', err);
            }
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
