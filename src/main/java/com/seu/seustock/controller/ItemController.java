package com.seu.seustock.controller;

import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.form.ItemForm;
import com.seu.seustock.service.ItemService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public String list(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("items", itemService.findAllByUsername(username));
        model.addAttribute("form", new ItemForm());
        return "items/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ItemForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model) {
        if (result.hasErrors()) {
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("items", itemService.findAllByUsername(username));
            return "items/list";
        }
        itemService.create((String) session.getAttribute("loginUser"), form);
        return "redirect:/items";
    }

    /* ── HTMX 인라인 수정 ── */

    @GetMapping("/{externalId}/edit")
    public String editRow(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("item", itemService.findByExternalId(externalId, username));
        return "items/fragments/row :: edit";
    }

    @PutMapping("/{externalId}")
    public String updateRow(@PathVariable UUID externalId,
                            @Valid @ModelAttribute("form") ItemForm form,
                            BindingResult result,
                            HttpSession session,
                            Model model) {
        if (result.hasErrors()) {
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("item", itemService.findByExternalId(externalId, username));
            return "items/fragments/row :: edit";
        }
        String username = (String) session.getAttribute("loginUser");
        ItemDTO updated = itemService.update(externalId, form, username);
        model.addAttribute("item", updated);
        return "items/fragments/row :: view";
    }

    @GetMapping("/{externalId}/cancel")
    public String cancelEdit(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("item", itemService.findByExternalId(externalId, username));
        return "items/fragments/row :: view";
    }

    @DeleteMapping("/{externalId}")
    @ResponseBody
    public String delete(@PathVariable UUID externalId, HttpSession session) {
        itemService.delete(externalId, (String) session.getAttribute("loginUser"));
        return "";
    }
}
