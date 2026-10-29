package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.ClassementService;
import com.takima.backskeleton.services.JeuService;
import com.takima.backskeleton.services.RawgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Contrôleur et vrai service en mémoire : aucun contexte Spring Boot ni connexion PostgreSQL.
@ExtendWith(MockitoExtension.class)
class JeuCreationValidationTest {
    @Mock private JeuDao jeuDao;
    @Mock private PartieDao partieDao;
    @Mock private ScoreDao scoreDao;
    @Mock private RawgService rawgService;
    @Mock private ClassementService classementService;
    @InjectMocks private JeuService jeuService;
    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        mvc = MockMvcBuilders.standaloneSetup(new JeuController(jeuService, classementService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @ParameterizedTest
    @MethodSource("saisiesInvalides")
    void creationInvalideRenvoie400SansEcriture(String nom, String type, String minimum, String maximum) throws Exception {
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":%s,"type":%s,"nbJoueursMin":%s,"nbJoueursMax":%s}
                """.formatted(nom, type, minimum, maximum)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(jeuDao, rawgService);
    }

    static Stream<Arguments> saisiesInvalides() {
        return Stream.of(
                Arguments.of("null", "\"SOCIETE\"", "2", "4"),
                Arguments.of("\"\"", "\"SOCIETE\"", "2", "4"),
                Arguments.of("\"   \"", "\"SOCIETE\"", "2", "4"),
                Arguments.of("\"Catan\"", "null", "2", "4"),
                Arguments.of("\"Catan\"", "\"INCONNU\"", "2", "4"),
                Arguments.of("\"Catan\"", "0", "2", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "null", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "0", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "-1", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "1.5", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "null"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "1"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "-1"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "0"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "201"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "201", "201"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "4.5"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "2", "2147483648"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "true", "4"),
                Arguments.of("\"Catan\"", "\"SOCIETE\"", "\"1.5\"", "4")
        );
    }

    @ParameterizedTest
    @CsvSource({"SOCIETE,2,4", "SOCIETE,1,200", "SOCIETE,200,200", "JEU_VIDEO,201,250"})
    void creationValideRespecteLeTypeEtLesEffectifs(String type, int minimum, int maximum) throws Exception {
        when(jeuDao.save(any(Jeu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":"Jeu test","type":"%s","nbJoueursMin":%d,"nbJoueursMax":%d}
                """.formatted(type, minimum, maximum)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.nbJoueursMin").value(minimum))
                .andExpect(jsonPath("$.nbJoueursMax").value(maximum))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.image").isEmpty());
        verifyNoInteractions(rawgService);
    }

    @ParameterizedTest
    @CsvSource({"1.5,4", "2,4.5", "201,201", "2,201", "-1,4", "2,-1", "null,4", "2,null"})
    void effectifSocieteInvalideRenvoieUnMessageClair(String minimum, String maximum) throws Exception {
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":"Jeu test","type":"SOCIETE","nbJoueursMin":%s,"nbJoueursMax":%s}
                """.formatted(minimum, maximum)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Indiquez un nombre de joueurs compris entre 1 et 200."));
        verifyNoInteractions(jeuDao, rawgService);
    }

    @ParameterizedTest
    @EnumSource(TypeJeu.class)
    void creationAccepteLesDeuxTypesSansImageEtIgnoreLIdentifiantClient(TypeJeu type) throws Exception {
        when(jeuDao.save(any(Jeu.class))).thenAnswer(invocation -> {
            Jeu jeu = invocation.getArgument(0);
            return new Jeu.Builder().id(42L).nom(jeu.getNom()).type(jeu.getType())
                    .nbJoueursMin(jeu.getNbJoueursMin()).nbJoueursMax(jeu.getNbJoueursMax()).build();
        });
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"id":999,"nom":"  Catan  ","type":"%s","nbJoueursMin":1,"nbJoueursMax":1}
                """.formatted(type)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.nom").value("Catan"))
                .andExpect(jsonPath("$.type").value(type.name()))
                .andExpect(jsonPath("$.image").isEmpty())
                .andExpect(jsonPath("$.description").isEmpty());
        ArgumentCaptor<Jeu> captor = ArgumentCaptor.forClass(Jeu.class);
        verify(jeuDao).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(type, captor.getValue().getType());
        verifyNoInteractions(rawgService);
    }
}
