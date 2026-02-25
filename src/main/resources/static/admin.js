document.addEventListener('DOMContentLoaded', () => {
    // Authentication Check
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    if (!token || role !== 'ADMIN') {
        alert('Access Denied: Admins Only');
        window.location.href = '/login.html'; // Assuming there's a main login page, or they are redirected
        return;
    }

    const username = localStorage.getItem('username');
    document.getElementById('adminUsername').textContent = username;

    const authHeaders = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    // Navigation Logic
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.page-section');
    const pageTitle = document.getElementById('pageTitle');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const target = item.getAttribute('data-target');

            // Update nav active state
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            // Update sections
            sections.forEach(s => s.classList.remove('active'));
            document.getElementById(target).classList.add('active');

            // Update Title
            pageTitle.textContent = item.textContent.replace(/[^\w\s]/gi, '').trim();

            // Load data when tab is opened
            if (target === 'users') loadUsers();
            if (target === 'peers') loadPeers();
            if (target === 'logs') loadLogs();
            if (target === 'reports') loadReports();
        });
    });

    // Logout
    document.getElementById('logoutBtn').addEventListener('click', () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('username');
        window.location.href = '/';
    });

    // ---------------- Users ----------------
    async function loadUsers() {
        try {
            const res = await fetch('/api/admin/users', { headers: authHeaders });
            if (!res.ok) throw new Error('Failed to load users');
            const users = await res.json();

            const tbody = document.querySelector('#usersTable tbody');
            tbody.innerHTML = '';
            users.forEach(user => {
                const now = new Date();
                const isBanned = user.bannedUntil && new Date(user.bannedUntil) > now;
                const statusHtml = isBanned 
                    ? `<span class="status-badge status-pending" title="Banned until ${new Date(user.bannedUntil).toLocaleString()}">BANNED</span>`
                    : `<span class="status-badge ${user.role === 'ADMIN' ? 'status-pending' : 'status-resolved'}">${user.role}</span>`;
                
                let actionsHtml = `<button class="btn btn-danger" onclick="deleteUser(${user.id})" style="margin-right: 5px;">Delete</button>`;
                
                if (isBanned) {
                    actionsHtml += `<button class="btn btn-success" onclick="unbanUser(${user.id})">Unban</button>`;
                } else {
                    actionsHtml += `<button class="btn btn-secondary" onclick="openBanModal(${user.id}, '${user.username}')">Ban</button>`;
                }

                tbody.innerHTML += `
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.username}</td>
                        <td>${statusHtml}</td>
                        <td>${actionsHtml}</td>
                    </tr>
                `;
            });
        } catch (e) {
            console.error(e);
            alert('Error loading users');
        }
    }

    window.deleteUser = async (id) => {
        if (!confirm('Are you sure you want to delete this user?')) return;
        try {
            const res = await fetch(`/api/admin/users/${id}`, {
                method: 'DELETE',
                headers: authHeaders
            });
            if (res.ok) loadUsers();
            else alert('Failed to delete user');
        } catch (e) { console.error(e); }
    };

    // Modals generic logic
    const modals = document.querySelectorAll('.modal');
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.getElementById(e.target.dataset.modal).style.display = 'none';
        });
    });

    window.onclick = (e) => {
        modals.forEach(modal => {
            if (e.target == modal) modal.style.display = 'none';
        });
    };

    // Add User
    document.getElementById('addUserBtn').addEventListener('click', () => {
        document.getElementById('addUserModal').style.display = 'flex';
    });

    document.getElementById('addUserForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('newUsername').value;
        const password = document.getElementById('newPassword').value;
        const role = document.getElementById('newRole').value;

        try {
            const res = await fetch('/api/admin/users', {
                method: 'POST',
                headers: authHeaders,
                body: JSON.stringify({ username, password, role })
            });
            if (res.ok) {
                document.getElementById('addUserModal').style.display = 'none';
                loadUsers();
                e.target.reset();
            } else {
                alert(await res.text());
            }
        } catch (err) { console.error(err); }
    });

    // Ban User Logic
    window.openBanModal = (id, username) => {
        document.getElementById('banUserId').value = id;
        document.getElementById('banUsernameDisplay').textContent = username;
        document.getElementById('banUserModal').style.display = 'flex';
    };

    document.getElementById('banUserForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('banUserId').value;
        const hours = document.getElementById('banDuration').value;

        try {
            const res = await fetch(`/api/admin/users/${id}/ban?hours=${hours}`, {
                method: 'POST',
                headers: authHeaders
            });
            if (res.ok) {
                document.getElementById('banUserModal').style.display = 'none';
                loadUsers();
            } else {
                alert('Failed to ban user');
            }
        } catch (err) { console.error(err); }
    });

    window.unbanUser = async (id) => {
        if (!confirm('Are you sure you want to unban this user?')) return;
        try {
            const res = await fetch(`/api/admin/users/${id}/unban`, {
                method: 'POST',
                headers: authHeaders
            });
            if (res.ok) loadUsers();
            else alert('Failed to unban user');
        } catch (e) { console.error(e); }
    };

    // ---------------- Peers ----------------
    document.getElementById('refreshPeersBtn').addEventListener('click', loadPeers);
    async function loadPeers() {
        try {
            const res = await fetch('/api/admin/peers', { headers: authHeaders });
            if (!res.ok) throw new Error('Failed to load peers');
            const peersObj = await res.json();
            
            // Format peer map into grid
            const grid = document.getElementById('peersGrid');
            grid.innerHTML = '';
            
            const entries = Object.entries(peersObj);
            if (entries.length === 0) {
                grid.innerHTML = '<p>No active WebSocket peers.</p>';
            } else {
                entries.forEach(([id, peer]) => {
                    grid.innerHTML += `
                        <div class="card" style="margin-bottom: 10px; border-left: 4px solid var(--success);">
                            <strong>ID:</strong> ${id}<br>
                            <strong>Username:</strong> ${peer.username || 'Anonymous'}
                        </div>
                    `;
                });
            }
        } catch (e) { console.error(e); }
    }

    // ---------------- Reports ----------------
    document.getElementById('refreshReportsBtn').addEventListener('click', loadReports);
    async function loadReports() {
        try {
            const res = await fetch('/api/admin/reports', { headers: authHeaders });
            if (!res.ok) throw new Error('Failed to load reports');
            const reports = await res.json();
            
            const tbody = document.querySelector('#reportsTable tbody');
            tbody.innerHTML = '';
            
            let pendingCount = 0;

            reports.forEach(r => {
                if (r.status === 'PENDING') pendingCount++;
                const statusClass = r.status === 'PENDING' ? 'status-pending' : 'status-resolved';
                
                tbody.innerHTML += `
                    <tr>
                        <td>${r.id}</td>
                        <td>${r.reporterUsername}</td>
                        <td>${r.reportedContext}</td>
                        <td>${r.reason}</td>
                        <td>${new Date(r.timestamp).toLocaleString()}</td>
                        <td><span class="status-badge ${statusClass}">${r.status}</span></td>
                        <td>
                            ${r.status === 'PENDING' ? `<button class="btn btn-success" onclick="resolveReport(${r.id})">Resolve</button>` : '-'}
                        </td>
                    </tr>
                `;
            });

            // Update badge
            const badge = document.getElementById('reportBadge');
            if (pendingCount > 0) {
                badge.textContent = pendingCount;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
            
        } catch (e) { console.error(e); }
    }

    window.resolveReport = async (id) => {
        try {
            const res = await fetch(`/api/admin/reports/${id}/resolve`, {
                method: 'PUT',
                headers: authHeaders
            });
            if (res.ok) loadReports();
            else alert('Failed to resolve report');
        } catch (e) { console.error(e); }
    }

    // ---------------- Logs ----------------
    document.getElementById('refreshLogsBtn').addEventListener('click', loadLogs);
    async function loadLogs() {
        const container = document.getElementById('logsContainer');
        container.textContent = 'Loading logs...';
        try {
            const res = await fetch('/api/admin/logs', { headers: authHeaders });
            if (!res.ok) throw new Error('Failed to load logs');
            const text = await res.text();
            container.textContent = text;
            container.scrollTop = container.scrollHeight; // Auto scroll to bottom
        } catch (e) {
            container.textContent = 'Error loading logs: ' + e.message;
        }
    }

    // Initialize 
    loadUsers();
    loadReports(); // Pre-load to get the badge count
});
