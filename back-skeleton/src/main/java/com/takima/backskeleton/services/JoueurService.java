package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.DTO.JoueurMapper;
import com.takima.backskeleton.models.Joueur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JoueurService {
    private final JoueurDao joueurDao;

    public JoueurService(JoueurDao joueurDao) {
        this.joueurDao = joueurDao;
    }

    public List<JoueurDto> findAll() {
        return joueurDao.findAll().stream()
                .map(JoueurMapper::toDto)
                .toList();
    }

    public JoueurDto getById(Long id) {
        return JoueurMapper.toDto(findJoueurById(id));
    }

    @Transactional
    public JoueurDto addJoueur(JoueurDto joueurDto) {
        Joueur joueur = JoueurMapper.fromDto(joueurDto, null);
        return JoueurMapper.toDto(joueurDao.save(joueur));
    }

    @Transactional
    public JoueurDto updateJoueur(JoueurDto joueurDto, Long id) {
        findJoueurById(id);
        Joueur joueur = JoueurMapper.fromDto(joueurDto, id);
        return JoueurMapper.toDto(joueurDao.save(joueur));
    }

    @Transactional
    public void deleteById(Long id) {
        findJoueurById(id);
        joueurDao.deleteById(id);
    }

    private Joueur findJoueurById(Long id) {
        return joueurDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le joueur " + id + " n'existe pas"));
    }
}