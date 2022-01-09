package com.realitica.parser.controller;

import com.realitica.parser.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
public class AdController {
    private final AdRepository adRepository;

    @GetMapping(path = {"/", "/stuns"})
    public ModelAndView ad(@RequestHeader String host,
                              @RequestParam(name = "redirect", defaultValue = "true") Boolean redirect) {
        // 302 redirect from deprecated heroku host
        if (redirect != null && redirect && host.contains("heroku")) {
            return new ModelAndView("redirect:http://realitica.struchev.site");
        }

        var stunsList = adRepository.findAll(Sort.by(Sort.Direction.DESC, "lastModified"));
        var attributes = Map.of("ads", stunsList);
        return new ModelAndView("ads", attributes);
    }
}
