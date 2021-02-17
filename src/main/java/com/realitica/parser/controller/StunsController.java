package com.realitica.parser.controller;

import com.realitica.parser.entity.Stun;
import com.realitica.parser.repo.StunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StunsController {
    @Autowired
    StunRepository stunRepository;

    @GetMapping(path = "stuns")
    public ModelAndView stuns() {
        List<Stun> stunsList = stunRepository.findAll();
        Map attributes = new HashMap<>();
        attributes.put("stuns", stunsList);
        return new ModelAndView("stuns", attributes);
    }
}
