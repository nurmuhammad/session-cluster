@import org.springframework.security.web.csrf.CsrfToken
@param boolean error
@param boolean logout
@param CsrfToken csrf
@template.layout(title = "Kirish", content = @`
    <h1>Tizimga kirish</h1>
    <p class="muted">Hazelcast session-cluster demo</p>

    @if(error)
        <div class="alert alert-err">Login yoki parol noto'g'ri.</div>
    @endif
    @if(logout)
        <div class="alert alert-ok">Tizimdan chiqdingiz.</div>
    @endif

    <form method="post" action="/login">
        @if(csrf != null)
            <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
        @endif
        <label for="username">Foydalanuvchi</label>
        <input type="text" id="username" name="username" value="admin" autofocus>

        <label for="password">Parol</label>
        <input type="password" id="password" name="password" value="admin123">

        <button type="submit">Kirish</button>
    </form>
    <p class="muted" style="margin-top:16px">Demo login: <b>admin / admin123</b></p>
`)
