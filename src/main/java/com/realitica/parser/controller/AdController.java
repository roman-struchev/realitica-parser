package com.realitica.parser.controller;

import com.realitica.parser.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
public class AdController {
    private final AdRepository adRepository;

    @GetMapping(path = {"/"})
    public ModelAndView load(@RequestParam(name = "type", defaultValue = "Rental") String type) {
        var ads = adRepository.findAllByTypeContainsIgnoreCase(type, Sort.by(Sort.Direction.DESC, "lastModified"));
        var attributes = Map.of("ads", ads);
        return new ModelAndView("ads", attributes);
    }
}
