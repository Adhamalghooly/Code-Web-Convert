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

### المشاكل المحلولة (آخر جلسة)
- **توقف الاستيراد عند قاعدة بيانات كبيرة (23+ ملف):** كانت `importBackupFiles` تقرأ كل الملفات في الذاكرة دفعةً واحدة → نفاد RAM. الحل: معالجة الملفات تباعًا مع تحرير ذاكرة كل ملف قبل قراءة التالي.
- **إشعار الترحيل لا يظهر:** كان `showNotification('تم تحميل النظام...')` يُلغي إشعار الترحيل مباشرةً بعده. الحل: حفظ إشعار الترحيل في `_pendingMigrationNotification` وعرضه بعد اكتمال التهيئة.
- **Race condition في `initIndexedDB`:** أُضيف قفل mutex (`_dbInitPromise`) يمنع فتح الاتصال مرتين متزامنتين.
- **`node_modules` و`capacitor-cordova-android-plugins` مفقودان:** يُعاد إنشاؤهما أثناء البناء (انظر الخطوات أدناه).

### كيفية بناء APK جديد
```bash
# 1. تأكد من Android SDK في:
#    /home/runner/workspace/android-sdk  (يُنشأ مرة واحدة لكل بيئة)

# 2. تثبيت التبعيات (إن لم تكن موجودة):
cd apk-build && npm install --legacy-peer-deps && cd ..

# 3. أنشئ capacitor-cordova-android-plugins يدوياً (مُستبعَد من git):
mkdir -p apk-build/android/capacitor-cordova-android-plugins/src/main
# -- cordova.variables.gradle --
cat > apk-build/android/capacitor-cordova-android-plugins/cordova.variables.gradle << 'EOF'
ext {
    minSdkVersion = 22; compileSdkVersion = 34; targetSdkVersion = 34
    androidxActivityVersion = '1.7.0'; androidxAppCompatVersion = '1.6.1'
    androidxCoreVersion = '1.10.0'; androidxFragmentVersion = '1.5.7'
}
EOF
# -- build.gradle --
cat > apk-build/android/capacitor-cordova-android-plugins/build.gradle << 'EOF'
apply plugin: 'com.android.library'
android {
    namespace "capacitor.cordova.android.plugins"
    compileSdkVersion 34
    defaultConfig { minSdkVersion 22; targetSdkVersion 34 }
}
dependencies {}
EOF
echo '<manifest xmlns:android="http://schemas.android.com/apk/res/android"></manifest>' \
  > apk-build/android/capacitor-cordova-android-plugins/src/main/AndroidManifest.xml

# 4. أنشئ ملفات assets:
mkdir -p apk-build/android/app/src/main/assets/public
echo '[{"pkg":"@capacitor-community/sqlite","classpath":"com.getcapacitor.community.database.sqlite.CapacitorSQLitePlugin"}]' \
  > apk-build/android/app/src/main/assets/capacitor.plugins.json
cp apk-build/capacitor.config.json apk-build/android/app/src/main/assets/capacitor.config.json
cp -r apk-build/www/. apk-build/android/app/src/main/assets/public/

# 5. بناء الـAPK:
export ANDROID_HOME=/home/runner/workspace/android-sdk
cd apk-build/android && ./gradlew assembleDebug --no-daemon

# 6. نسخ المخرج:
cp apk-build/android/app/build/outputs/apk/debug/app-debug.apk project-management-apk-sqlite.apk
```

### ملاحظات هامة
- `@capacitor/cli` **محذوف** من devDependencies عمداً (CVE في tar@6.2.1).
- `capacitor-cordova-android-plugins/` و`node_modules/` مُستبعَدان من git — يُعادان إنشاؤهما عند كل بناء.
- الـAPK المُسلَّم: `project-management-apk-sqlite.apk` في جذر المشروع.

### اختبار بعد التثبيت
1. **لا تمسح** بيانات التطبيق القديم قبل أول تشغيل (الترحيل يحتاج IndexedDB القديم).
2. عند أول فتح: يجب ظهور إشعار **"✅ تم ترحيل بياناتك..."**.
3. بعد 7 ثوانٍ يظهر **"تم تحميل النظام بنجاح"** — هذا طبيعي.
4. استيراد نسخة احتياطية (23 ملف أو أكثر) يجب أن **لا** يُوقف التطبيق.
5. للتشخيص المتقدم: `chrome://inspect#devices` → Console.
