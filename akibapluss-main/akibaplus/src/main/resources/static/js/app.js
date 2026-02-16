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
    }
  }
}

// ====================
// Chart Initialization
// ====================
function initializeDashboardCharts() {
  initSavingsChart();
  initDistributionChart();
}

function initSavingsChart() {
  const ctx = document.getElementById('savingsChart');
  if (!ctx || ctx.savingsChartInstance) return;

  ctx.savingsChartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
      datasets: [{
        label: 'Salio la Akiba (TZS)',
        data: [5000000, 5500000, 6000000, 6500000, 7000000, 7200000, 7500000, 7800000, 8000000, 8200000, 8300000, 8500000],
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

function initDistributionChart() {
  const ctx = document.getElementById('distributionChart');
  if (!ctx || ctx.distributionChartInstance) return;

  ctx.distributionChartInstance = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Akiba', 'Hisa', 'Mikopo'],
      datasets: [{
        data: [8500000, 4200000, 1200000],
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
      window.location.href = window.location.href; // Force reload to update balances
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

function simulatePayment() {
  alert('Malipo yamekubaliwa! Utapokea kupilikia SMS ndani ya dakika 2.');
  // Hide payment form and show success message
  document.querySelector('.card-body').innerHTML = '<div class="alert alert-success text-center"><i class="fas fa-check-circle fa-4x mb-3"></i><h4 class="fw-bold mt-3">Malipo Yamefanyika Kwa Mafanikio!</h4><p>Tunakushukuru kwa kulipa mkopo wako haraka.</p></div>';
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

function submitLoanApplication() {
  alert('Ombi lako limewasilishwa! Utapokea jibu ndani ya siku 3-7 za kazi.');
  console.log('Loan application submitted');
  // Here you would send the form data to the backend
  // fetch('/api/loans/apply', { method: 'POST', body: formData });
}

// ====================
// Transaction Modals
// ====================

function preparePayment(amount, description) {
  document.getElementById('penaltyAmount').textContent = 'TZS ' + amount.toLocaleString();
  document.getElementById('penaltyDescription').textContent = description;
}

function confirmPenaltyPayment() {
  alert('Malipo yamekubaliwa! Asante.');
  bootstrap.Modal.getInstance(document.getElementById('payPenaltyModal')).hide();
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
        
        if (distance < 1) { // Within 1 km
          // Call backend to record attendance
          fetch('/api/meetings/attend', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ meetingId: meetingId })
          })
          .then(response => {
            if(response.ok) {
              alert('Mahudhurio yamerekodiwa kwa mafanikio! Asante kwa kuhudhuria.');
              console.log('Attendance signed for: ' + meetingName);
            } else {
              alert('Imeshindwa kurekodi mahudhurio. Tafadhali jaribu tena.');
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
  alert(`Taarifa ya ${period} (${format}) inapakuliwa...`);
  console.log('Downloading: ' + period + ' in ' + format);
  // Here you would trigger actual download
  // window.location.href = `/api/statements/download?format=${format}&period=${period}`;
}

// ====================
// Analytics Charts
// ====================

function initializeAnalyticsCharts() {
  initSavingsSharesGrowthChart();
  initIncomeExpenseChart();
  initLoanRepaymentChart();
  initMonthlyContributionsChart();
  initInterestDividendsChart();
  initLoansPaidChart();
  initAssetAllocationChart();
}

function initSavingsSharesGrowthChart() {
  const ctx = document.getElementById('savingsSharesGrowthChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['2019', '2020', '2021', '2022', '2023', '2024', '2025'],
      datasets: [
        {
          label: 'Akiba',
          data: [1000000, 2000000, 3500000, 5000000, 6500000, 7500000, 8500000],
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        },
        {
          label: 'Hisa',
          data: [500000, 1000000, 1800000, 2500000, 3200000, 3800000, 4200000],
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

function initIncomeExpenseChart() {
  const ctx = document.getElementById('incomeExpenseChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
      datasets: [
        {
          label: 'Mapato',
          data: [600000, 650000, 700000, 680000, 750000, 800000, 820000, 850000, 900000, 950000, 1000000, 1100000],
          backgroundColor: '#10b981'
        },
        {
          label: 'Matumizi',
          data: [400000, 420000, 450000, 430000, 480000, 500000, 520000, 550000, 600000, 620000, 650000, 700000],
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

function initLoanRepaymentChart() {
  const ctx = document.getElementById('loanRepaymentChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Ililipwa', 'Salio Lililobaki'],
      datasets: [{
        data: [100000, 700000],
        backgroundColor: ['#10b981', '#f59e0b']
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initMonthlyContributionsChart() {
  const ctx = document.getElementById('monthlyContributionsChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
      datasets: [{
        label: 'Mchango wa Kila Mwezi',
        data: [500000, 500000, 500000, 500000, 500000, 500000, 500000, 500000, 500000, 500000, 500000, 500000],
        backgroundColor: '#2563eb'
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initInterestDividendsChart() {
  const ctx = document.getElementById('interestDividendsChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['2020', '2021', '2022', '2023', '2024', '2025'],
      datasets: [
        {
          label: 'Riba',
          data: [50000, 85000, 120000, 180000, 320000, 425000],
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4
        },
        {
          label: 'Gawio',
          data: [30000, 60000, 100000, 150000, 200000, 210000],
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

function initLoansPaidChart() {
  const ctx = document.getElementById('loansPaidChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Ililipwa', 'Salio'],
      datasets: [{
        label: 'Kiasi (TZS)',
        data: [1500000, 1200000],
        backgroundColor: ['#10b981', '#ef4444']
      }]
    },
    options: {
      responsive: true
    }
  });
}

function initAssetAllocationChart() {
  const ctx = document.getElementById('assetAllocationChart');
  if (!ctx) return;

  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Akiba', 'Hisa', 'Mikopo'],
      datasets: [{
        data: [8500000, 4200000, 1200000],
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

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
  initializeDashboardCharts();
  
  // Set default payment method
  selectPaymentMethod('mobile');
  
  // Initialize step indicator for loan application
  if (document.getElementById('stepIndicator')) {
    updateStepIndicator(1);
  }
});