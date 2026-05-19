package com.seu.seustock.controller;

import com.seu.seustock.model.dto.ShelfDTO;
import com.seu.seustock.model.form.ShelfForm;
import com.seu.seustock.service.BoxService;
import com.seu.seustock.service.ShelfService;
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
public class ShelfController {

    private final ShelfService shelfService;
    private final BoxService boxService;

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes")
    public String boxList(@PathVariable UUID spaceExternalId,
                          @PathVariable UUID shelfExternalId,
                          HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelf", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username));
        model.addAttribute("boxes", boxService.findAllByShelfId(spaceExternalId, shelfExternalId, username));
        return "shelves/fragments/box-list :: box-list";
    }

    @GetMapping("/spaces/{spaceExternalId}/shelves/new")
    public String newModal(@PathVariable UUID spaceExternalId, Model model) {
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("form", new ShelfForm());
        return "shelves/fragments/modal :: modal";
    }

    @PostMapping("/spaces/{spaceExternalId}/shelves")
    public String create(@PathVariable UUID spaceExternalId,
                         @Valid @ModelAttribute("form") ShelfForm form,
                         BindingResult result,
                         HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceExternalId", spaceExternalId);
            return "shelves/fragments/modal :: modal";
        }
        ShelfDTO shelf = shelfService.create(spaceExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelf", shelf);
        return "shelves/fragments/created :: created";
    }

    @DeleteMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}")
    @ResponseBody
    public String delete(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         HttpSession session) {
        shelfService.delete(spaceExternalId, shelfExternalId, (String) session.getAttribute("loginUser"));
        return "";
    }
}
