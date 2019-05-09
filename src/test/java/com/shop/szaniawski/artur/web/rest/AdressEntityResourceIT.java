package com.shop.szaniawski.artur.web.rest;

import com.shop.szaniawski.artur.JhIpsterApp;
import com.shop.szaniawski.artur.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static com.shop.szaniawski.artur.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link AdressEntityResource} REST controller.
 */
@SpringBootTest(classes = JhIpsterApp.class)
public class AdressEntityResourceIT {

    private static final String DEFAULT_CITY = "AAAAAAAAAA";
    private static final String UPDATED_CITY = "BBBBBBBBBB";

    private static final String DEFAULT_STREET = "AAAAAAAAAA";
    private static final String UPDATED_STREET = "BBBBBBBBBB";

    @Autowired
    private AdressEntityRepository adressEntityRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restAdressEntityMockMvc;

    private AdressEntity adressEntity;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final AdressEntityResource adressEntityResource = new AdressEntityResource(adressEntityRepository);
        this.restAdressEntityMockMvc = MockMvcBuilders.standaloneSetup(adressEntityResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AdressEntity createEntity(EntityManager em) {
        AdressEntity adressEntity = new AdressEntity()
            .city(DEFAULT_CITY)
            .street(DEFAULT_STREET);
        return adressEntity;
    }

    @BeforeEach
    public void initTest() {
        adressEntity = createEntity(em);
    }

    @Test
    @Transactional
    public void createAdressEntity() throws Exception {
        int databaseSizeBeforeCreate = adressEntityRepository.findAll().size();

        // Create the AdressEntity
        restAdressEntityMockMvc.perform(post("/api/adress-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(adressEntity)))
            .andExpect(status().isCreated());

        // Validate the AdressEntity in the database
        List<AdressEntity> adressEntityList = adressEntityRepository.findAll();
        assertThat(adressEntityList).hasSize(databaseSizeBeforeCreate + 1);
        AdressEntity testAdressEntity = adressEntityList.get(adressEntityList.size() - 1);
        assertThat(testAdressEntity.getCity()).isEqualTo(DEFAULT_CITY);
        assertThat(testAdressEntity.getStreet()).isEqualTo(DEFAULT_STREET);
    }

    @Test
    @Transactional
    public void createAdressEntityWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = adressEntityRepository.findAll().size();

        // Create the AdressEntity with an existing ID
        adressEntity.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAdressEntityMockMvc.perform(post("/api/adress-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(adressEntity)))
            .andExpect(status().isBadRequest());

        // Validate the AdressEntity in the database
        List<AdressEntity> adressEntityList = adressEntityRepository.findAll();
        assertThat(adressEntityList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllAdressEntities() throws Exception {
        // Initialize the database
        adressEntityRepository.saveAndFlush(adressEntity);

        // Get all the adressEntityList
        restAdressEntityMockMvc.perform(get("/api/adress-entities?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(adressEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].city").value(hasItem(DEFAULT_CITY.toString())))
            .andExpect(jsonPath("$.[*].street").value(hasItem(DEFAULT_STREET.toString())));
    }
    
    @Test
    @Transactional
    public void getAdressEntity() throws Exception {
        // Initialize the database
        adressEntityRepository.saveAndFlush(adressEntity);

        // Get the adressEntity
        restAdressEntityMockMvc.perform(get("/api/adress-entities/{id}", adressEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(adressEntity.getId().intValue()))
            .andExpect(jsonPath("$.city").value(DEFAULT_CITY.toString()))
            .andExpect(jsonPath("$.street").value(DEFAULT_STREET.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingAdressEntity() throws Exception {
        // Get the adressEntity
        restAdressEntityMockMvc.perform(get("/api/adress-entities/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAdressEntity() throws Exception {
        // Initialize the database
        adressEntityRepository.saveAndFlush(adressEntity);

        int databaseSizeBeforeUpdate = adressEntityRepository.findAll().size();

        // Update the adressEntity
        AdressEntity updatedAdressEntity = adressEntityRepository.findById(adressEntity.getId()).get();
        // Disconnect from session so that the updates on updatedAdressEntity are not directly saved in db
        em.detach(updatedAdressEntity);
        updatedAdressEntity
            .city(UPDATED_CITY)
            .street(UPDATED_STREET);

        restAdressEntityMockMvc.perform(put("/api/adress-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedAdressEntity)))
            .andExpect(status().isOk());

        // Validate the AdressEntity in the database
        List<AdressEntity> adressEntityList = adressEntityRepository.findAll();
        assertThat(adressEntityList).hasSize(databaseSizeBeforeUpdate);
        AdressEntity testAdressEntity = adressEntityList.get(adressEntityList.size() - 1);
        assertThat(testAdressEntity.getCity()).isEqualTo(UPDATED_CITY);
        assertThat(testAdressEntity.getStreet()).isEqualTo(UPDATED_STREET);
    }

    @Test
    @Transactional
    public void updateNonExistingAdressEntity() throws Exception {
        int databaseSizeBeforeUpdate = adressEntityRepository.findAll().size();

        // Create the AdressEntity

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAdressEntityMockMvc.perform(put("/api/adress-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(adressEntity)))
            .andExpect(status().isBadRequest());

        // Validate the AdressEntity in the database
        List<AdressEntity> adressEntityList = adressEntityRepository.findAll();
        assertThat(adressEntityList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteAdressEntity() throws Exception {
        // Initialize the database
        adressEntityRepository.saveAndFlush(adressEntity);

        int databaseSizeBeforeDelete = adressEntityRepository.findAll().size();

        // Delete the adressEntity
        restAdressEntityMockMvc.perform(delete("/api/adress-entities/{id}", adressEntity.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database is empty
        List<AdressEntity> adressEntityList = adressEntityRepository.findAll();
        assertThat(adressEntityList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdressEntity.class);
        AdressEntity adressEntity1 = new AdressEntity();
        adressEntity1.setId(1L);
        AdressEntity adressEntity2 = new AdressEntity();
        adressEntity2.setId(adressEntity1.getId());
        assertThat(adressEntity1).isEqualTo(adressEntity2);
        adressEntity2.setId(2L);
        assertThat(adressEntity1).isNotEqualTo(adressEntity2);
        adressEntity1.setId(null);
        assertThat(adressEntity1).isNotEqualTo(adressEntity2);
    }
}
