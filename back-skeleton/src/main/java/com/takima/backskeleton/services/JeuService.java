package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.JeuMapper;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.TypeJeu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JeuService {
    private final JeuDao jeuDao;

    public JeuService(JeuDao jeuDao) {
        this.jeuDao = jeuDao;
    }

    public List<JeuDto> findAll(TypeJeu type) {
        List<Jeu> jeux = type == null
                ? jeuDao.findAllByOrderByNomAsc()
                : jeuDao.findByTypeOrderByNomAsc(type);
        return jeux.stream()
                .map(JeuMapper::toDto)
                .toList();
    }

    public JeuDto getById(Long id) {
        return JeuMapper.toDto(findJeuById(id));
    }

    @Transactional
    public JeuDto addJeu(JeuDto jeuDto) {
        Jeu jeu = JeuMapper.fromDto(jeuDto, null);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public JeuDto updateJeu(JeuDto jeuDto, Long id) {
        findJeuById(id);
        Jeu jeu = JeuMapper.fromDto(jeuDto, id);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public void deleteById(Long id) {
        findJeuById(id);
        jeuDao.deleteById(id);
    }

    private Jeu findJeuById(Long id) {
        return jeuDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le jeu " + id + " n'existe pas"));
    }
}
