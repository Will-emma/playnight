package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.SoireeDao;
import com.takima.backskeleton.DAO.SoireePartieCount;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.DTO.SoireeMapper;
import com.takima.backskeleton.models.Soiree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class SoireeService {
    private final SoireeDao soireeDao;
    private final PartieDao partieDao;

    public SoireeService(SoireeDao soireeDao, PartieDao partieDao) {
        this.soireeDao = soireeDao;
        this.partieDao = partieDao;
    }

    @Transactional(readOnly = true)
    public List<SoireeDto> findAll() {
        List<Soiree> soirees = soireeDao.findAllWithJoueurs();
        Map<Long, Long> nbPartiesParSoiree = soireeDao.countPartiesBySoiree().stream()
                .collect(Collectors.toMap(SoireePartieCount::getSoireeId, SoireePartieCount::getNbParties));

        return soirees.stream()
                .map(soiree -> SoireeMapper.toDto(
                        soiree,
                        nbPartiesParSoiree.getOrDefault(soiree.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SoireeDetailDto getById(Long id) {
        Soiree soiree = findSoireeById(id);
        return SoireeMapper.toDetailDto(soiree, partieDao.findDetailsBySoireeId(id));
    }

    @Transactional
    public void deleteById(Long id) {
        soireeDao.delete(findSoireeById(id));
    }

    private Soiree findSoireeById(Long id) {
        return soireeDao.findWithJoueursById(id)
                .orElseThrow(() -> new NoSuchElementException("La soirée " + id + " n'existe pas"));
    }
}
