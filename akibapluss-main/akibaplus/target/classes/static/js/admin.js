// Cleaned and reorganized admin.js
// - Consolidated toast helpers
// - Grouped utilities, charts, meetings, members, loans, expenses, reports, users
// - Fixed event handler usage for filters
// - Kept functions global so templates with onclick continue working

// ============= GLOBAL VARIABLES =============
let dashGrowthChart, dashPortfolioChart;
let repGrowthChart, repPortfolioChart, repExpensesChart;
let meetingMap, meetingMarker, meetingCircle;

// ============= UTILITIES =============
function formatCurrency(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'TZS' }).format(amount);
}

function qs(selector, ctx = document) { return ctx.querySelector(selector); }
function qsa(selector, ctx = document) { return Array.from(ctx.querySelectorAll(selector)); }

function viewDetails(btn) {
  const title = btn.getAttribute('data-title');
  const fields = btn.getAttribute('data-fields').split('||');
  
  const modalTitle = document.getElementById('genericDetailsTitle');
  const modalBody = document.getElementById('genericDetailsBody');
  
  if(modalTitle) modalTitle.textContent = title;
  if(modalBody) {
      modalBody.innerHTML = '';
      const list = document.createElement('ul');
      list.className = 'list-group list-group-flush';
      
      fields.forEach(field => {
          const [key, value] = field.split('::');
          const item = document.createElement('li');
          item.className = 'list-group-item d-flex justify-content-between align-items-center px-0';
          item.innerHTML = `<span class="fw-bold text-muted">${key}</span> <span class="fw-medium text-dark">${value}</span>`;
          list.appendChild(item);
      });
      modalBody.appendChild(list);
  }
  
  new bootstrap.Modal(document.getElementById('genericDetailsModal')).show();
}

// ============= TOASTS =============
function showToast(message, type = 'success', toastId = 'actionToast', msgId = 'actionToastMessage') {
  const toast = document.getElementById(toastId);
  const msg = document.getElementById(msgId);
  if (!toast || !msg) return;
  msg.textContent = message;
  toast.classList.remove('bg-success', 'bg-info', 'bg-danger', 'bg-warning');
  const bgClass = type === 'success' ? 'bg-success' : type === 'info' ? 'bg-info' : type === 'warning' ? 'bg-warning' : 'bg-danger';
  toast.classList.add(bgClass);
  new bootstrap.Toast(toast).show();
}

// Convenience wrappers
const showReportToast = (m, t='success') => showToast(m, t, 'reportToast', 'reportToastMessage');
const showLogsToast = (m, t='info') => showToast(m, t, 'logsToast', 'logsToastMessage');
const showSharesToast = (m, t='info') => showToast(m, t, 'sharesToast', 'sharesToastMessage');
const showFinesToast = (m, t='success') => showToast(m, t, 'finesToast', 'finesToastMessage');
const showSettingsToast = (m, t='success') => showToast(m, t, 'settingsToast', 'settingsToastMessage');
const showNotificationsToast = (m, t='success') => showToast(m, t, 'notificationsToast', 'notificationsToastMessage');

// ============= NAVIGATION & UI =============
function loadPage(pageId) {
  const targetPage = document.getElementById(pageId);
  if (!targetPage) {
    console.error(`Page with id '${pageId}' not found.`);
    return;
  }

  // Hide all pages
  qsa('.page').forEach(p => {
    p.classList.remove('active');
    p.style.display = 'none'; // Explicitly hide
  });

  // Show the target page
  targetPage.classList.add('active');
  targetPage.style.display = 'block'; // Explicitly show

  // manage nav link active state
  qsa('.sidebar .nav-link').forEach(link => {
    const isActive = link.getAttribute('onclick')?.includes(`'${pageId}'`);
    link.classList.toggle('active', !!isActive);
  });

  // page-specific initializers
  if (pageId === 'dashboard') setTimeout(initDashboardCharts, 100);
  else if (pageId === 'reports') setTimeout(initReportsCharts, 100);
  else if (pageId === 'meetings') { cleanupMeetingMap(); setTimeout(initMeetingMap, 100); }
  else cleanupMeetingMap();

  closeMobileMenu();
}

function toggleMobileMenu() {
  document.querySelector('.sidebar')?.classList.toggle('show');
  document.querySelector('.overlay')?.classList.toggle('show');
}
function closeMobileMenu() {
  document.querySelector('.sidebar')?.classList.remove('show');
  document.querySelector('.overlay')?.classList.remove('show');
}

// ============= CHARTS =============
function safeDestroy(chart) { if (chart && typeof chart.destroy === 'function') chart.destroy(); }

function initDashboardCharts() {
  if (!window.akibaData) return;
  safeDestroy(dashGrowthChart); safeDestroy(dashPortfolioChart);

  const ctxGrowth = qs('#dashGrowthChart');
  if (ctxGrowth) {
    dashGrowthChart = new Chart(ctxGrowth, {
      type: 'line',
      data: {
        labels: window.akibaData.chartYears || [],
        datasets: [{
          label: 'Akiba (Milioni TZS)',
          data: window.akibaData.chartSavingsTrend || [],
          borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.1)',
          tension: 0.4, fill: true
        }]
      },
      options: { responsive: true, plugins: { legend: { position: 'top' } } }
    });
  }

  const ctxPortfolio = qs('#dashPortfolioChart');
  if (ctxPortfolio) {
    dashPortfolioChart = new Chart(ctxPortfolio, {
      type: 'doughnut',
      data: {
        labels: ['Akiba', 'Mikopo', 'Hisa'],
        datasets: [{
          data: [
            window.akibaData.chartSavings || 0,
            window.akibaData.chartLoans || 0,
            window.akibaData.chartShares || 0
          ],
          backgroundColor: ['#10b981', '#2563eb', '#f59e0b'], borderWidth: 0
        }]
      },
      options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    });
  }
}

function initReportsCharts() {
  if (!window.akibaData) return;
  safeDestroy(repGrowthChart); safeDestroy(repPortfolioChart); safeDestroy(repExpensesChart);

  const ctxRepGrowth = qs('#repGrowthChart');
  if (ctxRepGrowth) {
    repGrowthChart = new Chart(ctxRepGrowth, {
      type: 'line',
      data: {
        labels: window.akibaData.chartYears || [],
        datasets: [
          {
            label: 'Akiba (TZS)',
            data: window.akibaData.chartSavingsTrend || [],
            borderColor: '#10b981',
            backgroundColor: 'rgba(16,185,129,0.2)',
            tension: 0.4,
            fill: true,
            pointRadius: 5,
            pointHoverRadius: 7
          },
          {
            label: 'Mikopo (TZS)',
            data: window.akibaData.chartLoans || window.akibaData.chartSavingsTrend || [],
            borderColor: '#2563eb',
            backgroundColor: 'rgba(37,99,235,0.2)',
            tension: 0.4,
            fill: true,
            pointRadius: 5,
            pointHoverRadius: 7
          }
        ]
      },
      options: { 
        responsive: true, 
        plugins: { legend: { position: 'top', labels: { font: { size: 14 } } }, tooltip: { mode: 'index', intersect: false } }, 
        scales: { y: { beginAtZero: true, grid: { color: '#f0f0f0' } }, x: { grid: { display: false } } } 
      }
    });
  }

  const ctxRepPortfolio = qs('#repPortfolioChart');
  if (ctxRepPortfolio) {
    repPortfolioChart = new Chart(ctxRepPortfolio, {
      type: 'doughnut',
      data: {
        labels: ['Akiba', 'Mikopo', 'Hisa'],
        datasets: [{
          data: [
            window.akibaData.chartSavings || 0,
            window.akibaData.chartLoans || 0,
            window.akibaData.chartShares || 0
          ],
          backgroundColor: ['#10b981', '#2563eb', '#f59e0b'], borderWidth: 2, borderColor: '#ffffff'
        }]
      },
      options: { responsive: true, plugins: { legend: { position: 'right', labels: { font: { size: 13 } } } }, cutout: '60%' }
    });
  }

  const ctxRepExpenses = qs('#repExpensesChart');
  if (ctxRepExpenses) {
    repExpensesChart = new Chart(ctxRepExpenses, {
      type: 'bar',
      data: {
        labels: window.akibaData.expenseChartLabels || [],
        datasets: [{ 
          label: 'Matumizi (TZS)', 
          data: window.akibaData.expenseChartData || [], 
          backgroundColor: ['#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#10b981'],
          borderRadius: 5
        }]
      },
      options: { responsive: true, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, grid: { color: '#f0f0f0' } }, x: { grid: { display: false } } } }
    });
  }
}

// ============= MEETINGS (Leaflet) =============
function initMeetingMap() {
  if (!qs('#meetingMap')) return;
  if (meetingMap) { meetingMap.invalidateSize(); return; }

  meetingMap = L.map('meetingMap').setView([-6.8235, 39.2695], 13); // Default to Dar es Salaam
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(meetingMap);

  meetingMap.on('click', function(e) {
    const { lat, lng } = e.latlng;
    qs('#meetingLatLng').value = `${lat}, ${lng}`;
    if (meetingMarker) meetingMap.removeLayer(meetingMarker);
    if (meetingCircle) meetingMap.removeLayer(meetingCircle);
    meetingMarker = L.marker(e.latlng).addTo(meetingMap).bindPopup('Eneo la Mkutano').openPopup();
    meetingCircle = L.circle(e.latlng, { radius: 100 }).addTo(meetingMap);

    // Reverse geocode to get location name
    fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`)
      .then(res => res.json())
      .then(data => {
        if (qs('#meetingLocation')) qs('#meetingLocation').value = data.display_name || `${lat}, ${lng}`;
      });
  });
}

function cleanupMeetingMap() {
  if (!meetingMap) return;
  meetingMap.off();
  if (meetingMarker) meetingMap.removeLayer(meetingMarker);
  if (meetingCircle) meetingMap.removeLayer(meetingCircle);
  meetingMap.remove();
  meetingMap = meetingMarker = meetingCircle = null;
}

function getCurrentLocation() {
  if (!navigator.geolocation) return;
  navigator.geolocation.getCurrentPosition(position => {
    const { latitude: lat, longitude: lng } = position.coords;
    const el = qs('#meetingLatLng'); if (el) el.value = `${lat}, ${lng}`;
    if (meetingMap) {
      meetingMap.setView([lat, lng], 13);
      if (meetingMarker) meetingMap.removeLayer(meetingMarker);
      if (meetingCircle) meetingMap.removeLayer(meetingCircle);
      meetingMarker = L.marker([lat, lng]).addTo(meetingMap).bindPopup('Mahali Pangu Sasa').openPopup();
      meetingCircle = L.circle([lat, lng], { radius: 100 }).addTo(meetingMap);
    }
  });
}

// ============= REPORTS =============
function generateReport(type) {
  const reports = { members: 'Ripoti ya Wanachama', loans: 'Ripoti ya Mikopo', savings: 'Ripoti ya Akiba na Hisa', expenses: 'Ripoti ya Matumizi na Gawio' };
  showReportToast(`${reports[type] || type} inatengenezwa...`, 'info');
  setTimeout(() => showReportToast(`${reports[type] || type} imekamilika na inahamishwa!`, 'success'), 2000);
}

function downloadCustomReport() {
  showReportToast('Ripoti mpya inatengenezwa kulingana na chaguo lako...', 'info');
  setTimeout(() => showReportToast('Ripoti imekamilika na imehifadhiwa kwenye kifaa chako!', 'success'), 2000);
}

// ============= EXPENSES =============
function updateExpensesUI(data) {
  if (!data) return;
  if (qs('#expensesMonthlyTotal')) qs('#expensesMonthlyTotal').textContent = formatCurrency(data.expensesMonthlyTotal);
  if (qs('#expensesRemainingBudget')) qs('#expensesRemainingBudget').textContent = formatCurrency(data.expensesRemainingBudget);
  const tbody = qs('#expensesTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';
  (data.expensesList || []).forEach(expense => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td class="ps-3">${expense.date}</td>
      <td>${expense.desc}</td>
      <td>${formatCurrency(expense.amount)}</td>
      <td><span class="badge bg-success">${expense.status}</span></td>
      <td class="text-center"><button class="btn btn-sm btn-outline-primary">View</button></td>
    `;
    tbody.appendChild(row);
  });
}

function saveSingleExpense() {
  const form = qs('#singleExpenseForm');
  if (!form) return;
  const date = form.querySelector('input[type="date"]').value;
  const amount = qs('#singleAmount').value;
  const desc = form.querySelector('textarea').value;
  const category = form.querySelector('select').value;
  if (!date || !amount || !desc || !category) { alert('Tafadhali jaza taarifa zote muhimu.'); return; }
  const payload = { date, amount, desc, category };
  fetch('/admin/api/expenses/add', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) })
    .then(res => res.json()).then(data => {
      if (data.success) { alert(data.message); bootstrap.Modal.getInstance(document.getElementById('singleExpenseModal'))?.hide(); updateExpensesUI(data.updatedData); }
      else alert('Hitilafu: ' + data.message);
    })
    .catch(e => alert('Hitilafu: ' + e.message));
}

function saveOCRAutoFilled() {
  const date = qs('#ocrDate')?.value, amount = qs('#ocrAmount')?.value, desc = qs('#ocrDescription')?.value, category = qs('#ocrCategory')?.value;
  if(!date || !amount || !desc || !category) { showToast('Tafadhali jaza taarifa zote muhimu.', 'danger'); return; }
  fetch('/admin/api/expenses/add', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({date,amount,desc,category}) })
    .then(r=>r.json()).then(data => {
      if (data.success) { showToast(data.message,'success'); bootstrap.Modal.getInstance(document.getElementById('ocrUploadModal'))?.hide(); updateExpensesUI(data.updatedData); }
      else showToast('Hitilafu: ' + data.message, 'danger');
    }).catch(e => showToast('Hitilafu: ' + e.message, 'danger'));
}

// ============= MEMBERS =============
function openAddMemberModal() {
  qs('#addMemberForm')?.reset();
  new bootstrap.Modal(document.getElementById('addMemberModal')).show();
}

function saveNewMember() {
  const get = id => qs('#' + id)?.value;
  const payload = {
    name: get('fullName'), phone: get('phone'), email: get('email'), dob: get('dob'),
    gender: get('gender'), idNumber: get('idNumber'), address: get('address'),
    guarantor: get('guarantor'), guarantorPhone: get('guarantorPhone'),
    membershipType: get('membershipType'), initialShares: parseInt(get('initialShares') || '0', 10)
  };
  if (!payload.name || !payload.phone || !payload.dob || !payload.gender || !payload.idNumber || !payload.address || !payload.guarantor) {
    alert('Tafadhali jaza taarifa zote muhimu (*).'); return;
  }
  fetch('/admin/api/members/add', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) })
    .then(r=>r.json()).then(data => {
      if (data.success) { showToast('Mwanachama mpya ' + payload.name + ' ameongezwa kikamilifu!', 'success'); bootstrap.Modal.getInstance(document.getElementById('addMemberModal'))?.hide(); loadMembersData(); }
      else alert('Hitilafu: ' + data.message);
    }).catch(e => alert('Hitilafu: ' + e.message));
}

function loadMembersData() {
  fetch('/admin/api/members/list').then(r=>r.json()).then(data => {
    if (!data.success || !data.members) return;
    const tbody = qs('#membersTableBody'); if (tbody) {
      tbody.innerHTML = '';
      data.members.forEach(member => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td class="fw-semibold ps-4">${member.name}</td>
          <td>${member.membershipNumber || 'N/A'}</td>
          <td>${member.phone}</td>
          <td>${member.joinedOn || 'N/A'}</td>
          <td><span class="badge bg-success">${member.status || 'ACTIVE'}</span></td>
          <td>${formatCurrency(member.savingsBalance || 0)}</td>
          <td class="text-center"><button class="btn btn-sm btn-outline-primary">View</button></td>
        `;
        tbody.appendChild(row);
      });
    }
    if (qs('#totalMembersCount')) qs('#totalMembersCount').textContent = data.total || 0;
    if (qs('#activeMembers')) qs('#activeMembers').textContent = data.active || 0;
    if (qs('#inactiveMembers')) qs('#inactiveMembers').textContent = data.inactive || 0;
    if (qs('#pendingMembers')) qs('#pendingMembers').textContent = data.pending || 0;
  }).catch(e => console.error('Error loading members:', e));
}

function filterMembers() {
  const input = (qs('#memberSearchInput')?.value || '').toLowerCase();
  qsa('#membersTableBody tr').forEach(row => row.style.display = row.textContent.toLowerCase().includes(input) ? '' : 'none');
}
function exportMembers() { showToast('Inahamisha orodha ya wanachama...', 'info'); }
function printMembersList() { window.print(); }

// ============= LOANS =============
function loadLoansData() {
  fetch('/admin/api/loans/list').then(r=>r.json()).then(data => {
    if (!data.success || !data.loans) return;
    const tbody = qs('#allLoansBody'); if (tbody) {
      tbody.innerHTML = '';
      data.loans.forEach(loan => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td class="fw-semibold ps-4">${loan.memberName || 'N/A'}</td>
          <td>${formatCurrency(loan.amount || 0)}</td>
          <td>${loan.type || 'N/A'}</td>
          <td>${formatCurrency(loan.balance || 0)}</td>
          <td>${loan.date || 'N/A'}</td>
          <td>${loan.dueDate || 'N/A'}</td>
          <td><span class="badge bg-success">${loan.status || 'ACTIVE'}</span></td>
          <td class="text-center"><button class="btn btn-sm btn-outline-primary">View</button></td>
        `;
        tbody.appendChild(row);
      });
    }
    if (qs('#loansActiveLoans')) qs('#loansActiveLoans').textContent = data.activeCount || 0;
    if (qs('#loansTotalPortfolio')) qs('#loansTotalPortfolio').textContent = formatCurrency(data.totalPortfolio || 0);
    if (qs('#loansOutstandingAmount')) qs('#loansOutstandingAmount').textContent = formatCurrency(data.outstanding || 0);
    if (qs('#loansNplRatio')) qs('#loansNplRatio').textContent = data.nplRatio || '0%';
  }).catch(e => console.error('Error loading loans:', e));
}

function filterAllLoans(eventOrInput) {
  const input = typeof eventOrInput === 'string' ? eventOrInput.toLowerCase() : (eventOrInput?.target?.value || '').toLowerCase();
  qsa('#allLoansBody tr').forEach(row => row.style.display = row.textContent.toLowerCase().includes(input) ? '' : 'none');
}

// ============= FILTER HELPERS (generic) =============
function filterByInput(selectorForInput, selectorForRows) {
  const input = (qs(selectorForInput)?.value || '').toLowerCase();
  qsa(selectorForRows).forEach(row => row.style.display = row.textContent.toLowerCase().includes(input) ? '' : 'none');
}
function filterSavingsTable(event) { filterByInput('#savingsSearchInput', '#savingsMembersTableBody tr'); }
function filterNotifications() { filterByInput('#notificationsSearchInput', '#notificationsTableBody tr'); }
function filterFines() { filterByInput('#finesSearchInput', '#finesTableBody tr'); }
function filterLoginLogs() { filterByInput('#loginLogSearch', '#loginLogsTableBody tr'); }

// ============= USER MANAGEMENT =============
function saveNewUser() {
  showToast('Mtumiaji mpya amesajiliwa!', 'success');
  bootstrap.Modal.getInstance(document.getElementById('addUserModal'))?.hide();
}

function save2FASettings() {
  showToast('Mipangilio ya 2FA imehifadhiwa!', 'success');
  bootstrap.Modal.getInstance(document.getElementById('manage2FAModal'))?.hide();
}

function toggle2FA(checkbox) {
  showToast(checkbox.checked ? '2FA imewashwa' : '2FA imezimwa', 'info');
}

// ============= MEETINGS (Continued) =============
function loadMeetingsData() {
  fetch('/admin/api/meetings/list')
    .then(res => res.json())
    .then(data => {
      const tbody = qs('#meetingsPageTableBody');
      if (!tbody) return;
      tbody.innerHTML = '';
      if (data && data.length > 0) {
        data.forEach(mt => {
          const row = document.createElement('tr');
          row.innerHTML = `
            <td class="ps-3">${mt.name}</td>
            <td>${mt.date}</td>
            <td>${mt.location}</td>
            <td>${mt.expected || 0}</td>
            <td>${mt.attended || 0}</td>
            <td><span class="badge bg-info">${mt.status}</span></td>
            <td class="text-center"><button class="btn btn-sm btn-outline-primary">View</button></td>
          `;
          tbody.appendChild(row);
        });
      } else {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Hakuna mikutano iliyopangwa.</td></tr>';
      }
    })
    .catch(err => console.error('Failed to load meetings:', err));
}

function saveMeeting() {
  const payload = {
    title: qs('#meetingTitle')?.value,
    date: qs('#meetingDate')?.value,
    startTime: qs('#meetingStartTime')?.value,
    endTime: qs('#meetingEndTime')?.value,
    location: qs('#meetingLocation')?.value,
    latLng: qs('#meetingLatLng')?.value,
    agenda: qs('#meetingAgenda')?.value
  };

  if (!payload.title || !payload.date || !payload.startTime || !payload.endTime || !payload.latLng) {
    showToast('Tafadhali jaza taarifa zote muhimu za mkutano!', 'warning');
    return;
  }

  fetch('/admin/api/meetings/schedule', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
    .then(res => res.json())
    .then(data => {
      if (data.success) { showToast(data.message, 'success'); cleanupMeetingMap(); bootstrap.Modal.getInstance(qs('#scheduleMeetingModal'))?.hide(); loadMeetingsData(); } 
      else { showToast('Hitilafu: ' + data.message, 'danger'); }
    }).catch(err => showToast('Hitilafu ya mtandao: ' + err.message, 'danger'));
}
// ============= INIT ON READY =============
document.addEventListener('DOMContentLoaded', () => {
  // date header
  const dateEl = qs('#currentDate');
  if (dateEl) {
    const today = new Date();
    const options = { day: 'numeric', month: 'long', year: 'numeric' };
    dateEl.innerHTML = '<i class="fas fa-calendar me-2"></i>' + today.toLocaleDateString('sw-TZ', options);
  }

  // schedule meeting modal hook
  const scheduleModal = document.getElementById('scheduleMeetingModal');
  if (scheduleModal) scheduleModal.addEventListener('shown.bs.modal', () => { cleanupMeetingMap(); setTimeout(initMeetingMap, 100); });

  // Initial data loads
  const dashboard = document.getElementById('dashboard');
  if (dashboard && !dashboard.classList.contains('active')) {
    loadPage('dashboard');
  }
  loadMeetingsData(); // Load meetings on initial page load
});

// Expose key functions to window in case templates call them directly
window.loadPage = loadPage;
window.toggleMobileMenu = toggleMobileMenu;
window.closeMobileMenu = closeMobileMenu;
window.initDashboardCharts = initDashboardCharts;
window.initReportsCharts = initReportsCharts;
window.cleanupMeetingMap = cleanupMeetingMap;
window.getCurrentLocation = getCurrentLocation;
window.saveMeeting = saveMeeting;
window.generateReport = generateReport;
window.downloadCustomReport = downloadCustomReport;
window.saveSingleExpense = saveSingleExpense;
window.saveOCRAutoFilled = saveOCRAutoFilled;
window.openAddMemberModal = openAddMemberModal;
window.saveNewMember = saveNewMember;
window.loadMembersData = loadMembersData;
window.filterMembers = filterMembers;
window.loadLoansData = loadLoansData;
window.filterAllLoans = filterAllLoans;
window.filterSavingsTable = filterSavingsTable;
window.filterNotifications = filterNotifications;
window.filterFines = filterFines;
window.saveNewUser = saveNewUser;
window.save2FASettings = save2FASettings;
window.toggle2FA = toggle2FA;
window.filterLoginLogs = filterLoginLogs;
window.viewDetails = viewDetails;
