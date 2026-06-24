package com.engebag.gestaoti.config;

import com.engebag.gestaoti.model.Chamado;
import com.engebag.gestaoti.model.ChamadoParticipante;
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
          // ==========================================
            // 1. CRIAÇÃO DOS USUÁRIOS
            // ==========================================
            if (userRepository.findByEmail("admin@engebag.com.br").isEmpty()) {
                User admin = new User();
                admin.setNome("Administrador do Sistema");
                admin.setEmail("admin@engebag.com.br");
                admin.setSenha(passwordEncoder.encode("123456"));
                admin.setRole("ADMIN");
                admin.setEmpresaAcesso("AMBAS");
                admin.setCargo("Coordenador de T.I.");
                admin.setUsuarioRm("admin.rm");
                admin.setUtilizaOmaxprensa(false);
                admin.setPrimeiroAcesso(false); // Já configurado
                admin.setAtivo(true);
                userRepository.save(admin);
            }

            if (userRepository.findByEmail("roger.ti@engebag.com.br").isEmpty()) {
                User tecnico = new User();
                tecnico.setNome("Técnico de Suporte (Roger)");
                tecnico.setEmail("roger.ti@engebag.com.br");
                tecnico.setSenha(passwordEncoder.encode("123456"));
                tecnico.setRole("TECNICO");
                tecnico.setEmpresaAcesso("AMBAS");
                tecnico.setCargo("Analista de Suporte");
                tecnico.setUsuarioRm("roger.rm");
                tecnico.setUtilizaOmaxprensa(false);
                tecnico.setPrimeiroAcesso(false);
                tecnico.setAtivo(true);
                userRepository.save(tecnico);
            }

            if (userRepository.findByEmail("joao.rh@engebag.com.br").isEmpty()) {
                User comumEngebag = new User();
                comumEngebag.setNome("João (Usuário Engebag)");
                comumEngebag.setEmail("joao.rh@engebag.com.br");
                comumEngebag.setSenha(passwordEncoder.encode("123456"));
                comumEngebag.setRole("USER");
                comumEngebag.setEmpresaAcesso("ENGEBAG");
                comumEngebag.setCargo("Analista de RH");
                comumEngebag.setUsuarioRm("joao.rh"); // João usa o RM
                comumEngebag.setUtilizaOmaxprensa(false);
                comumEngebag.setPrimeiroAcesso(true);
                comumEngebag.setAtivo(true);
                userRepository.save(comumEngebag);
            }

            if (userRepository.findByEmail("maria@bagcleaner.com.br").isEmpty()) {
                User comumBag = new User();
                comumBag.setNome("Maria (Usuária Bag Cleaner)");
                comumBag.setEmail("maria@bagcleaner.com.br");
                comumBag.setSenha(passwordEncoder.encode("123456"));
                comumBag.setRole("USER");
                comumBag.setEmpresaAcesso("BAG_CLEANER");
                comumBag.setCargo("Operadora de Máquina");
                comumBag.setUsuarioRm(null); // Maria não usa o RM
                comumBag.setUtilizaOmaxprensa(true); // Mas utiliza a OmaxPrensa
                comumBag.setPrimeiroAcesso(true);
                comumBag.setAtivo(true);
                userRepository.save(comumBag);
            }


        } catch (Exception e) {
            System.out.println("⚠️ O Seeder detectou dados existentes ou encontrou um conflito. Erro: " + e.getMessage());
        }
    }
}