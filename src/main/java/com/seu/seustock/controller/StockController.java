package com.seu.seustock.controller;

import com.seu.seustock.model.dto.StockPanelDTO;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.service.ItemService;
import com.seu.seustock.service.ShelfService;
import com.seu.seustock.service.StockService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final ShelfService shelfService;
    private final ItemService itemService;

    /* ── 재고 패널 조회 ── */

    @GetMapping("/spaces/{spaceExternalId}/stocks")
    public String panelBySpace(@PathVariable UUID spaceExternalId,
                               HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("stocks", stockService.findPanelBySpace(spaceExternalId, username));
        model.addAttribute("breadcrumb", "공간 직접 재고");
        model.addAttribute("spaceExternalId", spaceExternalId);
        return "stocks/fragments/panel :: stock-panel";
    }

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/stocks")
    public String panelByShelf(@PathVariable UUID spaceExternalId,
                               @PathVariable UUID shelfExternalId,
                               HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("stocks", stockService.findPanelByShelf(spaceExternalId, shelfExternalId, username));
        model.addAttribute("breadcrumb", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName() + " (선반 직접 재고)");
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        return "stocks/fragments/panel :: stock-panel";
    }

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}/stocks")
    public String panelByBox(@PathVariable UUID spaceExternalId,
                             @PathVariable UUID shelfExternalId,
                             @PathVariable UUID boxExternalId,
                             HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("stocks", stockService.findPanelByBox(spaceExternalId, shelfExternalId, boxExternalId, username));
        model.addAttribute("breadcrumb", shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName());
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        return "stocks/fragments/panel :: stock-panel";
    }

    /* ── 재고 등록 ── */

    @GetMapping("/stocks/new")
    public String newModal(@RequestParam UUID spaceId,
                           @RequestParam(required = false) UUID shelfId,
                           @RequestParam(required = false) UUID boxId,
                           HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("items", itemService.findAllByUsername(username));
        model.addAttribute("spaceId", spaceId);
        model.addAttribute("shelfId", shelfId);
        model.addAttribute("boxId", boxId);
        model.addAttribute("form", new StockForm());
        return "stocks/fragments/modal :: modal";
    }

    @PostMapping("/stocks")
    public String create(@Valid @ModelAttribute("form") StockForm form,
                         BindingResult result,
                         HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("items", itemService.findAllByUsername(username));
            model.addAttribute("spaceId", form.getSpaceExternalId());
            model.addAttribute("shelfId", form.getShelfExternalId());
            model.addAttribute("boxId", form.getBoxExternalId());
            return "stocks/fragments/modal :: modal";
        }
        stockService.create(form, username);

        List<StockPanelDTO> stocks = resolvePanel(form, username);
        model.addAttribute("stocks", stocks);
        model.addAttribute("breadcrumb", resolveBreadcrumb(form, username));
        model.addAttribute("spaceExternalId", form.getSpaceExternalId());
        model.addAttribute("shelfExternalId", form.getShelfExternalId());
        model.addAttribute("boxExternalId", form.getBoxExternalId());
        return "stocks/fragments/panel :: stock-panel-response";
    }

    /* ── 재고 삭제 ── */

    @DeleteMapping("/stocks/{externalId}")
    @ResponseBody
    public String delete(@PathVariable UUID externalId, HttpSession session) {
        stockService.delete(externalId, (String) session.getAttribute("loginUser"));
        return "";
    }

    /* ── 입고 ── */

    @GetMapping("/stocks/{externalId}/in-form")
    public String inForm(@PathVariable UUID externalId, Model model) {
        model.addAttribute("stockExternalId", externalId);
        model.addAttribute("form", new StockInOutForm());
        return "stocks/fragments/in-modal :: modal";
    }

    @PostMapping("/stocks/{externalId}/in")
    public String processIn(@PathVariable UUID externalId,
                            @Valid @ModelAttribute("form") StockInOutForm form,
                            BindingResult result,
                            HttpSession session, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("stockExternalId", externalId);
            return "stocks/fragments/in-modal :: modal";
        }
        StockPanelDTO stock = stockService.processIn(externalId, form, (String) session.getAttribute("loginUser"));
        model.addAttribute("stock", stock);
        return "stocks/fragments/row :: view-response";
    }

    /* ── 출고 ── */

    @GetMapping("/stocks/{externalId}/out-form")
    public String outForm(@PathVariable UUID externalId, Model model) {
        model.addAttribute("stockExternalId", externalId);
        model.addAttribute("form", new StockInOutForm());
        return "stocks/fragments/out-modal :: modal";
    }

    @PostMapping("/stocks/{externalId}/out")
    public String processOut(@PathVariable UUID externalId,
                             @Valid @ModelAttribute("form") StockInOutForm form,
                             BindingResult result,
                             HttpSession session, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("stockExternalId", externalId);
            return "stocks/fragments/out-modal :: modal";
        }
        StockPanelDTO stock = stockService.processOut(externalId, form, (String) session.getAttribute("loginUser"));
        model.addAttribute("stock", stock);
        return "stocks/fragments/row :: view-response";
    }

    private List<StockPanelDTO> resolvePanel(StockForm form, String username) {
        if (form.getBoxExternalId() != null) {
            return stockService.findPanelByBox(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), username);
        } else if (form.getShelfExternalId() != null) {
            return stockService.findPanelByShelf(form.getSpaceExternalId(), form.getShelfExternalId(), username);
        } else {
            return stockService.findPanelBySpace(form.getSpaceExternalId(), username);
        }
    }

    private String resolveBreadcrumb(StockForm form, String username) {
        if (form.getShelfExternalId() != null) {
            return shelfService.findByExternalId(form.getSpaceExternalId(), form.getShelfExternalId(), username).getName();
        }
        return "공간 직접 재고";
    }
}
