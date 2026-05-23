package com.seu.seustock.controller;

import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.form.SpaceForm;
import com.seu.seustock.service.ShelfService;
import com.seu.seustock.service.SpaceService;
import com.seu.seustock.service.StockService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final ShelfService shelfService;
    private final StockService stockService;

    @GetMapping
    public String list(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("spaces", spaceService.findAllByUsername(username));
        model.addAttribute("form", new SpaceForm());
        return "spaces/list";
    }

    @GetMapping("/{externalId}")
    public String detail(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        SpaceDTO space = spaceService.findByExternalId(externalId, username);
        model.addAttribute("space", space);
        model.addAttribute("shelves", shelfService.findAllBySpaceId(externalId, username));
        model.addAttribute("stocks", stockService.findPanelBySpace(externalId, username));
        model.addAttribute("breadcrumb", space.getName());
        model.addAttribute("spaceExternalId", externalId);
        return "spaces/detail";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SpaceForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model) {
        if (result.hasErrors()) {
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("spaces", spaceService.findAllByUsername(username));
            return "spaces/list";
        }
        spaceService.create((String) session.getAttribute("loginUser"), form);
        return "redirect:/spaces";
    }

    /* ── HTMX 인라인 수정 ── */

    @GetMapping("/{externalId}/edit")
    public String editRow(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("space", spaceService.findByExternalId(externalId, username));
        return "spaces/fragments/row :: edit";
    }

    @PutMapping("/{externalId}")
    public String updateRow(@PathVariable UUID externalId,
                            @Valid SpaceForm form,
                            BindingResult result,
                            HttpSession session,
                            Model model) {
        if (result.hasErrors()) {
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("space", spaceService.findByExternalId(externalId, username));
            return "spaces/fragments/row :: edit";
        }
        String username = (String) session.getAttribute("loginUser");
        SpaceDTO updated = spaceService.update(externalId, form, username);
        model.addAttribute("space", updated);
        return "spaces/fragments/row :: view";
    }

    @GetMapping("/{externalId}/cancel")
    public String cancelEdit(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("space", spaceService.findByExternalId(externalId, username));
        return "spaces/fragments/row :: view";
    }

    @DeleteMapping("/{externalId}")
    public String delete(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        spaceService.delete(externalId, username);
        model.addAttribute("spaces", spaceService.findAllByUsername(username));
        return "spaces/list :: space-list";
    }
}
