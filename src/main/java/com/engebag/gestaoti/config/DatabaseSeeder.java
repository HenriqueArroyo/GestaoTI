package com.engebag.gestaoti.config;

import com.engebag.gestaoti.model.User;
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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
        // 1. Cria o Admin se a tabela estiver vazia (ou se o admin não existir)
        if (userRepository.findByEmail("admin@engebag.com.br").isEmpty()) {
            User admin = new User();
            admin.setNome("Administrador do Sistema");
            admin.setEmail("admin@engebag.com.br");
            admin.setSenha(passwordEncoder.encode("123456")); // Usa o gerador oficial!
            admin.setRole("ADMIN");
            admin.setEmpresaAcesso("AMBAS");
            admin.setAtivo(true);
            userRepository.save(admin);
            System.out.println("✅ Usuário Admin padrão criado no banco de dados.");
        }

        // 2. Cria o Técnico de Teste
        if (userRepository.findByEmail("roger.ti@engebag.com.br").isEmpty()) {
            User tecnico = new User();
            tecnico.setNome("Técnico de Suporte");
            tecnico.setEmail("tecnico@engebag.com.br");
            tecnico.setSenha(passwordEncoder.encode("159753"));
            tecnico.setRole("TECNICO");
            tecnico.setEmpresaAcesso("AMBAS");
            tecnico.setAtivo(true);
            userRepository.save(tecnico);
            System.out.println("✅ Usuário Técnico padrão criado no banco de dados.");
        }

        // 3. Cria o Usuário Comum de Teste
        if (userRepository.findByEmail("joao.rh@engebag.com.br").isEmpty()) {
            User comum = new User();
            comum.setNome("Usuário Comum");
            comum.setEmail("usuario@engebag.com.br");
            comum.setSenha(passwordEncoder.encode("123456"));
            comum.setRole("USER");
            comum.setEmpresaAcesso("ENGEBAG"); // Só vê chamados e ativos da Engebag
            comum.setAtivo(true);
            userRepository.save(comum);
            System.out.println("✅ Usuário Comum padrão criado no banco de dados.");
        }
        } catch (Exception e) {
            System.out.println("⚠️ O Seeder detectou dados existentes ou encontrou um conflito. Ignorando a inserção automática de usuários. Erro: " + e.getMessage());
        }
    }
}