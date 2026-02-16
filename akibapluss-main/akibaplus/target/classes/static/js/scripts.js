const Utils = {
  showToast: function(message, type='info'){
    const containerId = 'toast-container';
    let container = document.getElementById(containerId);
    if(!container){
      container = document.createElement('div');
      container.id = containerId;
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(()=> toast.remove(), 4000);
  }
};

const Auth = {
  login: function(email, password){
    Utils.showToast('Simulated login: '+email, 'success');
    // In real app, post to /api/auth/login then redirect.
    setTimeout(()=> window.location.href='/', 800);
  },
  register: function(payload){
    // call backend register endpoint
    fetch('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(r => r.json()).then(res => {
      if (res.error) {
        Utils.showToast(res.error, 'danger');
      } else {
        Utils.showToast('Registration successful, redirecting to login...', 'success');
        setTimeout(()=> window.location.href='/login', 1000);
      }
    }).catch(err => {
      Utils.showToast('Registration failed', 'danger');
    });
  }
};
