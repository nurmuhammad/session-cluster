@import gg.jte.Content
@param String title
@param gg.jte.Content content
<!DOCTYPE html>
<html lang="uz">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <!-- AlpineJS (CDN) -->
    <script src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
            background: #0f172a;
            color: #e2e8f0;
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .card {
            background: #1e293b;
            border: 1px solid #334155;
            border-radius: 14px;
            padding: 32px;
            width: 100%;
            max-width: 440px;
            box-shadow: 0 10px 40px rgba(0,0,0,.4);
        }
        h1 { margin: 0 0 4px; font-size: 22px; }
        .muted { color: #94a3b8; font-size: 14px; margin-top: 0; }
        label { display: block; font-size: 13px; margin: 14px 0 6px; color: #cbd5e1; }
        input[type=text], input[type=password] {
            width: 100%; padding: 10px 12px; border-radius: 8px;
            border: 1px solid #475569; background: #0f172a; color: #e2e8f0;
        }
        button {
            margin-top: 18px; width: 100%; padding: 11px; border: 0;
            border-radius: 8px; background: #3b82f6; color: white;
            font-weight: 600; cursor: pointer; font-size: 15px;
        }
        button:hover { background: #2563eb; }
        .btn-sm { width: auto; padding: 9px 16px; margin-top: 0; }
        .alert { padding: 10px 12px; border-radius: 8px; font-size: 14px; margin-bottom: 8px; }
        .alert-err { background: #7f1d1d; color: #fecaca; }
        .alert-ok { background: #14532d; color: #bbf7d0; }
        .row { display: flex; justify-content: space-between; align-items: center;
               padding: 10px 0; border-bottom: 1px solid #334155; }
        .row:last-child { border-bottom: 0; }
        .k { color: #94a3b8; font-size: 13px; }
        .v { font-weight: 600; font-family: ui-monospace, monospace; font-size: 13px; }
        .pill { background: #1d4ed8; color: #dbeafe; padding: 2px 10px; border-radius: 999px; font-size: 12px; }
        .logout { display:inline-block; margin-top: 18px; color: #94a3b8; font-size: 13px; text-decoration: none; }
        .logout:hover { color: #e2e8f0; }
    </style>
</head>
<body>
    <div class="card">
        ${content}
    </div>
</body>
</html>
