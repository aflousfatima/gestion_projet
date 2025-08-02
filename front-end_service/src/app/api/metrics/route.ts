import { Registry, collectDefaultMetrics, Counter, Gauge } from 'prom-client';

// Créer un registre pour les métriques
const register = new Registry();
collectDefaultMetrics({ register });

// Métrique pour les erreurs JavaScript
const jsErrorsTotal = new Counter({
  name: 'frontend_js_errors_total',
  help: 'Total number of JavaScript errors in the frontend',
  labelNames: ['page'],
  registers: [register],
});

// Métrique pour le temps de chargement des pages
const pageLoadTimeSeconds = new Gauge({
  name: 'frontend_page_load_time_seconds',
  help: 'Page load time in seconds',
  labelNames: ['page'],
  registers: [register],
});

// Métrique pour les interactions utilisateur
const userInteractionsTotal = new Counter({
  name: 'frontend_user_interactions_total',
  help: 'Total number of user interactions in the frontend',
  labelNames: ['page', 'action'],
  registers: [register],
});

export async function GET() {
  return new Response(await register.metrics(), {
    headers: { 'Content-Type': register.contentType },
  });
}

export async function POST(request: Request) {
  const { type, page, value, action } = await request.json();
  if (type === 'js_error') {
    jsErrorsTotal.inc({ page });
  } else if (type === 'page_load') {
    pageLoadTimeSeconds.set({ page }, value);
  } else if (type === 'user_interaction') {
    userInteractionsTotal.inc({ page, action });
  }
  return new Response(JSON.stringify({ status: 'ok' }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}