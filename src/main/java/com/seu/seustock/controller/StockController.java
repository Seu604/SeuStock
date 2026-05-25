package com.seu.seustock.controller;

import com.seu.seustock.model.dto.StockPanelDTO;
import com.seu.seustock.model.form.QuickStockForm;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.model.form.StockUpdateForm;
import com.seu.seustock.service.BoxService;
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
    private final BoxService boxService;
    private final ItemService itemService;

    /* ── 내 재고 페이지 ── */

    @GetMapping("/stocks")
    public String list(@RequestParam(required = false) UUID itemExternalId,
                       @RequestParam(required = false) UUID spaceExternalId,
                       @RequestParam(required = false) UUID shelfExternalId,
                       @RequestParam(required = false) UUID boxExternalId,
                       HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("stocks", stockService.searchDetails(
                itemExternalId, spaceExternalId, shelfExternalId, boxExternalId, username));
        model.addAttribute("itemExternalId", itemExternalId);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("filtered", itemExternalId != null || spaceExternalId != null
                || shelfExternalId != null || boxExternalId != null);
        return "stocks/list";
    }

    @GetMapping("/stocks/{stockExternalId}/edit")
    public String editRow(@PathVariable UUID stockExternalId,
                          HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        StockUpdateForm form = new StockUpdateForm();
        var stock = stockService.findDetailByExternalId(stockExternalId, username);
        form.setSerialNumber(stock.getSerialNumber());
        form.setLotNumber(stock.getLotNumber());
        form.setExpirationDate(stock.getExpirationDate());
        form.setMemo(stock.getMemo());
        model.addAttribute("stock", stock);
        model.addAttribute("form", form);
        return "stocks/fragments/detail-row :: edit";
    }

    @PutMapping("/stocks/{stockExternalId}")
    public String updateRow(@PathVariable UUID stockExternalId,
                            @Valid @ModelAttribute("form") StockUpdateForm form,
                            BindingResult result,
                            HttpSession session,
                            Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("stock", stockService.findDetailByExternalId(stockExternalId, username));
            return "stocks/fragments/detail-row :: edit";
        }
        model.addAttribute("stock", stockService.updateDetails(stockExternalId, form, username));
        return "stocks/fragments/detail-row :: view";
    }

    @GetMapping("/stocks/{stockExternalId}/cancel")
    public String cancelEdit(@PathVariable UUID stockExternalId,
                             HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("stock", stockService.findDetailByExternalId(stockExternalId, username));
        return "stocks/fragments/detail-row :: view";
    }

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
        model.addAttribute("breadcrumb", boxService.findByExternalId(spaceExternalId, shelfExternalId, boxExternalId, username).getName());
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

    /* ── 빠른 등록 (품목+재고 동시 생성) ── */

    @GetMapping("/stocks/quick")
    public String quickModal(@RequestParam UUID spaceId,
                             @RequestParam(required = false) UUID shelfId,
                             @RequestParam(required = false) UUID boxId,
                             Model model) {
        model.addAttribute("spaceId", spaceId);
        model.addAttribute("shelfId", shelfId);
        model.addAttribute("boxId", boxId);
        model.addAttribute("form", new QuickStockForm());
        return "stocks/fragments/quick-modal :: modal";
    }

    @PostMapping("/stocks/quick")
    public String createQuick(@Valid @ModelAttribute("form") QuickStockForm form,
                              BindingResult result,
                              HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceId", form.getSpaceExternalId());
            model.addAttribute("shelfId", form.getShelfExternalId());
            model.addAttribute("boxId", form.getBoxExternalId());
            return "stocks/fragments/quick-modal :: modal";
        }
        stockService.createWithNewItem(form, username);
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), username, model);
    }

    /* ── 재고 삭제 ── */

    @DeleteMapping("/stocks")
    public String delete(@RequestParam UUID itemExternalId,
                         @RequestParam UUID spaceExternalId,
                         @RequestParam(required = false) UUID shelfExternalId,
                         @RequestParam(required = false) UUID boxExternalId,
                         HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        stockService.deleteUnits(itemExternalId, spaceExternalId, shelfExternalId, boxExternalId, username);
        return buildPanelResponse(spaceExternalId, shelfExternalId, boxExternalId, username, model);
    }

    /* ── 통합 액션 모달 ── */

    @GetMapping("/stocks/action-form")
    public String actionForm(@RequestParam UUID itemExternalId,
                             @RequestParam String itemName,
                             @RequestParam UUID spaceExternalId,
                             @RequestParam(required = false) UUID shelfExternalId,
                             @RequestParam(required = false) UUID boxExternalId,
                             @RequestParam(defaultValue = "0") Integer count,
                             Model model) {
        model.addAttribute("itemName", itemName);
        model.addAttribute("itemExternalId", itemExternalId);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("currentCount", count);
        return "stocks/fragments/action-modal :: modal";
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
            breadcrumb = boxService.findByExternalId(spaceExternalId, shelfExternalId, boxExternalId, username).getName();
        } else if (shelfExternalId != null) {
            String shelfName = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName();
            stocks = stockService.findPanelByShelf(spaceExternalId, shelfExternalId, username);
            breadcrumb = shelfName + " (선반 직접 재고)";
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
