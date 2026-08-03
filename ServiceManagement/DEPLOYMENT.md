# Deploying KEYSTONE to a free host

The submission requirements call for live, reachable URLs — not just a working `docker compose`
on your laptop. This doc covers three options. Read the note below first — it affects which
option actually works for you.

## Important: Vercel and Netlify can only host the frontend

Vercel and Netlify are static-site / serverless-function platforms. They **cannot run a Spring
Boot backend** (a long-running JVM process) or **host a MySQL database** — there's no way around
this, it's a fundamental platform limitation, not a configuration issue. If you want to use
Vercel or Netlify, you still need a separate host for the backend + MySQL (Railway, covered
below, is the easiest one that keeps MySQL — Vercel/Netlify's own database offerings are
Postgres/serverless-SQL, not MySQL).

So the realistic setups are:
- **All-in on Railway** (Option 1 below): backend, frontend, and MySQL all on Railway. Simplest —
  one platform, one bill, no cross-origin config to juggle beyond one CORS entry.
- **Frontend on Vercel or Netlify + backend/MySQL on Railway** (Options 2/3 below): if you
  specifically want your frontend on Vercel/Netlify (faster global CDN, or you're already
  familiar with them), deploy the backend to Railway first using Option 1's backend steps, then
  point Vercel/Netlify at that backend's URL.

---

## Option 1: Everything on Railway (recommended — has managed MySQL)

[Railway](https://railway.app) is a good fit here because it has a genuine free tier, deploys
straight from a Dockerfile (which this project already has for both the backend and frontend),
and — unlike most "free Postgres" platforms — offers a **managed MySQL** plugin, so no database
migration is needed to deploy what's in this repo.

This is a step-by-step guide, since deploying isn't something I can do on your behalf from here —
you'll need a Railway account (GitHub sign-in is fastest) and this repo pushed to GitHub first.

### 1. Push this repo to GitHub

```bash
git init
git add .
git commit -m "Project KEYSTONE"
git branch -M main
git remote add origin https://github.com/<you>/keystone.git
git push -u origin main
```

### 2. Create the Railway project and add MySQL

1. [railway.app](https://railway.app) → **New Project** → **Deploy MySQL** (from the template
   gallery, or **Database → Add MySQL**).
2. Once it's provisioned, open the MySQL service → **Variables** tab and note
   `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD` — you'll reference
   these as env vars in the backend service, not hardcode them.

### 3. Deploy the backend

1. In the same Railway project: **New → GitHub Repo** → select this repo.
2. Railway auto-detects the `Dockerfile` at the repo root and builds from it. If it doesn't,
   set **Settings → Build → Dockerfile Path** to `Dockerfile` explicitly.
3. **Settings → Networking → Generate Domain** — this gives you a public HTTPS URL
   (`https://<something>.up.railway.app`) for the API.
4. **Variables** tab — add:
   ```
   DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   JWT_SECRET=<generate a long random string — e.g. `openssl rand -hex 32`>
   CORS_ALLOWED_ORIGINS=https://<your-frontend-domain-from-step-4>
   UPLOAD_DIR=/app/uploads
   PORT=8080
   ```
   Railway's `${{ServiceName.VARIABLE}}` syntax references another service's variables directly,
   so you don't copy/paste secrets between services.
5. Redeploy. Flyway runs automatically on boot and creates the schema + seed data against the
   Railway MySQL instance — no manual migration step needed.
6. **Attachments persistence**: add a **Volume** (Settings → Volumes) mounted at `/app/uploads`
   so uploaded files survive redeploys, same as the `keystone_uploads` Docker volume locally.

Verify: `https://<backend-domain>/swagger-ui.html` should load, and
`https://<backend-domain>/api/auth/login` should accept the seeded demo credentials.

### 4. Deploy the frontend (staying on Railway)

1. **New → GitHub Repo** again, same repo, but set **Settings → Root Directory** to `frontend`
   so Railway builds `frontend/Dockerfile` instead of the backend one.
2. **Variables**: set `VITE_API_BASE_URL` — but note this is a **build-time** variable (baked
   into the static JS bundle by Vite), so setting it after the first deploy requires a redeploy
   to take effect. Two options:
   - Simplest: leave the Dockerfile's default `VITE_API_BASE_URL=/api` and instead edit
     `frontend/nginx.conf` before pushing, changing `proxy_pass http://backend:8080` to your
     Railway backend's public domain (`https://<backend-domain>.up.railway.app`) — this keeps
     the same reverse-proxy-avoids-CORS approach as the local Docker Compose setup.
   - Alternative: set `VITE_API_BASE_URL=https://<backend-domain>.up.railway.app/api` directly
     and go back to the backend's `CORS_ALLOWED_ORIGINS` to include the frontend's Railway
     domain, since the browser will now call the backend cross-origin.
3. **Settings → Networking → Generate Domain** for the frontend too.

### 5. Sanity check the whole thing

- Open the frontend's public URL, log in with a seeded account (`manager@keystone.example` /
  `Password123!`), confirm the dashboard loads real data.
- Open two browser windows (or one normal + one incognito) logged in as a dispatcher and a
  technician; assign a work order in one and confirm the other's "Live" indicator flips a
  notification in real time — this proves the WebSocket path works through Railway's proxy too
  (Railway supports WebSockets natively, no extra config needed).

### Costs / limits to know about

Railway's free tier is usage-metered (a monthly credit, not unlimited), and both services plus
the database will consume it while running — expect it to be enough for grading/demo purposes but
not for leaving the app running 24/7 indefinitely. If you hit the limit, apps sleep rather than
get deleted; a redeploy or the next visit wakes them back up.

### Alternative backend host: Render + PlanetScale/Aiven MySQL

If you'd rather use Render for the app hosting (also free-tier friendly, also deploys from a
Dockerfile), Render itself doesn't offer a managed MySQL — pair it with a free MySQL instance from
[PlanetScale](https://planetscale.com) or [Aiven](https://aiven.io) instead, and point `DB_URL` at
that. Everything else in this guide (env vars, volume for uploads, CORS/nginx setup) applies the
same way.

---

## Option 2: Frontend on Vercel (backend still on Railway)

Deploy the backend first using **Option 1, steps 1–3** above (push to GitHub, create the Railway
MySQL + backend services, generate the backend's public domain). Once you have that backend URL
(e.g. `https://keystone-backend.up.railway.app`), come back here for the frontend.

### 1. Create a Vercel account and import the repo

1. [vercel.com](https://vercel.com) → sign up with GitHub (fastest — it can then read your repos
   directly).
2. **Add New… → Project** → select your `keystone` repo → **Import**.

### 2. Configure the build

Vercel auto-detects Vite projects, but your `frontend/` folder isn't the repo root, so you need
to point it there explicitly:

1. On the import screen, expand **Root Directory** and set it to `frontend`.
2. Framework Preset should auto-fill to **Vite**. If it doesn't, set it manually.
3. Build settings should auto-fill as:
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Install Command: `npm install`

### 3. Set the API URL environment variable

Still on the import/configure screen (or afterwards under **Settings → Environment Variables**):

```
VITE_API_BASE_URL = https://keystone-backend.up.railway.app/api
```

This is a **build-time** variable — Vite bakes it into the static JS bundle. If you add or change
it after the first deploy, you must trigger a redeploy (**Deployments → ⋯ → Redeploy**) for it to
take effect; just saving the variable alone doesn't update an already-built site.

### 4. Deploy

Click **Deploy**. Vercel builds and gives you a URL like `https://keystone-xyz.vercel.app`
immediately, plus automatic redeploys on every push to `main`.

### 5. Fix CORS on the backend

Since the frontend and backend are now on two different domains, the browser will block API calls
until the backend explicitly allows the Vercel origin. Back in Railway, on the **backend**
service's Variables tab, update:

```
CORS_ALLOWED_ORIGINS=https://keystone-xyz.vercel.app
```

(Use your actual Vercel domain — and if you later add a custom domain in Vercel, add that too,
comma-separated.) Redeploy the backend for the change to take effect.

### 6. Handle the WebSocket connection across origins

The live-update WebSocket (`/ws`) also needs to reach the Railway backend directly now, since
there's no nginx reverse-proxy sitting in front of it the way there is in the Docker Compose
setup. This should work automatically — `useWorkOrderSocket.ts` derives the WebSocket URL from
`VITE_API_BASE_URL` by stripping `/api`, so it'll correctly compute
`https://keystone-backend.up.railway.app/ws`. Just confirm in the browser dev tools (Network tab,
filter "WS") that the connection shows **101 Switching Protocols** rather than a CORS error — if
Railway's proxy ever blocks it, `setAllowedOriginPatterns("*")` in `WebSocketConfig.java` is
already permissive enough that it shouldn't be a config issue on the backend side.

### vercel.json (already included in this repo, for client-side routing)

React Router needs every path to fall back to `index.html` (otherwise refreshing on `/work-orders`
gives a 404). Vercel usually handles this automatically for detected SPA frameworks, but this repo
already includes `frontend/vercel.json` just in case:

```json
{
  "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
}
```

---

## Option 3: Frontend on Netlify (backend still on Railway)

Same prerequisite as Option 2: deploy the backend to Railway first (Option 1, steps 1–3) and have
its public URL ready.

### 1. Create a Netlify account and import the repo

1. [app.netlify.com](https://app.netlify.com) → sign up with GitHub.
2. **Add new site → Import an existing project** → **Deploy with GitHub** → select your `keystone`
   repo.

### 2. Configure the build

1. **Base directory**: `frontend`
2. **Build command**: `npm run build`
3. **Publish directory**: `frontend/dist` (Netlify usually needs the full path including the base
   directory here — if it errors, try just `dist` since Netlify sometimes resolves it relative to
   the base directory instead)

### 3. Set the API URL environment variable

Under **Site configuration → Environment variables → Add a variable**:

```
VITE_API_BASE_URL = https://keystone-backend.up.railway.app/api
```

Same build-time caveat as Vercel: changing this after the first deploy requires **Deploys →
Trigger deploy → Clear cache and deploy site** to actually rebuild with the new value.

### 4. Deploy

**Deploy site**. Netlify gives you a URL like `https://keystone-xyz.netlify.app`.

### 5. SPA redirect rule (already included in this repo)

Unlike Vercel, Netlify does **not** auto-detect SPA routing — without a redirect rule, every route
except `/` 404s on refresh. This repo already includes `frontend/public/_redirects` (Vite copies
anything in `public/` straight into `dist/` on build, so it ends up exactly where Netlify expects
it):

```
/*    /index.html   200
```

### 6. Fix CORS on the backend

Same as the Vercel case — add the Netlify domain to the backend's allowed origins in Railway:

```
CORS_ALLOWED_ORIGINS=https://keystone-xyz.netlify.app
```

Redeploy the backend.

### 7. WebSocket check

Same note as Option 2 — no config changes needed on Netlify's side, just verify in dev tools that
the `/ws` connection upgrades successfully rather than failing on CORS.

---

## Quick comparison

| | Backend + MySQL | Frontend | Best for |
|---|---|---|---|
| **Option 1: All Railway** | ✓ | ✓ | Simplest — one dashboard, minimal CORS config |
| **Option 2: Vercel + Railway** | Railway | Vercel | Familiar with Vercel, want its CDN/edge network for the frontend |
| **Option 3: Netlify + Railway** | Railway | Netlify | Familiar with Netlify, want its build/redirect tooling |

Whichever you pick, the submission checklist is the same: live backend URL, live frontend URL,
Swagger UI reachable, seeded demo logins working end to end.
