(function () {
    const display = document.getElementById("pinDisplay");
    const generateBtn = document.getElementById("generateBtn");
    const copyBtn = document.getElementById("copyBtn");
    const visBtn = document.getElementById("toggleVisibility");
    const clearBtn = document.getElementById("clearBtn");
    const downloadBtn = document.getElementById("downloadBtn");
    const list = document.getElementById("historyList");
    const empty = document.getElementById("historyEmpty");
    const countEl = document.getElementById("historyCount");
    const toastEl = document.getElementById("toast");
    const radios = document.querySelectorAll('input[name="length"]');

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let currentPin = null;
    let hidden = false;
    let busy = false;
    let history = [];

    const selectedLength = () => Number(document.querySelector('input[name="length"]:checked').value);
    const mask = (pin) => "•".repeat(pin.length);

    /* ---------- PIN boxes ---------- */
    function renderEmpty(length) {
        currentPin = null;
        display.classList.toggle("six", length === 6);
        display.innerHTML = "";
        for (let i = 0; i < length; i++) {
            const d = document.createElement("div");
            d.className = "digit empty";
            d.textContent = "–";
            display.appendChild(d);
        }
        display.setAttribute("aria-label", "No MPIN generated yet");
        copyBtn.disabled = true;
        visBtn.disabled = true;
    }

    function showDigits() {
        if (!currentPin) return;
        [...display.children].forEach((d, i) => {
            d.textContent = hidden ? "•" : currentPin[i];
        });
        display.setAttribute("aria-label", hidden ? "MPIN hidden" : "Generated MPIN " + currentPin.split("").join(" "));
    }

    function animateTo(pin) {
        return new Promise((resolve) => {
            display.classList.toggle("six", pin.length === 6);
            display.innerHTML = "";
            const boxes = [...pin].map(() => {
                const d = document.createElement("div");
                d.className = "digit rolling";
                display.appendChild(d);
                return d;
            });

            if (reduceMotion) {
                boxes.forEach((d) => d.classList.replace("rolling", "settled"));
                resolve();
                return;
            }

            const start = performance.now();
            const settleAt = boxes.map((_, i) => 380 + i * 110);
            (function frame(now) {
                const t = now - start;
                let done = true;
                boxes.forEach((d, i) => {
                    if (t >= settleAt[i]) {
                        if (!d.classList.contains("settled")) {
                            d.classList.replace("rolling", "settled");
                            d.textContent = hidden ? "•" : pin[i];
                        }
                    } else {
                        done = false;
                        d.textContent = Math.floor(Math.random() * 10);
                    }
                });
                if (done) resolve(); else setTimeout(() => requestAnimationFrame(frame), 45);
            })(start);
        });
    }

    /* ---------- Server calls ---------- */
    async function api(url, options) {
        const res = await fetch(url, Object.assign({ credentials: "same-origin" }, options));
        if (res.status === 401 || res.redirected) {
            location.href = "login.html?expired=1";
            throw new Error("Session expired");
        }
        const body = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(body.error || "Something went wrong");
        return body;
    }

    async function generate() {
        if (busy) return;
        busy = true;
        generateBtn.disabled = true;
        generateBtn.classList.add("loading");
        try {
            const length = selectedLength();
            const rec = await api("generateMpin", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "length=" + length
            });
            await animateTo(rec.mpin);
            currentPin = rec.mpin;
            showDigits();
            copyBtn.disabled = false;
            visBtn.disabled = false;
            history.push(rec);
            renderHistory();
        } catch (e) {
            toast(e.message, true);
        } finally {
            busy = false;
            generateBtn.disabled = false;
            generateBtn.classList.remove("loading");
        }
    }

    async function loadHistory() {
        try {
            history = await api("history");
            renderHistory();
        } catch (e) { /* page still usable */ }
    }

    async function clearHistory() {
        if (!history.length || !confirm("Clear all MPINs generated in this session?")) return;
        try {
            history = await api("history", { method: "DELETE" });
            renderHistory();
            renderEmpty(selectedLength());
            toast("History cleared");
        } catch (e) {
            toast(e.message, true);
        }
    }

    /* ---------- History ---------- */
    function renderHistory() {
        list.innerHTML = "";
        [...history].reverse().forEach((rec) => {
            const li = document.createElement("li");
            const pin = document.createElement("span");
            pin.className = "pin";
            pin.textContent = hidden ? mask(rec.mpin) : rec.mpin;

            const meta = document.createElement("span");
            meta.className = "meta";
            const badge = document.createElement("span");
            badge.className = "badge";
            badge.textContent = rec.length + "-digit";
            const time = new Date(rec.generatedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
            meta.append(badge, document.createTextNode(time));

            li.append(pin, meta);
            list.appendChild(li);
        });

        const n = history.length;
        countEl.textContent = n;
        empty.hidden = n > 0;
        list.hidden = n === 0;
        clearBtn.disabled = n === 0;
        downloadBtn.setAttribute("aria-disabled", String(n === 0));
    }

    /* ---------- Copy / hide ---------- */
    async function copyPin() {
        if (!currentPin) return;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(currentPin);
            } else {
                const ta = document.createElement("textarea");
                ta.value = currentPin;
                ta.style.position = "fixed";
                ta.style.opacity = "0";
                document.body.appendChild(ta);
                ta.select();
                document.execCommand("copy");
                ta.remove();
            }
            toast("MPIN copied to clipboard");
        } catch (e) {
            toast("Couldn't copy — please copy it manually", true);
        }
    }

    function toggleHidden() {
        hidden = !hidden;
        visBtn.classList.toggle("is-hidden", hidden);
        visBtn.setAttribute("aria-label", hidden ? "Show MPIN" : "Hide MPIN");
        showDigits();
        renderHistory();
    }

    /* ---------- Toast ---------- */
    let toastTimer;
    function toast(message, isError) {
        toastEl.textContent = message;
        toastEl.classList.toggle("error", !!isError);
        toastEl.classList.add("show");
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toastEl.classList.remove("show"), 2200);
    }

    /* ---------- Wiring ---------- */
    generateBtn.addEventListener("click", generate);
    copyBtn.addEventListener("click", copyPin);
    visBtn.addEventListener("click", toggleHidden);
    clearBtn.addEventListener("click", clearHistory);
    radios.forEach((r) => r.addEventListener("change", () => renderEmpty(selectedLength())));

    document.addEventListener("keydown", (e) => {
        if (e.ctrlKey || e.metaKey || e.altKey || e.target.matches("input[type=text], input[type=password], textarea")) return;
        const key = e.key.toLowerCase();
        if (key === "4" || key === "6") {
            const radio = document.querySelector('input[name="length"][value="' + key + '"]');
            if (!radio.checked) { radio.checked = true; renderEmpty(Number(key)); }
        } else if (key === "enter" && !e.target.closest("button, a")) {
            generate();
        } else if (key === "c") {
            copyPin();
        } else if (key === "h" && currentPin) {
            toggleHidden();
        }
    });

    renderEmpty(selectedLength());
    loadHistory();
})();
