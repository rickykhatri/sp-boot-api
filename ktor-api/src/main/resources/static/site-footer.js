class SiteFooter extends HTMLElement {
  connectedCallback() {
    const brand = this.getAttribute('brand') || 'RK Smart Chat - by Ricky Khatri';
    const year = this.getAttribute('year') || '2026';

    this.style.display = 'block';
    this.style.marginTop = '18px';

    this.innerHTML = `
      <style>
        :host {
          display: block;
        }

        .site-footer {
          padding-top: 14px;
          border-top: 1px solid rgba(255, 255, 255, 0.08);
          color: #94a3b8;
          font-size: 0.85rem;
          text-align: center;
          letter-spacing: 0.02em;
        }

        :host([variant="light"]) .site-footer {
          border-top-color: #e5e7eb;
          color: #6b7280;
          font-size: 0.82rem;
        }
      </style>
      <div class="site-footer">&copy; ${year} ${brand}. All rights reserved.</div>
    `;
  }
}

customElements.define('site-footer', SiteFooter);