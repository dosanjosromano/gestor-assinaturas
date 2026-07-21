package br.com.gestorAssinaturas.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private Usuario novoUsuario() {
        return new Usuario(UUID.randomUUID(), "Fulano", "fulano@teste.com");
    }

    @Test
    void novoUsuarioComecaAtivo() {
        Usuario usuario = novoUsuario();

        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(usuario.getDesativadoEm()).isNull();
    }

    @Test
    void atualizarDadosTrocaSomenteOsCamposInformados() {
        Usuario usuario = novoUsuario();

        usuario.atualizarDados("Novo Nome", null);
        assertThat(usuario.getNome()).isEqualTo("Novo Nome");
        assertThat(usuario.getEmail()).isEqualTo("fulano@teste.com");

        usuario.atualizarDados(null, "novo@teste.com");
        assertThat(usuario.getNome()).isEqualTo("Novo Nome");
        assertThat(usuario.getEmail()).isEqualTo("novo@teste.com");
    }

    @Test
    void excluirMarcaInativoComDataDeExclusao() {
        Usuario usuario = novoUsuario();

        usuario.excluir();

        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
        assertThat(usuario.getDesativadoEm()).isNotNull();
    }

    @Test
    void excluirDuasVezesEIdempotenteENaoAtualizaDataDeExclusao() {
        Usuario usuario = novoUsuario();

        usuario.excluir();
        var desativadoEmOriginal = usuario.getDesativadoEm();
        usuario.excluir();

        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
        assertThat(usuario.getDesativadoEm()).isEqualTo(desativadoEmOriginal);
    }
}
