@import org.springframework.security.web.csrf.CsrfToken
@param String username
@param String instance
@param String sessionId
@param int visits
@param CsrfToken csrf
@template.layout(title = "Dashboard", content = @`
    <h1>Salom, ${username} 👋</h1>
    <p class="muted">Sessiya Hazelcast'da saqlanmoqda — logout bo'lmaysiz.</p>

    <div class="row">
        <span class="k">Bu so'rovga javob bergan instance</span>
        <span class="pill">${instance}</span>
    </div>
    <div class="row">
        <span class="k">Session ID</span>
        <span class="v">${sessionId.length() > 16 ? sessionId.substring(0, 16) + "…" : sessionId}</span>
    </div>
    <div class="row">
        <span class="k">Sahifa ochilishi (session'da)</span>
        <span class="v">${visits}</span>
    </div>

    <!-- AlpineJS: /api/ping ni chaqiradi. LB har safar boshqa instance'ga
         yuborishi mumkin, lekin "clicks" hisobi sessiyada saqlanib qolaveradi. -->
    <div x-data="pinger()" style="margin-top:20px">
        <div class="row">
            <span class="k">Oxirgi ping'ga javob bergan instance</span>
            <span class="pill" x-text="lastInstance || '—'"></span>
        </div>
        <div class="row">
            <span class="k">Ping hisobi (session'da)</span>
            <span class="v" x-text="clicks"></span>
        </div>
        <button class="btn-sm" style="margin-top:16px" @@click="ping()">Ping yuborish</button>
        <span class="muted" style="margin-left:10px" x-text="status"></span>
    </div>

    <a class="logout" href="#" onclick="document.getElementById('logoutForm').submit();return false;">
        ← Tizimdan chiqish
    </a>
    <form id="logoutForm" method="post" action="/logout" style="display:none">
        @if(csrf != null)
            <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
        @endif
    </form>

    <script>
        function pinger() {
            return {
                clicks: 0,
                lastInstance: '',
                status: '',
                async ping() {
                    this.status = 'yuborilmoqda...';
                    const res = await fetch('/api/ping', {
                        method: 'POST',
                        headers: {
                            @if(csrf != null)
                            '${csrf.getHeaderName()}': '${csrf.getToken()}',
                            @endif
                            'Content-Type': 'application/json'
                        }
                    });
                    const data = await res.json();
                    this.clicks = data.clicks;
                    this.lastInstance = data.instance;
                    this.status = '';
                }
            }
        }
    </script>
`)
