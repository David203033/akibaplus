package com.akibaplus.saccos.akibaplus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/")
    public String root() {
        return "landing";
    }
}
