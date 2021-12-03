package com.realitica.parser.controller;

import com.realitica.parser.entity.Stun;
import com.realitica.parser.repo.StunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
public class StunsController {
    private final StunRepository stunRepository;

    @GetMapping(path = "stuns")
    public ModelAndView stuns(@RequestHeader String host,
                              @RequestParam(name = "redirect", defaultValue = "false") Boolean redirect) {
        // 302 redirect from deprecated domaim
        if (redirect != null && redirect && host.contains("heroku")) {
            log.info("Request from {}, redirect to realitica.struchev.site", host);
            return new ModelAndView("redirect:/realitica.struchev.site");
        } else {
            log.info("Request from {}, use this host", host);
        }

        var stunsList = stunRepository.findAll(Sort.by(Sort.Direction.DESC, "lastModified"));
        var attributes = new HashMap<String, Object>();
        attributes.put("stuns", stunsList);
        return new ModelAndView("stuns", attributes);
    }
}
