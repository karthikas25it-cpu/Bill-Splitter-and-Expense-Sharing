/**
 * SplitEasy - Bill Splitter Frontend (JavaScript)
 * Interacts with the Java REST API backend via fetch()
 */

const API = ''; // Same origin

// Local Application State
let users = [];
let expenses = [];
let balances = { net: {}, simplified: [] };

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    setupNavigation();
    setupForms();
    fetchAllData();
});

// Tab Navigation
function setupNavigation() {
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');
            showTab(tabId);
        });
    });
}

function showTab(tabId) {
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });

    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.toggle('active', content.id === `tab-${tabId}`);
    });

    const headers = {
        'dashboard': ['Dashboard & Smart Settlements', 'Track shared expenses and settle debts with minimum transactions.'],
        'add-expense': ['Add New Bill', 'Divide expenses equally among friends with instant calculations.'],
        'friends': ['Friends & Group Members', 'Manage who belongs to your bill sharing circle.'],
        'history': ['Expense Ledger', 'Complete audit history of all recorded bills.'],
        'settle': ['Settle Up Debts', 'Record UPI, Cash, or Bank Transfer repayments.']
    };

    if (headers[tabId]) {
        document.getElementById('headerTitle').textContent = headers[tabId][0];
        document.getElementById('headerSubtitle').textContent = headers[tabId][1];
    }
}

// Fetch all data from Java REST API
async function fetchAllData() {
    try {
        const [uRes, eRes, bRes] = await Promise.all([
            fetch(`${API}/api/users`).then(r => r.json()),
            fetch(`${API}/api/expenses`).then(r => r.json()),
            fetch(`${API}/api/balances`).then(r => r.json())
        ]);

        users = uRes || [];
        expenses = eRes || [];
        balances = bRes || { net: {}, simplified: [] };

        renderDashboard();
        renderExpenseForm();
        renderFriends();
        renderHistory();
        renderSettleTab();
    } catch (err) {
        console.error('Error fetching data from Java backend:', err);
        showToast('⚠️ Could not connect to Java REST server on port 8080');
    }
}

// 1. Render Dashboard
function renderDashboard() {
    // Total spend
    const total = expenses.reduce((sum, e) => sum + (e.amount || 0), 0);
    document.getElementById('statTotalSpend').textContent = `₹${total.toFixed(2)}`;
    document.getElementById('statTotalBills').textContent = expenses.length;
    document.getElementById('statTotalUsers').textContent = users.length;
    document.getElementById('statOptimalTx').textContent = (balances.simplified || []).length;

    // Simplified settlements (Minimum Cash Flow algorithm)
    const settleContainer = document.getElementById('simplifiedSettlements');
    const simplified = balances.simplified || [];

    if (simplified.length === 0) {
        settleContainer.innerHTML = `<p style="color:#c7d2fe; grid-column:1/-1;">✨ All debts are settled! No payments required.</p>`;
    } else {
        settleContainer.innerHTML = simplified.map(tx => `
            <div class="settlement-pill">
                <div class="settle-flow">
                    <span>${getUserName(tx.from)}</span>
                    <span class="arrow">➔</span>
                    <span>${getUserName(tx.to)}</span>
                </div>
                <span class="settle-value">₹${tx.amount.toFixed(2)}</span>
            </div>
        `).join('');
    }

    // Net balances list
    const netList = document.getElementById('netBalancesList');
    if (users.length === 0) {
        netList.innerHTML = `<p style="color:#64748b;">No users registered.</p>`;
        return;
    }

    netList.innerHTML = users.map(u => {
        const net = (balances.net && balances.net[u.id]) || 0;
        let badgeClass = 'zero';
        let badgeText = 'Settled';

        if (net > 0.01) {
            badgeClass = 'credit';
            badgeText = `Gets back ₹${net.toFixed(2)}`;
        } else if (net < -0.01) {
            badgeClass = 'debt';
            badgeText = `Owes ₹${Math.abs(net).toFixed(2)}`;
        }

        return `
            <div class="balance-item">
                <div class="user-cell">
                    <div class="avatar">${getInitials(u.name)}</div>
                    <div>
                        <strong>${u.name}</strong>
                        <div style="font-size:12px; color:#64748b;">@${u.id}</div>
                    </div>
                </div>
                <span class="badge ${badgeClass}">${badgeText}</span>
            </div>
        `;
    }).join('');
}

// 2. Render Expense Form Dropdowns and Checkboxes
function renderExpenseForm() {
    const paidBySelect = document.getElementById('expPaidBy');
    paidBySelect.innerHTML = users.map(u => `<option value="${u.id}">${u.name} (@${u.id})</option>`).join('');

    const partsContainer = document.getElementById('participantsList');
    partsContainer.innerHTML = users.map(u => `
        <label class="check-item">
            <input type="checkbox" class="part-check" value="${u.id}" checked onchange="updateLiveSplit()">
            <span>${u.name}</span>
        </label>
    `).join('');

    document.getElementById('expAmount').oninput = updateLiveSplit;
    updateLiveSplit();
}

function updateLiveSplit() {
    const amt = parseFloat(document.getElementById('expAmount').value) || 0;
    const checked = document.querySelectorAll('.part-check:checked');
    const summaryBadge = document.getElementById('splitSummaryText');

    if (checked.length === 0) {
        summaryBadge.textContent = 'Select at least 1 person';
        summaryBadge.style.color = 'var(--danger)';
    } else {
        const each = (amt / checked.length).toFixed(2);
        summaryBadge.textContent = `₹${each} each (${checked.length} friends)`;
        summaryBadge.style.color = 'var(--primary)';
    }
}

// 3. Render Friends Tab
function renderFriends() {
    document.getElementById('friendsCount').textContent = users.length;
    const grid = document.getElementById('friendsGrid');

    if (users.length === 0) {
        grid.innerHTML = `<p style="color:#64748b;">No friends registered yet.</p>`;
        return;
    }

    grid.innerHTML = users.map(u => {
        const net = (balances.net && balances.net[u.id]) || 0;
        let badgeHtml = `<span class="badge zero">Settled</span>`;
        if (net > 0.01) badgeHtml = `<span class="badge credit">+₹${net.toFixed(2)}</span>`;
        if (net < -0.01) badgeHtml = `<span class="badge debt">-₹${Math.abs(net).toFixed(2)}</span>`;

        return `
            <div class="friend-card">
                <div class="friend-top">
                    <div class="avatar">${getInitials(u.name)}</div>
                    <div>
                        <strong>${u.name}</strong>
                        <div style="font-size:12px; color:#64748b;">${u.email || '@' + u.id}</div>
                    </div>
                </div>
                <div class="friend-bottom">
                    ${badgeHtml}
                    <button class="btn-delete" onclick="deleteUser('${u.id}')">🗑️ Remove</button>
                </div>
            </div>
        `;
    }).join('');
}

// 4. Render Expense History Tab
function renderHistory() {
    document.getElementById('expenseHistoryCount').textContent = `${expenses.length} expenses`;
    const tbody = document.getElementById('historyTableBody');

    if (expenses.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:#64748b; padding:24px;">No expenses recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = expenses.map(e => `
        <tr>
            <td><small>${e.date || '-'}</small></td>
            <td><strong>${e.description}</strong></td>
            <td><span class="cat-badge">${e.category}</span></td>
            <td>${getUserName(e.paidBy)}</td>
            <td><strong>₹${e.amount.toFixed(2)}</strong></td>
            <td><small>${(e.participants || []).length} friends</small></td>
            <td><button class="btn-delete" onclick="deleteExpense('${e.id}')">🗑️ Delete</button></td>
        </tr>
    `).join('');
}

// 5. Render Settle Tab
function renderSettleTab() {
    const fromSelect = document.getElementById('settleFrom');
    const toSelect = document.getElementById('settleTo');

    fromSelect.innerHTML = users.map(u => `<option value="${u.id}">${u.name}</option>`).join('');
    toSelect.innerHTML = users.map(u => `<option value="${u.id}">${u.name}</option>`).join('');

    const chips = document.getElementById('quickSettleButtons');
    const simplified = balances.simplified || [];

    if (simplified.length === 0) {
        chips.innerHTML = `<span style="font-size:13px; color:#64748b;">No outstanding payments!</span>`;
    } else {
        chips.innerHTML = simplified.map(tx => `
            <button type="button" class="chip-btn" onclick="quickFillSettle('${tx.from}', '${tx.to}', ${tx.amount})">
                ⚡ ${getUserName(tx.from)} pays ${getUserName(tx.to)} ₹${tx.amount.toFixed(2)}
            </button>
        `).join('');
    }
}

function quickFillSettle(from, to, amount) {
    document.getElementById('settleFrom').value = from;
    document.getElementById('settleTo').value = to;
    document.getElementById('settleAmount').value = amount;
    showToast(`Loaded optimal payment: ₹${amount.toFixed(2)}`);
}

// Form Handlers
function setupForms() {
    // Add Expense
    document.getElementById('expenseForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const description = document.getElementById('expDesc').value.trim();
        const amount = parseFloat(document.getElementById('expAmount').value);
        const category = document.getElementById('expCategory').value;
        const paidBy = document.getElementById('expPaidBy').value;

        const checked = Array.from(document.querySelectorAll('.part-check:checked')).map(cb => cb.value);
        if (checked.length === 0) {
            alert('Please select at least one friend to split the bill with.');
            return;
        }

        try {
            const res = await fetch(`${API}/api/expenses`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ description, amount, paidBy, category, participants: checked })
            });

            if (res.ok) {
                showToast('✅ Bill split and recorded!');
                document.getElementById('expenseForm').reset();
                showTab('dashboard');
                await fetchAllData();
            } else {
                const err = await res.json();
                showToast('❌ ' + (err.error || 'Error saving expense'));
            }
        } catch (err) {
            showToast('❌ Failed to save expense');
        }
    });

    // Add Friend
    document.getElementById('friendForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('friendName').value.trim();
        const id = document.getElementById('friendId').value.trim().toLowerCase();
        const email = document.getElementById('friendEmail').value.trim();
        const phone = document.getElementById('friendPhone').value.trim();

        try {
            const res = await fetch(`${API}/api/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, name, email, phone })
            });

            if (res.ok) {
                showToast(`✅ Member ${name} added!`);
                document.getElementById('friendForm').reset();
                await fetchAllData();
            } else {
                const err = await res.json();
                showToast('❌ ' + (err.error || 'Error adding member'));
            }
        } catch (err) {
            showToast('❌ Error adding member');
        }
    });

    // Settle Up
    document.getElementById('settleForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const from = document.getElementById('settleFrom').value;
        const to = document.getElementById('settleTo').value;
        const amount = parseFloat(document.getElementById('settleAmount').value);

        if (from === to) {
            alert('Debtor and creditor cannot be the same person.');
            return;
        }

        try {
            const res = await fetch(`${API}/api/settle`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ from, to, amount })
            });

            if (res.ok) {
                showToast(`✅ Settlement of ₹${amount.toFixed(2)} recorded!`);
                document.getElementById('settleForm').reset();
                showTab('dashboard');
                await fetchAllData();
            } else {
                const err = await res.json();
                showToast('❌ ' + (err.error || 'Error recording settlement'));
            }
        } catch (err) {
            showToast('❌ Failed to record settlement');
        }
    });
}

// Delete Operations
async function deleteUser(id) {
    if (confirm(`Remove @${id}? All related expenses will also be deleted.`)) {
        try {
            const res = await fetch(`${API}/api/users?id=${id}`, { method: 'DELETE' });
            if (res.ok) {
                showToast('Member removed');
                await fetchAllData();
            }
        } catch (err) {
            showToast('Error deleting member');
        }
    }
}

async function deleteExpense(id) {
    if (confirm('Delete this bill?')) {
        try {
            const res = await fetch(`${API}/api/expenses?id=${id}`, { method: 'DELETE' });
            if (res.ok) {
                showToast('Expense deleted');
                await fetchAllData();
            }
        } catch (err) {
            showToast('Error deleting expense');
        }
    }
}

// Helpers
function getUserName(id) {
    const u = users.find(x => x.id === id);
    return u ? u.name : id;
}

function getInitials(name) {
    if (!name) return '?';
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

