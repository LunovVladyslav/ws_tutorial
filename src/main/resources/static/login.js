document.addEventListener('DOMContentLoaded', () => {
    
    // Check if already logged in as ADMIN
    const existingToken = localStorage.getItem('token');
    const existingRole = localStorage.getItem('role');
    if (existingToken && existingRole === 'ADMIN') {
        window.location.href = '/admin.html';
        return;
    }

    const form = document.getElementById('loginForm');
    const errorDiv = document.getElementById('errorMessage');
    const loginBtn = document.getElementById('loginBtn');
    const btnText = document.querySelector('.btn-text');
    const spinner = document.getElementById('btnSpinner');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();
        
        if (!username || !password) return;

        // UI Loading state
        loginBtn.disabled = true;
        spinner.style.display = 'block';
        errorDiv.style.display = 'none';

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.message || 'Invalid username or password');
            }

            const data = await res.json();
            
            // Check if user is an ADMIN
            if (data.role !== 'ADMIN') {
                throw new Error('Access denied: Admins only');
            }

            // Save auth details
            localStorage.setItem('token', data.token);
            localStorage.setItem('role', data.role);
            localStorage.setItem('username', data.username);

            // Redirect to dashboard
            window.location.href = '/admin.html';

        } catch (err) {
            errorDiv.textContent = err.message;
            errorDiv.style.display = 'block';
            
            // Shake effect for error
            form.classList.add('shake');
            setTimeout(() => form.classList.remove('shake'), 400);

            // Reset UI
            loginBtn.disabled = false;
            spinner.style.display = 'none';
        }
    });
});
