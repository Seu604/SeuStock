package com.seu.seustock.controller;

import com.google.zxing.WriterException;
import com.seu.seustock.model.dto.BoxDTO;
import com.seu.seustock.model.dto.ShelfDTO;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.service.BoxService;
import com.seu.seustock.service.QrCodeService;
import com.seu.seustock.service.ShelfService;
import com.seu.seustock.service.SpaceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class QrController {

    private final QrCodeService qrCodeService;
    private final BoxService boxService;
    private final ShelfService shelfService;
    private final SpaceService spaceService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/api/qr/modal")
    public String qrModal(@RequestParam String type,
                          @RequestParam UUID externalId,
                          @RequestParam String name,
                          org.springframework.ui.Model model) {
        String qrUrl = String.format("%s/qr/%ss/%s", baseUrl, type, externalId);
        model.addAttribute("title", name + " QR 코드");
        model.addAttribute("qrUrl", qrUrl);
        return "fragments/qr-modal :: modal";
    }

    @GetMapping(value = "/api/qr/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] generateQr(@RequestParam String content) throws IOException, WriterException {
        return qrCodeService.generateQrCodeImage(content, 300, 300);
    }

    @GetMapping("/qr/boxes/{externalId}")
    public String scanBox(@PathVariable UUID externalId, HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/login?redirect=/qr/boxes/" + externalId;
        }

        // 박스 정보 조회 (권한 체크 포함)
        // 현재 BoxService 등에는 externalId만으로 조회하는 기능이 제한적일 수 있음
        // BoxService를 보완하여 Space, Shelf UUID를 함께 가져오는 기능이 필요할 수 있음
        // 여기서는 일단 간략하게 구현하고 필요시 Service 보완
        try {
            BoxDTO box = boxService.findByExternalIdOnly(externalId);
            ShelfDTO shelf = shelfService.findById(box.getShelfId());
            SpaceDTO space = spaceService.findById(shelf.getSpaceId());
            
            // 소유권 확인
            if (!space.getUserId().equals(spaceService.getUserIdByUsername(username))) {
                return "redirect:/error/403";
            }

            return String.format("redirect:/spaces/%s/shelves/%s/boxes/%s/stocks",
                    space.getExternalId(), shelf.getExternalId(), box.getExternalId());
        } catch (Exception e) {
            return "redirect:/error/404";
        }
    }

    @GetMapping("/qr/shelves/{externalId}")
    public String scanShelf(@PathVariable UUID externalId, HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/login?redirect=/qr/shelves/" + externalId;
        }

        try {
            ShelfDTO shelf = shelfService.findByExternalIdOnly(externalId);
            SpaceDTO space = spaceService.findById(shelf.getSpaceId());

            if (!space.getUserId().equals(spaceService.getUserIdByUsername(username))) {
                return "redirect:/error/403";
            }

            return String.format("redirect:/spaces/%s/shelves/%s/stocks",
                    space.getExternalId(), shelf.getExternalId());
        } catch (Exception e) {
            return "redirect:/error/404";
        }
    }
}
