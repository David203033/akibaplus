// This file is intended to be loaded in the <head>
// All functions are defined on the window object or wrapped in a DOMContentLoaded listener

// --- Navigation & UI ---
function loadPage(pageId) {
    document.querySelectorAll('.page').forEach(page => {
        page.style.display = 'none';
        page.classList.remove('active');
    });
    const targetPage = document.getElementById(pageId);
    if (targetPage) {
        targetPage.style.display = 'block';
        targetPage.classList.add('active');
    }
    // Update active link in sidebar
    document.querySelectorAll('.sidebar .nav-link, .mobile-nav .nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('onclick') === `loadPage('${pageId}')`) {
            link.classList.add('active');
        }
    });
    
    const sidebar = document.getElementById('sidebarOffcanvas');
    if (sidebar && typeof bootstrap !== 'undefined') {
      const bsOffcanvas = bootstrap.Offcanvas.getInstance(sidebar);
      if (bsOffcanvas) bsOffcanvas.hide();
    }

    if ((pageId === 'analytics' || pageId === 'dashboard') && typeof window.initCharts === 'function') {
        setTimeout(window.initCharts, 100);
    }
}
window.loadPage = loadPage;

// --- General UI Helpers ---
function showToast(message, type = 'success') {
    // A simple alert fallback if no toast element is present
    alert(message);
}

// --- Loan Application Logic ---
window.nextStep = function(step) {
  document.querySelectorAll('.step-content').forEach(el => el.style.display = 'none');
  const target = document.getElementById('step' + step);
  if(target) target.style.display = 'block';
  
  document.querySelectorAll('.step-item').forEach((el, index) => {
    const iconDiv = el.querySelector('div');
    if (index < step -1) { // Mark previous as done
        iconDiv.classList.remove('bg-primary', 'text-white');
        iconDiv.classList.add('bg-success', 'text-white');
        iconDiv.innerHTML = '<i class="fas fa-check"></i>';
    } else if (index === step - 1) { // Mark current as active
        el.classList.add('active');
        iconDiv.classList.remove('bg-light', 'text-muted', 'bg-success');
        iconDiv.classList.add('bg-primary', 'text-white');
        iconDiv.innerText = step;
    } else { // Mark future as inactive
        el.classList.remove('active');
        iconDiv.classList.remove('bg-primary', 'text-white', 'bg-success');
        iconDiv.classList.add('bg-light', 'text-muted');
        iconDiv.innerText = index + 1;
    }
  });
};

window.toggleCollateral = function() {
  const section = document.getElementById('collateralSection');
  const checkbox = document.getElementById('hasCollateral');
  section.style.display = checkbox.checked ? 'block' : 'none';
};

window.toggleGuarantor = function() {
  const section = document.getElementById('guarantorSection');
  const checkbox = document.getElementById('hasGuarantor');
  section.style.display = checkbox.checked ? 'block' : 'none';
};

window.selectEmployment = function(type) {
  document.querySelectorAll('.employment-option').forEach(el => el.classList.remove('active-employment', 'border-primary'));
  if(event.currentTarget) event.currentTarget.classList.add('active-employment', 'border-primary');
  
  document.getElementById('employedDocs').style.display = type === 'employed' ? 'block' : 'none';
  document.getElementById('selfEmployedDocs').style.display = type === 'self-employed' ? 'block' : 'none';
};

window.submitLoanApplication = function() {
  const btn = document.getElementById('submitBtn');
  btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inawasilisha...';
  btn.disabled = true;
  
  const loanData = {
    type: document.getElementById('loanType').value,
    amount: document.getElementById('loanAmount').value,
    duration: parseInt(document.getElementById('loanDuration').value),
    reason: document.getElementById('loanReason').value,
  };

  fetch('/api/loans/apply', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(loanData),
  })
  .then(response => response.json())
  .then(data => {
    if (data.success) {
      alert(data.message);
      window.location.reload();
    } else {
      alert('Hitilafu: ' + data.message);
    }
  })
  .catch((error) => {
    console.error('Error:', error);
    alert('Imeshindwa kuwasilisha ombi. Tafadhali jaribu tena.');
  })
  .finally(() => {
    btn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Wasilisha Ombi Sasa';
    btn.disabled = false;
  });
};

document.addEventListener('change', function(e) {
  if(e.target.id === 'agreeTerms') {
    document.getElementById('submitBtn').disabled = !e.target.checked;
  }
});

// --- Loan Payment Logic ---
window.selectPaymentMethod = function(method) {
  document.querySelectorAll('.payment-method').forEach(el => el.classList.remove('active-payment', 'border-primary', 'border-3'));
  if(method === 'bank') {
    document.getElementById('bankMethod').classList.add('active-payment', 'border-primary', 'border-3');
    document.getElementById('bankDetails').style.display = 'block';
    document.getElementById('mobileDetails').style.display = 'none';
  } else {
    document.getElementById('mobileMethod').classList.add('active-payment', 'border-primary', 'border-3');
    document.getElementById('bankDetails').style.display = 'none';
    document.getElementById('mobileDetails').style.display = 'block';
  }
};

window.setMobileNumber = function(number, provider) {
  document.getElementById('mobileNumber').value = number;
};

window.submitLoanPayment = function(btn) {
  const amount = document.getElementById('paymentAmount').value;
  const loanId = document.getElementById('selectedLoan').value;
  
  let method = document.getElementById('bankDetails').style.display !== 'none' ? 'BANK' : 'MOBILE';

  if(!loanId || !amount || amount < 1000) {
    alert('Tafadhali chagua mkopo na weka kiasi halali.');
    return;
  }
  
  const originalText = btn.innerHTML;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inachakata...';
  btn.disabled = true;

  fetch('/api/loans/pay', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: amount, loanId: loanId, method: method })
  })
  .then(res => res.json())
  .then(data => {
    alert(data.message);
    if(data.success) window.location.reload();
  })
  .catch(err => alert('Hitilafu imetokea wakati wa malipo.'))
  .finally(() => {
      btn.innerHTML = originalText;
      btn.disabled = false;
  });
};

window.updatePaymentSummary = function() {
  const select = document.getElementById('selectedLoan');
  const selectedOption = select.options[select.selectedIndex];
  if (!selectedOption) return;
  
  const cleanNumber = (num) => parseFloat(String(num).replace(/[^0-9.-]+/g,"")) || 0;
  const currentBalance = cleanNumber(selectedOption.getAttribute('data-balance'));
  const amount = parseFloat(document.getElementById('paymentAmount').value) || 0;
  
  document.getElementById('currentBalance').innerText = 'TZS ' + currentBalance.toLocaleString();
  document.getElementById('summaryAmount').innerText = 'TZS ' + amount.toLocaleString();
  
  const newBalance = Math.max(0, currentBalance - amount);
  document.getElementById('newBalance').innerText = 'TZS ' + newBalance.toLocaleString();
};

// --- Other Actions ---
window.initiateLoanPayment = function(btn) {
  const id = btn.getAttribute('data-id');
  const nextPayment = btn.getAttribute('data-nextpayment');
  
  loadPage('loan-payment');
  
  const select = document.getElementById('selectedLoan');
  select.value = id;
  
  const cleanNumber = (num) => parseFloat(String(num).replace(/[^0-9.-]+/g,"")) || 0;
  document.getElementById('paymentAmount').value = cleanNumber(nextPayment) || 10000;
  updatePaymentSummary();
};

window.showLoanDetails = function(btn) {
  const data = btn.dataset;
  const formatMoney = (amount) => 'TZS ' + (parseFloat(String(amount).replace(/[^0-9.-]+/g,"")) || 0).toLocaleString();

  document.getElementById('detailType').innerText = (data.type || 'Mkopo') + (data.ref ? ' - ' + data.ref : '');
  document.getElementById('detailRef').innerText = data.ref || 'N/A';
  document.getElementById('detailDate').innerText = data.date || '-';
  document.getElementById('detailDueDate').innerText = data.duedate || '-';
  document.getElementById('detailStatus').innerText = data.status || 'PENDING';
  
  const progress = Math.round(parseFloat(data.progress) || 0);
  document.getElementById('detailProgressBar').style.width = progress + '%';
  document.getElementById('detailProgressText').innerText = progress + '%';
  
  document.getElementById('detailAmount').innerText = formatMoney(data.amount);
  document.getElementById('detailBalance').innerText = formatMoney(data.balance);
  
  // Reset and show loading
  document.getElementById('detailInterest').innerText = '...';
  document.getElementById('detailTotalDue').innerText = '...';
  document.getElementById('detailPaid').innerText = '...';
  document.getElementById('scheduleBody').innerHTML = '<tr><td colspan="7" class="text-center py-4"><i class="fas fa-spinner fa-spin me-2"></i>Inapakia...</td></tr>';
  document.getElementById('historyBody').innerHTML = '<tr><td colspan="4" class="text-center py-4"><i class="fas fa-spinner fa-spin me-2"></i>Inapakia...</td></tr>';

  loadPage('loan-details');

  if (data.ref) {
    fetch(`/api/loans/details?ref=${encodeURIComponent(data.ref)}`)
        .then(response => response.ok ? response.json() : Promise.reject('Loan not found'))
        .then(details => {
            document.getElementById('detailInterest').innerText = formatMoney(details.interest);
            document.getElementById('detailTotalDue').innerText = formatMoney(details.totalDue);
            document.getElementById('detailPaid').innerText = formatMoney(details.paidAmount);
            
            const scheduleBody = document.getElementById('scheduleBody');
            scheduleBody.innerHTML = '';
            (details.schedule || []).forEach((item, index) => {
                scheduleBody.innerHTML += `<tr><td>${index + 1}</td><td>${item.date}</td><td>${formatMoney(item.principal)}</td><td>${formatMoney(item.interest)}</td><td>${formatMoney(item.total)}</td><td>${formatMoney(item.balance)}</td><td><span class="badge bg-${item.status === 'PAID' ? 'success' : 'warning'}">${item.status}</span></td></tr>`;
            });

            const historyBody = document.getElementById('historyBody');
            historyBody.innerHTML = '';
            (details.history || []).forEach(item => {
                historyBody.innerHTML += `<tr><td>${item.date}</td><td>${item.reference}</td><td>${item.method}</td><td class="text-success fw-bold">${formatMoney(item.amount)}</td></tr>`;
            });
        })
        .catch(error => {
            console.error('Error fetching loan details:', error);
            document.getElementById('scheduleBody').innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Imeshindwa kupakia taarifa.</td></tr>';
            document.getElementById('historyBody').innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">Imeshindwa kupakia taarifa.</td></tr>';
        });
  }
};

window.submitDeposit = function() {
  const amount = parseFloat(document.getElementById('depositAmount').value);
  const phone = (document.getElementById('depositPhone') || {}).value || '';
  if(!amount || amount < 1000) { alert('Weka kiasi halali.'); return; }
  if(!phone) { if(!confirm('Huwezi kujumuisha namba ya simu; endelea bila namba?')) return; }

  // Inform user to confirm payment on their phone
  alert('Tafadhali thibitisha malipo kwenye simu yako.');

  fetch('/api/savings/deposit', { 
    method: 'POST', 
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' }, 
    body: JSON.stringify({ amount: amount, phone: phone }) 
  })
  .then(res => {
    if (!res.ok) return res.json().then(j => Promise.reject(j));
    return res.json();
  })
  .then(data => { alert(data.message); if(data.success) window.location.href = '/dashboard'; })
  .catch(err => { console.error('Deposit error', err); alert((err && err.message) ? err.message : 'Imeshindwa kuhifadhi amana. Jaribu tena.'); });
};

window.submitWithdraw = function() {
  const amount = document.getElementById('withdrawAmount').value;
  if(!amount || amount < 1000) { alert('Weka kiasi halali.'); return; }
  fetch('/api/savings/withdraw', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ amount }) })
  .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
};

window.calcGroupTotal = function() {
  document.getElementById('groupShareTotal').value = 'TZS ' + (document.getElementById('groupShareQty').value * 10000).toLocaleString();
};

window.submitBuyGroupShares = function() {
  const qty = document.getElementById('groupShareQty').value;
  fetch('/api/shares/buy', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ type: 'GROUP', quantity: qty }) })
  .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
};

window.submitSellShares = function() {
  const qty = document.getElementById('sellShareQty').value;
  const price = document.getElementById('sellSharePrice').value;
  fetch('/api/shares/sell', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ quantity: qty, price: price }) })
  .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
};

window.buyMarketShare = function(btn) {
  if(confirm('Unataka kununua hisa hizi?')) {
    fetch('/api/shares/buy-market', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ saleId: btn.dataset.id }) })
    .then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
  }
};

window.filterStatements = function(period) {
  document.querySelectorAll('.period-card').forEach(el => el.classList.remove('active'));
  if(event.currentTarget) event.currentTarget.classList.add('active');
  document.querySelectorAll('#statementsBody tr').forEach(row => {
    row.style.display = row.dataset.period === period ? 'table-row' : 'none';
  });
};

window.downloadStatement = (type, name) => alert(`Inapakua taarifa: ${name} (${type})...`);
window.signAttendance = (meeting) => alert(`Mahudhurio yamethibitishwa kwa ${meeting}!`);
window.preparePayment = (amount, desc) => {
  document.getElementById('penaltyAmount').innerText = 'TZS ' + parseFloat(amount).toLocaleString();
  document.getElementById('penaltyDescription').innerText = desc;
};
window.confirmPenaltyPayment = () => {
  fetch('/api/penalties/pay', { method: 'POST' }).then(res => res.json()).then(data => { alert(data.message); if(data.success) window.location.reload(); });
};
window.rejectGuarantor = () => alert('Ombi la udhamini limekataliwa.');
window.acceptGuarantor = () => alert('Asante! Umekubali kuwa mdhamini.');
window.saveProfileChanges = async (section) => {
  // Determine the button that triggered this call
  let btn = (document.activeElement && document.activeElement.tagName === 'BUTTON') ? document.activeElement : null;
  const orig = btn ? btn.innerHTML : null;
  if (btn) {
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Inasasisha...';
    btn.disabled = true;
  }

  try {
    const fd = new FormData();

    // Map of expected backend field names to element IDs in the template
    const fieldMap = {
      firstName: 'profileFirstName',
      middleName: 'profileMiddleName',
      lastName: 'profileLastName',
      dob: 'profileDob',
      gender: 'profileGender',
      maritalStatus: 'profileMarital',
      phone: 'profilePhone',
      street: 'profileStreet',
      district: 'profileDistrict',
      region: 'profileRegion',
      addressDescription: 'profileAddressDesc',
      nextOfKinName: 'nokName',
      nextOfKinPhone: 'nokPhone',
      nextOfKinRelation: 'nokRelation',
      nextOfKinPercent: 'nokPercent'
    };

    // If section provided, only collect fields relevant to that section for efficiency
    Object.entries(fieldMap).forEach(([key, id]) => {
      // restrict by section if desired
      if (section === 'personal' && !['firstName','middleName','lastName','dob','gender','maritalStatus','phone'].includes(key)) return;
      if (section === 'address' && !['street','district','region','addressDescription'].includes(key)) return;
      if (section === 'nextOfKin' && !['nextOfKinName','nextOfKinPhone','nextOfKinRelation','nextOfKinPercent'].includes(key)) return;

      const el = document.getElementById(id);
      if (el && el.value !== undefined) fd.append(key, el.value);
    });

    // Profile image input (id profilePicInput)
    const fileEl = document.getElementById('profilePicInput');
    if (fileEl && fileEl.files && fileEl.files[0]) fd.append('profileImage', fileEl.files[0]);

    // Send to backend
    const res = await fetch('/api/profile/update', { method: 'POST', body: fd });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Save failed');

    if (data.success) {
      // Update visible UI values
      const profileNameEl = document.getElementById('profileName');
      const profileFirst = document.getElementById('profileFirstName')?.value || '';
      const profileLast = document.getElementById('profileLastName')?.value || '';
      if (profileNameEl) profileNameEl.innerText = (profileFirst + ' ' + profileLast).trim();

      if (data.imageUrl) {
        const img = document.getElementById('profileImage');
        if (img) img.src = data.imageUrl;
      }

      alert(data.message || 'Taarifa zimehifadhiwa.');
    } else {
      alert(data.message || 'Imeshindwa kuhifadhi.');
    }
  } catch (e) {
    console.error(e);
    alert('Kosa: ' + (e.message || e));
  } finally {
    if (btn) { btn.innerHTML = orig; btn.disabled = false; }
  }
};
window.changePassword = () => alert('Nenosiri limebadilishwa.');
window.previewProfilePic = (event) => {
  if (event.target.files && event.target.files[0]) {
    document.getElementById('profileImage').src = URL.createObjectURL(event.target.files[0]);
    // In a real app, you would upload the file here.
    alert('Picha imepakiwa (simulation).');
  }
};
window.downloadMembershipCard = () => {
  const card = document.querySelector('.membership-card');
  if (card) html2canvas(card).then(canvas => {
    const link = document.createElement('a');
    link.download = 'AkibaPlus_MembershipCard.png';
    link.href = canvas.toDataURL();
    link.click();
  });
};

window.initCharts = function() {
  // Fetch analytics data from server and render charts
  fetch('/dashboard/analytics-data')
    .then(res => res.ok ? res.json() : Promise.reject('No analytics'))
    .then(data => {
      try {
        // 1) Savings chart (dashboard)
        const savingsCtx = document.getElementById('savingsChart');
        if (savingsCtx && data.savingsHistory) {
          const labels = data.savingsHistory.map(p => p.label);
          const values = data.savingsHistory.map(p => parseFloat(p.value || 0));
          new Chart(savingsCtx.getContext('2d'), {
            type: 'line',
            data: { labels, datasets: [{ label: 'Akiba', data: values, borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,0.08)', fill: true }] },
            options: { responsive: true, plugins: { legend: { display: false } } }
          });
        }

        // 2) Asset distribution (dashboard)
        const distCtx = document.getElementById('distributionChart');
        if (distCtx && data.assetAllocation) {
          const keys = Object.keys(data.assetAllocation);
          const vals = keys.map(k => parseFloat(data.assetAllocation[k] || 0));
          new Chart(distCtx.getContext('2d'), {
            type: 'doughnut',
            data: { labels: keys, datasets: [{ data: vals, backgroundColor: ['#10b981','#f59e0b','#ef4444'] }] },
            options: { responsive: true }
          });
        }

        // 3) Analytics page charts - simple summaries (if present)
        const sg = document.getElementById('savingsSharesGrowthChart');
        if (sg && data.savingsHistory) {
          const labels = data.savingsHistory.map(p => p.label);
          const values = data.savingsHistory.map(p => parseFloat(p.value || 0));
          new Chart(sg.getContext('2d'), { type: 'bar', data: { labels, datasets: [{ label: 'Akiba', data: values, backgroundColor: '#2563eb' }] } });
        }

        const ia = document.getElementById('assetAllocationChart');
        if (ia && data.assetAllocation) {
          const keys = Object.keys(data.assetAllocation);
          const vals = keys.map(k => parseFloat(data.assetAllocation[k] || 0));
          new Chart(ia.getContext('2d'), { type: 'pie', data: { labels: keys, datasets: [{ data: vals, backgroundColor: ['#2563eb','#10b981','#ef4444'] }] } });
        }
      } catch(e) { console.error('Chart error', e); }
    })
    .catch(err => { /* silently continue if no analytics */ console.warn('Charts init failed', err); });
};

// --- DOM Ready Initializations ---
document.addEventListener('DOMContentLoaded', function() {
    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        dateEl.innerText = new Date().toLocaleDateString('sw-TZ', { day: 'numeric', month: 'long', year: 'numeric' });
    }
    if (!document.querySelector('.page.active')) {
        loadPage('dashboard');
    }
});