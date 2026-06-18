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
                comumBag.setAtivo(true);
                userRepository.save(comumBag);
            }

            // ==========================================
            // 2. CRIAÇÃO DOS CHAMADOS (Apenas se a tabela estiver vazia)
            // ==========================================
            if (chamadoRepository.count() == 0) {
                User admin = userRepository.findByEmail("admin@engebag.com.br").get();
                User tecnico = userRepository.findByEmail("roger.ti@engebag.com.br").get();
                User joao = userRepository.findByEmail("joao.rh@engebag.com.br").get();
                User maria = userRepository.findByEmail("maria@bagcleaner.com.br").get();

                // Chamado 1: Engebag (Aberto por João, Atribuído ao Admin) - Terá participante
                Chamado c1 = new Chamado();
                c1.setTitulo("Sistema RM não abre"); c1.setDescricao("Erro de conexão com o banco");
                c1.setCategoria("Sistemas"); c1.setCriticidade("ALTA"); c1.setEmpresa("ENGEBAG");
                c1.setUsuarioAbriu(joao); c1.setTecnicoPrincipal(admin); c1.setStatus("EM_ANDAMENTO");
                chamadoRepository.save(c1);

                // Chamado 2: Engebag (Aberto por João, Atribuído ao Roger)
                Chamado c2 = new Chamado();
                c2.setTitulo("Troca de Mouse"); c2.setDescricao("O scroll quebrou");
                c2.setCategoria("Hardware"); c2.setCriticidade("BAIXA"); c2.setEmpresa("ENGEBAG");
                c2.setUsuarioAbriu(joao); c2.setTecnicoPrincipal(tecnico); c2.setStatus("EM_ANDAMENTO");
                chamadoRepository.save(c2);

                // Chamado 3: Engebag (Aberto por João, Atribuído ao Admin)
                Chamado c3 = new Chamado();
                c3.setTitulo("Instalação do pacote Office"); c3.setDescricao("Preciso do Excel para planilhas");
                c3.setCategoria("Software"); c3.setCriticidade("MEDIA"); c3.setEmpresa("ENGEBAG");
                c3.setUsuarioAbriu(joao); c3.setTecnicoPrincipal(admin); c3.setStatus("EM_ANDAMENTO");
                chamadoRepository.save(c3);

                // Chamado 4: Bag Cleaner (Aberto por Maria, Atribuído ao Roger) - Terá participante
                Chamado c4 = new Chamado();
                c4.setTitulo("Sem internet no setor"); c4.setDescricao("Roteador piscando vermelho");
                c4.setCategoria("Redes"); c4.setCriticidade("CRITICA"); c4.setEmpresa("BAG_CLEANER");
                c4.setUsuarioAbriu(maria); c4.setTecnicoPrincipal(tecnico); c4.setStatus("EM_ANDAMENTO");
                chamadoRepository.save(c4);

                // Chamado 5: Bag Cleaner (Aberto por ADMIN, Atribuído ao ADMIN)
                Chamado c5 = new Chamado();
                c5.setTitulo("Manutenção Preventiva Servidor"); c5.setDescricao("Atualização do Windows Server");
                c5.setCategoria("Infraestrutura"); c5.setCriticidade("MEDIA"); c5.setEmpresa("BAG_CLEANER");
                c5.setUsuarioAbriu(admin); c5.setTecnicoPrincipal(admin); c5.setStatus("EM_ANDAMENTO");
                chamadoRepository.save(c5);

                // ==========================================
                // 3. CRIAÇÃO DOS PARTICIPANTES
                // ==========================================
                
                // No Chamado 1 (Do Admin), entra o Roger como Técnico Auxiliar
                ChamadoParticipante p1 = new ChamadoParticipante();
                p1.setChamado(c1);
                p1.setUsuario(tecnico);
                p1.setPapel("TECNICO_AUXILIAR");
                participanteRepository.save(p1);

                // No Chamado 4 (Da Maria p/ Roger), entra o Admin como Solicitante Extra (acompanhando a resolução)
                ChamadoParticipante p2 = new ChamadoParticipante();
                p2.setChamado(c4);
                p2.setUsuario(admin);
                p2.setPapel("SOLICITANTE_EXTRA");
                participanteRepository.save(p2);

                System.out.println("✅ Seeder concluído: Usuários, 5 Chamados e 2 Participantes inseridos com sucesso!");
            }

        } catch (Exception e) {
            System.out.println("⚠️ O Seeder detectou dados existentes ou encontrou um conflito. Erro: " + e.getMessage());
        }
    }
}