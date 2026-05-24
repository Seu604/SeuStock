package com.seu.seustock.controller;

import com.seu.seustock.model.form.BoxForm;
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
public class BoxController {

    private final BoxService boxService;
    private final ShelfService shelfService;

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}/edit")
    public String editModal(@PathVariable UUID spaceExternalId,
                            @PathVariable UUID shelfExternalId,
                            @PathVariable UUID boxExternalId,
                            HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        var box = boxService.findByExternalId(spaceExternalId, shelfExternalId, boxExternalId, username);
        BoxForm form = new BoxForm();
        form.setName(box.getName());
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("form", form);
        return "boxes/fragments/modal :: edit-modal";
    }

    @PatchMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}")
    public String rename(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         @PathVariable UUID boxExternalId,
                         @Valid @ModelAttribute("form") BoxForm form,
                         BindingResult result,
                         HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceExternalId", spaceExternalId);
            model.addAttribute("shelfExternalId", shelfExternalId);
            model.addAttribute("boxExternalId", boxExternalId);
            return "boxes/fragments/modal :: edit-modal";
        }
        boxService.rename(spaceExternalId, shelfExternalId, boxExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelf", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username));
        model.addAttribute("boxes", boxService.findAllByShelfId(spaceExternalId, shelfExternalId, username));
        return "shelves/fragments/box-list :: box-list-response";
    }

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
        boxService.create(spaceExternalId, shelfExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("shelf", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username));
        model.addAttribute("boxes", boxService.findAllByShelfId(spaceExternalId, shelfExternalId, username));
        return "shelves/fragments/box-list :: box-list-response";
    }

    @DeleteMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}")
    public String delete(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         @PathVariable UUID boxExternalId,
                         HttpSession session,
                         Model model) {
        String username = (String) session.getAttribute("loginUser");
        boxService.delete(spaceExternalId, shelfExternalId, boxExternalId, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelf", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username));
        model.addAttribute("boxes", boxService.findAllByShelfId(spaceExternalId, shelfExternalId, username));
        return "shelves/fragments/box-list :: box-list-container";
    }
}
