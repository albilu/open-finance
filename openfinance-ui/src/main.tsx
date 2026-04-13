import { StrictMode, Suspense } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import 'flag-icons/css/flag-icons.min.css'
import './i18n.ts' // Initialize i18n before any component render
import App from './App.tsx'
import reportWebVitals from './utils/reportWebVitals.ts'
import { registerServiceWorker } from './utils/serviceWorkerRegistration.ts'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Suspense fallback={<div className="flex items-center justify-center h-screen">Loading...</div>}>
      <App />
    </Suspense>
  </StrictMode>,
)

// In development, log Core Web Vitals to the console
if (import.meta.env.DEV) {
  reportWebVitals(console.log);
}

// Register service worker in production for offline caching support
registerServiceWorker();
