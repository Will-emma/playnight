package com.takima.backskeleton.services;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Inclut aussi les parties enregistrées avant l'installation du bonus Elo.
@Component
public class ClassementInitialisation implements ApplicationRunner {
    private final ClassementService classementService;

    public ClassementInitialisation(ClassementService classementService) {
        this.classementService = classementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        classementService.recalculer();
    }
}
