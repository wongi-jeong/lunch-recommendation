const DB_NAME = 'mechu_voter_db'
const STORE_NAME = 'identity'
const COOKIE_KEY = 'mechu_vid'
const LS_KEY = 'mechu_voter_id'
const LS_FP_KEY = 'mechu_voter_fp'

async function sha256(message) {
  const data = new TextEncoder().encode(message)
  const buffer = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buffer))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}

async function generateFingerprint() {
  const signals = []

  signals.push(navigator.userAgent ?? '')
  signals.push(navigator.language ?? '')
  signals.push((navigator.languages ?? []).join(','))
  signals.push(String(navigator.hardwareConcurrency ?? ''))
  signals.push(navigator.platform ?? '')
  signals.push(`${screen.width}x${screen.height}x${screen.colorDepth}`)
  signals.push(String(window.devicePixelRatio ?? ''))
  signals.push(Intl.DateTimeFormat().resolvedOptions().timeZone ?? '')
  signals.push(String(new Date().getTimezoneOffset()))

  try {
    const canvas = document.createElement('canvas')
    canvas.width = 280
    canvas.height = 60
    const ctx = canvas.getContext('2d')
    ctx.textBaseline = 'alphabetic'
    ctx.font = '18px Arial, sans-serif'
    ctx.fillStyle = '#f60'
    ctx.fillRect(100, 1, 80, 24)
    ctx.fillStyle = '#069'
    ctx.fillText('mechu:fp:canvas', 2, 18)
    ctx.fillStyle = 'rgba(102,204,0,0.7)'
    ctx.fillText('mechu:fp:canvas', 4, 20)
    signals.push(canvas.toDataURL())
  } catch {
    signals.push('canvas:n/a')
  }

  try {
    const gl =
      document.createElement('canvas').getContext('webgl') ||
      document.createElement('canvas').getContext('experimental-webgl')
    if (gl) {
      const ext = gl.getExtension('WEBGL_debug_renderer_info')
      if (ext) {
        signals.push(gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) ?? '')
        signals.push(gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) ?? '')
      }
    }
  } catch {
    signals.push('webgl:n/a')
  }

  return sha256(signals.join('|||'))
}

// --------------- Cookie ---------------

function readCookie(key) {
  const m = document.cookie.match(new RegExp(`(?:^|; )${key}=([^;]*)`))
  return m ? decodeURIComponent(m[1]) : null
}

function writeCookie(key, value) {
  document.cookie = `${key}=${encodeURIComponent(value)};path=/;max-age=${365 * 86400};SameSite=Lax`
}

// --------------- IndexedDB ---------------

function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => {
      if (!req.result.objectStoreNames.contains(STORE_NAME)) {
        req.result.createObjectStore(STORE_NAME)
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function idbGet(key) {
  try {
    const db = await openDB()
    return new Promise(resolve => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const r = tx.objectStore(STORE_NAME).get(key)
      r.onsuccess = () => resolve(r.result ?? null)
      r.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

async function idbSet(key, value) {
  try {
    const db = await openDB()
    return new Promise(resolve => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      tx.objectStore(STORE_NAME).put(value, key)
      tx.oncomplete = () => resolve()
      tx.onerror = () => resolve()
    })
  } catch { /* silent */ }
}

// --------------- Persist helpers ---------------

async function persistId(id) {
  localStorage.setItem(LS_KEY, id)
  writeCookie(COOKIE_KEY, id)
  await idbSet('voter_id', id)
}

async function persistFp(fp) {
  localStorage.setItem(LS_FP_KEY, fp)
  await idbSet('voter_fp', fp)
}

// --------------- Public API ---------------

export async function getVoterIdentity() {
  const fromLS = localStorage.getItem(LS_KEY)
  const fromCookie = readCookie(COOKIE_KEY)
  const fromIDB = await idbGet('voter_id')
  const existingId = fromLS || fromCookie || fromIDB

  const fp = await generateFingerprint()

  if (existingId) {
    await persistId(existingId)
    await persistFp(fp)
    return { voterId: existingId, fingerprint: fp }
  }

  const newId = crypto.randomUUID()
  await persistId(newId)
  await persistFp(fp)
  return { voterId: newId, fingerprint: fp }
}

export function findExistingVote(voters, voterId, fingerprint) {
  if (!voters?.length) return null
  return (
    voters.find(v => v.voterId === voterId) ||
    voters.find(v => v.fingerprint === fingerprint) ||
    voters.find(v => v.odevi === voterId) ||
    null
  )
}
