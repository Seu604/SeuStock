package com.seu.seustock.controller;

import com.seu.seustock.configuration.HtmxResponse;
import com.seu.seustock.model.dto.StockPanelDTO;
import com.seu.seustock.model.enumeration.TransactionType;
import com.seu.seustock.model.form.QuickStockForm;
import com.seu.seustock.model.form.StockForm;
import com.seu.seustock.model.form.StockInOutForm;
import com.seu.seustock.model.form.StockMoveForm;
import com.seu.seustock.model.form.StockUpdateForm;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.ShelfDTO;
import com.seu.seustock.service.BoxService;
import com.seu.seustock.service.ItemService;
import com.seu.seustock.service.ShelfService;
import com.seu.seustock.service.SpaceService;
import com.seu.seustock.service.StockService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final SpaceService spaceService;
    private final ShelfService shelfService;
    private final BoxService boxService;
    private final ItemService itemService;

    /* ── 내 재고 페이지 ── */

    @GetMapping("/stocks")
    public String list(@RequestParam(required = false) UUID itemExternalId,
                       @RequestParam(required = false) UUID spaceExternalId,
                       @RequestParam(required = false) UUID shelfExternalId,
                       @RequestParam(required = false) UUID boxExternalId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "newest") String sortBy,
                       @RequestParam(required = false) Integer page,
                       HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        var stocksPage = stockService.searchDetailsPage(
                itemExternalId, spaceExternalId, shelfExternalId, boxExternalId, keyword, sortBy, username, page);
        model.addAttribute("stocks", stocksPage.content());
        model.addAttribute("page", stocksPage);
        model.addAttribute("itemExternalId", itemExternalId);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("filtered", itemExternalId != null || spaceExternalId != null
                || shelfExternalId != null || boxExternalId != null
                || (keyword != null && !keyword.isBlank()));
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
                            Model model,
                            HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("stock", stockService.findDetailByExternalId(stockExternalId, username));
            return "stocks/fragments/detail-row :: edit";
        }
        model.addAttribute("stock", stockService.updateDetails(stockExternalId, form, username));
        HtmxResponse.success(response, "재고 정보가 저장되었습니다.");
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

    @GetMapping("/spaces/{spaceExternalId}/stocks/all")
    public String panelBySpaceAll(@PathVariable UUID spaceExternalId,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false, defaultValue = "newest") String sortBy,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false, defaultValue = "false") boolean append,
                                  HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        SpaceDTO space = spaceService.findByExternalId(spaceExternalId, username);
        var stocksPage = stockService.findPanelPageBySpaceAll(spaceExternalId, keyword, sortBy, username, page);
        model.addAttribute("stocks", stocksPage.content());
        model.addAttribute("page", stocksPage);
        model.addAttribute("breadcrumb", space.getName() + " 전체보기");
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("isAllView", true);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        if (append) {
            return "stocks/fragments/panel :: stock-panel-more-response";
        }
        return "stocks/fragments/panel :: stock-panel";
    }

    @GetMapping("/spaces/{spaceExternalId}/stocks")
    public String panelBySpace(@PathVariable UUID spaceExternalId,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false, defaultValue = "false") boolean append,
                               HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        SpaceDTO space = spaceService.findByExternalId(spaceExternalId, username);
        var stocksPage = stockService.findPanelPageBySpace(spaceExternalId, username, page);
        model.addAttribute("stocks", stocksPage.content());
        model.addAttribute("page", stocksPage);
        model.addAttribute("breadcrumb", space.getName() + "에 대충 던져놓은 물건들");
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("isAllView", false);
        if (append) {
            return "stocks/fragments/panel :: stock-panel-more-response";
        }
        return "stocks/fragments/panel :: stock-panel";
    }

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/stocks")
    public String panelByShelf(@PathVariable UUID spaceExternalId,
                               @PathVariable UUID shelfExternalId,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false, defaultValue = "false") boolean append,
                               HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        ShelfDTO shelf = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username);
        var stocksPage = stockService.findPanelPageByShelf(spaceExternalId, shelfExternalId, username, page);
        model.addAttribute("stocks", stocksPage.content());
        model.addAttribute("page", stocksPage);
        model.addAttribute("breadcrumb", shelf.getName());
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("isAllView", false);
        if (append) {
            return "stocks/fragments/panel :: stock-panel-more-response";
        }
        return "stocks/fragments/panel :: stock-panel";
    }

    @GetMapping("/spaces/{spaceExternalId}/shelves/{shelfExternalId}/boxes/{boxExternalId}/stocks")
    public String panelByBox(@PathVariable UUID spaceExternalId,
                             @PathVariable UUID shelfExternalId,
                             @PathVariable UUID boxExternalId,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false, defaultValue = "false") boolean append,
                             HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        var stocksPage = stockService.findPanelPageByBox(spaceExternalId, shelfExternalId, boxExternalId, username, page);
        model.addAttribute("stocks", stocksPage.content());
        model.addAttribute("page", stocksPage);
        model.addAttribute("breadcrumb", boxService.findByExternalId(spaceExternalId, shelfExternalId, boxExternalId, username).getName());
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("isAllView", false);
        if (append) {
            return "stocks/fragments/panel :: stock-panel-more-response";
        }
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
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("items", itemService.findAllByUsername(username));
            model.addAttribute("spaceId", form.getSpaceExternalId());
            model.addAttribute("shelfId", form.getShelfExternalId());
            model.addAttribute("boxId", form.getBoxExternalId());
            return "stocks/fragments/modal :: modal";
        }
        stockService.create(form, username);
        HtmxResponse.success(response, "재고가 추가되었습니다.");
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
                              HttpSession session,
                              Model model,
                              HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("spaceId", form.getSpaceExternalId());
            model.addAttribute("shelfId", form.getShelfExternalId());
            model.addAttribute("boxId", form.getBoxExternalId());
            return "stocks/fragments/quick-modal :: modal";
        }
        stockService.createWithNewItem(form, username);
        HtmxResponse.success(response, "품목과 재고가 추가되었습니다.");
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(), username, model);
    }

    /* ── 재고 삭제 ── */

    @DeleteMapping("/stocks")
    public String delete(@RequestParam UUID itemExternalId,
                         @RequestParam UUID spaceExternalId,
                         @RequestParam(required = false) UUID shelfExternalId,
                         @RequestParam(required = false) UUID boxExternalId,
                         HttpSession session,
                         Model model,
                         HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        stockService.deleteUnits(itemExternalId, spaceExternalId, shelfExternalId, boxExternalId, username);
        HtmxResponse.success(response, "재고가 삭제되었습니다.");
        return buildPanelResponse(spaceExternalId, shelfExternalId, boxExternalId, username, model);
    }

    @DeleteMapping("/stocks/{stockExternalId}")
    @ResponseBody
    public String deleteRow(@PathVariable UUID stockExternalId,
                            HttpSession session,
                            HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        stockService.deleteUnit(stockExternalId, username);
        HtmxResponse.success(response, "재고가 삭제되었습니다.");
        return "";
    }

    /* ── 통합 액션 모달 ── */

    @GetMapping("/stocks/action-form")
    public String actionForm(@RequestParam UUID itemExternalId,
                             @RequestParam String itemName,
                             @RequestParam UUID spaceExternalId,
                             @RequestParam(required = false) UUID shelfExternalId,
                             @RequestParam(required = false) UUID boxExternalId,
                             @RequestParam(defaultValue = "0") Integer count,
                             HttpSession session,
                             Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("itemName", itemName);
        model.addAttribute("itemExternalId", itemExternalId);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("currentCount", count);
        model.addAttribute("inMemoSuggestions", stockService.findMemoSuggestions(TransactionType.IN, username));
        model.addAttribute("outMemoSuggestions", stockService.findMemoSuggestions(TransactionType.OUT, username));
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
                            HttpSession session,
                            Model model,
                            HttpServletResponse response) {
        if (result.hasErrors()) {
            return "stocks/fragments/in-modal :: modal";
        }
        stockService.addUnits(form, (String) session.getAttribute("loginUser"));
        HtmxResponse.success(response, "입고되었습니다.");
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
                             HttpSession session,
                             Model model,
                             HttpServletResponse response) {
        if (result.hasErrors()) {
            return "stocks/fragments/out-modal :: modal";
        }
        stockService.dispatchUnits(form, (String) session.getAttribute("loginUser"));
        HtmxResponse.success(response, "출고되었습니다.");
        return buildPanelResponse(form.getSpaceExternalId(), form.getShelfExternalId(), form.getBoxExternalId(),
                (String) session.getAttribute("loginUser"), model);
    }

    /* ── 이동 ── */

    @GetMapping("/stocks/move-form")
    public String moveForm(@ModelAttribute("form") StockMoveForm form,
                           HttpSession session,
                           Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("locationOptions", buildMoveLocationOptions(form, username));
        return "stocks/fragments/move-modal :: modal";
    }

    @PostMapping("/stocks/move")
    public String processMove(@Valid @ModelAttribute("form") StockMoveForm form,
                              BindingResult result,
                              HttpSession session,
                              Model model,
                              HttpServletResponse response) {
        String username = (String) session.getAttribute("loginUser");
        if (result.hasErrors()) {
            model.addAttribute("locationOptions", buildMoveLocationOptions(form, username));
            return "stocks/fragments/move-modal :: modal";
        }
        stockService.moveUnits(form, username);
        HtmxResponse.success(response, "재고가 이동되었습니다.");
        return buildPanelResponse(form.getSourceSpaceExternalId(), form.getSourceShelfExternalId(), form.getSourceBoxExternalId(),
                username, model);
    }

    private String buildPanelResponse(UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId,
                                      String username, Model model) {
        String breadcrumb;
        var page = boxExternalId != null
                ? stockService.findPanelPageByBox(spaceExternalId, shelfExternalId, boxExternalId, username, 1)
                : shelfExternalId != null
                ? stockService.findPanelPageByShelf(spaceExternalId, shelfExternalId, username, 1)
                : stockService.findPanelPageBySpace(spaceExternalId, username, 1);
        if (boxExternalId != null) {
            breadcrumb = boxService.findByExternalId(spaceExternalId, shelfExternalId, boxExternalId, username).getName();
        } else if (shelfExternalId != null) {
            breadcrumb = shelfService.findByExternalId(spaceExternalId, shelfExternalId, username).getName();
        } else {
            SpaceDTO space = spaceService.findByExternalId(spaceExternalId, username);
            breadcrumb = space.getName() + "에 대충 던져놓은 물건들";
        }
        model.addAttribute("stocks", page.content());
        model.addAttribute("page", page);
        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("spaceExternalId", spaceExternalId);
        model.addAttribute("shelfExternalId", shelfExternalId);
        model.addAttribute("boxExternalId", boxExternalId);
        model.addAttribute("isAllView", false);
        return "stocks/fragments/panel :: stock-panel-response";
    }

    private List<MoveLocationOption> buildMoveLocationOptions(StockMoveForm form, String username) {
        List<MoveLocationOption> options = new java.util.ArrayList<>();
        UUID spaceExternalId = form.getSourceSpaceExternalId();
        String spaceName = spaceService.findByExternalId(spaceExternalId, username).getName();
        options.add(new MoveLocationOption(
                spaceName + "에 대충 던져놓은 물건들",
                spaceExternalId,
                null,
                null,
                isSameLocation(form, spaceExternalId, null, null)));

        for (var shelf : shelfService.findAllBySpaceId(spaceExternalId, username)) {
            options.add(new MoveLocationOption(
                    shelf.getName(),
                    spaceExternalId,
                    shelf.getExternalId(),
                    null,
                    isSameLocation(form, spaceExternalId, shelf.getExternalId(), null)));

            for (var box : boxService.findAllByShelfId(spaceExternalId, shelf.getExternalId(), username)) {
                options.add(new MoveLocationOption(
                        shelf.getName() + " / " + box.getName(),
                        spaceExternalId,
                        shelf.getExternalId(),
                        box.getExternalId(),
                        isSameLocation(form, spaceExternalId, shelf.getExternalId(), box.getExternalId())));
            }
        }
        return options;
    }

    private boolean isSameLocation(StockMoveForm form, UUID spaceExternalId, UUID shelfExternalId, UUID boxExternalId) {
        return Objects.equals(form.getSourceSpaceExternalId(), spaceExternalId)
                && Objects.equals(form.getSourceShelfExternalId(), shelfExternalId)
                && Objects.equals(form.getSourceBoxExternalId(), boxExternalId);
    }

    public record MoveLocationOption(String label,
                                     UUID spaceExternalId,
                                     UUID shelfExternalId,
                                     UUID boxExternalId,
                                     boolean current) {
    }
}
