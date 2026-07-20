package com.example.demo.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Дашборд саҳифаси ва {@code /api/ping} endpoint'и.
 *
 * <p>Ҳолат (кўришлар сони, ping сони) {@link HttpSession}'га ёзилади — уни Spring
 * Session автоматик Hazelcast'га узатади. Шунинг учун сўров app1'га ҳам, app2'га
 * ҳам тушса, ҳисоб сақланиб қолади; айнан шу нарса session clustering'ни кўрсатади.
 */
@Controller
public class PageController {

    /** Сўровга қайси app нусхаси жавоб бераётганини аниқлайди ({@code INSTANCE_NAME} env, бўлмаса hostname). */
    private String instanceName() {
        String fromEnv = System.getenv("INSTANCE_NAME");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /**
     * Логин саҳифасини рендер қилади ({@code login.jte}).
     *
     * <p>CSRF token'ни аниқ моделга қўшамиз, чунки шаблон уни яширин {@code input}
     * сифатида чиқариши керак — усиз {@code POST /login} 403 (Invalid CSRF) беради.
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("error", error != null);
        model.addAttribute("logout", logout != null);
        model.addAttribute("csrf", (CsrfToken) request.getAttribute(CsrfToken.class.getName()));
        return "login";
    }

    /**
     * Дашбордни кўрсатади ва ҳар киришда сессиядаги {@code visits}'ни оширади.
     *
     * <p>{@code visits}'ни {@link HttpSession}'га ёзамиз — қиймат Hazelcast'га
     * боради ва хоҳлаган нусха уни ўқийди. Саҳифани F5 билан янгилаганда LB сизни
     * гоҳ app1, гоҳ app2'га юборса ҳам, ҳисоб ўсиб бораверади.
     */
    @GetMapping("/")
    public String dashboard(HttpServletRequest request,
                            HttpSession session,
                            Authentication authentication,
                            Model model) {
        Integer visits = (Integer) session.getAttribute("visits");
        visits = (visits == null) ? 1 : visits + 1;
        session.setAttribute("visits", visits);

        model.addAttribute("username", authentication.getName());
        model.addAttribute("instance", instanceName());
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("visits", visits);
        model.addAttribute("csrf", (CsrfToken) request.getAttribute(CsrfToken.class.getName()));
        return "dashboard";
    }

    /**
     * AlpineJS чақирадиган ping endpoint'и: ҳар чақирувда сессиядаги {@code clicks}
     * ошади.
     *
     * <p>LB сўровни турли нусхаларга юборса ҳам {@code clicks} сақланади, чунки у
     * сессияда — яъни Hazelcast'да. Жавоб {@code @ResponseBody} орқали JSON бўлиб қайтади.
     */
    @PostMapping("/api/ping")
    @ResponseBody
    public Map<String, Object> ping(HttpSession session) {
        Integer clicks = (Integer) session.getAttribute("clicks");
        clicks = (clicks == null) ? 1 : clicks + 1;
        session.setAttribute("clicks", clicks);

        Map<String, Object> res = new HashMap<>();
        res.put("instance", instanceName());
        res.put("clicks", clicks);
        res.put("sessionId", session.getId());
        return res;
    }
}
