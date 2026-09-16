import { api, money, escapeHtml } from "/shared.js";

const catalog = document.querySelector("#catalog");
const productDialog = document.querySelector("#product-dialog");
const ordersDialog = document.querySelector("#orders-dialog");
let products = [];
let toastTimer;

function toast(message) {
  const element = document.querySelector("#toast");
  element.textContent = message;
  element.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { element.hidden = true; }, 5000);
}

async function loadCatalog() {
  try {
    products = await api("/api/products");
    catalog.innerHTML = products.map(product => `
      <button class="product-card" data-product="${product.id}">
        <div class="product-art"><span class="category">${escapeHtml(product.category)}</span><img src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" loading="lazy"><span class="card-arrow">↗</span></div>
        <div class="product-meta"><span>${escapeHtml(product.brand)}</span><span>${String(product.id).padStart(2, "0")}</span></div>
        <h3>${escapeHtml(product.name)}</h3><p class="price">${money(product.price)}</p>
      </button>`).join("");
  } catch (error) {
    catalog.innerHTML = `<p class="error">${escapeHtml(error.message)} <button id="retry-catalog">다시 시도</button></p>`;
    document.querySelector("#retry-catalog").onclick = loadCatalog;
  }
}

function openProduct(id) {
  const product = products.find(item => item.id === id);
  if (!product) return;
  document.querySelector("#product-detail").innerHTML = `
    <div class="detail-art"><img src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}"></div>
    <form id="order-form" class="detail-copy">
      <p class="eyebrow">${escapeHtml(product.brand)}</p><h2 id="product-title">${escapeHtml(product.name)}</h2>
      <p class="detail-description">${escapeHtml(product.description)}</p><p class="detail-price">${money(product.price)}</p>
      <label for="option">색상 / 사이즈</label><select id="option" required>
        ${product.options.map(option => `<option value="${option.id}" data-stock="${option.stock}" ${option.stock === 0 ? "disabled" : ""}>${escapeHtml(option.color)} / ${escapeHtml(option.size)}${option.stock === 0 ? " · 품절" : ` · ${option.stock}개 남음`}</option>`).join("")}
      </select>
      <label for="quantity">수량</label><input id="quantity" type="number" min="1" max="5" value="1" required>
      <div class="total"><span>주문 금액</span><strong id="total">${money(product.price)}</strong></div>
      <p id="order-error" class="error" role="alert"></p>
      <button class="primary" type="submit" ${product.options.every(option => option.stock === 0) ? "disabled" : ""}>${product.options.every(option => option.stock === 0) ? "품절된 상품입니다" : "주문하기 →"}</button>
      <p class="form-note">주문 후 결제를 진행합니다. 한 번에 최대 5개까지 선택할 수 있습니다.</p>
    </form>`;
  const form = document.querySelector("#order-form");
  form.querySelector("#quantity").oninput = event => {
    document.querySelector("#total").textContent = money(product.price * Math.max(0, Number(event.target.value)));
  };
  form.onsubmit = async event => {
    event.preventDefault();
    const button = form.querySelector("button[type=submit]");
    button.disabled = true;
    button.textContent = "주문을 만들고 있습니다…";
    try {
      await api("/api/orders", "POST", {
        optionId: Number(form.querySelector("#option").value),
        quantity: Number(form.querySelector("#quantity").value),
        displayedUnitPrice: product.price,
      });
      productDialog.close();
      await openOrders();
      await loadCatalog();
      toast("주문이 생성되었습니다. 결제를 진행해 주세요.");
    } catch (error) {
      form.querySelector("#order-error").textContent = error.message;
    } finally {
      button.disabled = false;
      button.textContent = "주문하기 →";
    }
  };
  productDialog.showModal();
}

const labels = { UNPAID: "결제 전", PENDING: "승인 확인 중", PAID: "결제 완료" };
async function loadOrders() {
  const container = document.querySelector("#orders");
  try {
    const orders = await api("/api/orders");
    document.querySelector("#order-count").textContent = orders.length;
    container.innerHTML = orders.length ? orders.map(({ order, paymentStatus, approvalId }) => `
      <article class="order-card">
        <div class="order-top"><span>ORDER ${String(order.id).padStart(5, "0")}</span><span class="status ${paymentStatus.toLowerCase()}">${labels[paymentStatus] ?? escapeHtml(paymentStatus)}</span></div>
        <div class="order-item"><img src="${escapeHtml(order.image)}" alt=""><div><h3>${escapeHtml(order.productName)}</h3><p>${escapeHtml(order.optionName)} / ${order.quantity}개</p><strong>${money(order.totalAmount)}</strong></div></div>
        ${paymentStatus === "PAID" ? `<p class="order-note">승인이 확인되었습니다. ${escapeHtml(approvalId ?? "")}</p>` : paymentStatus === "PENDING" ? `<p class="order-note">결제 승인 결과를 기다리고 있습니다.<br>새로고침하면 최신 상태를 확인할 수 있습니다.</p>` : `<button class="primary" data-pay="${order.id}">${money(order.totalAmount)} 결제하기 →</button>`}
        <p class="error" id="payment-error-${order.id}" role="alert"></p>
      </article>`).join("") : '<div class="empty"><span>아직, 첫 번째 선택을 기다려요.</span><p>마음에 드는 상품을 골라보세요.</p><button class="primary" data-close="orders-dialog">컬렉션 둘러보기 →</button></div>';
  } catch (error) {
    container.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`;
  }
}

async function openOrders() {
  if (!ordersDialog.open) ordersDialog.showModal();
  await loadOrders();
}

async function pay(button) {
  const id = button.dataset.pay;
  button.disabled = true;
  button.textContent = "결제 결과를 확인하고 있습니다…";
  try {
    const result = await api(`/api/orders/${id}/payments`, "POST");
    await loadOrders();
    toast(result.status === "PAID" ? "결제가 완료되었습니다." : "승인 결과를 확인 중입니다.");
  } catch (error) {
    document.querySelector(`#payment-error-${id}`).textContent = error.message;
    button.disabled = false;
    button.textContent = "결제 다시 시도 →";
  }
}

document.addEventListener("click", event => {
  const close = event.target.closest("[data-close]");
  if (close) document.getElementById(close.dataset.close).close();
  const product = event.target.closest("[data-product]");
  if (product) openProduct(Number(product.dataset.product));
  const payment = event.target.closest("[data-pay]");
  if (payment) pay(payment);
});
document.querySelector("#open-orders").onclick = openOrders;
document.querySelector("#refresh-orders").onclick = loadOrders;
await Promise.all([loadCatalog(), loadOrders()]);
