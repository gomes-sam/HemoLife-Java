package com.hemolife.controller;

import com.hemolife.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TesteController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class TesteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void retornaJsonEsperadoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/teste"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"mensagem\":\"Backend HemoLife Java funcionando\"}"));
    }

    @Test
    void bloqueiaOutrasRotasSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void bloqueiaOutrasRotasMesmoComUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void naoLiberaPostNaRotaDeTesteMesmoComCsrfValido() throws Exception {
        mockMvc.perform(post("/api/teste").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
