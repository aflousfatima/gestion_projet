import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    'checks{status:200}': ['rate>0.95'],
  },
};

export default function loadTest() {
  const homeRes = http.get('http://localhost:3000');
  check(homeRes, {
    'home status is 200': (r) => r.status === 200,
    'home page body size > 1000': (r) => r.body && r.body.length > 1000,
  }, { status: 200 }); 

  const signinPageRes = http.get('http://localhost:3000/authentification/signin');
  check(signinPageRes, {
    'signin page status 200': (r) => r.status === 200,
    'signin page contains form': (r) => r.body && r.body.includes('<form'),
  }, { status: 200 }); 

  sleep(1);
}
