package com.engebag.gestaoti.config;

import com.engebag.gestaoti.model.User;
import com.engebag.gestaoti.repository.ChamadoParticipanteRepository;
import com.engebag.gestaoti.repository.ChamadoRepository;
import com.engebag.gestaoti.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ChamadoParticipanteRepository participanteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. CRIAÇÃO DO ADMIN
            if (userRepository.findByEmail("info@engebag.com.br").isEmpty()) {
                User admin = new User();
                admin.setNome("Gabriel Malheiro");
                admin.setEmail("info@engebag.com.br");
                admin.setSenha(passwordEncoder.encode("!Erp159753@ti"));
                admin.setRole("ADMIN");
                admin.setEmpresaAcesso("AMBAS");
                admin.setCargo("Analista");
                admin.setUsuarioRm("mestre");
                admin.setUtilizaOmaxprensa(false);
                admin.setPrimeiroAcesso(false);
                admin.setAtivo(true);
                userRepository.save(admin);
            }

            // 2. CRIAÇÃO DO TÉCNICO
            if (userRepository.findByEmail("info01@engebag.com.br").isEmpty()) {
                User tecnico = new User();
                tecnico.setNome("Henrique Arroyo");
                tecnico.setEmail("info01@engebag.com.br");
                tecnico.setSenha(passwordEncoder.encode("!Erp159753@ti"));
                tecnico.setRole("ADMIN"); // Alterado para ADMIN conforme solicitado
                tecnico.setEmpresaAcesso("AMBAS");
                tecnico.setCargo("Tecnico");
                tecnico.setUsuarioRm("mestre2");
                tecnico.setUtilizaOmaxprensa(false);
                tecnico.setPrimeiroAcesso(false);
                tecnico.setAtivo(true);
                userRepository.save(tecnico);
            }

            // 3. CRIAÇÃO USUÁRIO COMUM ENGE BAG
            if (userRepository.findByEmail("compras2@engebag.com.br").isEmpty()) {
                User comumEngebag = new User();
                comumEngebag.setNome("José Amaragi");
                comumEngebag.setEmail("compras2@engebag.com.br");
                comumEngebag.setSenha(passwordEncoder.encode("123456"));
                comumEngebag.setRole("USER");
                comumEngebag.setEmpresaAcesso("ENGEBAG");
                comumEngebag.setCargo("Analista");
                comumEngebag.setUsuarioRm(null);
                comumEngebag.setUtilizaOmaxprensa(false);
                comumEngebag.setPrimeiroAcesso(true);
                comumEngebag.setAtivo(true);
                userRepository.save(comumEngebag);
            }

            // 4. CRIAÇÃO USUÁRIO COMUM BAG CLEANER
            if (userRepository.findByEmail("info@bagcleaner.com.br").isEmpty()) {
                User comumBag = new User();
                comumBag.setNome("Felipe Guirau");
                comumBag.setEmail("info@bagcleaner.com.br");
                comumBag.setSenha(passwordEncoder.encode("123456"));
                comumBag.setRole("TECNICO"); // Alterado para TECNICO conforme solicitado
                comumBag.setEmpresaAcesso("BAG_CLEANER");
                comumBag.setCargo("Auxiliar");
                comumBag.setUsuarioRm(null);
                comumBag.setUtilizaOmaxprensa(true);
                comumBag.setPrimeiroAcesso(true);
                comumBag.setAtivo(true);
                userRepository.save(comumBag);
            }

        } catch (Exception e) {
            System.out.println("⚠️ O Seeder encontrou um conflito: " + e.getMessage());
        }
    }
}