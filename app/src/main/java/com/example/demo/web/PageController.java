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

@Controller
public class PageController {

    /** Qaysi app instance so'rovga javob berayotganini aniqlaydi. */
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

    @GetMapping("/")
    public String dashboard(HttpServletRequest request,
                            HttpSession session,
                            Authentication authentication,
                            Model model) {
        // Sessiyaga yozamiz -> bu qiymat Hazelcast'ga boradi va barcha instance ko'radi
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
     * AlpineJS shu endpoint'ni chaqiradi. Har chaqiruvda sessiyadagi "clicks"
     * oshadi. LB so'rovni turli instance'larga yuborsa ham, hisob saqlanadi.
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
