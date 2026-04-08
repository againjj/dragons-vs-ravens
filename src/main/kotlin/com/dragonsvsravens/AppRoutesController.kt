package com.dragonsvsravens

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AppRoutesController {
    @GetMapping("/g/{gameId:[A-Za-z0-9]+}")
    fun gameRoute(): String = "forward:/index.html"
}
