import { api } from "/shared.js";

let receipt;
const orderId = document.querySelector("#order-id");
const notify = document.querySelector("#notify");
async function perform(button, action) {
  button.disabled = true;
  try {
    const result = await action();
    document.querySelector("#dev-status").textContent = "요청 완료";
    document.querySelector("#output").textContent = JSON.stringify(result ?? { completed: true }, null, 2);
  } catch (error) {
    document.querySelector("#dev-status").textContent = "요청 실패";
    document.querySelector("#output").textContent = error.message;
  } finally {
    button.disabled = false;
  }
}
function selectedId() {
  const id = Number(orderId.value);
  if (!Number.isSafeInteger(id) || id < 1) throw new Error("올바른 주문 번호를 입력해 주세요.");
  return id;
}
orderId.oninput = () => { receipt = undefined; notify.disabled = true; };
document.querySelector("#set-mode").onclick = event => perform(event.currentTarget, () =>
  api(`/dev/pg/mode/${document.querySelector("#mode").value}`, "PUT"));
document.querySelector("#complete").onclick = event => perform(event.currentTarget, async () => {
  receipt = undefined;
  notify.disabled = true;
  receipt = await api(`/dev/pg/${selectedId()}/complete`, "POST");
  notify.disabled = false;
  return receipt;
});
notify.onclick = event => perform(event.currentTarget, () => api("/api/payments/notifications", "POST", receipt));
document.querySelector("#lookup").onclick = event => perform(event.currentTarget, () => api(`/api/orders/${selectedId()}`));
