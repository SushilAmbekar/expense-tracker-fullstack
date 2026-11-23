# Expense Tracker – Angular frontend

Angular 20 (standalone components, signals), Angular Material and Chart.js.

| Command | What it does |
| --- | --- |
| `npm start` | Dev server on http://localhost:4200, proxying `/api` to the Spring Boot backend on port 8080 |
| `npm run start:demo` | Runs without a backend: the API is answered in the browser with sample data |
| `npm test` | Unit tests (Karma + Jasmine) |
| `npm run build` | Production build in `dist/frontend/browser` |

## Structure

```
src/app
├── core/        API client, JWT auth service, interceptor and guards, models
├── layout/      App shell: side navigation and toolbar
├── pages/       auth, dashboard, expenses, budgets, categories
├── shared/      Chart.js wrapper, rupee pipe, month switcher, confirm dialog
└── demo/        In-browser copy of the API used by the GitHub Pages demo
```

See the [main README](../README.md) for the full project.
