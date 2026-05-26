package com.seu.seustock.controller;

import com.seu.seustock.configuration.HtmxResponse;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.form.SpaceForm;
import com.seu.seustock.service.ShelfService;
import com.seu.seustock.service.SpaceService;
import com.seu.seustock.service.StockService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final ShelfService shelfService;
    private final StockService stockService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "newest") String sortBy,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false, defaultValue = "false") boolean append,
                       Principal principal, Model model) {
        String username = principal.getName();
        var spacesPage = spaceService.findPageByUsername(username, keyword, sortBy, page);
        model.addAttribute("spaces", spacesPage.content());
        model.addAttribute("page", spacesPage);
        model.addAttribute("form", new SpaceForm());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        if (append) {
            return "spaces/fragments/list-response :: space-more-response";
        }
        return "spaces/list";
    }

    @GetMapping("/{externalId}")
    public String detail(@PathVariable UUID externalId, Principal principal, Model model) {
        String username = principal.getName();
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
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false, defaultValue = "newest") String sortBy,
                         @RequestParam(required = false) Integer page,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String username = principal.getName();
        if (result.hasErrors()) {
            var spacesPage = spaceService.findPageByUsername(username, keyword, sortBy, page);
            model.addAttribute("spaces", spacesPage.content());
            model.addAttribute("page", spacesPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sortBy", sortBy);
            return "spaces/list";
        }
        spaceService.create(username, form);
        redirectAttributes.addFlashAttribute("toastType", "success");
        redirectAttributes.addFlashAttribute("toastMessage", "공간이 추가되었습니다.");
        return "redirect:/spaces";
    }

    /* ── HTMX 인라인 수정 ── */

    @GetMapping("/{externalId}/edit")
    public String editRow(@PathVariable UUID externalId, Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("space", spaceService.findByExternalId(externalId, username));
        return "spaces/fragments/row :: edit";
    }

    @PutMapping("/{externalId}")
    public String updateRow(@PathVariable UUID externalId,
                            @Valid SpaceForm form,
                            BindingResult result,
                            Principal principal,
                            Model model,
                            HttpServletResponse response) {
        String username = principal.getName();
        if (result.hasErrors()) {
            model.addAttribute("space", spaceService.findByExternalId(externalId, username));
            return "spaces/fragments/row :: edit";
        }
        SpaceDTO updated = spaceService.update(externalId, form, username);
        model.addAttribute("space", updated);
        HtmxResponse.success(response, "공간이 저장되었습니다.");
        return "spaces/fragments/row :: view";
    }

    @GetMapping("/{externalId}/cancel")
    public String cancelEdit(@PathVariable UUID externalId, Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("space", spaceService.findByExternalId(externalId, username));
        return "spaces/fragments/row :: view";
    }

    @DeleteMapping("/{externalId}")
    public String delete(@PathVariable UUID externalId,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false, defaultValue = "newest") String sortBy,
                         @RequestParam(required = false) Integer page,
                         Principal principal,
                         Model model,
                         HttpServletResponse response) {
        String username = principal.getName();
        spaceService.delete(externalId, username);
        var spacesPage = spaceService.findPageByUsername(username, keyword, sortBy, page);
        model.addAttribute("spaces", spacesPage.content());
        model.addAttribute("page", spacesPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        HtmxResponse.success(response, "공간이 삭제되었습니다.");
        return "spaces/list :: space-list-section";
    }
}
