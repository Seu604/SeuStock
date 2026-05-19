package com.seu.seustock.controller;

import com.seu.seustock.model.dto.BoxDTO;
import com.seu.seustock.model.form.BoxForm;
import com.seu.seustock.service.BoxService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/new")
    public String newModal(@PathVariable UUID spaceExternalId,
                           @PathVariable UUID shelfExternalId,
                           Model model) {
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("form", new BoxForm());
        return "boxes/fragments/modal :: modal";
    }

    @PostMapping("/shelves/{shelfExternalId}/boxes")
    public String create(@PathVariable UUID shelfExternalId,
                         @RequestParam UUID spaceExternalId,
                         @Valid @ModelAttribute("form") BoxForm form,
                         BindingResult result,
                         HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceExternalId", spaceExternalId);
            model.addAttribute("shelfExternalId", shelfExternalId);
            return "boxes/fragments/modal :: modal";
        }
        BoxDTO box = boxService.create(spaceExternalId, shelfExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("box", box);
        return "boxes/fragments/created :: created";
    }

    @DeleteMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}")
    @ResponseBody
    public String delete(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         @PathVariable UUID boxExternalId,
                         HttpSession session) {
        boxService.delete(spaceExternalId, shelfExternalId, boxExternalId,
                (String) session.getAttribute("loginUser"));
        return "";
    }
}
