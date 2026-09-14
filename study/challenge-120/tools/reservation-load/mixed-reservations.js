import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
const base = __ENV.BASE_URL;
const created = new Counter('reservations_created');
const soldOut = new Counter('reservations_sold_out');
export const options = {
  scenarios: { mixed: { executor: 'ramping-vus', startVUs: 0, stages: [
    { duration: '10s', target: 50 },
    { duration: '10s', target: 200 },
    { duration: '10s', target: 500 },
    { duration: '10s', target: 1000 },
    { duration: '10s', target: 1000 },
    { duration: '10s', target: 0 },
  ],
  gracefulRampDown: '30s', gracefulStop: '30s' } },
  thresholds: { http_req_failed: ['rate==0'], checks: ['rate==1'] },
};
// 매진은 정상적인 업무 응답이다. 네트워크/5xx 오류와 구별한다.
http.setResponseCallback(http.expectedStatuses(200,409));
export function setup() {
  const state=http.get(`${base}/state`).json();
  if(state.remaining!==0 || state.reservations!==1) throw new Error('Unexpected fixture');
  const result=http.post(`${base}/cancel?id=10`,null,{tags:{name:'cancel'}});
  if(result.json('status')!=='CANCELLED') throw new Error('Initial cancellation failed');
}
export default function () {
  // 모두 event=1 한 행을 두고 경합. VU별 사용자는 다르고 성공한 예약만 본인이 취소한다.
  const response=http.post(`${base}/reserve?user=${1000+__VU}`,null,{tags:{name:'reserve'},timeout:'15s'});
  let value;
  try { value=response.json(); } catch (_) { check(false,{'valid JSON response':v=>v}); return; }
  check(response,{'reservation contract':r=>
    (r.status===200 && value.status==='CREATED' && value.reservationId>0) ||
    (r.status===409 && value.status==='SOLD_OUT' && value.reservationId===null)});
  if(value.status==='CREATED') {
    created.add(1);
    const cancelled=http.post(`${base}/cancel?id=${value.reservationId}`,null,{tags:{name:'cancel'},timeout:'15s'});
    check(cancelled,{'cancellation contract':r=>r.status===200 && r.json('status')==='CANCELLED'});
  } else if(value.status==='SOLD_OUT') soldOut.add(1);
}
export function teardown() {
  const value=http.get(`${base}/state`).json();
  check(value,{
    'final seats and reservations':s=>s.remaining===1 && s.reservations===0,
    'no server exceptions':s=>s.errors===0,
    'committed operation counts agree':s=>1+s.created-s.cancelled===s.reservations,
  });
  console.log(`FINAL_STATE ${JSON.stringify(value)}`);
}
export function handleSummary(data) {
  return { [__ENV.SUMMARY_PATH || 'build/k6-summary.json']:JSON.stringify(data,null,2) };
}
