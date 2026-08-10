import http from 'k6/http';

const DEFAULT_TIMEOUT = '10s';

function requestParams(tags, extraHeaders = {}) {
  return {
    headers: {
      Accept: 'application/json',
      ...extraHeaders,
    },
    tags,
    timeout: __ENV.REQUEST_TIMEOUT || DEFAULT_TIMEOUT,
  };
}

export function getJson(url, tags = {}) {
  return http.get(url, requestParams(tags));
}

export function postJson(url, body, tags = {}, extraHeaders = {}) {
  return http.post(
    url,
    JSON.stringify(body),
    requestParams(tags, {
      'Content-Type': 'application/json',
      ...extraHeaders,
    }),
  );
}

export function requestJson(method, url, body, tags = {}, extraHeaders = {}) {
  const normalizedMethod = method.toUpperCase();
  const payload = normalizedMethod === 'GET' || normalizedMethod === 'HEAD' ? null : JSON.stringify(body);
  const headers = payload === null ? extraHeaders : { 'Content-Type': 'application/json', ...extraHeaders };

  return http.request(normalizedMethod, url, payload, requestParams(tags, headers));
}

export function tryJson(response) {
  try {
    return response.json();
  } catch (_error) {
    return null;
  }
}
