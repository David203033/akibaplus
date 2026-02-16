package com.akibaplus.saccos.akibaplus.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class GlobalErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object message = request.getAttribute("jakarta.servlet.error.message");
        Object exception = request.getAttribute("jakarta.servlet.error.exception");

        int statusCode = status != null ? (int) status : 500;
        
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("message", message != null ? message : "An unexpected error occurred");
        model.addAttribute("exception", exception != null ? exception.toString() : null);

        if (statusCode == 404) {
            return "error-404";
        } else if (statusCode == 403) {
            return "error-403";
        } else {
            return "error";
        }
    }

    @GetMapping("/error/500")
    public String error500(Model model) {
        model.addAttribute("statusCode", 500);
        model.addAttribute("message", "Internal Server Error");
        return "error";
    }

    @GetMapping("/error/404")
    public String error404(Model model) {
        model.addAttribute("statusCode", 404);
        model.addAttribute("message", "Page Not Found");
        return "error-404";
    }
}
