# Workspace

## Overview

pnpm workspace monorepo using TypeScript. Each package manages its own dependencies.

## Stack

- **Monorepo tool**: pnpm workspaces
- **Node.js version**: 24
- **Package manager**: pnpm
- **TypeScript version**: 5.9
- **API framework**: Express 5
- **Database**: PostgreSQL + Drizzle ORM
- **Validation**: Zod (`zod/v4`), `drizzle-zod`
- **API codegen**: Orval (from OpenAPI spec)
- **Build**: esbuild (CJS bundle)

## Key Commands

- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- `pnpm --filter @workspace/api-server run dev` — run API server locally

See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.

## Artifacts

### نظام تحليل بيانات المشاريع (`artifacts/project-management`)

- **Type**: Static web app (Vite dev server serving plain HTML/CSS/JS)
- **Preview path**: `/`
- **Port**: 25074
- **Source**: Converted from a 27,172-line single HTML file
- **Architecture**: The app is fully client-side — no backend needed.
  - `index.html` — main HTML shell with all CDN scripts (XLSX, Chart.js, jsPDF, Leaflet, idb)
  - `public/app.css` — extracted CSS (~2,904 lines)
  - `public/app.js` — extracted JavaScript (~22,294 lines)

### App Sections (16 main modules)
1. إدارة البيانات — Excel upload, date tracking, backup/restore
2. التقارير — 15+ report types with Excel export
3. لوحة المؤشرات — KPI dashboard with project table
4. التحليلات المرئية — S-Curve, charts, branch comparison
5. تحليل الأداء — Historical charts, KPI trends
6. تفاصيل المشروع — Project search, history, EVM per project
7. مؤشرات EVM — Project-level EVM analytics
8. مركز الإحصائيات — Smart stats dashboard with gauges
9. الزمن الحرج — Critical Path Method (CPM)
10. مؤشرات الأداء الخاصة — 7 branch-level custom KPIs
11. الخطة الزمنية — Gantt chart, deviation tracking
12. إدارة المواقع — Leaflet map, geo-coordinates
13. وضع الجدولة — Execution mode per project type
14. الجدولة التلقائية — Classic auto-scheduling engine
15. جدولة المسارات — Stream-based geographic scheduling
16. التوصيات الذكية — Unified smart recommendations with What-If simulation

### CDN Libraries Used
- **xlsx** 0.18.5 — Excel file reading
- **xlsx-js-style** 1.2.0 — Excel export with styles
- **chart.js** — All charts
- **jsPDF** 2.5.1 + autotable 3.5.25 — PDF export
- **idb** 7 — IndexedDB wrapper for local storage
- **Leaflet** 1.9.4 — Interactive maps

### Workflow Fix
The workflow uses port 25074. The `restart_workflow` tool may fail because it checks via IPv6 (::1).
Use `restartWorkflow({ workflowName: "artifacts/project-management: web" })` via code_execution instead.

## APK Build (`apk-build/`)

نسخة Capacitor 6 + SQLite من التطبيق كـAPK أندرويد.

### المشكلة المحلولة
- ملف `android/app/src/main/assets/capacitor.plugins.json` كان مفقوداً لأن `cap sync android` لم تُنفَّذ.
- `@capacitor/cli` محظور بسبب تبعية `tar@6.2.1` (CVE).

### كيفية بناء APK جديد
```bash
# 1. تأكد من Android SDK في:
#    /home/runner/workspace/android-sdk  (يُنشأ مرة واحدة لكل بيئة)

# 2. أنشئ ملفات cap sync يدوياً:
mkdir -p apk-build/android/app/src/main/assets/public
echo '[{"pkg":"@capacitor-community/sqlite","classpath":"com.getcapacitor.community.database.sqlite.CapacitorSQLitePlugin"}]' \
  > apk-build/android/app/src/main/assets/capacitor.plugins.json
cp apk-build/capacitor.config.json apk-build/android/app/src/main/assets/capacitor.config.json
cp -r apk-build/www/. apk-build/android/app/src/main/assets/public/

# 3. بناء الـAPK:
export ANDROID_HOME=/home/runner/workspace/android-sdk
cd apk-build/android && ./gradlew assembleDebug --no-daemon

# 4. المخرج:
# apk-build/android/app/build/outputs/apk/debug/app-debug.apk
```

### ملاحظات هامة
- `@capacitor/cli` **محذوف** من devDependencies عمداً (CVE في tar@6.2.1).
- `cordova.variables.gradle` يجب أن يكون موجوداً في `android/capacitor-cordova-android-plugins/`.
- الـAPK المُسلَّم: `project-management-apk-sqlite.apk` في جذر المشروع.
