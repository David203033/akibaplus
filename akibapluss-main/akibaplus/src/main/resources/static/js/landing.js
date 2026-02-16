// Smooth scrolling for navigation links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Counter animation
function animateCounters() {
    const counters = document.querySelectorAll('.counter');
    const speed = 200;

    counters.forEach(counter => {
        const updateCount = () => {
            const target = +counter.getAttribute('data-target');
            const count = +counter.innerText;
            const increment = target / speed;

            if (count < target) {
                counter.innerText = Math.ceil(count + increment);
                setTimeout(updateCount, 10);
            } else {
                counter.innerText = target;
            }
        };

        updateCount();
    });
}

// Trigger counter animation when section is in view
const observerOptions = {
    threshold: 0.5
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting && entry.target.classList.contains('stats-section')) {
            animateCounters();
            observer.unobserve(entry.target);
        }
    });
}, observerOptions);

document.addEventListener('DOMContentLoaded', () => {
    const statsSection = document.querySelector('.stats-section');
    if (statsSection) {
        observer.observe(statsSection);
    }
});

// Animate elements on scroll
const fadeElements = document.querySelectorAll('.fade-in-up, .feature-card, .benefit-item, .service-box, .contact-box');

const scrollObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.animation = 'fadeInUp 0.6s ease-out forwards';
            scrollObserver.unobserve(entry.target);
        }
    });
}, { threshold: 0.1 });

fadeElements.forEach(element => {
    scrollObserver.observe(element);
});

// Navbar active link highlighting
window.addEventListener('scroll', () => {
    let current = '';
    const sections = document.querySelectorAll('section');

    sections.forEach(section => {
        const sectionTop = section.offsetTop;
        const sectionHeight = section.clientHeight;
        if (pageYOffset >= (sectionTop - 200)) {
            current = section.getAttribute('id');
        }
    });

    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href').slice(1) === current) {
            link.classList.add('active');
        }
    });
});

// Navbar background change on scroll
window.addEventListener('scroll', () => {
    const navbar = document.querySelector('.navbar');
    if (window.pageYOffset > 50) {
        navbar.style.backgroundColor = 'rgba(26, 26, 46, 0.95)';
        navbar.style.backdropFilter = 'blur(10px)';
    } else {
        navbar.style.backgroundColor = '';
        navbar.style.backdropFilter = '';
    }
});

// Modal form submission
document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('joinModal');
    if (modal) {
        const form = modal.querySelector('form#registerForm');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                if (!validateForm(form)) return;
                const data = {
                    fullName: form.querySelector('input[type="text"]').value,
                    email: form.querySelector('input[type="email"]').value,
                    phone: form.querySelector('input[type="tel"]').value,
                    password: Math.random().toString(36).slice(-8),
                    nida: "",
                };
                try {
                    const res = await fetch('/auth/register', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ fullName: data.fullName, email: data.email, phone: data.phone, password: data.password, nida: data.nida })
                    });
                    const json = await res.json();
                    if (res.ok) {
                        // close register modal and open login modal
                        const bsReg = bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal);
                        bsReg.hide();
                        const loginModal = document.getElementById('loginModal');
                        const bsLogin = new bootstrap.Modal(loginModal);
                        bsLogin.show();
                    } else {
                        alert(json.error || json.message || 'Registration failed');
                    }
                } catch (err) {
                    console.error(err);
                    alert('Registration failed');
                }
            });
        }
    }
});

// Handle keyboard escape closing handled earlier

// Optionally pre-fill login modal email after registration
document.addEventListener('registration:success', (e) => {
    const email = e.detail?.email;
    if (email) {
        const el = document.getElementById('modalEmail');
        if (el) el.value = email;
    }
});

// Add parallax effect to hero section
window.addEventListener('scroll', () => {
    const heroSection = document.querySelector('.hero-section');
    if (heroSection) {
        const scrollPosition = window.pageYOffset;
        heroSection.style.backgroundPosition = `center ${scrollPosition * 0.5}px`;
    }
});

// Animate on page load
window.addEventListener('load', () => {
    document.body.style.opacity = '1';
});

// Ripple effect on buttons
document.querySelectorAll('.btn').forEach(button => {
    button.addEventListener('click', function(e) {
        const ripple = document.createElement('span');
        const rect = this.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;

        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        ripple.classList.add('ripple');

        // Remove existing ripple
        const existingRipple = this.querySelector('.ripple');
        if (existingRipple) {
            existingRipple.remove();
        }

        this.appendChild(ripple);
    });
});

// Add ripple style
const style = document.createElement('style');
style.textContent = `
    .btn {
        position: relative;
        overflow: hidden;
    }
    .ripple {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.6);
        transform: scale(0);
        animation: ripple-animation 0.6s ease-out;
        pointer-events: none;
    }
    @keyframes ripple-animation {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Keyboard navigation
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const modals = document.querySelectorAll('.modal.show');
        modals.forEach(modal => {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) bsModal.hide();
        });
    }
});

// Form validation
function validateForm(form) {
    let isValid = true;
    form.querySelectorAll('input[required]').forEach(input => {
        if (!input.value.trim()) {
            input.classList.add('is-invalid');
            isValid = false;
        } else {
            input.classList.remove('is-invalid');
        }
    });
    return isValid;
}

// Add to document for global use
window.validateForm = validateForm;

// Log message on page load
console.log('%cKaribu kwenye Akiba Plus SACCOS! 🎉', 'font-size: 20px; color: #0d6efd; font-weight: bold;');
console.log('%cKujenga Akiba, Kujenga Kinema', 'font-size: 16px; color: #764ba2;');
