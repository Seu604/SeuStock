package com.seu.seustock.controller;

import com.seu.seustock.configuration.HtmxResponse;
import com.seu.seustock.model.form.ShelfForm;
import com.seu.seustock.service.BoxService;
import com.seu.seustock.service.ShelfService;
import jakarta.servlet.http.HttpServletResponse;
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

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/edit")
    public String editModal(@PathVariable UUID spaceExternalId,
                            @PathVariable UUID shelfExternalId,
                            HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        var shelf = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username);
        ShelfForm form = new ShelfForm();
        form.setName(shelf.getName());
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("form", form);
        return "shelves/fragments/modal :: edit-modal";
    }

    @PatchMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}")
    public String rename(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         @Valid @ModelAttribute("form") ShelfForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceExternalId", spaceExternalId);
            model.addAttribute("shelfExternalId", shelfExternalId);
            return "shelves/fragments/modal :: edit-modal";
        }
        shelfService.rename(spaceExternalId, shelfExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelves", shelfService.findAllBySpaceId(spaceExternalId, username));
        HtmxResponse.success(response, "선반이 변경되었습니다.");
        return "spaces/fragments/shelf-list-response :: shelf-list-response";
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
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceExternalId", spaceExternalId);
            return "shelves/fragments/modal :: modal";
        }
        shelfService.create(spaceExternalId, form, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelves", shelfService.findAllBySpaceId(spaceExternalId, username));
        HtmxResponse.success(response, "선반이 추가되었습니다.");
        return "spaces/fragments/shelf-list-response :: shelf-list-response";
    }

    @DeleteMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}")
    public String delete(@PathVariable UUID spaceExternalId,
                         @PathVariable UUID shelfExternalId,
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        shelfService.delete(spaceExternalId, shelfExternalId, username);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelves", shelfService.findAllBySpaceId(spaceExternalId, username));
        HtmxResponse.success(response, "선반이 삭제되었습니다.");
        return "spaces/detail :: shelf-list";
    }
}
