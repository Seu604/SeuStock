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
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), username, model);
    }

    /* ── 재고 삭제 ── */

    @DeleteMapping("/stocks")
    @ResponseBody
    public String delete(@RequestParam UUID itemExternalId,
                         @RequestParam UUID spaceExternalId,
                         @RequestParam(required = false) UUID shelfExternalId,
                         @RequestParam(required = false) UUID boxExternalId,
                         HttpSession session) {
        stockService.deleteUnits(itemExternalId, spaceExternalId, shelfExternalId, boxExternalId,
                (String) session.getAttribute("loginUser"));
        return "";
    }

    /* ── 입고 ── */

    @GetMapping("/stocks/in-form")
    public String inForm(@RequestParam UUID itemExternalId,
                         @RequestParam UUID spaceExternalId,
                         @RequestParam(required = false) UUID shelfExternalId,
                         @RequestParam(required = false) UUID boxExternalId,
                         Model model) {
        StockInOutForm form = new StockInOutForm();
        form.setItemExternalId(itemExternalId);
        form.setSpaceExternalId(spaceExternalId);
        form.setShelfExternalId(shelfExternalId);
        form.setBoxExternalId(boxExternalId);
        model.addAttribute("form", form);
        return "stocks/fragments/in-modal :: modal";
    }

    @PostMapping("/stocks/in")
    public String processIn(@Valid @ModelAttribute("form") StockInOutForm form,
                            BindingResult result,
                            HttpSession session, Model model) {
        if (result.hasErrors()) {
            return "stocks/fragments/in-modal :: modal";
        }
        stockService.addUnits(form, (String) session.getAttribute("loginUser"));
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(),
                (String) session.getAttribute("loginUser"), model);
    }

    /* ── 출고 ── */

    @GetMapping("/stocks/out-form")
    public String outForm(@RequestParam UUID itemExternalId,
                          @RequestParam UUID spaceExternalId,
                          @RequestParam(required = false) UUID shelfExternalId,
                          @RequestParam(required = false) UUID boxExternalId,
                          Model model) {
        StockInOutForm form = new StockInOutForm();
        form.setItemExternalId(itemExternalId);
        form.setSpaceExternalId(spaceExternalId);
        form.setShelfExternalId(shelfExternalId);
        form.setBoxExternalId(boxExternalId);
        model.addAttribute("form", form);
        return "stocks/fragments/out-modal :: modal";
    }

    @PostMapping("/stocks/out")
    public String processOut(@Valid @ModelAttribute("form") StockInOutForm form,
                             BindingResult result,
                             HttpSession session, Model model) {
        if (result.hasErrors()) {
            return "stocks/fragments/out-modal :: modal";
        }
        stockService.dispatchUnits(form, (String) session.getAttribute("loginUser"));
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(),
                (String) session.getAttribute("loginUser"), model);
    }

    private String buildPanelResponse(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId,
                                      String username, Model model) {
        List<StockPanelDTO> stocks;
        String breadcrumb;
        if (boxExternalId != null) {
            stocks = stockService.findPanelByBox(spaceExternalId, shelfExternalId, boxExternalId, username);
            breadcrumb = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName();
        } else if (shelfExternalId != null) {
            stocks = stockService.findPanelByShelf(spaceExternalId, shelfExternalId, username);
            breadcrumb = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName() + " (선반 직접 재고)";
        } else {
            stocks = stockService.findPanelBySpace(spaceExternalId, username);
            breadcrumb = "공간 직접 재고";
        }
        model.addAttribute("stocks", stocks);
        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        return "stocks/fragments/panel :: stock-panel-response";
    }
}
