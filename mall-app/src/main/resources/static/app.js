const state = {
  products: [], cart: { items: [], totalAmount: 0 }, categories: [], category: '全部',
  selectedProduct: null, user: null, token: localStorage.getItem('authToken') || ''
};
const money = value => `¥${Number(value || 0).toFixed(2)}`;
const $ = selector => document.querySelector(selector);
const headers = { 'Content-Type': 'application/json' };

async function request(url, options = {}) {
  const authHeaders = state.token ? { Authorization: `Bearer ${state.token}` } : {};
  const response = await fetch(url, {
    ...options,
    headers: { ...headers, ...authHeaders, ...(options.headers || {}) }
  });
  const result = await response.json().catch(() => ({}));
  if (response.status === 401) {
    state.user = null;
    state.token = '';
    localStorage.removeItem('authToken');
    renderAuthState();
    openAuth('login');
    throw new Error('请先登录');
  }
  if (!response.ok || (result.code && result.code !== 0)) {
    throw new Error(result.message || '请求失败，请稍后再试');
  }
  return result.data;
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 2600);
}

function openDrawer(id) {
  document.querySelectorAll('.drawer.open').forEach(item => { if (item.id !== id) item.classList.remove('open'); });
  $(`#${id}`).classList.add('open');
}

function closeDrawer() { document.querySelectorAll('.drawer.open').forEach(item => item.classList.remove('open')); }

function openAssistant() { $('#assistantModal').classList.add('open'); }

function closeAssistant() { $('#assistantModal').classList.remove('open'); }

function toggleAssistant() {
  $('#assistantModal').classList.contains('open') ? closeAssistant() : openAssistant();
}

function openModal(html) {
  $('#modalCard').innerHTML = html;
  $('#modal').classList.add('open');
  bindModalActions();
}

function closeModal() { $('#modal').classList.remove('open'); }

function renderAuthState() {
  const button = $('#authBtn');
  button.textContent = state.user ? `${state.user.displayName} · 退出` : '登录 / 注册';
  $('#assistantLoginState').textContent = state.user
    ? `当前用户 ${state.user.displayName} · 正在线`
    : '登录后可查询订单 · 正在线';
}

function openAuth(mode = 'login') {
  const isLogin = mode === 'login';
  openModal(`
    <h3>${isLogin ? '欢迎回来' : '创建账号'}</h3>
    <form id="authForm">
      <div class="form-grid">
        <div class="field"><label>用户名</label><input name="username" value="${isLogin ? 'customer-1' : ''}" required></div>
        <div class="field"><label>密码</label><input name="password" type="password" value="${isLogin ? '123456' : ''}" required></div>
        ${isLogin ? '' : '<div class="field wide"><label>昵称（可选）</label><input name="displayName"></div>'}
      </div>
      ${isLogin ? '<p class="cart-price">演示账号：customer-1 / 123456</p>' : ''}
      <div class="modal-actions">
        <button type="button" class="secondary-btn" data-auth-switch="${isLogin ? 'register' : 'login'}">
          ${isLogin ? '去注册' : '去登录'}
        </button>
        <button class="primary-btn">${isLogin ? '登录' : '注册'}</button>
      </div>
    </form>`);
  $('#modalCard').querySelector('[data-auth-switch]').addEventListener('click', event => {
    openAuth(event.currentTarget.dataset.authSwitch);
  });
  $('#authForm').addEventListener('submit', async event => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target));
    try {
      const result = await request(`/api/auth/${isLogin ? 'login' : 'register'}`, {
        method: 'POST', body: JSON.stringify(payload)
      });
      state.token = result.token;
      state.user = result.customer;
      localStorage.setItem('authToken', result.token);
      renderAuthState();
      closeModal();
      showToast(isLogin ? '登录成功' : '注册成功');
      await loadCart();
    } catch (error) {
      showToast(error.message);
    }
  });
}

async function logout() {
  try { await request('/api/auth/logout', { method: 'POST' }); } catch (error) { }
  state.user = null;
  state.token = '';
  state.cart = { items: [], totalAmount: 0 };
  localStorage.removeItem('authToken');
  renderAuthState();
  renderCart();
  showToast('已退出登录');
}

function requireLogin() {
  if (state.user) return true;
  openAuth('login');
  showToast('请先登录');
  return false;
}

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, item => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[item]));
}

async function loadProducts() {
  $('#productGrid').innerHTML = '<div class="loading">周末商城正在铺货…</div>';
  const keyword = $('#searchInput').value.trim();
  const query = new URLSearchParams({ category: state.category });
  if (keyword) query.set('keyword', keyword);
  state.products = await request(`/api/mall/products?${query}`);
  renderProducts();
}

function renderCategories() {
  const categories = ['全部', '居家', '咖啡', '香薰', '旅行', '数码', '家居', '影音', '甜品', '户外', '绿植', '游戏', '个护'];
  state.categories = categories;
  $('#categoryList').innerHTML = categories.map(item =>
    `<button data-category="${item}" class="${item === state.category ? 'active' : ''}">${item}</button>`).join('');
}

function renderProducts() {
  if (!state.products.length) {
    $('#productGrid').innerHTML = '<div class="loading">没有找到匹配的周末好物，换个关键词试试。</div>';
    return;
  }
  $('#productGrid').innerHTML = state.products.map(product => `
    <article class="product">
      <div class="product-cover" data-product="${product.id}">
        <div class="emoji">${product.emoji}</div>
      </div>
      <div class="product-body">
        <div class="product-brand"><span>${esc(product.companyName)} · ${esc(product.brand)}</span><span>★ ${product.rating.toFixed(1)}</span></div>
        <h3>${esc(product.name)}</h3>
        <div class="policy-badge">不加班公司 · ${esc(product.workPolicy)}</div>
        <p class="tagline">${esc(product.tagline)}</p>
        <div class="price-row">
          <div><span class="price"><small>¥</small>${Number(product.price).toFixed(0)}</span><span class="original">¥${Number(product.originalPrice).toFixed(0)}</span></div>
          <button class="add-btn" data-add="${product.id}">加入购物车</button>
        </div>
      </div>
    </article>`).join('');
}

async function loadCart() {
  state.cart = await request('/api/mall/cart');
  renderCart();
}

async function loadCartIfAuthenticated() {
  if (state.user) await loadCart();
}

function renderCart() {
  const items = state.cart.items || [];
  $('#cartCount').textContent = items.reduce((sum, item) => sum + item.quantity, 0);
  $('#cartTotal').textContent = money(state.cart.totalAmount);
  $('#cartItems').innerHTML = items.length ? items.map(item => `
    <div class="cart-item">
      <div class="cart-thumb">${item.emoji}</div>
      <div>
        <h4 class="cart-name">${esc(item.name)}</h4>
        <div class="cart-price">${money(item.unitPrice)} × ${item.quantity}</div>
        <div class="qty">
          <button data-update="${item.productId}" data-qty="${item.quantity - 1}">-</button>
          <span>${item.quantity}</span>
          <button data-update="${item.productId}" data-qty="${item.quantity + 1}">+</button>
          <button class="remove" data-remove="${item.productId}" title="删除">🗑</button>
        </div>
      </div>
      <strong>${money(item.subtotal)}</strong>
    </div>`).join('') : '<div class="empty-state">购物车还是空的<br><br>先给自己挑一件周末礼物 🎁</div>';
}

function showProductDetail(product) {
  state.selectedProduct = product;
  openModal(`
    <div class="detail-hero"><span class="emoji">${product.emoji}</span>
      <div><h4>${esc(product.name)}</h4><p>${esc(product.brand)} · ${esc(product.category)}</p></div>
    </div>
    <p>${esc(product.description)}</p>
    <div class="kv">
      <div><strong>不加班公司：</strong>${esc(product.companyName)} · ${esc(product.workPolicy)}</div>
      <div><strong>周末理由：</strong>${esc(product.weekendTip)}</div>
      <div><strong>库存：</strong>${product.stock} 件 · <strong>评分：</strong>★ ${product.rating.toFixed(1)}</div>
      <div><strong>价格：</strong>${money(product.price)}（原价 ${money(product.originalPrice)}）</div>
    </div>
    <div class="modal-actions"><button class="secondary-btn" data-modal-close>继续逛逛</button>
      <button class="primary-btn" data-modal-add="${product.id}">加入购物车</button></div>`);
}

function showCheckout() {
  if (!requireLogin()) return;
  if (!state.cart.items?.length) { showToast('购物车是空的'); return; }
  openModal(`
    <h3>确认订单</h3>
    <form id="checkoutForm">
      <div class="form-grid">
        <div class="field"><label>收件人</label><input name="receiver" value="${esc(state.user.displayName)}" required></div>
        <div class="field"><label>手机号</label><input name="phone" value="13800000000" required></div>
        <div class="field wide"><label>收货地址</label><input name="address" value="上海市徐汇区周末生活区 2D 座 101" required></div>
        <div class="field wide"><label>备注（可选）</label><textarea name="remark">周末送，不用太着急。</textarea></div>
      </div>
      <p class="pay-label">支付方式</p>
      <div class="pay-options" id="payOptions">
        <button type="button" data-pay="WECHAT" class="active">微信支付</button>
        <button type="button" data-pay="ALIPAY">支付宝</button>
        <button type="button" data-pay="BANK_CARD">银行卡</button>
      </div>
      <div class="order-head"><span>合计</span><strong class="order-amount">${money(state.cart.totalAmount)}</strong></div>
      <div class="modal-actions">
        <button type="button" class="secondary-btn" data-modal-close>返回</button>
        <button class="primary-btn">创建订单</button>
      </div>
    </form>`);
  let paymentMethod = 'WECHAT';
  $('#payOptions').addEventListener('click', event => {
    const button = event.target.closest('[data-pay]');
    if (!button) return;
    paymentMethod = button.dataset.pay;
    document.querySelectorAll('#payOptions button').forEach(item => item.classList.remove('active'));
    button.classList.add('active');
  });
  $('#checkoutForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    const order = await request('/api/mall/orders', {
      method: 'POST',
      body: JSON.stringify(Object.fromEntries(form))
    });
    showPayment(order, paymentMethod);
    loadCart();
  });
}

function showPayment(order, paymentMethod) {
  const names = { WECHAT: '微信支付', ALIPAY: '支付宝', BANK_CARD: '银行卡' };
  openModal(`
    <h3>虚拟收银台</h3>
    <div class="detail-hero"><span class="emoji">💳</span><div><h4>订单 ${order.orderNo}</h4><p>${names[paymentMethod]} · 模拟支付</p></div></div>
    <div class="kv"><div><strong>支付金额：</strong>${money(order.totalAmount)}</div><div><strong>商品：</strong>${order.items.map(item => `${item.name}×${item.quantity}`).join('，')}</div></div>
    <div class="modal-actions"><button class="secondary-btn" data-modal-close>稍后支付</button><button class="primary-btn" data-pay-now="${order.orderNo}">确认支付</button></div>`);
}

async function loadOrders() {
  if (!requireLogin()) return;
  const orders = await request('/api/mall/orders');
  if (!orders.length) {
    openModal('<h3>我的订单</h3><div class="empty-state">还没有订单，周末的快乐可以先放购物车里。</div>');
    return;
  }
  const labels = { CREATED: '待支付', PAID: '已支付', CANCELLED: '已取消' };
  openModal(`
    <h3>我的订单</h3>
    ${orders.map(order => `
      <div class="order-card">
        <div class="order-head"><strong>${order.orderNo}</strong><span class="status ${order.status}">${labels[order.status]}</span></div>
        <div class="order-product">${order.items.map(item => `${item.emoji} ${esc(item.name)}×${item.quantity}`).join(' · ')}</div>
        <div class="order-head"><span class="cart-price">下单时间 ${new Date(order.createdAt).toLocaleString('zh-CN')}</span><span class="order-amount">${money(order.totalAmount)}</span></div>
        <div class="link-row">
          ${order.status === 'CREATED' ? `<button class="link-btn" data-pay-now="${order.orderNo}">去支付</button>` : ''}
          ${order.status === 'CREATED' ? `<button class="link-btn danger" data-cancel="${order.orderNo}">取消订单</button>` : ''}
          ${order.status === 'PAID' ? `<button class="link-btn" data-logistics="${order.orderNo}">查看物流</button>` : ''}
        </div>
        <div class="order-extra" data-extra="${order.orderNo}"></div>
      </div>`).join('')}`);
}

async function loadLogistics(orderNo, target) {
  const logistics = await request(`/api/mall/orders/${orderNo}/logistics`);
  target.innerHTML = `
    <div class="timeline">
      <div class="timeline-item"><strong>${logistics.trackingNo}</strong> · ${esc(logistics.carrier)} · ${esc(logistics.status)}</div>
      ${logistics.events.map(event => `
        <div class="timeline-item">${new Date(event.time).toLocaleString('zh-CN')}<br>${esc(event.location)} · ${esc(event.description)}</div>`).join('')}
      <div class="timeline-item"><strong>预计送达</strong> ${new Date(logistics.estimatedDelivery).toLocaleString('zh-CN')} · 取件码 ${logistics.pickupCode}</div>
    </div>`;
}

function appendMessage(role, content, typing = false) {
  $('#assistantMessages').insertAdjacentHTML('beforeend',
    role === 'assistant'
      ? `<div class="message assistant"><div class="agent-avatar small">周</div><div class="chat-body"><div class="bubble${typing ? ' typing' : ''}"></div></div></div>`
      : `<div class="message user"><div class="chat-body"><div class="bubble${typing ? ' typing' : ''}"></div></div></div>`);
  const body = $('#assistantMessages').lastElementChild.querySelector('.chat-body');
  const bubble = body.querySelector('.bubble');
  bubble.textContent = content;
  $('#assistantMessages').scrollTop = $('#assistantMessages').scrollHeight;
  return { bubble, body };
}

function inlineMarkdown(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+?)`/g, '<code>$1</code>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
}

function isTableSeparator(line) {
  return /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)+\|?\s*$/.test(line);
}

function tableRow(value) {
  return value.trim().replace(/^\||\|$/g, '').split('|').map(cell => cell.trim());
}

function renderMarkdown(value) {
  const lines = String(value ?? '').replace(/\r/g, '').split('\n');
  let html = '';
  let listType = null;
  let listItems = [];
  let tableRows = [];
  const flushList = () => {
    if (!listType) return;
    html += `<${listType}>${listItems.map(item => `<li>${inlineMarkdown(item)}</li>`).join('')}</${listType}>`;
    listType = null;
    listItems = [];
  };
  const flushTable = () => {
    if (!tableRows.length) return;
    const header = tableRows[0];
    const bodyRows = tableRows.slice(1);
    html += `<div class="md-table"><table><thead><tr>${
      header.map(cell => `<th>${inlineMarkdown(cell)}</th>`).join('')
    }</tr></thead><tbody>${
      bodyRows.map(row => `<tr>${
        header.map((_, index) => `<td>${inlineMarkdown(row[index] ?? '')}</td>`).join('')
      }</tr>`).join('')
    }</tbody></table></div>`;
    tableRows = [];
  };
  lines.forEach(line => {
    if (/^```/.test(line)) {
      flushList();
      flushTable();
      return;
    }
    if (/^\s*\|.*\|\s*$/.test(line)) {
      flushList();
      if (isTableSeparator(line)) return;
      tableRows.push(tableRow(line));
      return;
    }
    flushTable();
    if (/^#{1,3}\s+/.test(line)) {
      const level = Math.min(3, line.match(/^#+/)[0].length);
      flushList();
      html += `<h${level}>${inlineMarkdown(line.replace(/^#{1,3}\s+/, ''))}</h${level}>`;
      return;
    }
    if (/^\s*[-*]\s+/.test(line)) {
      if (listType !== 'ul') {
        flushList();
        listType = 'ul';
      }
      listItems.push(line.replace(/^\s*[-*]\s+/, ''));
      return;
    }
    if (/^\s*\d+\.\s+/.test(line)) {
      if (listType !== 'ol') {
        flushList();
        listType = 'ol';
      }
      listItems.push(line.replace(/^\s*\d+\.\s+/, ''));
      return;
    }
    if (!line.trim()) {
      flushList();
      return;
    }
    flushList();
    html += `<p>${inlineMarkdown(line)}</p>`;
  });
  flushList();
  flushTable();
  return html || '<p></p>';
}

function setAssistantText(bubble, value) {
  bubble.classList.remove('typing');
  bubble.innerHTML = renderMarkdown(value);
}

function showSlothLoader(body, text = '等等我，我在给你找') {
  if (!body || body.querySelector('.sloth-loader')) return;
  body.insertAdjacentHTML('afterbegin', `
    <div class="sloth-loader">
      <span class="sloth">🦥</span>
      <span class="sloth-speech">${esc(text)}<span class="sloth-dots"><i></i><i></i><i></i></span></span>
    </div>`);
}

function setSlothText(body, text) {
  const speech = body?.querySelector('.sloth-speech');
  if (speech) speech.innerHTML = `${esc(text)}<span class="sloth-dots"><i></i><i></i><i></i></span>`;
}

function hideSlothLoader(body) {
  body?.querySelector('.sloth-loader')?.remove();
}

function showAssistantStatus(text) {
  const body = $('#assistantMessages').lastElementChild?.querySelector('.chat-body');
  if (!body || body.querySelector('.status-chip')) return;
  body.insertAdjacentHTML('beforeend', '<div class="status-chip"></div>');
  body.querySelector('.status-chip').textContent = text;
}

function hideAssistantStatus() {
  const chip = $('#assistantMessages').lastElementChild?.querySelector('.status-chip');
  if (chip) chip.remove();
}

function parseToolResult(toolName, result) {
  try {
    const parsed = JSON.parse(result);
    const data = parsed?.data;
    if (toolName.includes('search_products') || toolName.includes('get_product')) {
      return { kind: 'products', items: Array.isArray(data) ? data : [data] };
    }
    if (toolName.includes('query_orders') || toolName.includes('get_order')) {
      return { kind: 'orders', items: Array.isArray(data) ? data : [data] };
    }
    if (toolName.includes('query_logistics')) {
      return { kind: 'logistics', data };
    }
  } catch (error) {
  }
  return null;
}

function renderMallCard(body, info) {
  if (!info || !body) return;
  let html = '';
  if (info.kind === 'products') {
    html = `
      <div class="mall-card">
        <div class="mall-card-title"><span>商城商品</span><span>不加班公司</span></div>
        ${info.items.slice(0, 3).map(product => `
          <div class="mini-product">
            <div class="mini-thumb">${product.emoji || '🛍️'}</div>
            <div>
              <h4 class="mini-name">${esc(product.name)}</h4>
              <div class="mini-meta">${esc(product.companyName || '')} · ${esc(product.workPolicy || '')}</div>
              <div class="mini-price">¥${Number(product.price).toFixed(0)} <span class="mini-meta">库存 ${product.stock}</span></div>
            </div>
          </div>`).join('')}
      </div>`;
  } else if (info.kind === 'orders') {
    html = `
      <div class="mall-card">
        <div class="mall-card-title"><span>订单信息</span><span>虚拟商城</span></div>
        ${info.items.slice(0, 2).map(order => `
          <div class="mini-order-row" style="margin-top: 8px;">
            <div><strong>${esc(order.orderNo)}</strong><div class="mini-meta">${esc(order.status)}</div></div>
            <div class="mini-price">¥${Number(order.totalAmount).toFixed(2)}</div>
          </div>`).join('')}
      </div>`;
  } else if (info.kind === 'logistics') {
    html = `
      <div class="mall-card">
        <div class="mall-card-title"><span>物流轨迹</span><span>${esc(info.data.trackingNo)}</span></div>
        <div class="mini-order-row"><strong>${esc(info.data.currentLocation)}</strong><div class="mini-price">${esc(info.data.status)}</div></div>
        <div class="timeline-card">
          ${info.data.events.slice(-3).map(event => `
            <div>${new Date(event.time).toLocaleString('zh-CN')}<br>${esc(event.description)}</div>`).join('')}
        </div>
      </div>`;
  }
  if (html) body.insertAdjacentHTML('beforeend', html);
}

async function askAssistant(message) {
  if (!message.trim()) return;
  if (!requireLogin()) return;
  openAssistant();
  appendMessage('user', message);
  const reply = appendMessage('assistant', '', true);
  const bubble = reply.bubble;
  const body = reply.body;
  showSlothLoader(body);
  const waitTimer = setTimeout(() => setSlothText(body, '客服正在思考，可能需要 1 分钟左右…'), 5000);
  let answer = '';
  try {
    const response = await fetch('/api/assistant/stream', {
      method: 'POST',
      headers: state.token ? { ...headers, Authorization: `Bearer ${state.token}` } : headers,
      body: JSON.stringify({ message })
    });
    if (!response.ok) throw new Error('客服连接失败');
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const chunks = buffer.split(/\n\n/);
      buffer = chunks.pop();
      chunks.forEach(chunk => {
        let eventName = '';
        let data = '';
        chunk.split('\n').forEach(line => {
          if (line.startsWith('event:')) eventName = line.slice(6).trim();
          if (line.startsWith('data:')) data += line.slice(5).trim();
        });
        if (!data) return;
        try {
          const payload = JSON.parse(data);
          if (eventName === 'step_break' && payload.toolName && payload.status === 'running') {
            const label = payload.toolName.includes('search_products') || payload.toolName.includes('get_product')
              ? '正在为你查商城商品…'
              : payload.toolName.includes('logistics') ? '正在追踪物流…'
              : '正在核对订单信息…';
            setSlothText(body, label);
          }
          if (eventName === 'chunk' && payload.content) {
            answer += payload.content;
            hideSlothLoader(body);
            setAssistantText(bubble, answer);
          }
          if (eventName === 'error' && (payload.message || payload.content)) {
            answer += payload.message || payload.content;
            hideSlothLoader(body);
            setAssistantText(bubble, answer);
          }
          if (eventName === 'done' && payload.messages) {
            hideSlothLoader(body);
            const toolMessages = payload.messages.filter(item => item.role === 'tool');
            const lastTool = toolMessages.at(-1);
            if (lastTool) renderMallCard(body, parseToolResult(lastTool.toolName || '', lastTool.result || ''));
          }
        } catch (error) {
          if (eventName === 'error') {
            bubble.classList.remove('typing');
            bubble.textContent = '智能客服服务暂时不可用，请确认 Harness 已启动。';
          }
        }
        $('#assistantMessages').scrollTop = $('#assistantMessages').scrollHeight;
      });
    }
  } catch (error) {
    hideSlothLoader(body);
    setAssistantText(bubble, '智能客服暂时不可用，请确认 DeepSeek Harness 已启动。');
  } finally {
    clearTimeout(waitTimer);
  }
}

function bindModalActions() {
  $('#modalCard').querySelectorAll('[data-modal-close]').forEach(item => item.addEventListener('click', closeModal));
  $('#modalCard').querySelectorAll('[data-modal-add]').forEach(item => item.addEventListener('click', async event => {
    await addToCart(event.currentTarget.dataset.modalAdd, 1);
    closeModal();
  }));
  $('#modalCard').querySelectorAll('[data-pay-now]').forEach(item => item.addEventListener('click', async event => {
    const orderNo = event.currentTarget.dataset.payNow;
    const order = await request(`/api/mall/orders/${orderNo}/payment`, { method: 'POST', body: JSON.stringify({}) });
    openModal(`<h3>支付成功</h3><div class="detail-hero"><span class="emoji">🎉</span><div><h4>包裹已安排</h4><p>订单 ${order.orderNo} 已支付</p></div></div><p>现在可以去“我的订单”查看虚拟物流啦。</p><div class="modal-actions"><button class="secondary-btn" data-modal-close>好的</button><button class="primary-btn" data-modal-orders>查看订单</button></div>`);
  }));
  $('#modalCard').querySelectorAll('[data-modal-orders]').forEach(item => item.addEventListener('click', loadOrders));
  $('#modalCard').querySelectorAll('[data-cancel]').forEach(item => item.addEventListener('click', async event => {
    const orderNo = event.currentTarget.dataset.cancel;
    await request(`/api/mall/orders/${orderNo}/cancel`, { method: 'POST' });
    showToast('订单已取消');
    loadOrders();
  }));
  $('#modalCard').querySelectorAll('[data-logistics]').forEach(item => item.addEventListener('click', event => {
    const orderNo = event.currentTarget.dataset.logistics;
    const target = $(`[data-extra="${orderNo}"]`);
    if (target.innerHTML) { target.innerHTML = ''; return; }
    loadLogistics(orderNo, target);
  }));
}

async function addToCart(productId, quantity) {
  if (!requireLogin()) return;
  state.cart = await request('/api/mall/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) });
  renderCart();
  showToast('已加入购物车');
}

function bindEvents() {
  document.addEventListener('click', event => {
    if (event.target.closest('[data-close]')) closeDrawer();
    if (event.target === $('#modal')) closeModal();
    const category = event.target.closest('[data-category]');
    if (category) {
      state.category = category.dataset.category;
      renderCategories();
      loadProducts().catch(error => showToast(error.message));
    }
    const product = event.target.closest('[data-product]');
    if (product) showProductDetail(state.products.find(item => item.id === product.dataset.product));
    const add = event.target.closest('[data-add]');
    if (add) addToCart(add.dataset.add, 1).catch(error => showToast(error.message));
    const update = event.target.closest('[data-update]');
    if (update) {
      request(`/api/mall/cart/items/${update.dataset.update}`, {
        method: 'PUT', body: JSON.stringify({ quantity: Number(update.dataset.qty) })
      }).then(data => { state.cart = data; renderCart(); }).catch(error => showToast(error.message));
    }
    const remove = event.target.closest('[data-remove]');
    if (remove) request(`/api/mall/cart/items/${remove.dataset.remove}`, { method: 'DELETE' })
      .then(data => { state.cart = data; renderCart(); }).catch(error => showToast(error.message));
  });
  $('#cartBtn').addEventListener('click', () => openDrawer('cartDrawer'));
  $('#authBtn').addEventListener('click', () => state.user ? logout() : openAuth('login'));
  $('#ordersBtn').addEventListener('click', () => loadOrders().catch(error => showToast(error.message)));
  $('#checkoutBtn').addEventListener('click', showCheckout);
  $('#exploreBtn').addEventListener('click', () => window.scrollTo({ top: 540, behavior: 'smooth' }));
  $('#assistantFab').addEventListener('click', toggleAssistant);
  $('#askAssistantBtn').addEventListener('click', () => openAssistant());
  document.querySelectorAll('[data-assistant-close]').forEach(item => item.addEventListener('click', closeAssistant));
  $('#searchInput').addEventListener('keydown', event => {
    if (event.key === 'Enter') loadProducts().catch(error => showToast(error.message));
  });
  $('#assistantForm').addEventListener('submit', event => {
    event.preventDefault();
    const input = $('#assistantInput');
    askAssistant(input.value);
    input.value = '';
  });
  document.querySelectorAll('[data-prompt]').forEach(item => item.addEventListener('click', () => askAssistant(item.dataset.prompt)));
}

async function init() {
  renderCategories();
  bindEvents();
  try {
    if (state.token) {
      state.user = await request('/api/auth/me');
    }
  } catch (error) {
    showToast(error.message);
  }
  renderAuthState();
  await loadProducts();
  await loadCartIfAuthenticated();
}

init();
