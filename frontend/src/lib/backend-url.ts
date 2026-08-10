const defaultBackendOrigin = 'http://localhost:8080'

export function backendUrl(path: string) {
  const configuredOrigin = process.env.BACKEND_ORIGIN?.trim()
  const origin = configuredOrigin || defaultBackendOrigin
  const url = new URL(path, origin.endsWith('/') ? origin : `${origin}/`)

  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    throw new Error('BACKEND_ORIGIN must use http or https')
  }

  return url
}
