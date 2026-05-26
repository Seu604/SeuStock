package com.seu.seustock.controller;

import com.seu.seustock.configuration.HtmxResponse;
import com.seu.seustock.model.dto.ItemDTO;
import com.seu.seustock.model.form.ItemForm;
import com.seu.seustock.service.ItemService;
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
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "newest") String sortBy,
                       @RequestParam(required = false) Integer page,
                       HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        var itemsPage = itemService.findPageByUsername(username, keyword, sortBy, page);
        model.addAttribute("items", itemsPage.content());
        model.addAttribute("page", itemsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        return "items/list";
    }

    @GetMapping("/new")
    public String newModal(Model model) {
        model.addAttribute("form", new ItemForm());
        return "items/fragments/modal :: modal";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ItemForm form,
                         BindingResult result,
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        if (result.hasErrors()) {
            return "items/fragments/modal :: modal";
        }
        String username = (String) session.getAttribute("loginUser");
        ItemDTO created = itemService.create(username, form);
        model.addAttribute("item", created);
        HtmxResponse.success(response, "품목이 추가되었습니다.");
        return "items/fragments/modal :: created";
    }

    /* ── HTMX 인라인 수정 ── */

    @GetMapping("/{externalId}/edit")
    public String editRow(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("item", itemService.findByExternalId(externalId, username));
        return "items/fragments/card :: edit";
    }

    @PutMapping("/{externalId}")
    public String updateRow(@PathVariable UUID externalId,
                            @Valid @ModelAttribute("form") ItemForm form,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            HttpServletResponse response) {
        if (result.hasErrors()) {
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("item", itemService.findByExternalId(externalId, username));
            return "items/fragments/card :: edit";
        }
        String username = (String) session.getAttribute("loginUser");
        ItemDTO updated = itemService.update(externalId, form, username);
        model.addAttribute("item", updated);
        HtmxResponse.success(response, "품목이 저장되었습니다.");
        return "items/fragments/card :: view";
    }

    @GetMapping("/{externalId}/cancel")
    public String cancelEdit(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("item", itemService.findByExternalId(externalId, username));
        return "items/fragments/card :: view";
    }

    @GetMapping("/{externalId}/spaces")
    public String spaces(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("externalId", externalId);
        model.addAttribute("itemName", itemService.findByExternalId(externalId, username).getName());
        model.addAttribute("spaceStocks", itemService.findSpaceStock(externalId, username));
        return "items/fragments/space-stock-modal :: modal";
    }

    @GetMapping("/{externalId}/history")
    public String history(@PathVariable UUID externalId, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("itemName", itemService.findByExternalId(externalId, username).getName());
        model.addAttribute("history", itemService.findTransactionHistory(externalId, username));
        return "items/fragments/history-modal :: modal";
    }

    @DeleteMapping("/{externalId}")
    public String delete(@PathVariable UUID externalId,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false, defaultValue = "newest") String sortBy,
                         @RequestParam(required = false) Integer page,
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        itemService.delete(externalId, username);
        var itemsPage = itemService.findPageByUsername(username, keyword, sortBy, page);
        model.addAttribute("items", itemsPage.content());
        model.addAttribute("page", itemsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        HtmxResponse.success(response, "품목이 삭제되었습니다.");
        return "items/list :: item-list-section";
    }
}
