// AkibaPlus SACCOS - Member Dashboard JavaScript
// Handles page navigation, charts, and dynamic interactions

// ====================
// Page Navigation
// ====================
function loadPage(pageId) {
  // Hide all pages
  const pages = document.querySelectorAll('.page');
  pages.forEach(page => page.classList.remove('active'));

  // Show selected page
  const selectedPage = document.getElementById(pageId);
  if (selectedPage) {
    selectedPage.classList.add('active');

    // Update sidebar active link
    const navLinks = document.querySelectorAll('.sidebar .nav-link, .offcanvas .nav-link');
    navLinks.forEach(link => link.classList.remove('active'));

    // Mark current page link as active
    const currentLink = document.querySelector(`a[onclick="loadPage('${pageId}')"]`);
    if (currentLink) {
      currentLink.classList.add('active');
    }

    // Initialize charts if on dashboard
    if (pageId === 'dashboard') {
      initializeDashboardCharts();
    } else if (pageId === 'analytics') {
      initializeAnalyticsCharts();
    }
    
    // Save state
    localStorage.setItem('akibaActivePage', pageId);
  }
}

// Toast Helper (Fallback to alert for now)
function showToast(message, type = 'success') {
  alert(message);
}

// ====================
// Chart Initialization
// ====================
function initializeDashboardCharts() {
  fetch('/api/member/analytics')
    .then(res => res.json())
    .then(data => {
      initSavingsChart(data);
      initDistributionChart(data);
    })
    .catch(err => console.error('Failed to load dashboard data', err));
}

function initSavingsChart(data) {
  const ctx = document.getElementById('savingsChart');
  if (!ctx) return;

  if (ctx.savingsChartInstance) {
    ctx.savingsChartInstance.destroy();
    ctx.savingsChartInstance = null;
  }

  ctx.savingsChartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: data.months || [],
      datasets: [{
        label: 'Salio la Akiba (TZS)',
        data: data.savingsTrend || [],
        borderColor: '#2563eb',
        backgroundColor: 'rgba(37, 99, 235, 0.1)',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointRadius: 6,
        pointBackgroundColor: '#2563eb',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointHoverRadius: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          display: true,
          labels: { font: { size: 12 } }
        }
      },
      scales: {
        y: {
          beginAtZero: false,
          ticks: {
            callback: function(value) {
              return (value / 1000000).toFixed(1) + 'M';
            }
          }
        }
      }
    }
  });
}

function initDistributionChart(data) {
  const ctx = document.getElementById('distributionChart');
  if (!ctx) return;

  if (ctx.distributionChartInstance) {
    ctx.distributionChartInstance.destroy();
    ctx.distributionChartInstance = null;
  }

  ctx.distributionChartInstance = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Akiba', 'Hisa', 'Mikopo'],
      datasets: [{
        data: [data.currentSavings || 0, data.currentShares || 0, data.currentLoans || 0],
        backgroundColor: [
          '#2563eb',
          '#10b981',
          '#f59e0b'
        ],
        borderColor: '#fff',
        borderWidth: 3
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { font: { size: 12 } }
        }
      }
    }
  });
}

// ====================
// Form Handling
// ====================

// Deposit Modal
function openDepositModal() {
  const depositModal = new bootstrap.Modal(document.getElementById('depositModal'));
  depositModal.show();
}

function submitDeposit() {
  const amount = document.getElementById('depositAmount').value;
  const phone = document.getElementById('depositPhone').value;

  if (!amount || amount < 1000) {
    alert('Tafadhali ingiza kiasi halali (kianzio 1000).');
    return;
  }

  const btn = document.querySelector('#depositModal button');
  const originalText = btn.innerHTML;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inachakata...';
  btn.disabled = true;

  // backend endpoint is under /api/savings/deposit in MemberApiController
  fetch('/api/savings/deposit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ amount: amount, phone: phone })
  })
  .then(response => {
    if (response.ok) {
      alert('Amana imepokelewa kikamilifu!');
      window.location.reload(); // Force reload to update balances
    } else {
      throw new Error('Imeshindwa kuweka akiba');
    }
  })
  .catch(error => {
    console.error('Error:', error);
    alert('Kuna tatizo. Tafadhali jaribu tena.');
    btn.innerHTML = originalText;
    btn.disabled = false;
  });
}

// Payment Selection
function selectPaymentMethod(method) {
  // Remove active class from all payment methods
  document.querySelectorAll('.payment-method').forEach(el => {
    el.classList.remove('active-payment', 'border-primary', 'border-3');
  });

  // Add active class to selected method
  const methodElement = document.getElementById(method + 'Method');
  if (methodElement) {
    methodElement.classList.add('active-payment', 'border-primary', 'border-3');
  }

  // Hide all detail sections
  document.getElementById('bankDetails').style.display = 'none';
  document.getElementById('mobileDetails').style.display = 'none';

  // Show selected detail section
  if (method === 'bank') {
    document.getElementById('bankDetails').style.display = 'block';
  } else if (method === 'mobile') {
    document.getElementById('mobileDetails').style.display = 'block';
  }
}

function setMobileNumber(number, provider) {
  document.getElementById('mobileNumber').value = number;
  console.log('Selected ' + provider + ': ' + number);
}

function submitWithdraw() {
  const amount = document.getElementById('withdrawAmount').value;
  if(!amount || amount < 1000) { alert('Weka kiasi halali.'); return; }
  fetch('/api/savings/withdraw', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ amount }) })
  .then(res => res.json())
  .then(data => { alert(data.message); if(data.success) window.location.reload(); });
}

// Loan Application Multi-Step Form
let currentStep = 1;

function nextStep(step) {
  // Hide current step
  document.getElementById('step' + currentStep).style.display = 'none';
  
  // Show next step
  document.getElementById('step' + step).style.display = 'block';
  
  // Update step indicator
  updateStepIndicator(step);
  
  currentStep = step;
  
  // Scroll to top
  window.scrollTo(0, 0);
}

function updateStepIndicator(step) {
  const stepItems = document.querySelectorAll('.step-item');
  stepItems.forEach((item, index) => {
    if (index + 1 <= step) {
      item.classList.add('active');
      item.querySelector('div').classList.remove('bg-light', 'text-muted');
      item.querySelector('div').classList.add('bg-primary', 'text-white');
      item.querySelector('span').classList.remove('text-muted');
    } else {
      item.classList.remove('active');
      item.querySelector('div').classList.remove('bg-primary', 'text-white');
      item.querySelector('div').classList.add('bg-light', 'text-muted');
      item.querySelector('span').classList.add('text-muted');
    }
  });
}

function toggleCollateral() {
  const checkbox = document.getElementById('hasCollateral');
  const section = document.getElementById('collateralSection');
  section.style.display = checkbox.checked ? 'block' : 'none';
}

function toggleGuarantor() {
  const checkbox = document.getElementById('hasGuarantor');
  const section = document.getElementById('guarantorSection');
  section.style.display = checkbox.checked ? 'block' : 'none';
}

function selectEmployment(type) {
  const employedDocs = document.getElementById('employedDocs');
  const selfEmployedDocs = document.getElementById('selfEmployedDocs');
  
  // Remove active class from all options
  document.querySelectorAll('.employment-option').forEach(el => el.classList.remove('active-employment', 'border-primary', 'border-3'));
  
  // Add active class to selected option
  if (type === 'employed') {
    document.querySelector('.employment-option').classList.add('active-employment', 'border-primary', 'border-3');
    employedDocs.style.display = 'block';
    selfEmployedDocs.style.display = 'none';
  } else {
    document.querySelectorAll('.employment-option')[1].classList.add('active-employment', 'border-primary', 'border-3');
    employedDocs.style.display = 'none';
    selfEmployedDocs.style.display = 'block';
  }
}

document.addEventListener('DOMContentLoaded', function() {
  const termsCheckbox = document.getElementById('agreeTerms');
  const submitBtn = document.getElementById('submitBtn');
  
  if (termsCheckbox && submitBtn) {
    termsCheckbox.addEventListener('change', function() {
      submitBtn.disabled = !this.checked;
    });
  }
});

// Hide/disable sign buttons for meetings that have already ended and mark as past
function refreshMeetingButtons() {
  document.querySelectorAll('[data-meeting-id]').forEach(btn => {
    try {
      const dateStr = btn.dataset.meetingDate; // format: yyyy-MM-dd
      const startStr = btn.dataset.meetingStart; // HH:MM:SS
      const endStr = btn.dataset.meetingEnd; // HH:MM:SS
      if (!dateStr) return;
      const meetingDate = new Date(dateStr);
      const now = new Date();
      // If meeting is today and has end time, compare
      if (meetingDate.toDateString() === now.toDateString() && endStr) {
        const [eh, em] = endStr.split(':');
        const endTime = new Date(meetingDate.getFullYear(), meetingDate.getMonth(), meetingDate.getDate(), parseInt(eh), parseInt(em || '0'));
        if (now > endTime) {
          // disable button and mark as past
          btn.style.display = 'none';
          const card = btn.closest('.card');
          if (card) card.classList.add('meeting-past');
        }
      }
    } catch (e) {
      console.warn('Error evaluating meeting button', e);
    }
  });
}

document.addEventListener('DOMContentLoaded', function() {
  refreshMeetingButtons();
});

function submitLoanApplication() {
  const btn = document.getElementById('submitBtn');
  btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inawasilisha...';
  btn.disabled = true;
  
  const loanData = {
    type: document.getElementById('loanType').value,
    amount: document.getElementById('loanAmount').value,
    duration: parseInt(document.getElementById('loanDuration').value),
    reason: document.getElementById('loanReason').value,
  };

  const fd = new FormData();
  fd.append('type', loanData.type);
  fd.append('amount', loanData.amount);
  fd.append('duration', loanData.duration);
  fd.append('reason', loanData.reason);

  fetch('/api/loans/apply', {
    method: 'POST',
    body: fd
  })
  .then(response => response.json())
  .then(data => {
    alert(data.message);
    if (data.success) window.location.reload();
  })
  .catch(error => { console.error(error); alert('Imeshindwa kuwasilisha ombi.'); })
  .finally(() => { btn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Wasilisha Ombi Sasa'; btn.disabled = false; });
}

// ====================
// Transaction Modals
// ====================

function preparePayment(amount, description) {
  document.getElementById('penaltyAmount').textContent = 'TZS ' + amount.toLocaleString();
  document.getElementById('penaltyDescription').textContent = description;
}

function confirmPenaltyPayment() {
  fetch('/api/penalties/pay', { method: 'POST' }).then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
}

// ====================
// Guarantor Functions
// ====================

function acceptGuarantor() {
  alert('Umekubali kuwa mdhamini. Mtombaji atapokea taarifa.');
  bootstrap.Modal.getInstance(document.getElementById('reviewGuarantorModal')).hide();
  console.log('Guarantor request accepted');
}

function rejectGuarantor() {
  const reason = prompt('Tafadhali eleza sababu ya kukataa (hiari):');
  if (reason !== null) {
    alert('Umekataaa ombi. Mtombaji atapokea taarifa.');
    bootstrap.Modal.getInstance(document.getElementById('reviewGuarantorModal')).hide();
    console.log('Guarantor request rejected: ' + reason);
  }
}

// ====================
// Meeting Functions
// ====================

function signAttendance(meetingId, meetingName, latitude, longitude) {
  // Check geolocation
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function(position) {
        const userLat = position.coords.latitude;
        const userLng = position.coords.longitude;
        
        // Calculate distance (simple distance formula)
        const distance = Math.sqrt(
          Math.pow(userLat - latitude, 2) + Math.pow(userLng - longitude, 2)
        ) * 111; // Approximate km conversion
        
        // Use meeting radius if provided on the meeting button (fallback to 200m)
        let allowedMeters = 200; // default fallback 200m
        const btn = document.querySelector('[data-meeting-id="' + meetingId + '"]');
        if (btn && btn.dataset && btn.dataset.meetingRadius) {
          const r = parseInt(btn.dataset.meetingRadius);
          if (!isNaN(r) && r > 0) allowedMeters = r;
        }
        if (distance <= allowedMeters) {
          // Call backend to record attendance. Include coordinates so server verifies location and time.
          fetch('/api/meetings/attend', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ meetingId: meetingId, latitude: userLat, longitude: userLng })
          })
          .then(response => {
            if(response.ok) {
              alert('Mahudhurio yamerekodiwa kwa mafanikio! Asante kwa kuhudhuria.');
              console.log('Attendance signed for: ' + meetingName);
              // Hide the sign button for this meeting and mark card as attended
              try {
                const btn = document.querySelector('[data-meeting-id="' + meetingId + '"]');
                if (btn) {
                  btn.style.display = 'none';
                  const card = btn.closest('.card');
                  if (card) card.classList.add('meeting-attended');
                }
              } catch (e) { console.warn('Failed to update meeting UI after sign', e); }

              // Reload page to surface pending fines and updated meeting lists
              setTimeout(() => window.location.reload(), 700);

            } else {
              response.json().then(j => alert(j.message || 'Imeshindwa kurekodi mahudhurio. Tafadhali jaribu tena.')).catch(() => {
                alert('Imeshindwa kurekodi mahudhurio. Tafadhali jaribu tena.');
              });
            }
          });
        } else {
          alert('Haipo mahali pa mkutano. Karibu zaidi ili kusaini mahudhurio.');
        }
      },
      function(error) {
        alert('Haiwezi kukapata mahali peako. Ruhusu programu kufikia mahali.');
        console.error('Geolocation error:', error);
      }
    );
  }
}

// ====================
// Statement Filtering
// ====================

function filterStatements(period) {
  // Remove active class from all period cards
  document.querySelectorAll('.period-card').forEach(el => {
    el.classList.remove('active');
  });

  // Add active class to selected period
  document.querySelector(`[data-period="${period}"]`).classList.add('active');

  // Filter statement rows
  document.querySelectorAll('#statementsBody tr').forEach(row => {
    const rowPeriod = row.getAttribute('data-period');
    if (rowPeriod === period) {
      row.style.display = 'table-row';
    } else {
      row.style.display = 'none';
    }
  });
}

function downloadStatement(format, period) {
  alert(`Inapakua taarifa: ${period} (${format})...`);
}

// ====================
// Analytics Charts
// ====================

function initializeAnalyticsCharts() {
  fetch('/api/member/analytics')
    .then(res => res.json())
    .then(data => {
      initSavingsSharesGrowthChart(data);
      initIncomeExpenseChart(data);
      initLoanRepaymentChart(data);
      initMonthlyContributionsChart(data);
      initInterestDividendsChart(data);
      initLoansPaidChart(data);
      initAssetAllocationChart(data);
    })
    .catch(err => console.error('Failed to load analytics data', err));
}

function initSavingsSharesGrowthChart(data) {
  const ctx = document.getElementById('savingsSharesGrowthChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: data.months,
      datasets: [
        {
          label: 'Akiba',
          data: data.savingsTrend,
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        },
        {
          label: 'Hisa',
          data: data.sharesTrend,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function(value) {
              return (value / 1000000).toFixed(1) + 'M';
            }
          }
        }
      }
    }
  });
}

function initIncomeExpenseChart(data) {
  const ctx = document.getElementById('incomeExpenseChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: data.months,
      datasets: [
        {
          label: 'Mapato',
          data: data.incomeTrend,
          backgroundColor: '#10b981'
        },
        {
          label: 'Matumizi',
          data: data.expenseTrend,
          backgroundColor: '#ef4444'
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true }
      }
    }
  });
}

function initLoanRepaymentChart(data) {
  const ctx = document.getElementById('loanRepaymentChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Ililipwa', 'Salio Lililobaki'],
      datasets: [{
        data: [data.loanPaid, data.loanOutstanding],
        backgroundColor: ['#10b981', '#f59e0b']
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initMonthlyContributionsChart(data) {
  const ctx = document.getElementById('monthlyContributionsChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: data.months,
      datasets: [{
        label: 'Mchango wa Kila Mwezi',
        data: data.contributionsTrend,
        backgroundColor: '#2563eb'
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initInterestDividendsChart(data) {
  const ctx = document.getElementById('interestDividendsChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: data.months,
      datasets: [
        {
          label: 'Riba',
          data: data.interestTrend,
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        },
        {
          label: 'Gawio',
          data: data.dividendsTrend,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        }
      ]
    },
    options: {
      responsive: true
    }
  });
}

function initLoansPaidChart(data) {
  const ctx = document.getElementById('loansPaidChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Ililipwa', 'Salio'],
      datasets: [{
        label: 'Kiasi (TZS)',
        data: [data.loanPaid, data.loanOutstanding],
        backgroundColor: ['#10b981', '#ef4444']
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initAssetAllocationChart(data) {
  const ctx = document.getElementById('assetAllocationChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Akiba', 'Hisa', 'Mikopo'],
      datasets: [{
        data: [data.currentSavings, data.currentShares, data.currentLoans],
        backgroundColor: ['#2563eb', '#10b981', '#f59e0b'],
        borderColor: '#fff',
        borderWidth: 3
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    }
  });
}

// ====================
// Shares Functions
// ====================

function calcGroupTotal() {
  const qty = document.getElementById('groupShareQty').value;
  document.getElementById('groupShareTotal').value = 'TZS ' + (qty * 10000).toLocaleString();
}

function submitBuyGroupShares() {
  const qty = document.getElementById('groupShareQty').value;
  fetch('/api/shares/buy', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ type: 'GROUP', quantity: qty }) })
  .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
}

function submitSellShares() {
  const qty = document.getElementById('sellShareQty').value;
  const price = document.getElementById('sellSharePrice').value;
  fetch('/api/shares/sell', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ quantity: qty, price: price }) })
  .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
}

function buyMarketShare(btn) {
  if(confirm('Unataka kununua hisa hizi?')) {
    fetch('/api/shares/buy-market', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ saleId: btn.dataset.id }) })
    .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
  }
}

// ====================
// Profile & Loan Management
// ====================

async function saveProfileChanges(section) {
  let btn = (document.activeElement && document.activeElement.tagName === 'BUTTON') ? document.activeElement : null;
  const orig = btn ? btn.innerHTML : null;
  if (btn) { btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inasasisha...'; btn.disabled = true; }

  try {
    const fd = new FormData();
    const fieldMap = {
      firstName: 'profileFirstName', middleName: 'profileMiddleName', lastName: 'profileLastName',
      dob: 'profileDob', gender: 'profileGender', maritalStatus: 'profileMarital', phone: 'profilePhone',
      street: 'profileStreet', district: 'profileDistrict', region: 'profileRegion', addressDescription: 'profileAddressDesc',
      nextOfKinName: 'nokName', nextOfKinPhone: 'nokPhone', nextOfKinRelation: 'nokRelation', nextOfKinPercent: 'nokPercent'
    };

    Object.entries(fieldMap).forEach(([key, id]) => {
      const el = document.getElementById(id);
      if (el && el.value !== undefined) fd.append(key, el.value);
    });

    const fileEl = document.getElementById('profilePicInput');
    if (fileEl && fileEl.files && fileEl.files[0]) fd.append('profileImage', fileEl.files[0]);

    const res = await fetch('/api/profile/update', { method: 'POST', body: fd });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Save failed');

    if (data.success) { alert(data.message || 'Taarifa zimehifadhiwa.'); window.location.reload(); } 
    else { alert(data.message || 'Imeshindwa kuhifadhi.'); }
  } catch (e) { console.error(e); alert('Kosa: ' + (e.message || e)); } 
  finally { if (btn) { btn.innerHTML = orig; btn.disabled = false; } }
}

function changePassword() {
  const current = document.getElementById('currentPassword').value;
  const newPass = document.getElementById('newPassword').value;
  const confirmPass = document.getElementById('confirmPassword').value;
  if (newPass !== confirmPass) { alert('Nenosiri mpya hazifanani.'); return; }
  fetch('/api/profile/password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ currentPassword: current, newPassword: newPass }) })
  .then(res => res.json()).then(data => { alert(data.message); if (data.success) document.getElementById('passwordForm').reset(); });
}

function previewProfilePic(event) {
  if (event.target.files && event.target.files[0]) {
    document.getElementById('profileImage').src = URL.createObjectURL(event.target.files[0]);
  }
}

function downloadMembershipCard() {
  const card = document.querySelector('.membership-card');
  if (card && typeof html2canvas !== 'undefined') {
    html2canvas(card).then(canvas => {
      const link = document.createElement('a');
      link.download = 'AkibaPlus_MembershipCard.png';
      link.href = canvas.toDataURL();
      link.click();
    });
  } else { alert('Maktaba ya html2canvas haijapakiwa.'); }
}

function initiateLoanPayment(btn) {
  const id = btn.getAttribute('data-id');
  const nextPayment = btn.getAttribute('data-nextpayment');
  loadPage('loan-payment');
  const select = document.getElementById('selectedLoan');
  if (select) { select.value = id; updatePaymentSummary(); }
  const amountInput = document.getElementById('paymentAmount');
  if (amountInput) amountInput.value = parseFloat(String(nextPayment).replace(/[^0-9.-]+/g,"")) || 10000;
  updatePaymentSummary();
}

function updatePaymentSummary() {
  const select = document.getElementById('selectedLoan');
  if (!select) return;
  const selectedOption = select.options[select.selectedIndex];
  if (!selectedOption) return;
  const currentBalance = parseFloat(String(selectedOption.getAttribute('data-balance')).replace(/[^0-9.-]+/g,"")) || 0;
  const amount = parseFloat(document.getElementById('paymentAmount').value) || 0;
  document.getElementById('currentBalance').innerText = 'TZS ' + currentBalance.toLocaleString();
  document.getElementById('summaryAmount').innerText = 'TZS ' + amount.toLocaleString();
  document.getElementById('newBalance').innerText = 'TZS ' + Math.max(0, currentBalance - amount).toLocaleString();
}

function submitLoanPayment(btn) {
  const amount = document.getElementById('paymentAmount').value;
  const loanId = document.getElementById('selectedLoan').value;
  let method = document.getElementById('bankDetails').style.display !== 'none' ? 'BANK' : 'MOBILE';
  if (!loanId || !amount || amount < 1000) { alert('Tafadhali chagua mkopo na weka kiasi halali.'); return; }
  const originalText = btn.innerHTML; btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inachakata...'; btn.disabled = true;
  fetch('/api/loans/pay', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ amount: amount, loanId: loanId, method: method }) })
  .then(res => res.json()).then(data => { alert(data.message); if (data.success) window.location.reload(); })
  .catch(err => alert('Hitilafu imetokea.')).finally(() => { btn.innerHTML = originalText; btn.disabled = false; });
}

function showLoanDetails(btn) {
  const data = btn.dataset;
  const formatMoney = (amount) => 'TZS ' + (parseFloat(String(amount).replace(/[^0-9.-]+/g,"")) || 0).toLocaleString();
  document.getElementById('detailType').innerText = (data.type || 'Mkopo') + (data.ref ? ' - ' + data.ref : '');
  document.getElementById('detailRef').innerText = data.ref || 'N/A';
  document.getElementById('detailDate').innerText = data.date || '-';
  document.getElementById('detailDueDate').innerText = data.duedate || '-';
  document.getElementById('detailStatus').innerText = data.status || 'PENDING';
  
  const progress = Math.round(parseFloat(data.progress) || 0);
  const progressBar = document.getElementById('detailProgressBar');
  const progressText = document.getElementById('detailProgressText');
  if (progressBar) progressBar.style.width = progress + '%';
  if (progressText) progressText.innerText = progress + '%';
  
  document.getElementById('detailAmount').innerText = formatMoney(data.amount);
  document.getElementById('detailBalance').innerText = formatMoney(data.balance);
  
  // Reset and show loading
  const scheduleBody = document.getElementById('scheduleBody');
  const historyBody = document.getElementById('historyBody');
  if (scheduleBody) scheduleBody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><i class="fas fa-spinner fa-spin me-2"></i>Inapakia...</td></tr>';
  if (historyBody) historyBody.innerHTML = '<tr><td colspan="4" class="text-center py-4"><i class="fas fa-spinner fa-spin me-2"></i>Inapakia...</td></tr>';

  loadPage('loan-details');

  if (data.ref) {
    fetch(`/api/loans/details?ref=${encodeURIComponent(data.ref)}`)
        .then(response => response.ok ? response.json() : Promise.reject('Loan not found'))
        .then(details => {
            if (document.getElementById('detailInterest')) document.getElementById('detailInterest').innerText = formatMoney(details.interest);
            if (document.getElementById('detailTotalDue')) document.getElementById('detailTotalDue').innerText = formatMoney(details.totalDue);
            if (document.getElementById('detailPaid')) document.getElementById('detailPaid').innerText = formatMoney(details.paidAmount);
            
            if (scheduleBody) {
                scheduleBody.innerHTML = '';
                (details.schedule || []).forEach((item, index) => {
                    scheduleBody.innerHTML += `<tr>
                        <td>${index + 1}</td>
                        <td>${item.date}</td>
                        <td>${formatMoney(item.principal)}</td>
                        <td>${formatMoney(item.interest)}</td>
                        <td>${formatMoney(item.total)}</td>
                        <td>${formatMoney(item.balance)}</td>
                        <td><span class="badge bg-${item.status === 'PAID' ? 'success' : (item.status === 'OVERDUE' ? 'danger' : 'warning')}">${item.status}</span></td>
                    </tr>`;
                });
            }

            if (historyBody) {
                historyBody.innerHTML = '';
                if (details.history && details.history.length > 0) {
                    details.history.forEach(item => {
                        historyBody.innerHTML += `<tr>
                            <td>${item.date}</td>
                            <td>${item.reference}</td>
                            <td>${item.method}</td>
                            <td class="text-success fw-bold">${formatMoney(item.amount)}</td>
                        </tr>`;
                    });
                } else {
                    historyBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">Hakuna malipo yaliyofanyika.</td></tr>';
                }
            }
        })
        .catch(error => {
            console.error('Error fetching loan details:', error);
            if (scheduleBody) scheduleBody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Imeshindwa kupakia taarifa.</td></tr>';
            if (historyBody) historyBody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">Imeshindwa kupakia taarifa.</td></tr>';
        });
  }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
  const savedPage = localStorage.getItem('akibaActivePage');
  if (savedPage) {
    loadPage(savedPage);
  } else {
    initializeDashboardCharts();
  }
  
  // Set default payment method
  selectPaymentMethod('mobile');
  
  // Initialize step indicator for loan application
  if (document.getElementById('stepIndicator')) {
    updateStepIndicator(1);
  }
  
  // Initialize analytics if that's the active page
  if (document.getElementById('analytics') && document.getElementById('analytics').classList.contains('active')) {
    initializeAnalyticsCharts();
  }
  // Start polling for newly added meetings so users see admin-created meetings without manual refresh
  startUpcomingMeetingsPoll();
});

// Export functions to window to ensure access from HTML
window.loadPage = loadPage;
window.showToast = showToast;
window.submitDeposit = submitDeposit;
window.submitWithdraw = submitWithdraw;
window.submitBuyGroupShares = submitBuyGroupShares;
window.submitSellShares = submitSellShares;
window.buyMarketShare = buyMarketShare;
window.calcGroupTotal = calcGroupTotal;
window.saveProfileChanges = saveProfileChanges;
window.previewProfilePic = previewProfilePic;
window.downloadMembershipCard = downloadMembershipCard;
window.changePassword = changePassword;
window.initiateLoanPayment = initiateLoanPayment;
window.updatePaymentSummary = updatePaymentSummary;
window.submitLoanPayment = submitLoanPayment;
window.showLoanDetails = showLoanDetails;
window.selectPaymentMethod = selectPaymentMethod;
window.setMobileNumber = setMobileNumber;
window.nextStep = nextStep;
window.toggleCollateral = toggleCollateral;
window.toggleGuarantor = toggleGuarantor;
window.selectEmployment = selectEmployment;
window.submitLoanApplication = submitLoanApplication;
window.preparePayment = preparePayment;
window.confirmPenaltyPayment = confirmPenaltyPayment;
window.signAttendance = signAttendance;
window.filterStatements = filterStatements;

// If any loadPage calls were queued before the real implementation loaded, process them now
try {
  if (window._loadPageQueue && Array.isArray(window._loadPageQueue) && window._loadPageQueue.length) {
    const queued = window._loadPageQueue.slice();
    window._loadPageQueue = [];
    queued.forEach(pid => {
      try { window.loadPage(pid); } catch (e) { console.warn('queued loadPage failed for', pid, e); }
    });
  }
} catch (e) {
  console.warn('Error processing queued loadPage calls', e);
}

// Poll upcoming meetings endpoint and reload page if new meetings are detected
function startUpcomingMeetingsPoll() {
  try {
    const poll = async () => {
      try {
        const resp = await fetch('/api/meetings/upcoming');
        if (!resp.ok) return;
        const data = await resp.json();
        // Count meeting cards in DOM
        const domCount = document.querySelectorAll('#upcoming-meetings [data-meeting-id]').length;
        const serverCount = Array.isArray(data) ? data.length : 0;
        if (serverCount !== domCount) {
          // simple strategy: reload to refresh rendered meetings
          window.location.reload();
        }
      } catch (e) {
        // ignore transient errors
      }
    };
    // poll every 30 seconds
    setInterval(poll, 30000);
  } catch (e) {
    console.warn('Failed to start meetings poll', e);
  }
}