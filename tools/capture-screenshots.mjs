// Captures the screenshots used by the presentation (presentation/assets/*.png).
//
// OPTIONAL TOOL. It needs a puppeteer installed OUTSIDE this repository (it is not a project
// dependency, D-040 / D-051), for example:
//
//   npm install --prefix ~/.cache/wastems-shots puppeteer
//   PUPPETEER_DIR=~/.cache/wastems-shots node tools/capture-screenshots.mjs BASE_URL OUT_DIR
//
// BASE_URL is a Vite dev server (for example http://localhost:5174) whose /api proxy points at a
// `dev`-profile backend with the MN01 demonstration loaded. Use a scratch database: the script is
// read-only except for (a) creating one canonical experiment batch when none exists (the
// evaluation screenshots need one) and (b) toggling UI state such as the traffic switch. It never
// accepts a proposal, starts, completes or cancels a tour.
//
// It writes 12 PNG files (1440x900 viewport) to OUT_DIR and exits 1 if one screenshot failed or no
// OpenStreetMap tile was loaded.
import { createRequire } from 'node:module'
import fs from 'node:fs'
import path from 'node:path'

if (!process.env.PUPPETEER_DIR) {
  console.error('PUPPETEER_DIR is not set: point it to a directory whose node_modules contains puppeteer.')
  console.error('Example: npm install --prefix ~/.cache/wastems-shots puppeteer')
  process.exit(2)
}
const [base, out] = process.argv.slice(2)
if (!base || !out) {
  console.error('usage: PUPPETEER_DIR=... node tools/capture-screenshots.mjs BASE_URL OUT_DIR')
  process.exit(2)
}
const require = createRequire(path.join(process.env.PUPPETEER_DIR, 'package.json'))
const puppeteer = require('puppeteer')
fs.mkdirSync(out, { recursive: true })

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] })
const failures = []
let tilesOk = 0
let tilesFailed = 0

async function newPage(contextPage) {
  const page = contextPage ?? (await (await browser.createBrowserContext()).newPage())
  await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 })
  page.on('response', (response) => {
    if (response.url().includes('tile.openstreetmap.org')) {
      if (response.ok()) tilesOk += 1
      else tilesFailed += 1
    }
  })
  return page
}

async function login(page, user, pass) {
  await page.goto(`${base}/login`, { waitUntil: 'networkidle0' })
  await page.type('#username', user)
  await page.type('#password', pass)
  await Promise.all([
    page.click('button[type=submit]'),
    page.waitForNavigation({ waitUntil: 'networkidle0' }).catch(() => {}),
  ])
}

async function session(user, pass) {
  const page = await newPage()
  await login(page, user, pass)
  return page
}

async function settle(page) {
  await page.waitForNetworkIdle({ idleTime: 500, timeout: 20000 }).catch(() => {})
  await page
    .waitForFunction(
      () => [...document.querySelectorAll('img.leaflet-tile')].every((img) => img.complete),
      { timeout: 15000 },
    )
    .catch(() => {})
  await sleep(800)
}

async function open(page, route) {
  await page.goto(`${base}${route}`, { waitUntil: 'networkidle0' })
  await settle(page)
}

async function api(page, url, init) {
  return page.evaluate(
    async (u, i) => {
      const readToken = () => {
        const found = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))
        return found ? decodeURIComponent(found.slice('XSRF-TOKEN='.length)) : undefined
      }
      const headers = {}
      if (i && i.method && i.method !== 'GET') {
        if (!readToken()) await fetch('/api/auth/csrf')
        headers['X-XSRF-TOKEN'] = readToken()
        headers['Content-Type'] = 'application/json'
      }
      const response = await fetch(u, {
        method: i?.method ?? 'GET',
        headers,
        body: i?.body ? JSON.stringify(i.body) : undefined,
      })
      if (!response.ok) throw new Error(`${u} -> ${response.status}`)
      return response.json()
    },
    url,
    init,
  )
}

async function shot(page, name, options = {}) {
  try {
    await page.screenshot({ path: path.join(out, name), ...options })
    console.log(`ok   ${name}`)
  } catch (error) {
    failures.push(name)
    console.error(`FAIL ${name}: ${error.message}`)
  }
}

async function cardShot(page, title, name) {
  const handle = await page.evaluateHandle((wanted) => {
    for (const card of document.querySelectorAll('[data-slot="card"]')) {
      const heading = card.querySelector('[data-slot="card-title"]') ?? card.querySelector('h1,h2,h3')
      if (heading && heading.textContent.trim() === wanted) return card
    }
    return null
  }, title)
  const element = handle.asElement()
  if (!element) {
    console.warn(`warning: card "${title}" not found, taking a full-page shot for ${name}`)
    await shot(page, name, { fullPage: true })
    return
  }
  await element.evaluate((el) => el.scrollIntoView({ block: 'center' }))
  await settle(page)
  try {
    await element.screenshot({ path: path.join(out, name) })
    console.log(`ok   ${name}`)
  } catch (error) {
    failures.push(name)
    console.error(`FAIL ${name}: ${error.message}`)
  }
}

try {
  // 01: login page (no session)
  const anonymous = await newPage()
  await open(anonymous, '/login')
  await shot(anonymous, '01-login.png')

  // planner
  const planner = await session('planner', 'planner123')
  await open(planner, '/collection-points')
  await shot(planner, '02-collection-points-map.png')

  const draftTours = await api(planner, '/api/tours?status=DRAFT')
  const drafts = (Array.isArray(draftTours) ? draftTours : draftTours.items ?? [])
    .filter((t) => t.status === 'DRAFT')
    .sort((a, b) => String(a.planningDate).localeCompare(String(b.planningDate)))
  if (!drafts.length) throw new Error('no DRAFT tour found: load the dev demonstration first')
  await open(planner, `/tours/${drafts[0].id}`)
  await cardShot(planner, 'Route evaluation', '03-tour-route-evaluation.png')
  await planner.evaluate(() => window.scrollTo(0, 0))
  await settle(planner)
  await shot(planner, '04-tour-planner-overview.png')

  // agent
  const agent = await session('agent', 'agent123')
  const mine = await api(agent, '/api/tours/mine')
  const running = (Array.isArray(mine) ? mine : mine.items ?? []).find((t) => t.status === 'IN_PROGRESS')
  if (!running) throw new Error('no IN_PROGRESS tour for the agent: load the dev demonstration first')
  await open(agent, `/my-tours/${running.id}`)
  await shot(agent, '05-agent-tour-in-progress.png', { fullPage: true })

  // planner again
  await open(planner, '/dashboard')
  await shot(planner, '06-dashboard.png')

  // admin: evaluation batch
  const admin = await session('admin', 'admin123')
  let batches = await api(admin, '/api/evaluations/batches')
  let canonical = batches.filter((b) => b.canonical)
  if (!canonical.length) {
    console.log('no canonical batch yet: running the experiment (about 40 s)')
    await api(admin, '/api/admin/experiments', { method: 'POST', body: { label: 'Canonical campaign (screenshots)' } })
    batches = await api(admin, '/api/evaluations/batches')
    canonical = batches.filter((b) => b.canonical)
  }
  canonical.sort((a, b) => String(b.createdAt ?? '').localeCompare(String(a.createdAt ?? '')))
  const batch = canonical[0]
  if (!batch) throw new Error('no canonical batch available')
  await open(admin, `/evaluations?batch=${batch.id}`)
  await shot(admin, '07-evaluation-batch.png')
  await shot(admin, '07b-evaluation-batch-full.png', { fullPage: true })

  const runs = await api(
    admin,
    `/api/evaluations?kind=EXPERIMENT&batchId=${batch.id}&pointCount=25&page=0&size=200`,
  )
  const run = runs.items.find((r) => r.scenarioKey === 'mn01-UNIFORM_LOW-s1-n25' && r.repetition === 1)
  if (!run) throw new Error('run mn01-UNIFORM_LOW-s1-n25 (repetition 1) not found in the batch')
  await open(admin, `/evaluations/${run.id}`)
  await shot(admin, '08-evaluation-run-routes.png', { fullPage: true })

  await open(planner, '/history')
  await shot(planner, '09-history.png')

  await open(admin, '/admin/audit')
  await shot(admin, '10-audit-log.png')

  // traffic overlay (display only)
  await open(planner, '/collection-points')
  await planner.click('#traffic-switch')
  await settle(planner)
  await shot(planner, '11-traffic-overlay.png')
} catch (error) {
  failures.push(error.message)
  console.error(`error: ${error.message}`)
}

await browser.close()
console.log(`OSM tiles: ${tilesOk} ok, ${tilesFailed} failed`)
if (failures.length || tilesOk === 0) {
  console.error(`failures: ${failures.length ? failures.join('; ') : 'no OSM tile loaded'}`)
  process.exit(1)
}
