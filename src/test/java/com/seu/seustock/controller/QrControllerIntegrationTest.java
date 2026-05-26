package com.seu.seustock.controller;

import com.seu.seustock.mapper.BoxMapper;
import com.seu.seustock.mapper.ShelfMapper;
import com.seu.seustock.mapper.SpaceMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.BoxDTO;
import com.seu.seustock.model.dto.ShelfDTO;
import com.seu.seustock.model.dto.SpaceDTO;
import com.seu.seustock.model.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QrControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SpaceMapper spaceMapper;

    @Autowired
    private ShelfMapper shelfMapper;

    @Autowired
    private BoxMapper boxMapper;

    private UserDTO testUser;
    private SpaceDTO testSpace;
    private ShelfDTO testShelf;
    private BoxDTO testBox;

    @BeforeEach
    void setUp() {
        testUser = new UserDTO();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        userMapper.insertUser(testUser);
        testUser = userMapper.findByUsername("testuser").orElseThrow();

        testSpace = new SpaceDTO();
        testSpace.setUserId(testUser.getId());
        testSpace.setName("Test Space");
        spaceMapper.insertSpace(testSpace);
        testSpace = spaceMapper.findByUserId(testUser.getId()).get(0);

        testShelf = new ShelfDTO();
        testShelf.setSpaceId(testSpace.getId());
        testShelf.setName("Test Shelf");
        shelfMapper.insertShelf(testShelf);
        testShelf = shelfMapper.findBySpaceId(testSpace.getId()).get(0);

        testBox = new BoxDTO();
        testBox.setShelfId(testShelf.getId());
        testBox.setName("Test Box");
        boxMapper.insertBox(testBox);
        testBox = boxMapper.findByShelfId(testShelf.getId()).get(0);
    }

    @Test
    @DisplayName("박스 QR 스캔 시 해당 재고 페이지로 리다이렉트 (로그인 상태)")
    void scanBoxRedirect() throws Exception {
        mockMvc.perform(get("/qr/boxes/" + testBox.getExternalId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(String.format("/spaces/%s/shelves/%s/boxes/%s/stocks",
                        testSpace.getExternalId(), testShelf.getExternalId(), testBox.getExternalId())));
    }

    @Test
    @DisplayName("로그인하지 않은 상태에서 박스 QR 스캔 시 로그인 페이지로 리다이렉트 (redirect 파라미터 포함)")
    void scanBoxUnauthenticated() throws Exception {
        mockMvc.perform(get("/qr/boxes/" + testBox.getExternalId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/qr/boxes/" + testBox.getExternalId()));
    }
}
