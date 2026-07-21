document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const statusEl = document.getElementById('status');

    try {
        const res = await fetch('/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();

        if (!res.ok) {
            statusEl.textContent = Object.values(data).join(' ');
            statusEl.style.color = 'red';
            return;
        }

        statusEl.textContent = data.message;
        statusEl.style.color = 'green';

        setTimeout(() => {
            window.location.href = '/login.html';
        }, 1500);

    } catch (err) {
        statusEl.textContent = 'Something went wrong.';
        statusEl.style.color = 'red';
    }
});