(function () {
    const params = new URLSearchParams(location.search);
    const alert = document.getElementById("alert");

    if (params.has("error")) {
        alert.textContent = "Invalid username or password. Please try again.";
        alert.hidden = false;
    } else if (params.has("loggedOut")) {
        alert.textContent = "You have been signed out.";
        alert.classList.add("success");
        alert.hidden = false;
    } else if (params.has("expired")) {
        alert.textContent = "Your session expired. Please sign in again.";
        alert.hidden = false;
    }

    const password = document.getElementById("password");
    const toggle = document.getElementById("togglePassword");
    toggle.addEventListener("click", () => {
        const show = password.type === "password";
        password.type = show ? "text" : "password";
        toggle.setAttribute("aria-label", show ? "Hide password" : "Show password");
    });
})();
