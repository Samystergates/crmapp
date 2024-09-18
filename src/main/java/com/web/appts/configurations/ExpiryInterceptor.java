
package com.web.appts.configurations;

import java.time.LocalDate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ExpiryInterceptor implements HandlerInterceptor {
    private LocalDate allowedEndDate;
    private String activationKey;

    public ExpiryInterceptor(LocalDate allowedEndDate, String activationKey) {
        this.allowedEndDate = allowedEndDate;
        this.activationKey = activationKey;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LocalDate currentDate = LocalDate.now();
        String key = request.getParameter("key");
        boolean isKeyValid = this.activationKey.equals(key);
        if (!isKeyValid && currentDate.isAfter(this.allowedEndDate)) {
            response.getWriter().write("The application has expired. Please contact the administrator to extend the access.");
            response.setStatus(403);
            return false;
        } else {
            return true;
        }
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
