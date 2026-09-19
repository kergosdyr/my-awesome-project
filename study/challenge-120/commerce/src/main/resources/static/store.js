import { api, money, escapeHtml } from "/shared.js";

const catalog = document.querySelector("#catalog");
const productDialog = document.querySelector("#product-dialog");
const ordersDialog = document.querySelector("#orders-dialog");
let products = [];
let catalogPage = 0;
let catalogLoading = false;
let cart = [];
let checkingOut = false;
const cartDialog = document.querySelector("#cart-dialog");
let toastTimer;

function toast(message) {
  const element = document.querySelector("#toast");
  element.textContent = message;
  element.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { element.hidden = true; }, 5000);
}

async function loadCatalog(append = false) {
  if (catalogLoading) return;
  catalogLoading = true;
  const more = document.querySelector("#more-products");
  more.disabled = true;
  try {
    let lastPage;
    let loaded = append ? [...products] : [];
    const targetPage = append ? catalogPage + 1 : catalogPage;
    for (let page = append ? targetPage : 0; page <= targetPage; page++) {
      lastPage = await api(`/api/products?page=${page}&size=20`);
      loaded.push(...lastPage);
    }
    products = loaded;
    document.querySelector("#catalog-count").textContent = String(products.length).padStart(2, "0");
    catalogPage = targetPage;
    more.hidden = lastPage.length < 20;
    catalog.innerHTML = products.map(product => `
      <button class="product-card" data-product="${product.id}">
        <div class="product-art"><span class="category">${escapeHtml(product.category)}</span><img src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" loading="lazy"><span class="card-arrow">↗</span></div>
        <div class="product-meta"><span>${escapeHtml(product.brand)}</span><span>${String(product.id).padStart(2, "0")}</span></div>
        <h3>${escapeHtml(product.name)}</h3><p class="price">${money(product.price)}</p>
      </button>`).join("");
  } catch (error) {
    catalog.innerHTML = `<p class="error">${escapeHtml(error.message)} <button id="retry-catalog">다시 시도</button></p>`;
    document.querySelector("#retry-catalog").onclick = () => loadCatalog();
  } finally {
    catalogLoading = false;
    more.disabled = false;
  }
}
document.querySelector("#more-products").onclick = () => loadCatalog(true);

function openProduct(id) {
  if (checkingOut) { toast("주문 처리가 끝난 뒤 상품을 추가해 주세요."); return; }
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
      <button class="primary" type="submit" ${product.options.every(option => option.stock === 0) ? "disabled" : ""}>${product.options.every(option => option.stock === 0) ? "품절된 상품입니다" : "장바구니에 담기 →"}</button>
      <p class="form-note">장바구니에 여러 상품을 담고 함께 주문할 수 있습니다. 옵션별 최대 5개입니다.</p>
    </form>`;
  const form = document.querySelector("#order-form");
  form.querySelector("#quantity").oninput = event => {
    document.querySelector("#total").textContent = money(product.price * Math.max(0, Number(event.target.value)));
  };
  form.onsubmit = event => {
    event.preventDefault();
    const optionId = Number(form.querySelector("#option").value);
    const quantity = Number(form.querySelector("#quantity").value);
    const option = product.options.find(item => item.id === optionId);
    const existing = cart.find(item => item.optionId === optionId);
    if (!option || quantity < 1 || !Number.isInteger(quantity) || quantity + (existing?.quantity ?? 0) > 5) {
      form.querySelector("#order-error").textContent = "같은 옵션은 최대 5개까지 담을 수 있습니다.";
      return;
    }
    if (existing && existing.displayedUnitPrice !== product.price) {
      form.querySelector("#order-error").textContent = "장바구니에서 현재 가격을 다시 확인해 주세요.";
      return;
    }
    if (!existing && cart.length >= 20) {
      form.querySelector("#order-error").textContent = "한 주문에 최대 20종류까지 담을 수 있습니다.";
      return;
    }
    if (existing) existing.quantity += quantity;
    else cart.push({ optionId, quantity, displayedUnitPrice: product.price,
      productName: product.name, optionName: `${option.color} / ${option.size}`, image: product.image });
    renderCart();
    productDialog.close();
    toast("장바구니에 담았습니다. 다른 상품도 함께 선택해 보세요.");
  };
  productDialog.showModal();
}

const labels = { UNPAID: "결제 전", PENDING: "승인 확인 중", PAID: "결제 완료" };
let ordersPage = 0;
let ordersRequest = 0;
async function loadOrders(page = ordersPage) {
  const container = document.querySelector("#orders");
  const requestId = ++ordersRequest;
  const previous = document.querySelector("#previous-orders");
  const next = document.querySelector("#next-orders");
  const status = document.querySelector("#orders-page-status");
  previous.disabled = next.disabled = true;
  container.setAttribute("aria-busy", "true");
  status.textContent = "주문을 불러오는 중…";
  try {
    const result = await api(`/api/orders?page=${page}&size=20`);
    if (requestId !== ordersRequest) return;
    const orders = result.entries;
    if (ordersPage !== result.page) ordersDialog.scrollTop = 0;
    ordersPage = result.page;
    previous.disabled = ordersPage === 0;
    next.disabled = !result.hasNext;
    status.textContent = `${ordersPage + 1}페이지 · ${orders.length}건`;

    container.innerHTML = orders.length ? orders.map(({ order, paymentStatus, approvalId }) => `
      <article class="order-card">
        <div class="order-top"><span>ORDER ${String(order.id).padStart(5, "0")}</span><span class="status ${paymentStatus.toLowerCase()}">${labels[paymentStatus] ?? escapeHtml(paymentStatus)}</span></div>
        ${order.items.map(item => `<div class="order-item"><img src="${escapeHtml(item.image)}" alt=""><div><h3>${escapeHtml(item.productName)}</h3><p>${escapeHtml(item.optionName)} / ${item.quantity}개 · 개당 ${money(item.unitPrice)}</p><strong>${money(item.totalAmount)}</strong></div></div>`).join("")}
        <div class="total"><span>주문 합계</span><strong>${money(order.totalAmount)}</strong></div>
        ${paymentStatus === "PAID" ? `<p class="order-note">승인이 확인되었습니다. ${escapeHtml(approvalId ?? "")}</p>` : paymentStatus === "PENDING" ? `<p class="order-note">결제 승인 결과를 기다리고 있습니다.<br>새로고침하면 최신 상태를 확인할 수 있습니다.</p>` : `<button class="primary" data-pay="${order.id}">${money(order.totalAmount)} 결제하기 →</button>`}
        <p class="error" id="payment-error-${order.id}" role="alert"></p>
      </article>`).join("") : '<div class="empty"><span>아직, 첫 번째 선택을 기다려요.</span><p>마음에 드는 상품을 골라보세요.</p><button class="primary" data-close="orders-dialog">컬렉션 둘러보기 →</button></div>';
  } catch (error) {
    if (requestId !== ordersRequest) return;
    container.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`;
    status.textContent = "불러오지 못했습니다. 새로고침해 주세요.";
  } finally {
    if (requestId === ordersRequest) container.setAttribute("aria-busy", "false");
  }
}

async function openOrders() {
  if (!ordersDialog.open) ordersDialog.showModal();
  await loadOrders(0);
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
document.querySelector("#refresh-orders").onclick = () => loadOrders(0);
document.querySelector("#previous-orders").onclick = () => loadOrders(ordersPage - 1);
document.querySelector("#next-orders").onclick = () => loadOrders(ordersPage + 1);


function renderCart() {
  document.querySelector("#cart-count").textContent = cart.reduce((sum, item) => sum + item.quantity, 0);
  document.querySelector("#cart-items").innerHTML = cart.length ? cart.map(item => `
    <article class="order-item"><img src="${escapeHtml(item.image)}" alt=""><div>
      <h3>${escapeHtml(item.productName)}</h3><p>${escapeHtml(item.optionName)} · 개당 ${money(item.displayedUnitPrice)}</p>
      <label>수량 <input type="number" min="1" max="5" value="${item.quantity}" data-cart-quantity="${item.optionId}" aria-label="${escapeHtml(item.productName)} 수량" ${checkingOut ? "disabled" : ""}></label>
      <strong>${money(item.displayedUnitPrice * item.quantity)}</strong>
      <button class="text-button" data-remove-item="${item.optionId}" ${checkingOut ? "disabled" : ""}>삭제</button>
    </div></article>`).join("") : '<p class="empty">함께 주문할 상품을 담아 주세요.</p>';
  document.querySelector("#cart-total").textContent = money(cart.reduce((sum, item) => sum + item.displayedUnitPrice * item.quantity, 0));
  document.querySelector("#checkout").disabled = checkingOut || !cart.length;
  document.querySelector("#checkout").textContent = checkingOut ? "주문을 만들고 있습니다…" : "함께 주문하기 →";
  document.querySelector("#refresh-cart").disabled = checkingOut || !cart.length;
}

document.querySelector("#open-cart").onclick = () => { renderCart(); cartDialog.showModal(); };
document.querySelector("#cart-items").onclick = event => {
  const button = event.target.closest("[data-remove-item]");
  if (!button || checkingOut) return;
  cart = cart.filter(item => item.optionId !== Number(button.dataset.removeItem));
  document.querySelector("#cart-error").textContent = "";
  renderCart();
};
document.querySelector("#cart-items").onchange = event => {
  const id = Number(event.target.dataset.cartQuantity);
  const item = cart.find(item => item.optionId === id);
  if (!item || checkingOut) return;
  const quantity = Number(event.target.value);
  if (Number.isInteger(quantity) && quantity >= 1 && quantity <= 5) item.quantity = quantity;
  renderCart();
};
document.querySelector("#refresh-cart").onclick = async () => {
  checkingOut = true;
  renderCart();
  try {
    const current = [];
    for (let page = 0; page <= catalogPage; page++) {
      current.push(...await api(`/api/products?page=${page}&size=20`));
    }
    const refreshed = cart.map(item => {
      const product = current.find(product => product.options.some(option => option.id === item.optionId));
      const option = product?.options.find(option => option.id === item.optionId);
      if (!product || option.stock < item.quantity) throw new Error(`${item.productName}: 선택한 수량의 재고가 없습니다. 수량을 줄이거나 삭제해 주세요.`);
      return { ...item, displayedUnitPrice: product.price };
    });
    cart = refreshed;
    document.querySelector("#cart-error").textContent = "현재 가격을 반영했습니다. 금액을 확인한 뒤 주문해 주세요.";
  } catch (error) { document.querySelector("#cart-error").textContent = error.message; }
  finally { checkingOut = false; renderCart(); }
};
document.querySelector("#checkout").onclick = async () => {
  if (checkingOut || !cart.length) return;
  checkingOut = true;
  document.querySelector("#cart-error").textContent = "";
  renderCart();
  try {
    await api("/api/orders", "POST", { items: cart.map(({ optionId, quantity, displayedUnitPrice }) => ({ optionId, quantity, displayedUnitPrice })) });
    cart = [];
    cartDialog.close();
    await openOrders();
    await loadCatalog();
    toast("상품을 하나의 주문으로 묶었습니다. 결제를 진행해 주세요.");
  } catch (error) { document.querySelector("#cart-error").textContent = error.message; }
  finally { checkingOut = false; renderCart(); }
};
renderCart();

await Promise.all([loadCatalog(), loadOrders()]);
